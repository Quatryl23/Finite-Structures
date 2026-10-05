package net.mesomods.finitestructures.notmixin;

import net.mesomods.finitestructures.StructureCountLimitManager;

public interface MinecraftServerMixin {
    StructureCountLimitManager finiteStructures$getStructureCountLimitManager();
}
