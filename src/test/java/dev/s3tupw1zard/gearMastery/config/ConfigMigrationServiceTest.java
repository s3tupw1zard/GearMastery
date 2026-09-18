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

    @Test void migratesFoundationStatsByAddingTheCompleteDurabilityDefault() throws Exception {
        write("stats.yml", "# Foundation schema without defaults\n");
        assertTrue(service().migrateInstalledConfigs());
        final YamlConfiguration stats = load("stats.yml");
        assertEquals(3, stats.getInt("config-version")); assertTrue(stats.getBoolean("defaults.DURABILITY.enabled"));
        assertEquals("MULTIPLICATIVE", stats.getString("defaults.DURABILITY.mode")); assertEquals(0.005D, stats.getDouble("defaults.DURABILITY.per-level")); assertEquals(1.5D, stats.getDouble("defaults.DURABILITY.cap"));
    }

    @Test void keepsV1PartialDurabilityAndV2MissingDurabilityUntouched() throws Exception {
        write("stats.yml", "defaults:\n  DURABILITY:\n    cap: 9.0\n"); assertTrue(service().migrateInstalledConfigs());
        assertFalse(load("stats.yml").contains("defaults.DURABILITY.enabled"));
        write("stats.yml", "config-version: 2\ndefaults: {}\n"); assertTrue(service().migrateInstalledConfigs());
        assertFalse(load("stats.yml").contains("defaults.DURABILITY"));
    }

    @Test void explicitVersionOneMigrates() throws Exception {
        write("stats.yml", "config-version: 1\ndefaults: {}\n");
        assertTrue(service().migrateInstalledConfigs()); assertEquals(3, load("stats.yml").getInt("config-version"));
    }

    @Test void versionTwoMigratesAndRewritesAnExactBundledLegacyParent() throws Exception {
        write("items.yml", """
            config-version: 2
            profiles:
              mining_tool:
                items: []
                xp-sources: []
                stats:
                  MINING_SPEED: {enabled: true, mode: MULTIPLICATIVE, per-level: 0.003, cap: 1.3}
              pickaxes:
                extends: mining_tool
                items: [DIAMOND_PICKAXE]
                xp-sources: []
            """);
        assertTrue(service().migrateInstalledConfigs());
        assertEquals("_gearmastery_mining_tool", load("items.yml").getString("profiles.pickaxes.extends"));
    }

    @Test void versionThreeIsANoOp() throws Exception {
        final String current = "config-version: 3\ndefaults:\n  custom: value\n"; write("stats.yml", current);
        assertTrue(service().migrateInstalledConfigs()); assertEquals(current, Files.readString(directory.resolve("stats.yml")));
    }

    @Test void rejectsZeroVersionWithoutReplacingTheOriginal() throws Exception { assertInvalidVersion("config-version: 0\ndefaults: {}\n"); }
    @Test void rejectsNegativeVersionWithoutReplacingTheOriginal() throws Exception { assertInvalidVersion("config-version: -1\ndefaults: {}\n"); }
    @Test void rejectsFutureVersionWithoutReplacingTheOriginal() throws Exception { assertInvalidVersion("config-version: 4\ndefaults: {}\n"); }
    @Test void rejectsNonNumericVersionWithoutTreatingItAsFoundation() throws Exception { assertInvalidVersion("config-version: banana\ndefaults: {}\n"); }
    @Test void rejectsNullOrDecimalVersionsWithoutTreatingThemAsFoundation() throws Exception {
        assertInvalidVersion("config-version: null\ndefaults: {}\n"); assertInvalidVersion("config-version: 1.0\ndefaults: {}\n");
    }

    @Test void validatesEverySourceVersionBeforeReplacingAnyFile() throws Exception {
        final String items = "profiles:\n  pickaxes:\n    items: [DIAMOND_PICKAXE]\n    xp-sources: []\n";
        final String stats = "config-version: 4\ndefaults: {}\n"; write("items.yml", items); write("stats.yml", stats);
        assertFalse(service().migrateInstalledConfigs());
        assertEquals(items, Files.readString(directory.resolve("items.yml"))); assertEquals(stats, Files.readString(directory.resolve("stats.yml")));
    }

    @Test void malformedInstalledConfigFailsWithoutReplacingTheOriginal() throws Exception {
        final String invalid = "profiles: ["; write("items.yml", invalid); write("stats.yml", "defaults: {}\n");
        assertFalse(service().migrateInstalledConfigs()); assertEquals(invalid, Files.readString(directory.resolve("items.yml")));
    }

    @Test void preservesAdministratorProfileThatUsesALegacyParentName() throws Exception {
        write("items.yml", "profiles:\n  mining_tool:\n    items: [DIAMOND_PICKAXE]\n    xp-sources: [custom]\n  pickaxes:\n    items: [IRON_PICKAXE]\n    xp-sources: []\n");
        assertTrue(service().migrateInstalledConfigs());
        final YamlConfiguration items = load("items.yml");
        assertEquals(java.util.List.of("DIAMOND_PICKAXE"), items.getStringList("profiles.mining_tool.items"));
        assertEquals(java.util.List.of("custom"), items.getStringList("profiles.mining_tool.xp-sources")); assertEquals("_gearmastery_mining_tool", items.getString("profiles.pickaxes.extends"));
    }

    @Test void preservesCustomV2LegacyParentBehaviorInsteadOfRewritingItsChildren() throws Exception {
        write("items.yml", """
            config-version: 2
            profiles:
              mining_tool:
                items: []
                xp-sources: []
                curve: custom
                stats:
                  MINING_SPEED: {enabled: true, mode: MULTIPLICATIVE, per-level: 0.05, cap: 4.0}
              pickaxes:
                extends: mining_tool
                items: [DIAMOND_PICKAXE]
                xp-sources: []
            """);
        assertTrue(service().migrateInstalledConfigs()); assertEquals("mining_tool", load("items.yml").getString("profiles.pickaxes.extends"));
    }

    @Test void acceptsAnExistingExactBundledReservedProfile() throws Exception {
        write("items.yml", """
            profiles:
              _gearmastery_mining_tool:
                items: []
                xp-sources: []
                stats:
                  MINING_SPEED: {enabled: true, mode: MULTIPLICATIVE, per-level: 0.003, cap: 1.3}
              pickaxes:
                items: [DIAMOND_PICKAXE]
                xp-sources: []
            """);
        assertTrue(service().migrateInstalledConfigs());
    }

    @Test void rejectsCustomReservedProfileCollisionBeforeAddingDependentProfiles() throws Exception {
        final String current = "profiles:\n  _gearmastery_mining_tool:\n    items: [DIAMOND_SWORD]\n    xp-sources: []\n"; write("items.yml", current);
        assertFalse(service().migrateInstalledConfigs()); assertEquals(current, Files.readString(directory.resolve("items.yml")));
    }

    @Test void rejectsAV2RewriteWhenItsReservedTargetCollides() throws Exception {
        final String current = """
            config-version: 2
            profiles:
              _gearmastery_mining_tool:
                items: [DIAMOND_SWORD]
                xp-sources: []
              mining_tool:
                items: []
                xp-sources: []
                stats:
                  MINING_SPEED: {enabled: true, mode: MULTIPLICATIVE, per-level: 0.003, cap: 1.3}
              pickaxes:
                extends: mining_tool
                items: [DIAMOND_PICKAXE]
                xp-sources: []
            """;
        write("items.yml", current); assertFalse(service().migrateInstalledConfigs());
        assertEquals(current, Files.readString(directory.resolve("items.yml")));
    }

    @Test void rejectsReservedProfilesWithDifferentStatsCurveSourcesExtendsOrUnknownKeys() throws Exception {
        for (final String difference : java.util.List.of("stats: {MINING_SPEED: {enabled: false}}", "curve: custom", "xp-sources: [custom]", "extends: custom", "unknown: value")) {
            final String current = "profiles:\n  _gearmastery_mining_tool:\n    items: []\n    xp-sources: []\n    " + difference + "\n";
            write("items.yml", current); assertFalse(service().migrateInstalledConfigs()); assertEquals(current, Files.readString(directory.resolve("items.yml")));
        }
    }

    @Test void doesNotLinkAV1ChildToACollidingReservedParent() throws Exception {
        final String current = "profiles:\n  _gearmastery_mining_tool:\n    items: [DIAMOND_SWORD]\n    xp-sources: []\n  pickaxes:\n    items: [DIAMOND_PICKAXE]\n    xp-sources: []\n"; write("items.yml", current);
        assertFalse(service().migrateInstalledConfigs()); assertEquals(current, Files.readString(directory.resolve("items.yml")));
    }

    @Test void keepsDeletedLeavesAndCustomMaterialOwnershipAbsentFromV1Migration() throws Exception {
        write("items.yml", "profiles:\n  custom_weapons:\n    items: [DIAMOND_SWORD]\n    xp-sources: []\n");
        assertTrue(service().migrateInstalledConfigs()); assertFalse(load("items.yml").contains("profiles.swords"));
    }

    private void assertInvalidVersion(final String content) throws Exception {
        write("stats.yml", content); assertFalse(service().migrateInstalledConfigs()); assertEquals(content, Files.readString(directory.resolve("stats.yml")));
    }
    private ConfigMigrationService service() {
        final Map<String, String> defaults = Map.of("items.yml", """
            config-version: 3
            profiles:
              _gearmastery_mining_tool:
                items: []
                xp-sources: []
                stats:
                  MINING_SPEED: {enabled: true, mode: MULTIPLICATIVE, per-level: 0.003, cap: 1.3}
              _gearmastery_melee_weapon:
                items: []
                xp-sources: []
              _gearmastery_combat_tool:
                extends: _gearmastery_mining_tool
                items: []
                xp-sources: []
              _gearmastery_armor_piece:
                items: []
                xp-sources: []
            """, "stats.yml", """
            config-version: 3
            defaults:
              DURABILITY:
                enabled: true
                mode: MULTIPLICATIVE
                per-level: 0.005
                cap: 1.5
            """);
        final Logger logger = Logger.getAnonymousLogger(); logger.setUseParentHandlers(false);
        return new ConfigMigrationService(directory.toFile(), name -> new ByteArrayInputStream(defaults.get(name).getBytes(StandardCharsets.UTF_8)), logger);
    }
    private void write(final String name, final String content) throws Exception { Files.writeString(directory.resolve(name), content); }
    private YamlConfiguration load(final String name) throws Exception { final YamlConfiguration yaml = new YamlConfiguration(); yaml.load(directory.resolve(name).toFile()); return yaml; }
}
