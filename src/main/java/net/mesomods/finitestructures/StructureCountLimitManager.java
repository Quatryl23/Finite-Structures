package net.mesomods.finitestructures;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.*;

public class StructureCountLimitManager {
    List<StructureCountLimit.Rule> rules = new ArrayList<>();

    public StructureCountLimitManager(RegistryAccess access) {
        Map<Holder<Structure>, StructureCountLimit.Rule> structureLimits = new HashMap<>();
        Optional<Registry<StructureCountLimit>> registry = access.lookup(FiniteStructureRegistries.STRUCTURE_LIMITS_REGISTRY_KEY);
        registry.ifPresent(reg -> {
            List<StructureCountLimit> sorted = reg.stream().sorted(
                    Comparator.comparingInt(StructureCountLimit::getPriority)
            ).toList();
            for (StructureCountLimit limit : sorted) {
                HolderSet<Structure> targets = limit.getStructure();
                StructureCountLimit.Rule rule = limit.getRule();
                if (rule == null) {
                    ResourceLocation id = reg.getKey(limit);
                    FiniteStructures.LOGGER.warn("Structure Limit {} failed to load, likely due to invalid structure references", id);
                    continue;
                }
                for (Holder<Structure> target : targets) {
                    StructureCountLimit.Rule overridden = structureLimits.put(target, rule);
                    if (overridden != null) overridden.onOverride(target);
                }
            }
        });
        Set<Holder<Structure>> ruledStructures = new HashSet<>();
        for (Map.Entry<Holder<Structure>, StructureCountLimit.Rule> entry : structureLimits.entrySet()) {
            if (ruledStructures.contains(entry.getKey())) continue;
            StructureCountLimit.Rule rule = entry.getValue();
            Set<Holder<Structure>> structures = rule.getStructures();
            ruledStructures.addAll(structures);
            rules.add(rule);
        }
    }
}
