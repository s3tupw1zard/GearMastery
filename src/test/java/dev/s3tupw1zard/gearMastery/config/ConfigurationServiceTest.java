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
}
