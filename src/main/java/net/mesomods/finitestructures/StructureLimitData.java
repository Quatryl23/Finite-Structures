package net.mesomods.finitestructures;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class StructureLimitData extends SavedData {
    public static final Logger LOGGER = FiniteStructures.LOGGER;
    public static final Codec<StructureLimitData> CODEC = RecordCodecBuilder.create((instance) -> instance.group(
            Codec.BOOL.fieldOf("initialized").forGetter(StructureLimitData::isInitialized),
            Codec.unboundedMap(Structure.CODEC, Positions.CODEC).fieldOf("positions").forGetter(StructureLimitData::getStructurePositions),
            Codec.list(Codec.list(Structure.CODEC).xmap(list -> (Set<Holder<Structure>>) list.stream().collect( Collectors.toCollection(ConcurrentHashMap::newKeySet)), set -> set.stream().toList())).fieldOf("synchronized").forGetter(StructureLimitData::getSynchronizedSets)
    ).apply(instance, StructureLimitData::new));
    public static final SavedDataType<StructureLimitData> TYPE = new SavedDataType<>(
            "structure_limits",
            () -> new StructureLimitData(false, new HashMap<>(), new ArrayList<>()),
            CODEC,
            null
    );
    private final boolean initialized;
    private final Map<Holder<Structure>, Positions> structurePositions;
    private final List<Set<Holder<Structure>>> synchronizedSets;

    StructureLimitData(boolean initialized, Map<Holder<Structure>, Positions> structurePositions, List<Set<Holder<Structure>>> synchronizedSets) {
        this.initialized = initialized;
        this.structurePositions = structurePositions;
        this.synchronizedSets = new CopyOnWriteArrayList<>(synchronizedSets);
    }

    public static StructureLimitData create(ServerLevel level, StructureCountLimitManager manager) {
        Map<Holder<Structure>, Positions> positions = new ConcurrentHashMap<>();
        List<Set<Holder<Structure>>> synchronizedSets = new ArrayList<>();
        LOGGER.info("Preparing structure limits for {}", level.dimension().location());
        manager.rules.forEach((rule) -> {
            if (rule.isLocated()) {
                Map<Holder<Structure>, Set<ChunkPos>> located = rule.apply(level);
                located.forEach((structure, pos) -> {
                    positions.put(structure, new Positions(pos, 0));
                });
            } else {
                Set<Holder<Structure>> structures = rule.getStructures();
                int count = rule.resolveCount(level);
                structures.removeIf(structure -> !StructureFinder.doBiomesMatch(structure.value(), level));
                for (Holder<Structure> structure : structures) {
                    positions.put(structure, new Positions(new HashSet<>(), count));
                }
                if (rule.isGroup() && structures.size() > 1) {
                    Set<Holder<Structure>> set = ConcurrentHashMap.newKeySet();
                    set.addAll(structures);
                    synchronizedSets.add(set);
                }
            }
        });
        return new StructureLimitData(true, positions, synchronizedSets);
    }

    public boolean isInitialized() {
        return initialized;
    }

    private Map<Holder<Structure>, Positions> getStructurePositions() {
        return this.structurePositions;
    }

    private List<Set<Holder<Structure>>> getSynchronizedSets() {
        return synchronizedSets;
    }

    public boolean allowStructureAtPosition(Holder<Structure> structure, ChunkPos chunkPos, boolean saveLocation) {
        if (structurePositions.containsKey(structure)) {
            return structurePositions.get(structure).allowsStructureAtPosition(chunkPos, (remaining) -> synchronizeRemaining(structure, remaining), saveLocation);
        }
        return true;
    }

    public boolean isLimited(Holder<Structure> structure) {
        return structurePositions.containsKey(structure) && this.structurePositions.get(structure).isComplete();
    }

    public Pair<BlockPos, Double> getNearestStructure(Holder<Structure> structure, BlockPos pos, StructurePlacement placement) {
        if (!structurePositions.containsKey(structure)) return null;
        Positions positions = structurePositions.get(structure);
        if (!positions.isComplete()) return null;
        double currentMin = Double.MAX_VALUE;
        BlockPos currentPos = null;
        for (ChunkPos chunkPos : positions.getPositions()) {
            BlockPos blockPos = placement.getLocatePos(chunkPos);
            double distance = pos.distSqr(blockPos);
            if (distance < currentMin) {
                currentMin = distance;
                currentPos = blockPos;
            }
        }
        return Pair.of(currentPos, currentMin);
    }

    public void synchronizeRemaining(Holder<Structure> structure, int remaining) {
        for (Set<Holder<Structure>> set : synchronizedSets) {
            if (set.contains(structure)) {
                for (Holder<Structure> s : set) {
                    structurePositions.get(s).remaining.set(remaining);
                }
            }
            if (remaining == 0) synchronizedSets.remove(set);
        }
        this.setDirty();
    }

    public static class Positions {
        public static final Codec<Positions> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.list(ChunkPos.CODEC).xmap(list -> (Set<ChunkPos>) list.stream().collect(Collectors.toCollection(ConcurrentHashMap::newKeySet)), set -> set.stream().toList()).fieldOf("positions").forGetter(Positions::getPositions),
                Codec.INT.optionalFieldOf("remaining", 0).forGetter(Positions::getRemaining)
        ).apply(instance, Positions::new));
        private final Set<ChunkPos> positions;
        private AtomicInteger remaining;

        public Positions(Set<ChunkPos> positions, int remaining) {
            this.positions = ConcurrentHashMap.newKeySet();
            this.positions.addAll(positions);
            this.remaining = new AtomicInteger(remaining);
        }

        public boolean allowsStructureAtPosition(ChunkPos pos, Consumer<Integer> synchronizeRemaining, boolean saveLocation) {
            if (positions.contains(pos)) return true;
            if (!isComplete()) {
                if (saveLocation) {
                    positions.add(pos);
                    remaining.decrementAndGet();
                    synchronizeRemaining.accept(remaining.get());
                }
                return true;
            }
            return false;
        }

        public boolean isComplete() {
            return remaining.get() == 0;
        }

        public Set<ChunkPos> getPositions() {
            return positions;
        }

        public int getRemaining() {
            return remaining.get();
        }
    }
}
