package dev.s3tupw1zard.gearMastery.config;

import dev.s3tupw1zard.gearMastery.stat.StatRule;
import dev.s3tupw1zard.gearMastery.stat.StatType;
import org.bukkit.Material;

import java.util.Map;
import java.util.Set;

/** Fully resolved profile after inheritance has been applied. */
public record ItemProfile(String id, Set<Material> materials, String curveId, Set<String> experienceSources,
                          Map<StatType, StatRule> statRules) {
    public ItemProfile {
        materials = Set.copyOf(materials);
        experienceSources = Set.copyOf(experienceSources);
        statRules = Map.copyOf(statRules);
    }
    public boolean supportsSource(final String sourceId) { return experienceSources.contains(sourceId); }
}
