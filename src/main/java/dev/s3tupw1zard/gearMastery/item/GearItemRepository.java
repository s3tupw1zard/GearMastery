package dev.s3tupw1zard.gearMastery.item;

import dev.s3tupw1zard.gearMastery.data.GearItemData;
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
    private final NamespacedKey schema, id, profile, level, experience, lifetimeExperience;
    public GearItemRepository(final Plugin plugin) {
        schema = new NamespacedKey(plugin, "schema"); id = new NamespacedKey(plugin, "gear-id");
        profile = new NamespacedKey(plugin, "profile"); level = new NamespacedKey(plugin, "level");
        experience = new NamespacedKey(plugin, "experience"); lifetimeExperience = new NamespacedKey(plugin, "lifetime-experience");
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
}
