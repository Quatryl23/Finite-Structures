package net.mesomods.finitestructures;

import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FiniteStructures implements ModInitializer {
    public static final String MODID = "finite_structures";
    public static final Logger LOGGER = LoggerFactory.getLogger("Finite Structures Mod");

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }

    @Override
    public void onInitialize() {
        FiniteStructureRegistries.initialize();
    }
}
