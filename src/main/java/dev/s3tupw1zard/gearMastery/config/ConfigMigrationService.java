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
import java.util.Map;
import java.util.logging.Logger;

/** Migrates installed configuration schemas without replacing administrator values. */
public final class ConfigMigrationService {
    public static final int CURRENT_VERSION = 2;
    private static final Map<String, String> PROFILE_PARENTS = Map.ofEntries(
        Map.entry("swords", "melee_weapon"), Map.entry("axes", "combat_tool"), Map.entry("pickaxes", "mining_tool"),
        Map.entry("shovels", "mining_tool"), Map.entry("hoes", "mining_tool"), Map.entry("tridents", "melee_weapon"),
        Map.entry("helmets", "armor_piece"), Map.entry("chestplates", "armor_piece"), Map.entry("leggings", "armor_piece"), Map.entry("boots", "armor_piece"));
    private final File dataFolder; private final ResourceReader resources; private final Logger logger;
    public ConfigMigrationService(final JavaPlugin plugin) { this(plugin.getDataFolder(), plugin::getResource, plugin.getLogger()); }
    ConfigMigrationService(final File dataFolder, final ResourceReader resources, final Logger logger) { this.dataFolder = dataFolder; this.resources = resources; this.logger = logger; }
    public boolean migrateInstalledConfigs() {
        try {
            final Map<String, YamlConfiguration> migrated = new LinkedHashMap<>();
            for (final String name : new String[] {"items.yml", "stats.yml"}) {
                final File file = new File(dataFolder, name); if (!file.isFile()) continue;
                final YamlConfiguration current = load(file); if (current.getInt("config-version", 1) >= CURRENT_VERSION) continue;
                final YamlConfiguration defaults = loadDefault(name); mergeMissing(defaults, current);
                if (name.equals("items.yml")) addMissingProfileParents(current);
                current.set("config-version", CURRENT_VERSION); migrated.put(name, current);
            }
            if (migrated.isEmpty()) return true;
            final Map<String, Path> temporary = new LinkedHashMap<>();
            for (final Map.Entry<String, YamlConfiguration> entry : migrated.entrySet()) {
                final Path target = new File(dataFolder, entry.getKey()).toPath(); final Path temp = Files.createTempFile(target.getParent(), entry.getKey(), ".migration");
                entry.getValue().save(temp.toFile()); temporary.put(entry.getKey(), temp);
            }
            for (final String name : migrated.keySet()) {
                final Path target = new File(dataFolder, name).toPath(); final Path backup = target.resolveSibling(name + ".v1.bak");
                if (!Files.exists(backup)) Files.copy(target, backup);
            }
            for (final Map.Entry<String, Path> entry : temporary.entrySet()) replace(entry.getValue(), new File(dataFolder, entry.getKey()).toPath());
            logger.info("Migrated GearMastery configuration schema to version " + CURRENT_VERSION + "."); return true;
        } catch (final IOException | InvalidConfigurationException exception) {
            logger.log(java.util.logging.Level.SEVERE, "GearMastery configuration migration failed: " + exception.getMessage(), exception); return false;
        }
    }
    private static void addMissingProfileParents(final YamlConfiguration configuration) {
        final ConfigurationSection profiles = configuration.getConfigurationSection("profiles"); if (profiles == null) return;
        PROFILE_PARENTS.forEach((profile, parent) -> { final ConfigurationSection section = profiles.getConfigurationSection(profile); if (section != null && !section.contains("extends")) section.set("extends", parent); });
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
    private static void replace(final Path source, final Path target) throws IOException {
        try { Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
        catch (final AtomicMoveNotSupportedException ignored) { Files.move(source, target, StandardCopyOption.REPLACE_EXISTING); }
    }
    private YamlConfiguration loadDefault(final String name) throws IOException, InvalidConfigurationException {
        try (InputStream input = resources.open(name)) { if (input == null) throw new IOException("Missing bundled default " + name); final YamlConfiguration yaml = new YamlConfiguration(); yaml.loadFromString(new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8)); return yaml; }
    }
    private static YamlConfiguration load(final File file) throws IOException, InvalidConfigurationException { final YamlConfiguration yaml = new YamlConfiguration(); yaml.load(file); return yaml; }
    @FunctionalInterface interface ResourceReader { InputStream open(String name); }
}
