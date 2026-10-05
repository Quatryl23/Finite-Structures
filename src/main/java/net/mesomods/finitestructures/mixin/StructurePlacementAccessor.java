package net.mesomods.finitestructures.mixin;

import net.minecraft.core.Vec3i;
import org.spongepowered.asm.mixin.gen.Accessor;

@org.spongepowered.asm.mixin.Mixin(net.minecraft.world.level.levelgen.structure.placement.StructurePlacement.class)
public interface StructurePlacementAccessor {
    @Accessor
    Vec3i getLocateOffset();
}
