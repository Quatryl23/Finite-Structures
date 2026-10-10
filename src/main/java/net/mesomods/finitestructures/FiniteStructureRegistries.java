package net.mesomods.finitestructures;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DataPackRegistryEvent;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class FiniteStructureRegistries {
    public static final ResourceKey<Registry<StructureCountLimit>> STRUCTURE_LIMITS_REGISTRY_KEY =
            ResourceKey.createRegistryKey(new ResourceLocation("worldgen","structure_limit"));

    @SubscribeEvent
    public static void registerDatapackRegistries(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(STRUCTURE_LIMITS_REGISTRY_KEY, StructureCountLimit.CODEC);
    }
}