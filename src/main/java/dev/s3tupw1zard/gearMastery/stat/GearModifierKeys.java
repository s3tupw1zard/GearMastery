package dev.s3tupw1zard.gearMastery.stat;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.EquipmentSlot;

/** Centralizes stable, slot-specific GearMastery modifier ownership keys. */
final class GearModifierKeys {
    private static final String NAMESPACE = "gearmastery";
    private GearModifierKeys() { }
    static NamespacedKey current(final StatType type, final EquipmentSlot slot) {
        final String suffix = switch (slot) {
            case HEAD -> "head"; case CHEST -> "chest"; case LEGS -> "legs"; case FEET -> "feet"; default -> "mainhand";
        };
        return new NamespacedKey(NAMESPACE, type.name().toLowerCase(java.util.Locale.ROOT) + '_' + suffix);
    }
    static NamespacedKey legacy(final StatType type) { return new NamespacedKey(NAMESPACE, type.name().toLowerCase(java.util.Locale.ROOT)); }
}
