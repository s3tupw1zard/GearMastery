package dev.s3tupw1zard.gearMastery.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigMigrationServiceTest {
    @TempDir Path directory;
    @Test void migratesFoundationConfigsWithoutOverwritingAdministratorValues() throws Exception {
        write("items.yml", "profiles:\n  swords:\n    items: [DIAMOND_SWORD]\n    xp-sources: [custom]\n");
        write("stats.yml", "defaults:\n  DURABILITY:\n    cap: 9.0\n");
        final ConfigMigrationService service = service(); assertTrue(service.migrateInstalledConfigs());
        final YamlConfiguration items = load("items.yml"); final YamlConfiguration stats = load("stats.yml");
        assertEquals(2, items.getInt("config-version")); assertEquals("melee_weapon", items.getString("profiles.swords.extends"));
        assertEquals(java.util.List.of("DIAMOND_SWORD"), items.getStringList("profiles.swords.items")); assertTrue(items.contains("profiles.mining_tool"));
        assertEquals(9.0D, stats.getDouble("defaults.DURABILITY.cap")); assertEquals(2, stats.getInt("config-version"));
        assertTrue(Files.exists(directory.resolve("items.yml.v1.bak"))); assertTrue(Files.exists(directory.resolve("stats.yml.v1.bak")));
        assertTrue(service.migrateInstalledConfigs());
    }
    @Test void malformedInstalledConfigFailsWithoutReplacingTheOriginal() throws Exception {
        final String invalid = "profiles: ["; write("items.yml", invalid); write("stats.yml", "defaults: {}\n");
        assertFalse(service().migrateInstalledConfigs()); assertEquals(invalid, Files.readString(directory.resolve("items.yml")));
    }
    private ConfigMigrationService service() {
        final Map<String, String> defaults = Map.of("items.yml", "config-version: 2\nprofiles:\n  melee_weapon:\n    items: []\n    xp-sources: []\n  mining_tool:\n    items: []\n    xp-sources: []\n  swords:\n    extends: melee_weapon\n    items: []\n    xp-sources: []\n", "stats.yml", "config-version: 2\ndefaults:\n  DURABILITY:\n    enabled: true\n    mode: MULTIPLICATIVE\n    per-level: 0.005\n    cap: 1.5\n");
        final Logger logger = Logger.getAnonymousLogger(); logger.setUseParentHandlers(false);
        return new ConfigMigrationService(directory.toFile(), name -> new ByteArrayInputStream(defaults.get(name).getBytes(StandardCharsets.UTF_8)), logger);
    }
    private void write(final String name, final String content) throws Exception { Files.writeString(directory.resolve(name), content); }
    private YamlConfiguration load(final String name) throws Exception { final YamlConfiguration yaml = new YamlConfiguration(); yaml.load(directory.resolve(name).toFile()); return yaml; }
}
