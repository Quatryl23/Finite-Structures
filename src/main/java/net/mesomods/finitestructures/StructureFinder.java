package net.mesomods.finitestructures;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureCheckResult;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import org.slf4j.Logger;

import java.util.*;

public class StructureFinder {
    public static final Logger LOGGER = FiniteStructures.LOGGER;

    public static LocatedStructurePositions findNearestStructures(Set<Holder<Structure>> structures, ServerLevel level, BlockPos center, int count) {
        Set<Holder<Structure>> matchingStructures = new HashSet<>(structures);
        matchingStructures.removeIf(structure -> !doBiomesMatch(structure.value(), level));
        if (matchingStructures.isEmpty()) return null;
        ChunkGeneratorStructureState structureState = level.getChunkSource().getGeneratorState();
        long seed = structureState.getLevelSeed();
        List<Pair<RandomSpreadStructurePlacement, Holder<Structure>>> placements = new ArrayList<>();
        for (Holder<Structure> structure : matchingStructures) {
            structureState.getPlacementsForStructure(structure).stream().filter(p -> p instanceof RandomSpreadStructurePlacement).map(p -> (RandomSpreadStructurePlacement) p).forEach(p -> {
                placements.add(Pair.of(p, structure));
            });
        }
        if (placements.isEmpty()) return null;
        long time = System.currentTimeMillis();
        LOGGER.debug("Searching for {} structures of type {} in {}", count, structures, level.dimension());
        LocatedStructurePositions locatedPositions = new LocatedStructurePositions(count, matchingStructures);
        if (count == 0) return locatedPositions;
        int centerX = SectionPos.blockToSectionCoord(center.getX());
        int centerZ = SectionPos.blockToSectionCoord(center.getZ());
        for (int i = 0; i < 1_000; i++) {
            List<Pair<ChunkPos, Holder<Structure>>> pendingPositions = new ArrayList<>();
            for (Pair<RandomSpreadStructurePlacement, Holder<Structure>> pair : placements) {
                RandomSpreadStructurePlacement placement = pair.getFirst();
                int spacing = placement.spacing();
                if (i % placement.spacing() != 0) continue;
                int x = -i;
                for (int z = -i; z <= i; z += spacing) {
                    Pair<ChunkPos, Holder<Structure>> result = checkChunk(pair.getSecond(), level, level.structureManager(), placement, placement.getPotentialStructureChunk(seed, centerX + x, centerZ + z));
                    if (result != null) pendingPositions.add(result);
                }
                if (i == 0) continue;
                x = i;
                for (int z = -i; z <= i; z += spacing) {
                    Pair<ChunkPos, Holder<Structure>> result = checkChunk(pair.getSecond(), level, level.structureManager(), placement, placement.getPotentialStructureChunk(seed, centerX + x, centerZ + z));
                    if (result != null) pendingPositions.add(result);
                }
                int z = -i;
                for (x = -i + spacing; x <= i - spacing; x += spacing) {
                    Pair<ChunkPos, Holder<Structure>> result = checkChunk(pair.getSecond(), level, level.structureManager(), placement, placement.getPotentialStructureChunk(seed, centerX + x, centerZ + z));
                    if (result != null) pendingPositions.add(result);
                }
                z = i;
                for (x = -i + spacing; x <= i - spacing; x += spacing) {
                    Pair<ChunkPos, Holder<Structure>> result = checkChunk(pair.getSecond(), level, level.structureManager(), placement, placement.getPotentialStructureChunk(seed, centerX + x, centerZ + z));
                    if (result != null) pendingPositions.add(result);
                }
            }
            if (pendingPositions.isEmpty()) continue;
            if (locatedPositions.doElementsFit(pendingPositions.size())) {
                boolean isFull = false;
                for (Pair<ChunkPos, Holder<Structure>> pendingPosition : pendingPositions) {
                    isFull = locatedPositions.addPosition(pendingPosition.getFirst(), pendingPosition.getSecond());
                }
                if (isFull) return locatedPositions;
            } else {
                pendingPositions.sort(Comparator.comparingDouble(pending -> center.distSqr(pending.getFirst().getMiddleBlockPosition(0))));
                for (Pair<ChunkPos, Holder<Structure>> pending : pendingPositions) {
                    if (locatedPositions.addPosition(pending.getFirst(), pending.getSecond())) return locatedPositions;
                }
            }
        }
        LOGGER.debug("Locating {} structures of type {} in {} took {} ms", count, structures, level.dimension(), System.currentTimeMillis() - time);
        return locatedPositions;
    }

    private static Pair<ChunkPos, Holder<Structure>> checkChunk(Holder<Structure> structure, LevelReader level, StructureManager manager, StructurePlacement placement, ChunkPos pos) {
        StructureCheckResult checkResult = manager.checkStructurePresence(pos, structure.value(), placement, false);
        if (checkResult == StructureCheckResult.CHUNK_LOAD_NEEDED) {
            ChunkAccess chunkAccess = level.getChunk(pos.x(), pos.z(), ChunkStatus.STRUCTURE_STARTS);
            StructureStart structureStart = manager.getStartForStructure(structure.value(), chunkAccess);
            if (structureStart != null && structureStart.isValid()) {
                return Pair.of(structureStart.getChunkPos(), structure);
            }
        }
        return null;
    }

    static boolean doBiomesMatch(Structure structure, ServerLevel level) {
        HolderSet<Biome> structureBiomes = structure.biomes();
        Set<Holder<Biome>> levelBiomes = level.getChunkSource().getGenerator().getBiomeSource().possibleBiomes();
        for (Holder<Biome> holder : structureBiomes) {
            if (levelBiomes.contains(holder)) return true;
        }
        return false;
    }
}
