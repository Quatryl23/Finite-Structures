package net.mesomods.finitestructures.notmixin;

import net.mesomods.finitestructures.StructureCountLimitManager;
import org.spongepowered.asm.mixin.Unique;

public interface MinecraftServerMixin {
    @Unique
    StructureCountLimitManager getStructureCountLimitManager();
}
