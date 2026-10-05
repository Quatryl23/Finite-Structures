package net.mesomods.finitestructures;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(FiniteStructures.MODID)
public class FiniteStructures {
    public static final String MODID = "finite_structures";
    static final Logger LOGGER = LogUtils.getLogger();

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }

    public FiniteStructures(IEventBus modEventBus, ModContainer modContainer) {
    }
}
