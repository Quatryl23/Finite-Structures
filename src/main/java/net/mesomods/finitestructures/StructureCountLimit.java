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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class StructureCountLimit {
    public static final Codec<StructureCountLimit> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            RegistryCodecs.homogeneousList(Registries.STRUCTURE).fieldOf("target").forGetter(StructureCountLimit::getStructure),
            Codec.INT.optionalFieldOf("priority", 0).forGetter(StructureCountLimit::getPriority),
            GroupMode.CODEC.optionalFieldOf("group_mode", GroupMode.SEPARATE).forGetter(StructureCountLimit::getGroupMode),
            GroupRule.MemberOverrideReaction.CODEC.optionalFieldOf("override_mode", GroupRule.MemberOverrideReaction.REDUCE_COUNT).forGetter(StructureCountLimit::getOverrideReaction),
            LimitMode.CODEC.optionalFieldOf("limit_mode", LimitMode.NEAREST).forGetter(StructureCountLimit::getLimitMode),
            Codec.INT.fieldOf("count").forGetter(StructureCountLimit::getCount),
            CenterPos.CODEC.optionalFieldOf("center", CenterPos.DEFAULT).forGetter(StructureCountLimit::getCenter)
    ).apply(instance, StructureCountLimit::new));
    private final HolderSet<Structure> structure;
    private final int priority;
    private final GroupMode groupMode;
    private final GroupRule.MemberOverrideReaction memberOverrideReaction;
    private final Rule rule;

    public StructureCountLimit(HolderSet<Structure> structure, int priority, GroupMode groupMode, GroupRule.MemberOverrideReaction memberOverrideReaction, LimitMode limitMode, int count, CenterPos center) {
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

    public int getCount() {
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
        protected final int count;
        protected final CenterPos center;
        protected final LimitMode limitMode;

        public Rule(int count, CenterPos center, LimitMode limitMode) {
            this.count = count;
            this.center = center;
            this.limitMode = limitMode;
        }

        public boolean isLocated() {
            return limitMode == LimitMode.NEAREST;
        }

        public int resolveCount(ServerLevel level) {
            return Mth.clamp(count, 0, 1024);
        }

        public abstract boolean isGroup();

        public abstract Set<Holder<Structure>> getStructures();

        public abstract void onOverride(Holder<Structure> structure);

        public abstract Map<Holder<Structure>, Set<ChunkPos>> apply(ServerLevel level);
    }

    public static class SingleRule extends Rule {
        private final HolderSet<Structure> holderSet;
        private Set<Holder<Structure>> structures;

        public SingleRule(int count, CenterPos center, LimitMode limitMode, HolderSet<Structure> structures) {
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
            for (Holder<Structure> structure : this.getStructures()) {
                BlockPos pos = center.getCenter(level);
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
        private final MemberOverrideReaction reaction;
        private Set<Holder<Structure>> group;
        private int originalSize;

        public GroupRule(int count, CenterPos center, LimitMode limitMode, HolderSet<Structure> holderSet, MemberOverrideReaction reaction) {
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
            double multiplier = (reaction == MemberOverrideReaction.REDUCE_COUNT) ? (double) group.size() / (double) originalSize : 1.0;
            return (int) Mth.clamp(Math.round(count * multiplier), 0, 1024);
        }

        @Override
        public Map<Holder<Structure>, Set<ChunkPos>> apply(ServerLevel level) {
            BlockPos pos = center.getCenter(level);
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
                Codec.INT.optionalFieldOf("offset_x", 0).forGetter(CenterPos::getX),
                Codec.INT.optionalFieldOf("offset_z", 0).forGetter(CenterPos::getZ)
        ).apply(instance, CenterPos::new));
        public static final CenterPos DEFAULT = new CenterPos(CenterSource.SPAWN, 0, 0);
        private final CenterSource source;
        private final int x;
        private final int z;

        CenterPos(CenterSource source, int x, int z) {
            this.source = source;
            this.x = x;
            this.z = z;
        }

        public BlockPos getCenter(ServerLevel level) {
            BlockPos pos = source == CenterSource.SPAWN ? level.getSharedSpawnPos() : BlockPos.ZERO;
            double multiplier = 1.0 / level.dimensionType().coordinateScale();
            return BlockPos.containing(offset(pos).getCenter().multiply(multiplier, 1, multiplier));
        }

        private BlockPos offset(BlockPos pos) {
            return pos.offset(x, 0, z);
        }

        public int getZ() {
            return z;
        }

        public int getX() {
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