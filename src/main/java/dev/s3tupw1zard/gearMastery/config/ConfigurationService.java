package dev.s3tupw1zard.gearMastery.config;

import dev.s3tupw1zard.gearMastery.level.*;
import dev.s3tupw1zard.gearMastery.stat.*;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;
import java.util.logging.Level;

/** Loads configurations into an immutable snapshot. A failed reload keeps the prior snapshot. */
public final class ConfigurationService {
    private final JavaPlugin plugin;
    private volatile GearConfiguration current;
    private long generation;
    public ConfigurationService(final JavaPlugin plugin) { this.plugin = plugin; }
    public GearConfiguration current() { return Objects.requireNonNull(current, "Configuration not loaded"); }
    public boolean reload() {
        try { current = load(++generation); return true; }
        catch (final RuntimeException exception) {
            plugin.getLogger().log(Level.SEVERE, "GearMastery configuration was not reloaded: " + exception.getMessage(), exception);
            return false;
        }
    }
    private GearConfiguration load(final long nextGeneration) {
        final YamlConfiguration leveling = yaml("leveling.yml");
        final int maxLevel = Math.toIntExact(positive(leveling.getInt("leveling.max-level", 100), "leveling.max-level"));
        final String defaultCurve = requireString(leveling, "leveling.default-curve");
        final Map<String, LevelingCurve> curves = loadCurves(leveling.getConfigurationSection("curves"));
        if (!curves.containsKey(defaultCurve)) throw new IllegalArgumentException("Default curve does not exist: " + defaultCurve);
        final YamlConfiguration items = yaml("items.yml");
        final YamlConfiguration stats = yaml("stats.yml");
        final Map<StatType, StatRule> globalRules = parseRules(stats.getConfigurationSection("defaults"));
        final Map<String, RawProfile> rawProfiles = rawProfiles(items.getConfigurationSection("profiles"));
        final Map<String, ItemProfile> profiles = new LinkedHashMap<>();
        for (final String id : rawProfiles.keySet()) resolve(id, rawProfiles, profiles, new HashSet<>(), defaultCurve, globalRules);
        final Map<Material, String> overrides = materialProfileMap(items.getConfigurationSection("material-overrides"));
        final Map<String, String> aliases = stringMap(items.getConfigurationSection("profile-aliases"));
        for (final String profileId : overrides.values()) if (!profiles.containsKey(profileId)) throw new IllegalArgumentException("Override references unknown profile: " + profileId);
        for (final String profileId : aliases.values()) if (!profiles.containsKey(profileId)) throw new IllegalArgumentException("Alias references unknown profile: " + profileId);
        final YamlConfiguration blocks = yaml("xp/blocks.yml");
        final Map<Material, Long> blockXp = materialLongMap(blocks.getConfigurationSection("blocks"));
        final YamlConfiguration main = yaml("config.yml");
        return new GearConfiguration(nextGeneration, maxLevel, defaultCurve, curves, profiles, overrides, aliases, blockXp,
            main.getBoolean("safety.exclude-creative", true));
    }
    private YamlConfiguration yaml(final String name) { return YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), name)); }
    private Map<String, LevelingCurve> loadCurves(final ConfigurationSection section) {
        if (section == null) throw new IllegalArgumentException("Missing curves section");
        final Map<String, LevelingCurve> result = new HashMap<>();
        for (final String id : section.getKeys(false)) {
            final ConfigurationSection c = Objects.requireNonNull(section.getConfigurationSection(id), "Curve must be a section: " + id);
            final String type = requireString(c, "type").toUpperCase(Locale.ROOT);
            final long base = positive(c.getLong("base-xp"), id + ".base-xp");
            result.put(id, switch (type) {
                case "LINEAR" -> new LinearLevelingCurve(base, nonNegative(c.getLong("growth", 0), id + ".growth"));
                case "QUADRATIC" -> new QuadraticLevelingCurve(base, nonNegative(c.getLong("growth", 0), id + ".growth"));
                case "EXPONENTIAL" -> new ExponentialLevelingCurve(base, Math.max(1.0D, c.getDouble("growth", 1.0D)));
                default -> throw new IllegalArgumentException("Unknown curve type: " + type);
            });
        }
        return result;
    }
    private Map<String, RawProfile> rawProfiles(final ConfigurationSection section) {
        if (section == null) throw new IllegalArgumentException("Missing profiles section");
        final Map<String, RawProfile> result = new LinkedHashMap<>();
        for (final String id : section.getKeys(false)) {
            final ConfigurationSection p = Objects.requireNonNull(section.getConfigurationSection(id), "Profile must be a section: " + id);
            result.put(id, new RawProfile(p.getString("extends"), parseMaterials(p.getStringList("items")),
                p.getString("curve"), Set.copyOf(p.getStringList("xp-sources")), parseRules(p.getConfigurationSection("stats"))));
        }
        return result;
    }
    private ItemProfile resolve(final String id, final Map<String, RawProfile> raw, final Map<String, ItemProfile> done, final Set<String> visiting, final String defaultCurve, final Map<StatType, StatRule> globalRules) {
        if (done.containsKey(id)) return done.get(id);
        if (!visiting.add(id)) throw new IllegalArgumentException("Profile inheritance cycle at " + id);
        final RawProfile value = Optional.ofNullable(raw.get(id)).orElseThrow(() -> new IllegalArgumentException("Unknown profile " + id));
        final Set<Material> materials = new HashSet<>(); final Set<String> sources = new HashSet<>(); final Map<StatType, StatRule> rules = new EnumMap<>(StatType.class); rules.putAll(globalRules);
        String curve = value.curve;
        if (value.parent != null) { final ItemProfile parent = resolve(value.parent, raw, done, visiting, defaultCurve, globalRules); materials.addAll(parent.materials()); sources.addAll(parent.experienceSources()); rules.putAll(parent.statRules()); if (value.curve == null) curve = parent.curveId(); }
        if (curve == null) curve = defaultCurve;
        materials.addAll(value.materials); sources.addAll(value.sources); rules.putAll(value.rules);
        final ItemProfile profile = new ItemProfile(id, materials, curve, sources, rules); done.put(id, profile); visiting.remove(id); return profile;
    }
    private Set<Material> parseMaterials(final List<String> names) { final Set<Material> result = new HashSet<>(); for (final String name : names) { final Material material = Material.matchMaterial(name); if (material == null || !material.isItem()) throw new IllegalArgumentException("Invalid item material: " + name); result.add(material); } return result; }
    static Map<StatType, StatRule> parseRules(final ConfigurationSection section) { final Map<StatType, StatRule> result = new EnumMap<>(StatType.class); if (section == null) return result; for (final String key : section.getKeys(false)) { final ConfigurationSection s = Objects.requireNonNull(section.getConfigurationSection(key)); result.put(StatType.valueOf(key.toUpperCase(Locale.ROOT)), new StatRule(s.getBoolean("enabled", true), StatScaleMode.valueOf(requireString(s, "mode").toUpperCase(Locale.ROOT)), s.getDouble("per-level"), s.getDouble("cap"))); } return result; }
    private Map<Material, String> materialProfileMap(final ConfigurationSection section) { final Map<Material, String> result = new HashMap<>(); if (section == null) return result; for (final String key : section.getKeys(false)) { final Material material = Material.matchMaterial(key); if (material == null) throw new IllegalArgumentException("Invalid override material: " + key); result.put(material, requireString(section, key)); } return result; }
    private Map<String, String> stringMap(final ConfigurationSection section) { final Map<String, String> result = new HashMap<>(); if (section != null) for (final String key : section.getKeys(false)) result.put(key, requireString(section, key)); return result; }
    private Map<Material, Long> materialLongMap(final ConfigurationSection section) { final Map<Material, Long> result = new HashMap<>(); if (section == null) return result; for (final String key : section.getKeys(false)) { final Material material = Material.matchMaterial(key); if (material == null || !material.isBlock()) throw new IllegalArgumentException("Invalid block material: " + key); result.put(material, positive(section.getLong(key), "blocks." + key)); } return result; }
    private static String requireString(final ConfigurationSection s, final String path) { final String value = s.getString(path); if (value == null || value.isBlank()) throw new IllegalArgumentException("Missing value: " + path); return value; }
    private static long positive(final long value, final String name) { if (value <= 0) throw new IllegalArgumentException(name + " must be positive"); return value; }
    private static long nonNegative(final long value, final String name) { if (value < 0) throw new IllegalArgumentException(name + " must not be negative"); return value; }
    private record RawProfile(String parent, Set<Material> materials, String curve, Set<String> sources, Map<StatType, StatRule> rules) { }
}
