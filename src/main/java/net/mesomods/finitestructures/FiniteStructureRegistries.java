package net.mesomods.finitestructures;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;

@EventBusSubscriber
public class FiniteStructureRegistries {
    public static final ResourceKey<Registry<StructureCountLimit>> STRUCTURE_LIMITS_REGISTRY_KEY =
            ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath("worldgen","structure_limit"));

    @SubscribeEvent
    public static void registerDatapackRegistries(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(STRUCTURE_LIMITS_REGISTRY_KEY, StructureCountLimit.CODEC);
    }
}