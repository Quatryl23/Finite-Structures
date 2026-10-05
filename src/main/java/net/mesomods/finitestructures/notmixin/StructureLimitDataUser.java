package net.mesomods.finitestructures.notmixin;

import net.mesomods.finitestructures.StructureLimitData;
import org.spongepowered.asm.mixin.Unique;

public interface StructureLimitDataUser {
    @Unique
    void setStructureLimitData(StructureLimitData structureLimitData);
}
