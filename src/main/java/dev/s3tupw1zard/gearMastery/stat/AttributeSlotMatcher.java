package dev.s3tupw1zard.gearMastery.stat;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Equippable;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;

/** Uses Paper's group predicate so aggregate groups such as HAND and ANY are handled correctly. */
final class AttributeSlotMatcher {
    private AttributeSlotMatcher() { }
    static boolean appliesTo(final EquipmentSlotGroup group, final EquipmentSlot slot) { return group.test(slot); }
    static java.util.Optional<EquipmentSlot> targetSlot(final ItemStack item) {
        final Equippable equippable = item.getData(DataComponentTypes.EQUIPPABLE);
        if (equippable == null) return java.util.Optional.of(EquipmentSlot.HAND);
        return targetSlot(equippable.slot());
    }
    static java.util.Optional<EquipmentSlot> targetSlot(final EquipmentSlot slot) {
        return java.util.Optional.of(slot);
    }
    static EquipmentSlotGroup targetGroup(final EquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> EquipmentSlotGroup.HEAD; case CHEST -> EquipmentSlotGroup.CHEST; case LEGS -> EquipmentSlotGroup.LEGS;
            case FEET -> EquipmentSlotGroup.FEET; case OFF_HAND -> EquipmentSlotGroup.OFFHAND; case BODY -> EquipmentSlotGroup.BODY; case SADDLE -> EquipmentSlotGroup.SADDLE; case HAND -> EquipmentSlotGroup.MAINHAND;
        };
    }
}
