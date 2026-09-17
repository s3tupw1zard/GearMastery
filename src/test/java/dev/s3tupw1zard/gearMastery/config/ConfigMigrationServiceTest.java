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
        assertEquals(3, items.getInt("config-version")); assertEquals("_gearmastery_melee_weapon", items.getString("profiles.swords.extends"));
        assertEquals(java.util.List.of("DIAMOND_SWORD"), items.getStringList("profiles.swords.items")); assertTrue(items.contains("profiles._gearmastery_mining_tool"));
        assertEquals(9.0D, stats.getDouble("defaults.DURABILITY.cap")); assertEquals(3, stats.getInt("config-version"));
        assertTrue(Files.exists(directory.resolve("items.yml.v1.bak"))); assertTrue(Files.exists(directory.resolve("stats.yml.v1.bak")));
        assertTrue(service.migrateInstalledConfigs());
    }
    @Test void malformedInstalledConfigFailsWithoutReplacingTheOriginal() throws Exception {
        final String invalid = "profiles: ["; write("items.yml", invalid); write("stats.yml", "defaults: {}\n");
        assertFalse(service().migrateInstalledConfigs()); assertEquals(invalid, Files.readString(directory.resolve("items.yml")));
    }
    @Test void preservesAdministratorProfileThatUsesALegacyParentName() throws Exception {
        write("items.yml", "profiles:\n  mining_tool:\n    items: [DIAMOND_PICKAXE]\n    xp-sources: [custom]\n  pickaxes:\n    items: [IRON_PICKAXE]\n    xp-sources: []\n");
        write("stats.yml", "defaults: {}\n");
        assertTrue(service().migrateInstalledConfigs());
        final YamlConfiguration items = load("items.yml");
        assertEquals(java.util.List.of("DIAMOND_PICKAXE"), items.getStringList("profiles.mining_tool.items"));
        assertEquals(java.util.List.of("custom"), items.getStringList("profiles.mining_tool.xp-sources"));
        assertEquals("_gearmastery_mining_tool", items.getString("profiles.pickaxes.extends"));
    }
    private ConfigMigrationService service() {
        final Map<String, String> defaults = Map.of("items.yml", "config-version: 3\nprofiles:\n  _gearmastery_melee_weapon:\n    items: []\n    xp-sources: []\n  _gearmastery_mining_tool:\n    items: []\n    xp-sources: []\n  swords:\n    extends: _gearmastery_melee_weapon\n    items: []\n    xp-sources: []\n", "stats.yml", "config-version: 3\ndefaults:\n  DURABILITY:\n    enabled: true\n    mode: MULTIPLICATIVE\n    per-level: 0.005\n    cap: 1.5\n");
        final Logger logger = Logger.getAnonymousLogger(); logger.setUseParentHandlers(false);
        return new ConfigMigrationService(directory.toFile(), name -> new ByteArrayInputStream(defaults.get(name).getBytes(StandardCharsets.UTF_8)), logger);
    }
    private void write(final String name, final String content) throws Exception { Files.writeString(directory.resolve(name), content); }
    private YamlConfiguration load(final String name) throws Exception { final YamlConfiguration yaml = new YamlConfiguration(); yaml.load(directory.resolve(name).toFile()); return yaml; }
}
