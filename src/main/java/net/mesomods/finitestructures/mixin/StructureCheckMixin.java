package net.mesomods.finitestructures.mixin;

import net.mesomods.finitestructures.StructureLimitData;
import net.mesomods.finitestructures.notmixin.StructureLimitDataUser;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureCheck;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(StructureCheck.class)
public abstract class StructureCheckMixin implements StructureLimitDataUser {
    @Shadow
    @Final
    private RegistryAccess registryAccess;
    @Unique
    private StructureLimitData finiteStructures$structureLimitData;

    @Unique
    @Override
    public void finiteStructures$setStructureLimitData(StructureLimitData structureLimitData) {
        this.finiteStructures$structureLimitData = structureLimitData;
    }

    @Inject(method = "canCreateStructure", at = @At("RETURN"), cancellable = true)
    private void finiteStructures$canCreateStructure(ChunkPos chunkPos, Structure structure, CallbackInfoReturnable<Boolean> cir) {
        if (this.finiteStructures$structureLimitData != null) {
            Registry<Structure> registry = this.registryAccess.registryOrThrow(Registries.STRUCTURE);
            if (cir.getReturnValue() & !finiteStructures$structureLimitData.allowStructureAtPosition(registry.wrapAsHolder(structure), chunkPos, false)) {
                cir.setReturnValue(false);
            }
        }
    }
}
