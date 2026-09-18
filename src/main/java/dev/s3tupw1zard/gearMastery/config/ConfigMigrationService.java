package dev.s3tupw1zard.gearMastery.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.AtomicMoveNotSupportedException;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/** Migrates installed configuration schemas without replacing administrator values. */
public final class ConfigMigrationService {
    public static final int CURRENT_VERSION = ConfigSchemaVersion.CURRENT_VERSION;
    private static final Map<String, String> PROFILE_PARENTS = Map.ofEntries(
        Map.entry("swords", "_gearmastery_melee_weapon"), Map.entry("axes", "_gearmastery_combat_tool"), Map.entry("pickaxes", "_gearmastery_mining_tool"),
        Map.entry("shovels", "_gearmastery_mining_tool"), Map.entry("hoes", "_gearmastery_mining_tool"), Map.entry("tridents", "_gearmastery_melee_weapon"),
        Map.entry("helmets", "_gearmastery_armor_piece"), Map.entry("chestplates", "_gearmastery_armor_piece"), Map.entry("leggings", "_gearmastery_armor_piece"), Map.entry("boots", "_gearmastery_armor_piece"));
    private final File dataFolder; private final ResourceReader resources; private final Logger logger; private final FileReplacer replacer; private final RollbackSnapshotter snapshotter;
    public ConfigMigrationService(final JavaPlugin plugin) { this(plugin.getDataFolder(), plugin::getResource, plugin.getLogger()); }
    ConfigMigrationService(final File dataFolder, final ResourceReader resources, final Logger logger) { this(dataFolder, resources, logger, ConfigMigrationService::replaceAtomically, ConfigMigrationService::createSnapshot); }
    ConfigMigrationService(final File dataFolder, final ResourceReader resources, final Logger logger, final FileReplacer replacer) { this(dataFolder, resources, logger, replacer, ConfigMigrationService::createSnapshot); }
    ConfigMigrationService(final File dataFolder, final ResourceReader resources, final Logger logger, final FileReplacer replacer, final RollbackSnapshotter snapshotter) { this.dataFolder = dataFolder; this.resources = resources; this.logger = logger; this.replacer = replacer; this.snapshotter = snapshotter; }
    public boolean migrateInstalledConfigs() {
        try {
            final Map<String, YamlConfiguration> installed = new LinkedHashMap<>();
            final Map<String, YamlConfiguration> migrated = new LinkedHashMap<>();
            final Map<String, Integer> sourceVersions = new LinkedHashMap<>();
            for (final String name : new String[] {"items.yml", "stats.yml"}) {
                final File file = new File(dataFolder, name); if (!file.isFile()) continue;
                final YamlConfiguration current = load(file); final int version = sourceVersion(name, file, current);
                installed.put(name, current); sourceVersions.put(name, version);
            }
            for (final Map.Entry<String, YamlConfiguration> entry : installed.entrySet()) {
                final String name = entry.getKey(); final YamlConfiguration current = entry.getValue(); final int version = sourceVersions.get(name);
                if (version == CURRENT_VERSION) continue;
                final YamlConfiguration defaults = loadDefault(name);
                // V1 files are administrator-owned: do not recreate legacy leaves/values that were deliberately removed.
                if (version == 1 && name.equals("items.yml")) { validateReservedProfiles(defaults, current); addReservedProfiles(defaults, current); addMissingProfileParents(current); }
                if (version == 1 && name.equals("stats.yml")) migrateV1Stats(defaults, current);
                if (version == 2 && name.equals("items.yml")) { validateReservedProfiles(defaults, current); addReservedProfiles(defaults, current); migrateSafeLegacyParentLinks(defaults, current); }
                current.set("config-version", CURRENT_VERSION); migrated.put(name, current);
            }
            if (migrated.isEmpty()) return true;
            final Map<String, Path> temporary = new LinkedHashMap<>();
            final Map<String, Path> snapshots = new LinkedHashMap<>(); final List<Path> rollbackTemporary = new ArrayList<>();
            try {
                for (final Map.Entry<String, YamlConfiguration> entry : migrated.entrySet()) {
                    final Path target = new File(dataFolder, entry.getKey()).toPath(); final Path temp = Files.createTempFile(target.getParent(), entry.getKey(), ".migration");
                    temporary.put(entry.getKey(), temp); entry.getValue().save(temp.toFile());
                }
                for (final String name : migrated.keySet()) {
                    final Path target = new File(dataFolder, name).toPath(); final Path backup = target.resolveSibling(name + ".v" + sourceVersions.get(name) + ".bak");
                    if (!Files.exists(backup)) Files.copy(target, backup);
                }
                for (final String name : migrated.keySet()) {
                    final Path target = new File(dataFolder, name).toPath();
                    snapshots.put(name, snapshotter.snapshot(target, target.getParent(), name));
                }
                final List<String> attempted = new ArrayList<>();
                try {
                for (final Map.Entry<String, Path> entry : temporary.entrySet()) {
                        attempted.add(entry.getKey()); replacer.replace(entry.getValue(), new File(dataFolder, entry.getKey()).toPath());
                    }
                } catch (final IOException replaceFailure) {
                    rollbackAttemptedFiles(attempted, snapshots, rollbackTemporary, replaceFailure); throw replaceFailure;
                }
                logger.info("Migrated GearMastery configuration schema to version " + CURRENT_VERSION + "."); return true;
            } finally {
                cleanupTemporaryFiles(temporary.values()); cleanupTemporaryFiles(snapshots.values()); cleanupTemporaryFiles(rollbackTemporary);
            }
        } catch (final IOException | InvalidConfigurationException | IllegalArgumentException exception) {
            logger.log(java.util.logging.Level.SEVERE, "GearMastery configuration migration failed: " + exception.getMessage(), exception); return false;
        }
    }
    private static int sourceVersion(final String name, final File file, final YamlConfiguration configuration) throws IOException { return ConfigSchemaVersion.read(name, file, configuration); }
    /** DURABILITY is the only global stat default introduced after the Foundation schema. */
    private static void migrateV1Stats(final YamlConfiguration defaults, final YamlConfiguration current) {
        if (current.contains("defaults.DURABILITY")) return;
        final ConfigurationSection bundled = defaults.getConfigurationSection("defaults.DURABILITY");
        if (bundled == null) return;
        final ConfigurationSection target = current.getConfigurationSection("defaults") == null ? current.createSection("defaults") : current.getConfigurationSection("defaults");
        mergeMissing(bundled, target.createSection("DURABILITY"));
    }
    private static void validateReservedProfiles(final YamlConfiguration defaults, final YamlConfiguration current) {
        final ConfigurationSection source = defaults.getConfigurationSection("profiles"), target = current.getConfigurationSection("profiles"); if (source == null || target == null) return;
        for (final String id : target.getKeys(false)) {
            if (!id.startsWith("_gearmastery_")) continue;
            final ConfigurationSection bundled = source.getConfigurationSection(id), installed = target.getConfigurationSection(id);
            if (bundled == null || installed == null || !canonical(installed).equals(canonical(bundled))) {
                throw new IllegalArgumentException("Reserved profile ID collision: '" + id + "' already exists with administrator-defined content.");
            }
        }
    }
    private static void addReservedProfiles(final YamlConfiguration defaults, final YamlConfiguration current) {
        final ConfigurationSection source = defaults.getConfigurationSection("profiles"), target = current.getConfigurationSection("profiles"); if (source == null || target == null) return;
        for (final String id : source.getKeys(false)) if (id.startsWith("_gearmastery_") && !target.contains(id)) mergeMissing(source.getConfigurationSection(id), target.createSection(id));
    }
    private static void addMissingProfileParents(final YamlConfiguration configuration) {
        final ConfigurationSection profiles = configuration.getConfigurationSection("profiles"); if (profiles == null) return;
        PROFILE_PARENTS.forEach((profile, parent) -> { final ConfigurationSection section = profiles.getConfigurationSection(profile); if (section != null && !section.contains("extends")) section.set("extends", parent); });
    }
    private static void migrateSafeLegacyParentLinks(final YamlConfiguration defaults, final YamlConfiguration configuration) {
        final ConfigurationSection profiles = configuration.getConfigurationSection("profiles"); if (profiles == null) return;
        final ConfigurationSection bundledProfiles = defaults.getConfigurationSection("profiles"); if (bundledProfiles == null) return;
        PROFILE_PARENTS.forEach((leaf, reserved) -> {
            final ConfigurationSection child = profiles.getConfigurationSection(leaf); if (child == null) return;
            final String legacy = child.getString("extends"); if (legacy == null || !Map.of("mining_tool", "_gearmastery_mining_tool", "melee_weapon", "_gearmastery_melee_weapon", "combat_tool", "_gearmastery_combat_tool", "armor_piece", "_gearmastery_armor_piece").containsKey(legacy)) return;
            final ConfigurationSection parent = profiles.getConfigurationSection(legacy);
            final ConfigurationSection bundledParent = bundledProfiles.getConfigurationSection(reserved);
            final ConfigurationSection installedReserved = profiles.getConfigurationSection(reserved);
            if (parent != null && bundledParent != null && installedReserved != null && canonical(installedReserved).equals(canonical(bundledParent)) && semanticallyEquivalentLegacyParent(parent, bundledParent)) child.set("extends", reserved);
        });
    }
    /** A legacy link is rewritten only when every known and unknown parent value matches the bundled V2 parent. */
    private static boolean semanticallyEquivalentLegacyParent(final ConfigurationSection actual, final ConfigurationSection bundled) {
        return canonical(actual, false).equals(canonical(bundled, true));
    }
    private static Object canonical(final ConfigurationSection section) { return canonical(section, false); }
    private static Object canonical(final ConfigurationSection section, final boolean bundledLegacyParent) {
        final Map<String, Object> values = new java.util.TreeMap<>();
        for (final String key : section.getKeys(false)) {
            final ConfigurationSection nested = section.getConfigurationSection(key);
            Object value = nested == null ? canonicalValue(section.get(key)) : canonical(nested, bundledLegacyParent);
            if (bundledLegacyParent && key.equals("extends") && value instanceof String parent) value = parent.replace("_gearmastery_", "");
            values.put(key, value);
        }
        return values;
    }
    private static Object canonicalValue(final Object value) {
        if (value instanceof ConfigurationSection section) return canonical(section);
        if (value instanceof Map<?, ?> map) {
            final Map<String, Object> values = new java.util.TreeMap<>();
            map.forEach((key, nested) -> values.put(String.valueOf(key), canonicalValue(nested))); return values;
        }
        if (value instanceof java.util.List<?> list) return list.stream().map(ConfigMigrationService::canonicalValue).sorted(java.util.Comparator.comparing(String::valueOf)).toList();
        return value;
    }
    private static void mergeMissing(final ConfigurationSection defaults, final ConfigurationSection target) {
        for (final String key : defaults.getKeys(false)) {
            final Object value = defaults.get(key); final ConfigurationSection defaultChild = defaults.getConfigurationSection(key); final ConfigurationSection targetChild = target.getConfigurationSection(key);
            if (defaultChild != null) {
                if (targetChild != null) mergeMissing(defaultChild, targetChild);
                else if (!target.contains(key)) mergeMissing(defaultChild, target.createSection(key));
            }
            else if (!target.contains(key)) target.set(key, value);
        }
    }
    private void rollbackAttemptedFiles(final List<String> attempted, final Map<String, Path> snapshots, final List<Path> rollbackTemporary, final IOException replaceFailure) {
        for (int index = attempted.size() - 1; index >= 0; index--) {
            final String name = attempted.get(index); final Path target = new File(dataFolder, name).toPath(); final Path snapshot = snapshots.get(name);
            try {
                final Path rollback = Files.createTempFile(target.getParent(), name, ".migration"); rollbackTemporary.add(rollback);
                Files.copy(snapshot, rollback, StandardCopyOption.REPLACE_EXISTING); replacer.replace(rollback, target);
            } catch (final IOException rollbackFailure) {
                replaceFailure.addSuppressed(rollbackFailure);
                logger.log(java.util.logging.Level.SEVERE, "GearMastery configuration migration rollback failed for " + name + ": " + rollbackFailure.getMessage(), rollbackFailure);
            }
        }
    }
    private void cleanupTemporaryFiles(final Iterable<Path> temporary) {
        for (final Path path : temporary) {
            try { Files.deleteIfExists(path); }
            catch (final IOException cleanupFailure) { logger.log(java.util.logging.Level.WARNING, "GearMastery configuration migration temporary cleanup failed for " + path.getFileName() + ": " + cleanupFailure.getMessage(), cleanupFailure); }
        }
    }
    private static Path createSnapshot(final Path source, final Path directory, final String name) throws IOException {
        final Path snapshot = Files.createTempFile(directory, name, ".rollback");
        try { Files.copy(source, snapshot, StandardCopyOption.REPLACE_EXISTING); return snapshot; }
        catch (final IOException failure) { try { Files.deleteIfExists(snapshot); } catch (final IOException cleanupFailure) { failure.addSuppressed(cleanupFailure); } throw failure; }
    }
    private static void replaceAtomically(final Path source, final Path target) throws IOException {
        try { Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
        catch (final AtomicMoveNotSupportedException ignored) { Files.move(source, target, StandardCopyOption.REPLACE_EXISTING); }
    }
    private YamlConfiguration loadDefault(final String name) throws IOException, InvalidConfigurationException {
        try (InputStream input = resources.open(name)) { if (input == null) throw new IOException("Missing bundled default " + name); final YamlConfiguration yaml = new YamlConfiguration(); yaml.loadFromString(new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8)); return yaml; }
    }
    private static YamlConfiguration load(final File file) throws IOException, InvalidConfigurationException { final YamlConfiguration yaml = new YamlConfiguration(); yaml.load(file); return yaml; }
    @FunctionalInterface interface ResourceReader { InputStream open(String name); }
    @FunctionalInterface interface FileReplacer { void replace(Path source, Path target) throws IOException; }
    @FunctionalInterface interface RollbackSnapshotter { Path snapshot(Path source, Path directory, String name) throws IOException; }
}
