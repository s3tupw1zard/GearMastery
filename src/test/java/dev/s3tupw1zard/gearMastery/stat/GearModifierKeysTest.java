package dev.s3tupw1zard.gearMastery.stat;

import org.bukkit.inventory.EquipmentSlot;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class GearModifierKeysTest {
    @Test void armorKeysAreUniquePerEquipmentSlotAndLegacyKeysRemainRecognizable() {
        final Set<String> armor = Set.of(
            GearModifierKeys.current(StatType.ARMOR, EquipmentSlot.HEAD).getKey(),
            GearModifierKeys.current(StatType.ARMOR, EquipmentSlot.CHEST).getKey(),
            GearModifierKeys.current(StatType.ARMOR, EquipmentSlot.LEGS).getKey(),
            GearModifierKeys.current(StatType.ARMOR, EquipmentSlot.FEET).getKey());
        assertEquals(4, armor.size());
        assertEquals("armor", GearModifierKeys.legacy(StatType.ARMOR).getKey());
        assertEquals("attack_damage_mainhand", GearModifierKeys.current(StatType.ATTACK_DAMAGE, EquipmentSlot.HAND).getKey());
        assertFalse(armor.contains(GearModifierKeys.legacy(StatType.ARMOR).getKey()));
    }
}
