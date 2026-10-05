package net.mesomods.finitestructures;

import net.minecraft.core.Holder;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.*;

public class LocatedStructurePositions {
    Map<Holder<Structure>, Set<ChunkPos>> positions;
    int maxCount;
    int currentCount;

    public LocatedStructurePositions(int count, Set<Holder<Structure>> structures) {
        this.currentCount = 0;
        this.maxCount = count;
        this.positions = new HashMap<>();
        for (Holder<Structure> structure : structures) {
            this.positions.put(structure, new HashSet<>());
        }
    }

    public boolean addPosition(ChunkPos pos, Holder<Structure> structure) {
        positions.get(structure).add(pos);
        currentCount++;
        return currentCount == maxCount;
    }

    public Map<Holder<Structure>,Set<ChunkPos>> getPositions() {
        return positions;
    }

    public boolean doElementsFit(int count) {
        return currentCount + count <= maxCount;
    }
}
