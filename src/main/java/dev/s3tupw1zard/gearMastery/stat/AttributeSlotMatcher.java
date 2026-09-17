package dev.s3tupw1zard.gearMastery.stat;

import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.EquipmentSlotGroup;

/** Uses Paper's group predicate so aggregate groups such as HAND and ANY are handled correctly. */
final class AttributeSlotMatcher {
    private AttributeSlotMatcher() { }
    static boolean appliesTo(final EquipmentSlotGroup group, final EquipmentSlot slot) { return group.test(slot); }
    static EquipmentSlot targetSlot(final String materialName) {
        if (materialName.endsWith("_HELMET")) return EquipmentSlot.HEAD;
        if (materialName.endsWith("_CHESTPLATE")) return EquipmentSlot.CHEST;
        if (materialName.endsWith("_LEGGINGS")) return EquipmentSlot.LEGS;
        if (materialName.endsWith("_BOOTS")) return EquipmentSlot.FEET;
        return EquipmentSlot.HAND;
    }
    static EquipmentSlotGroup targetGroup(final String materialName) {
        return switch (targetSlot(materialName)) {
            case HEAD -> EquipmentSlotGroup.HEAD; case CHEST -> EquipmentSlotGroup.CHEST; case LEGS -> EquipmentSlotGroup.LEGS;
            case FEET -> EquipmentSlotGroup.FEET; default -> EquipmentSlotGroup.MAINHAND;
        };
    }
}
