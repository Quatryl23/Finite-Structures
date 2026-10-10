package net.mesomods.finitestructures;

import net.fabricmc.fabric.api.event.registry.DynamicRegistries;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;

public class FiniteStructureRegistries {
    public static final ResourceKey<Registry<StructureCountLimit>> STRUCTURE_LIMITS_REGISTRY_KEY =
            ResourceKey.createRegistryKey(new ResourceLocation("worldgen", "structure_limit"));

    public static void initialize() {
        DynamicRegistries.register(STRUCTURE_LIMITS_REGISTRY_KEY, StructureCountLimit.CODEC);
    }
}