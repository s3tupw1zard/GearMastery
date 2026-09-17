package dev.s3tupw1zard.gearMastery.config;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigurationReloadValidationTest {
    @TempDir Path directory;

    @Test void rejectsUnknownProfileCurveAndKeepsTheActiveSnapshot() throws IOException {
        writeBase();
        final ConfigurationService service = service();
        assertTrue(service.reload()); final GearConfiguration active = service.current();

        write("items.yml", items("    curve: missing\n"));
        assertFalse(service.reload());
        assertSame(active, service.current());
    }

    @Test void indexesAUniqueMaterialOwnerDeterministically() {
        final ItemProfile profile = profile("tools", Set.of(Material.DIAMOND_PICKAXE));
        assertEquals("tools", ConfigurationService.materialProfileIndex(Map.of("tools", profile), Map.of()).get(Material.DIAMOND_PICKAXE));
    }

    @Test void rejectsAmbiguousInheritedMaterialOwnershipWithoutAnOverride() {
        final ItemProfile tools = profile("tools", Set.of(Material.DIAMOND_PICKAXE));
        final ItemProfile specialTools = profile("special_tools", Set.of(Material.DIAMOND_PICKAXE));
        final IllegalArgumentException exception = org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
            () -> ConfigurationService.materialProfileIndex(Map.of("tools", tools, "special_tools", specialTools), Map.of()));
        assertTrue(exception.getMessage().contains("DIAMOND_PICKAXE"));
    }

    @Test void acceptsAmbiguousOwnershipWhenAnOverrideSelectsTheProfile() {
        final ItemProfile tools = profile("tools", Set.of(Material.DIAMOND_PICKAXE));
        final ItemProfile specialTools = profile("special_tools", Set.of(Material.DIAMOND_PICKAXE));
        final Map<Material, String> index = ConfigurationService.materialProfileIndex(Map.of("tools", tools, "special_tools", specialTools),
            Map.of(Material.DIAMOND_PICKAXE, "special_tools"));
        assertEquals("special_tools", index.get(Material.DIAMOND_PICKAXE));
    }

    @Test void rejectsMalformedYamlInEveryLoadedFileAndKeepsTheActiveSnapshot() throws IOException {
        for (final String file : List.of("config.yml", "leveling.yml", "items.yml", "stats.yml", "xp/blocks.yml")) {
            writeBase(); final ConfigurationService service = service(); assertTrue(service.reload());
            final GearConfiguration active = service.current();
            write(file, "invalid: [");
            assertFalse(service.reload(), file);
            assertSame(active, service.current(), file);
        }
    }

    @Test void validatesCurveArithmeticOnlyForReachableLevelTransitions() throws IOException {
        writeBase();
        final ConfigurationService service = service(); assertTrue(service.reload());
        final GearConfiguration active = service.current();
        write("leveling.yml", leveling(2, "LINEAR", Long.MAX_VALUE, 1));
        assertFalse(service.reload(), "linear overflow before maximum level must fail");
        assertSame(active, service.current());

        writeBase();
        write("leveling.yml", leveling(2, "QUADRATIC", 1, Long.MAX_VALUE));
        assertFalse(service().reload(), "quadratic overflow before maximum level must fail");

        writeBase();
        write("leveling.yml", leveling(1, "LINEAR", Long.MAX_VALUE, 1));
        assertTrue(service().reload(), "max level has no transition beyond it");

        writeBase();
        write("leveling.yml", leveling(3, "QUADRATIC", Long.MAX_VALUE, 0));
        assertTrue(service().reload(), "high but representable values must remain valid");

        writeBase();
        write("leveling.yml", leveling(Integer.MAX_VALUE, "LINEAR", 1, 0));
        assertTrue(service().reload(), "endpoint validation must support very large configured level bounds");
    }

    private ConfigurationService service() {
        final Logger logger = Logger.getAnonymousLogger(); logger.setUseParentHandlers(false);
        return new ConfigurationService(directory.toFile(), logger);
    }
    private void writeBase() throws IOException {
        Files.createDirectories(directory.resolve("xp"));
        write("config.yml", "safety:\n  exclude-creative: true\n");
        write("leveling.yml", leveling(3, "LINEAR", 10, 1));
        write("items.yml", items(""));
        write("stats.yml", "defaults: {}\n");
        write("xp/blocks.yml", "blocks: {}\n");
    }
    private static String leveling(final int maxLevel, final String type, final long base, final long growth) {
        return """
            leveling:
              max-level: %d
              default-curve: standard
            curves:
              standard:
                type: %s
                base-xp: %d
                growth: %d
            """.formatted(maxLevel, type, base, growth);
    }
    private static String items(final String profileExtra) {
        return """
            profiles:
              pickaxes:
                xp-sources: [block_break]
            %s""".formatted(profileExtra);
    }
    private static ItemProfile profile(final String id, final Set<Material> materials) {
        return new ItemProfile(id, materials, "standard", Set.of("block_break"), Map.of());
    }
    private void write(final String relativePath, final String content) throws IOException {
        final Path file = directory.resolve(relativePath); Files.createDirectories(file.getParent()); Files.writeString(file, content);
    }
}
