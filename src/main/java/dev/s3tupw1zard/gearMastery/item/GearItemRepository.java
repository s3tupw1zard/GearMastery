package dev.s3tupw1zard.gearMastery.item;

import dev.s3tupw1zard.gearMastery.data.GearItemData;
import dev.s3tupw1zard.gearMastery.stat.StatType;
import io.papermc.paper.persistence.PersistentDataContainerView;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.Optional;
import java.util.UUID;

/** Owns the stable PDC schema for GearMastery item data. */
public final class GearItemRepository {
    public static final int CURRENT_SCHEMA = 1;
    private final NamespacedKey schema, id, profile, level, experience, lifetimeExperience, appliedLevel, appliedGeneration, appliedRevision, appliedSchema, baselineMaxDamage, baselineAttackSpeedEffective, baselineToolDefaultSpeed, baselineToolRuleCount, toolRuleSnapshots;
    public GearItemRepository(final Plugin plugin) {
        schema = new NamespacedKey(plugin, "schema"); id = new NamespacedKey(plugin, "gear-id");
        profile = new NamespacedKey(plugin, "profile"); level = new NamespacedKey(plugin, "level");
        experience = new NamespacedKey(plugin, "experience"); lifetimeExperience = new NamespacedKey(plugin, "lifetime-experience");
        appliedLevel = new NamespacedKey(plugin, "applied-level"); appliedGeneration = new NamespacedKey(plugin, "applied-generation"); appliedRevision = new NamespacedKey(plugin, "applied-stat-revision");
        appliedSchema = new NamespacedKey(plugin, "applied-stat-schema"); baselineMaxDamage = new NamespacedKey(plugin, "baseline-max-damage"); baselineAttackSpeedEffective = new NamespacedKey(plugin, "baseline-attack-speed-effective");
        baselineToolDefaultSpeed = new NamespacedKey(plugin, "baseline-tool-default-speed"); baselineToolRuleCount = new NamespacedKey(plugin, "baseline-tool-rule-count"); toolRuleSnapshots = new NamespacedKey(plugin, "tool-rule-snapshots");
    }
    public Optional<GearItemData> read(final ItemStack item) {
        final PersistentDataContainerView pdc = item.getPersistentDataContainer();
        final String uuid = pdc.get(id, PersistentDataType.STRING); final String profileId = pdc.get(profile, PersistentDataType.STRING);
        if (uuid == null || profileId == null) return Optional.empty();
        try {
            return Optional.of(new GearItemData(pdc.getOrDefault(schema, PersistentDataType.INTEGER, CURRENT_SCHEMA), UUID.fromString(uuid), profileId,
                pdc.getOrDefault(level, PersistentDataType.INTEGER, 0), pdc.getOrDefault(experience, PersistentDataType.LONG, 0L),
                pdc.getOrDefault(lifetimeExperience, PersistentDataType.LONG, 0L)));
        } catch (final IllegalArgumentException invalidId) { return Optional.empty(); }
    }
    public void write(final ItemStack item, final GearItemData data) {
        item.editPersistentDataContainer(pdc -> {
            pdc.set(schema, PersistentDataType.INTEGER, data.schemaVersion()); pdc.set(id, PersistentDataType.STRING, data.gearId().toString());
            pdc.set(profile, PersistentDataType.STRING, data.profileId()); pdc.set(level, PersistentDataType.INTEGER, data.level());
            pdc.set(experience, PersistentDataType.LONG, data.experience()); pdc.set(lifetimeExperience, PersistentDataType.LONG, data.lifetimeExperience());
        });
    }
    public Optional<Double> baseline(final ItemStack item, final StatType type) { return optional(item, baselineKey(type), PersistentDataType.DOUBLE); }
    public void baseline(final ItemStack item, final StatType type, final double value) { set(item, baselineKey(type), PersistentDataType.DOUBLE, value); }
    public Optional<Integer> baselineMaxDamage(final ItemStack item) { return optional(item, baselineMaxDamage, PersistentDataType.INTEGER); }
    public void baselineMaxDamage(final ItemStack item, final int value) { set(item, baselineMaxDamage, PersistentDataType.INTEGER, value); }
    public Optional<Double> baselineAttackSpeedEffective(final ItemStack item) { return optional(item, baselineAttackSpeedEffective, PersistentDataType.DOUBLE); }
    public void baselineAttackSpeedEffective(final ItemStack item, final double value) { set(item, baselineAttackSpeedEffective, PersistentDataType.DOUBLE, value); }
    public Optional<Float> baselineToolDefaultSpeed(final ItemStack item) { return optional(item, baselineToolDefaultSpeed, PersistentDataType.FLOAT); }
    public void baselineToolDefaultSpeed(final ItemStack item, final float value) { set(item, baselineToolDefaultSpeed, PersistentDataType.FLOAT, value); }
    public Optional<Integer> baselineToolRuleCount(final ItemStack item) { return optional(item, baselineToolRuleCount, PersistentDataType.INTEGER); }
    public void baselineToolRuleCount(final ItemStack item, final int value) { set(item, baselineToolRuleCount, PersistentDataType.INTEGER, value); }
    public Optional<Float> baselineToolRuleSpeed(final ItemStack item, final String identity) { return optional(item, toolRuleKey("baseline-tool-rule-", identity), PersistentDataType.FLOAT); }
    public void baselineToolRuleSpeed(final ItemStack item, final String identity, final float value) { set(item, toolRuleKey("baseline-tool-rule-", identity), PersistentDataType.FLOAT, value); }
    public Optional<Float> expectedToolRuleSpeed(final ItemStack item, final String identity) { return optional(item, toolRuleKey("applied-tool-rule-", identity), PersistentDataType.FLOAT); }
    public void expectedToolRuleSpeed(final ItemStack item, final String identity, final float value) { set(item, toolRuleKey("applied-tool-rule-", identity), PersistentDataType.FLOAT, value); }
    public Optional<Float> expectedToolDefaultSpeed(final ItemStack item) { return optional(item, new NamespacedKey("gearmastery", "applied-tool-default-speed"), PersistentDataType.FLOAT); }
    public void expectedToolDefaultSpeed(final ItemStack item, final float value) { set(item, new NamespacedKey("gearmastery", "applied-tool-default-speed"), PersistentDataType.FLOAT, value); }
    public Optional<String> toolRuleSnapshots(final ItemStack item) { return optional(item, toolRuleSnapshots, PersistentDataType.STRING); }
    public void toolRuleSnapshots(final ItemStack item, final String value) { set(item, toolRuleSnapshots, PersistentDataType.STRING, value); }
    public Optional<Float> baselineToolRuleSpeed(final ItemStack item, final int index) { return optional(item, new NamespacedKey("gearmastery", "baseline-tool-rule-" + index), PersistentDataType.FLOAT); }
    public void baselineToolRuleSpeed(final ItemStack item, final int index, final float value) { set(item, new NamespacedKey("gearmastery", "baseline-tool-rule-" + index), PersistentDataType.FLOAT, value); }
    public boolean isStatApplicationCurrent(final ItemStack item, final int currentLevel, final long revision, final int statSchema) {
        final PersistentDataContainerView pdc = item.getPersistentDataContainer();
        return pdc.getOrDefault(appliedLevel, PersistentDataType.INTEGER, -1) == currentLevel && pdc.getOrDefault(appliedRevision, PersistentDataType.LONG, Long.MIN_VALUE) == revision && pdc.getOrDefault(appliedSchema, PersistentDataType.INTEGER, -1) == statSchema;
    }
    public void markStatApplication(final ItemStack item, final int currentLevel, final long revision, final int statSchema) {
        item.editPersistentDataContainer(pdc -> { pdc.set(appliedLevel, PersistentDataType.INTEGER, currentLevel); pdc.set(appliedRevision, PersistentDataType.LONG, revision); pdc.set(appliedSchema, PersistentDataType.INTEGER, statSchema); pdc.remove(appliedGeneration); });
    }
    private NamespacedKey baselineKey(final StatType type) { return new NamespacedKey("gearmastery", "baseline-" + type.name().toLowerCase(java.util.Locale.ROOT)); }
    private static NamespacedKey toolRuleKey(final String prefix, final String identity) {
        long hash = 0xcbf29ce484222325L;
        for (int i = 0; i < identity.length(); i++) { hash ^= identity.charAt(i); hash *= 0x100000001b3L; }
        return new NamespacedKey("gearmastery", prefix + Long.toUnsignedString(hash, 36));
    }
    private static <T, Z> Optional<Z> optional(final ItemStack item, final NamespacedKey key, final PersistentDataType<T, Z> type) { return Optional.ofNullable(item.getPersistentDataContainer().get(key, type)); }
    private static <T, Z> void set(final ItemStack item, final NamespacedKey key, final PersistentDataType<T, Z> type, final Z value) { item.editPersistentDataContainer(pdc -> pdc.set(key, type, value)); }
}
