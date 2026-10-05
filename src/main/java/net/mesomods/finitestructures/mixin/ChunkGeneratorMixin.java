package net.mesomods.finitestructures.mixin;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.datafixers.util.Pair;
import net.mesomods.finitestructures.StructureLimitData;
import net.mesomods.finitestructures.notmixin.StructureLimitDataUser;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Mixin(ChunkGenerator.class)
public abstract class ChunkGeneratorMixin implements StructureLimitDataUser {
    @Unique
    private StructureLimitData finiteStructures$structureLimitData;

    @Unique
    private RandomSpreadStructurePlacement finiteStructures$successfulPlacement = null;

    @Unique
    @Override
    public void finiteStructures$setStructureLimitData(StructureLimitData structureLimitData) {
        this.finiteStructures$structureLimitData = structureLimitData;
    }

    @Inject(method = "findNearestMapStructure", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/SectionPos;blockToSectionCoord(I)I", ordinal = 0), cancellable = true)
    public void finiteStructures$findNearestMapStructureFaster(ServerLevel serverLevel, HolderSet<Structure> holderSet, BlockPos blockPos, int i, boolean bl, CallbackInfoReturnable<Pair<BlockPos, Holder<Structure>>> cir, @Local List<Map.Entry<StructurePlacement, Set<Holder<Structure>>>> list) {
        for (Holder<Structure> holder : holderSet) {
            if (!finiteStructures$structureLimitData.isLimited(holder)) return;

        }
        double minDistance = Double.MAX_VALUE;
        Pair<BlockPos, Holder<Structure>> find = null;
        for (Map.Entry<StructurePlacement, Set<Holder<Structure>>> entry : list) {
            for (Holder<Structure> structureHolder : entry.getValue()) {
                Pair<BlockPos, Double> nearest = this.finiteStructures$structureLimitData.getNearestStructure(structureHolder, blockPos, entry.getKey());
                if (nearest == null) continue;
                if (nearest.getSecond() < minDistance) {
                    minDistance = nearest.getSecond();
                    find = Pair.of(nearest.getFirst(), structureHolder);
                }
            }
        }
        cir.setReturnValue(find);
    }

    @Definition(id = "pair2", local = @Local(type = Pair.class, ordinal = 0))
    @Definition(id = "pair1", local = @Local(type = Pair.class, index = 20))
    @Expression("pair2 = pair1")
    @Inject(method = "findNearestMapStructure", at = @At(value = "MIXINEXTRAS:EXPRESSION"))
    public void finiteStructures$storeSuccessfulPlacement(ServerLevel serverLevel, HolderSet<Structure> holderSet, BlockPos blockPos, int i, boolean bl, CallbackInfoReturnable<Pair<BlockPos, Holder<Structure>>> cir, @Local RandomSpreadStructurePlacement placement) {
        this.finiteStructures$successfulPlacement = placement;
    }

    @Inject(method = "findNearestMapStructure", at = @At(value = "RETURN", ordinal = 2), cancellable = true)
    public void finiteStructures$saveFoundMapStructure(ServerLevel serverLevel, HolderSet<Structure> holderSet, BlockPos blockPos, int i, boolean bl, CallbackInfoReturnable<Pair<BlockPos, Holder<Structure>>> cir) {
        if (this.finiteStructures$structureLimitData != null && this.finiteStructures$successfulPlacement != null) {
            Vec3i locateOffset = ((StructurePlacementAccessor) finiteStructures$successfulPlacement).getLocateOffset();
            Vec3i invertedOffset = locateOffset.multiply(-1);
            ChunkPos chunkPos = new ChunkPos(cir.getReturnValue().getFirst().offset(invertedOffset));
            if (!finiteStructures$structureLimitData.allowStructureAtPosition(cir.getReturnValue().getSecond(), chunkPos, true)) {
                cir.setReturnValue(null);
            }
        }
    }

    @Inject(method = "tryGenerateStructure", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/StructureManager;setStartForStructure(Lnet/minecraft/core/SectionPos;Lnet/minecraft/world/level/levelgen/structure/Structure;Lnet/minecraft/world/level/levelgen/structure/StructureStart;Lnet/minecraft/world/level/chunk/StructureAccess;)V"), cancellable = true)
    public void finiteStructures$nTryGenerateStructure(StructureSet.StructureSelectionEntry structureSelectionEntry, StructureManager structureManager, RegistryAccess registryAccess, RandomState randomState, StructureTemplateManager structureTemplateManager, long l, ChunkAccess chunkAccess, ChunkPos chunkPos, SectionPos sectionPos, ResourceKey<Level> resourceKey, CallbackInfoReturnable<Boolean> cir, @Local Structure structure) {
        if (this.finiteStructures$structureLimitData != null) {
            Registry<Structure> registry = registryAccess.lookupOrThrow(Registries.STRUCTURE);
            if (!finiteStructures$structureLimitData.allowStructureAtPosition(registry.wrapAsHolder(structure), chunkPos, true)) {
                cir.setReturnValue(false);
            }
        }
    }
}
