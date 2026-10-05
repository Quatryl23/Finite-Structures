package net.mesomods.finitestructures;

import com.mojang.logging.LogUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(value = FiniteStructures.MODID, dist = Dist.DEDICATED_SERVER)
public class FiniteStructures {
    public static final String MODID = "finite_structures";
    static final Logger LOGGER = LogUtils.getLogger();

    public FiniteStructures(IEventBus modEventBus, ModContainer modContainer) {
    }
}
