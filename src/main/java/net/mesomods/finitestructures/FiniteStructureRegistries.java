package net.mesomods.finitestructures;

import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.NewDatapackRegistryEvent;

@EventBusSubscriber
public class FiniteStructureRegistries {
    public static final ResourceKey<Registry<StructureCountLimit>> STRUCTURE_LIMITS_REGISTRY_KEY =
            ResourceKey.createRegistryKey(Identifier.fromNamespaceAndPath("worldgen","structure_limit"));

    @SubscribeEvent
    public static void registerDatapackRegistries(NewDatapackRegistryEvent event) {
        event.worldRegistry(STRUCTURE_LIMITS_REGISTRY_KEY, StructureCountLimit.CODEC);
    }
}