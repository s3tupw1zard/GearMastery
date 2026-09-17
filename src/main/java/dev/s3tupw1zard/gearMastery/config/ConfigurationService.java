package dev.s3tupw1zard.gearMastery.config;

import dev.s3tupw1zard.gearMastery.level.*;
import dev.s3tupw1zard.gearMastery.stat.*;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

/** Loads configurations into an immutable snapshot. A failed reload keeps the prior snapshot. */
public final class ConfigurationService {
    private final File dataFolder;
    private final java.util.logging.Logger logger;
    private volatile GearConfiguration current;
    private long generation;
    public ConfigurationService(final JavaPlugin plugin) { this(plugin.getDataFolder(), plugin.getLogger()); }
    ConfigurationService(final File dataFolder, final java.util.logging.Logger logger) { this.dataFolder = dataFolder; this.logger = logger; }
    public GearConfiguration current() { return Objects.requireNonNull(current, "Configuration not loaded"); }
    public boolean reload() {
        try { final GearConfiguration loaded = load(generation + 1); current = loaded; generation++; return true; }
        catch (final RuntimeException exception) {
            logger.log(Level.SEVERE, "GearMastery configuration was not reloaded: " + exception.getMessage(), exception);
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
        validateProfileCurves(profiles, curves);
        validateCurves(curves, maxLevel);
        final Map<Material, String> overrides = materialProfileMap(items.getConfigurationSection("material-overrides"));
        final Map<String, String> aliases = stringMap(items.getConfigurationSection("profile-aliases"));
        validateAliases(profiles, aliases);
        for (final String profileId : overrides.values()) if (!profiles.containsKey(profileId)) throw new IllegalArgumentException("Override references unknown profile: " + profileId);
        for (final String profileId : aliases.values()) if (!profiles.containsKey(profileId)) throw new IllegalArgumentException("Alias references unknown profile: " + profileId);
        final Map<Material, String> materialProfiles = materialProfileIndex(profiles, overrides);
        final YamlConfiguration blocks = yaml("xp/blocks.yml");
        final Map<Material, Long> blockXp = materialLongMap(blocks.getConfigurationSection("blocks"));
        final YamlConfiguration main = yaml("config.yml");
        final long statRevision = statRevision(profiles, materialProfiles, aliases);
        return new GearConfiguration(nextGeneration, statRevision, maxLevel, defaultCurve, curves, profiles, overrides, materialProfiles, aliases, blockXp,
            main.getBoolean("safety.exclude-creative", true));
    }
    private YamlConfiguration yaml(final String name) {
        final YamlConfiguration configuration = new YamlConfiguration();
        try { configuration.load(new File(dataFolder, name)); return configuration; }
        catch (final IOException | InvalidConfigurationException exception) { throw new IllegalArgumentException("Could not load " + name, exception); }
    }
    private static void validateProfileCurves(final Map<String, ItemProfile> profiles, final Map<String, LevelingCurve> curves) {
        for (final ItemProfile profile : profiles.values()) if (!curves.containsKey(profile.curveId())) throw new IllegalArgumentException("Profile " + profile.id() + " references unknown curve " + profile.curveId());
    }
    private static void validateCurves(final Map<String, LevelingCurve> curves, final int maxLevel) {
        final int lastReachableLevel = maxLevel - 1;
        for (final Map.Entry<String, LevelingCurve> entry : curves.entrySet()) {
            try { if (entry.getValue().experienceForNextLevel(lastReachableLevel) <= 0) throw new IllegalArgumentException("Curve " + entry.getKey() + " has non-positive XP at level " + lastReachableLevel); }
            catch (final ArithmeticException exception) { throw new IllegalArgumentException("Curve " + entry.getKey() + " overflows at level " + lastReachableLevel, exception); }
        }
    }
    static Map<Material, String> materialProfileIndex(final Map<String, ItemProfile> profiles, final Map<Material, String> overrides) {
        final Map<Material, List<String>> owners = new HashMap<>();
        for (final ItemProfile profile : profiles.values()) for (final Material material : profile.materials()) owners.computeIfAbsent(material, ignored -> new ArrayList<>()).add(profile.id());
        final Map<Material, String> index = new HashMap<>();
        for (final Map.Entry<Material, List<String>> entry : owners.entrySet()) {
            final String override = overrides.get(entry.getKey());
            if (override != null) index.put(entry.getKey(), override);
            else if (entry.getValue().size() == 1) index.put(entry.getKey(), entry.getValue().getFirst());
            else throw new IllegalArgumentException("Material " + entry.getKey() + " belongs to multiple profiles: " + String.join(", ", entry.getValue()));
        }
        for (final Map.Entry<Material, String> entry : overrides.entrySet()) index.put(entry.getKey(), entry.getValue());
        return index;
    }
    static void validateAliases(final Map<String, ItemProfile> profiles, final Map<String, String> aliases) {
        for (final String legacyId : aliases.keySet()) if (profiles.containsKey(legacyId)) throw new IllegalArgumentException("Alias shadows active profile: " + legacyId);
        for (final String profileId : aliases.values()) if (!profiles.containsKey(profileId)) throw new IllegalArgumentException("Alias references unknown profile: " + profileId);
    }
    static long statRevision(final Map<String, ItemProfile> profiles, final Map<Material, String> materialProfiles, final Map<String, String> aliases) {
        final StringBuilder canonical = new StringBuilder();
        profiles.keySet().stream().sorted().forEach(id -> {
            final ItemProfile profile = profiles.get(id); canonical.append("profile=").append(id).append('|').append(profile.curveId()).append('|');
            profile.materials().stream().map(Material::name).sorted().forEach(material -> canonical.append(material).append(','));
            canonical.append('|');
            profile.statRules().entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
                final StatRule rule = entry.getValue(); canonical.append(entry.getKey()).append(':').append(rule.enabled()).append(':').append(rule.mode()).append(':')
                    .append(Double.toString(rule.perLevel())).append(':').append(Double.toString(rule.cap())).append(';');
            });
            canonical.append('\n');
        });
        materialProfiles.entrySet().stream().sorted(Map.Entry.comparingByKey(java.util.Comparator.comparing(Material::name)))
            .forEach(entry -> canonical.append("material=").append(entry.getKey().name()).append(':').append(entry.getValue()).append('\n'));
        aliases.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> canonical.append("alias=").append(entry.getKey()).append(':').append(entry.getValue()).append('\n'));
        long hash = 0xcbf29ce484222325L;
        for (int i = 0; i < canonical.length(); i++) { hash ^= canonical.charAt(i); hash *= 0x100000001b3L; }
        return hash;
    }
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
