package net.mesomods.finitestructures;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSet;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.NumberProviders;

import java.util.*;
import java.util.stream.Collectors;

public class StructureCountLimit {
    public static final Codec<StructureCountLimit> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            RegistryCodecs.homogeneousList(Registries.STRUCTURE).fieldOf("target").forGetter(StructureCountLimit::getStructure),
            Codec.INT.optionalFieldOf("priority", 0).forGetter(StructureCountLimit::getPriority),
            GroupMode.CODEC.optionalFieldOf("group_mode", GroupMode.SEPARATE).forGetter(StructureCountLimit::getGroupMode),
            GroupRule.MemberOverrideReaction.CODEC.optionalFieldOf("override_mode", GroupRule.MemberOverrideReaction.REDUCE_COUNT).forGetter(StructureCountLimit::getOverrideReaction),
            LimitMode.CODEC.optionalFieldOf("limit_mode", LimitMode.NEAREST).forGetter(StructureCountLimit::getLimitMode),
            NumberProviders.CODEC.fieldOf("count").forGetter(StructureCountLimit::getCount),
            CenterPos.CODEC.optionalFieldOf("center", CenterPos.DEFAULT).forGetter(StructureCountLimit::getCenter)
    ).apply(instance, StructureCountLimit::new));
    private final HolderSet<Structure> structure;
    private final int priority;
    private final GroupMode groupMode;
    private final GroupRule.MemberOverrideReaction memberOverrideReaction;
    private final Rule rule;

    public StructureCountLimit(HolderSet<Structure> structure, int priority, GroupMode groupMode, GroupRule.MemberOverrideReaction memberOverrideReaction, LimitMode limitMode, NumberProvider count, CenterPos center) {
        this.structure = structure;
        this.priority = priority;
        this.groupMode = groupMode;
        this.memberOverrideReaction = memberOverrideReaction;
        this.rule = groupMode == GroupMode.GROUP ? new GroupRule(count, center, limitMode, structure, memberOverrideReaction) : new SingleRule(count, center, limitMode, structure);
    }

    public Rule getRule() {
        return rule;
    }

    public HolderSet<Structure> getStructure() {
        return structure;
    }

    public NumberProvider getCount() {
        return rule.count;
    }

    public LimitMode getLimitMode() {
        return rule.limitMode;
    }

    public GroupMode getGroupMode() {
        return groupMode;
    }

    public GroupRule.MemberOverrideReaction getOverrideReaction() {
        return memberOverrideReaction;
    }

    public int getPriority() {
        return priority;
    }

    public CenterPos getCenter() {
        return rule.center;
    }

    public enum GroupMode implements StringRepresentable {
        GROUP("group"),
        SEPARATE("separate");

        static final StringRepresentable.EnumCodec<GroupMode> CODEC = StringRepresentable.fromEnum(GroupMode::values);
        private final String string;

        GroupMode(String string) {
            this.string = string;
        }

        @Override
        public String getSerializedName() {
            return this.string;
        }
    }

    public enum LimitMode implements StringRepresentable {
        NEAREST("nearest"),
        FOUND_FIRST("found_first");

        static final StringRepresentable.EnumCodec<LimitMode> CODEC = StringRepresentable.fromEnum(LimitMode::values);
        private final String string;

        LimitMode(String string) {
            this.string = string;
        }

        @Override
        public String getSerializedName() {
            return this.string;
        }
    }

    public abstract static class Rule {
        protected final NumberProvider count;
        protected final CenterPos center;
        protected final LimitMode limitMode;

        public Rule(NumberProvider count, CenterPos center, LimitMode limitMode) {
            this.count = count;
            this.center = center;
            this.limitMode = limitMode;
        }

        public static LootContext createContext(ServerLevel level) {
            return new LootContext.Builder(new LootParams.Builder(level).create(new LootContextParamSet.Builder().build())).create(Optional.empty());
        }

        public boolean isLocated() {
            return limitMode == LimitMode.NEAREST;
        }

        public int resolveCount(ServerLevel level) {
            LootContext context = createContext(level);
            return Mth.clamp(count.getInt(context), 0, 1024);
        }

        public abstract boolean isGroup();

        public abstract Set<Holder<Structure>> getStructures();

        public abstract void onOverride(Holder<Structure> structure);

        public abstract Map<Holder<Structure>, Set<ChunkPos>> apply(ServerLevel level);
    }

    public static class SingleRule extends Rule {
        private final HolderSet<Structure> holderSet;
        private Set<Holder<Structure>> structures;

        public SingleRule(NumberProvider count, CenterPos center, LimitMode limitMode, HolderSet<Structure> structures) {
            super(count, center, limitMode);
            this.holderSet = structures;
        }

        @Override
        public Set<Holder<Structure>> getStructures() {
            if (structures == null) {
                structures = holderSet.stream().collect(Collectors.toCollection(HashSet::new));
            }
            return structures;
        }

        public void onOverride(Holder<Structure> structure) {
            this.getStructures().remove(structure);
        }

        public Map<Holder<Structure>, Set<ChunkPos>> apply(ServerLevel level) {
            Map<Holder<Structure>, Set<ChunkPos>> positions = new HashMap<>();
            LootContext context = createContext(level);
            for (Holder<Structure> structure : this.getStructures()) {
                BlockPos pos = center.getCenter(level, context);
                LocatedStructurePositions found = StructureFinder.findNearestStructures(Set.of(structure), level, pos, resolveCount(level));
                if (found != null) positions.putAll(found.getPositions());
            }
            return positions;
        }

        @Override
        public boolean isGroup() {
            return false;
        }
    }

    public static class GroupRule extends Rule {
        private final HolderSet<Structure> holderSet;
        private Set<Holder<Structure>> group;
        private int originalSize;
        private final MemberOverrideReaction reaction;

        public GroupRule(NumberProvider count, CenterPos center, LimitMode limitMode, HolderSet<Structure> holderSet, MemberOverrideReaction reaction) {
            super(count, center, limitMode);
            this.holderSet = holderSet;
            this.reaction = reaction;
        }

        @Override
        public Set<Holder<Structure>> getStructures() {
            if (group == null) {
                group = holderSet.stream().collect(Collectors.toCollection(HashSet::new));
                this.originalSize = group.size();
            }
            return group;
        }

        @Override
        public void onOverride(Holder<Structure> structure) {
            if (reaction == MemberOverrideReaction.REMOVE) {
                group.removeIf((holder) -> true);
            } else {
                group.remove(structure);
            }
        }

        @Override
        public int resolveCount(ServerLevel level) {
            LootContext context = createContext(level);
            double multiplier = (reaction == MemberOverrideReaction.REDUCE_COUNT) ? (double) group.size() / (double) originalSize : 1.0;
            return (int) Mth.clamp(Math.round(count.getInt(context) * multiplier), 0, 1024);
        }

        @Override
        public Map<Holder<Structure>, Set<ChunkPos>> apply(ServerLevel level) {
            LootContext context = createContext(level);
            BlockPos pos = center.getCenter(level, context);
            LocatedStructurePositions found = StructureFinder.findNearestStructures(group, level, pos, resolveCount(level));
            return found == null ? Map.of() : found.getPositions();
        }

        @Override
        public boolean isGroup() {
            return true;
        }

        public enum MemberOverrideReaction implements StringRepresentable {
            REMOVE("remove_group"),
            IGNORE("ignore"),
            REDUCE_COUNT("reduce_count");

            static final StringRepresentable.EnumCodec<MemberOverrideReaction> CODEC = StringRepresentable.fromEnum(MemberOverrideReaction::values);
            private final String string;

            MemberOverrideReaction(String string) {
                this.string = string;
            }

            @Override
            public String getSerializedName() {
                return this.string;
            }
        }
    }

    public static class CenterPos {
        public static final Codec<CenterPos> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                CenterSource.CODEC.fieldOf("source").forGetter(CenterPos::getSource),
                NumberProviders.CODEC.optionalFieldOf("offset_x").forGetter(CenterPos::getX),
                NumberProviders.CODEC.optionalFieldOf("offset_z").forGetter(CenterPos::getZ)
        ).apply(instance, CenterPos::new));
        public static final CenterPos DEFAULT = new CenterPos(CenterSource.SPAWN, Optional.empty(), Optional.empty());
        private final CenterSource source;
        private final Optional<NumberProvider> x;
        private final Optional<NumberProvider> z;
        private Integer resolved_x;
        private Integer resolved_z;

        CenterPos(CenterSource source, Optional<NumberProvider> x, Optional<NumberProvider> z) {
            this.source = source;
            this.x = x;
            this.z = z;
        }

        public BlockPos getCenter(ServerLevel level, LootContext context) {
            BlockPos pos = source == CenterSource.SPAWN ? level.getSharedSpawnPos() : BlockPos.ZERO;
            double multiplier = 1.0 / level.dimensionType().coordinateScale();
            return BlockPos.containing(offset(pos, context).getCenter().multiply(multiplier, 1, multiplier));
        }

        private BlockPos offset(BlockPos pos, LootContext context) {
            if (resolved_x == null) {
                resolved_x = x.orElse(ConstantValue.exactly(0)).getInt(context);
            }
            if (resolved_z == null) {
                resolved_z = z.orElse(ConstantValue.exactly(0)).getInt(context);
            }
            return pos.offset(resolved_x, 0, resolved_z);
        }

        public Optional<NumberProvider> getZ() {
            return z;
        }

        public Optional<NumberProvider> getX() {
            return x;
        }

        public CenterSource getSource() {
            return source;
        }

        public enum CenterSource implements StringRepresentable {
            ZERO("fixed"),
            SPAWN("spawn");

            static final StringRepresentable.EnumCodec<CenterSource> CODEC = StringRepresentable.fromEnum(CenterSource::values);
            private final String string;

            CenterSource(String string) {
                this.string = string;
            }

            @Override
            public String getSerializedName() {
                return string;
            }
        }
    }
}