package dev.s3tupw1zard.gearMastery.config;

import dev.s3tupw1zard.gearMastery.stat.StatScaleMode;
import dev.s3tupw1zard.gearMastery.stat.StatType;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigurationServiceTest {
    @Test void parsesGlobalStatDefaultsWithRuntimeScaleFields() {
        final YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("defaults.DURABILITY.enabled", true);
        yaml.set("defaults.DURABILITY.mode", "MULTIPLICATIVE");
        yaml.set("defaults.DURABILITY.per-level", 0.005D);
        yaml.set("defaults.DURABILITY.cap", 1.5D);
        final var rule = ConfigurationService.parseRules(yaml.getConfigurationSection("defaults")).get(StatType.DURABILITY);
        assertTrue(rule.enabled()); assertEquals(StatScaleMode.MULTIPLICATIVE, rule.mode());
        assertEquals(0.005D, rule.perLevel()); assertEquals(1.5D, rule.cap());
    }
    @Test void statRevisionIsStableForEquivalentEffectiveProfilesAndChangesForRules() {
        final var first = new java.util.EnumMap<StatType, dev.s3tupw1zard.gearMastery.stat.StatRule>(StatType.class);
        first.put(StatType.DURABILITY, new dev.s3tupw1zard.gearMastery.stat.StatRule(true, StatScaleMode.MULTIPLICATIVE, 0.01D, 2.0D));
        final var profile = new ItemProfile("pickaxes", java.util.Set.of(), "standard", java.util.Set.of("block_break"), first);
        final long revision = ConfigurationService.statRevision(java.util.Map.of("pickaxes", profile), java.util.Map.of(), java.util.Map.of());
        assertEquals(revision, ConfigurationService.statRevision(java.util.Map.of("pickaxes", profile), java.util.Map.of(), java.util.Map.of()));
        first.put(StatType.DURABILITY, new dev.s3tupw1zard.gearMastery.stat.StatRule(true, StatScaleMode.MULTIPLICATIVE, 0.02D, 2.0D));
        final var changed = new ItemProfile("pickaxes", java.util.Set.of(), "standard", java.util.Set.of("block_break"), first);
        org.junit.jupiter.api.Assertions.assertNotEquals(revision, ConfigurationService.statRevision(java.util.Map.of("pickaxes", changed), java.util.Map.of(), java.util.Map.of()));
    }
}
