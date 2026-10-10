package net.mesomods.finitestructures;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FiniteStructures implements ModInitializer {
    public static final String MODID = "finite_structures";
    public static final Logger LOGGER = LoggerFactory.getLogger("Finite Structures Mod");

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MODID, path);
    }

    @Override
    public void onInitialize() {
        FiniteStructureRegistries.initialize();
        ServerLifecycleEvents.SERVER_STARTING.register(server -> StructureLimitData.setLookupProvider(server.registryAccess()));
    }
}
