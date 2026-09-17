package dev.s3tupw1zard.gearMastery.stat;

import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AttributeSlotMatcherTest {
    @Test void followsPaperSlotGroupPredicateSemantics() {
        assertTrue(AttributeSlotMatcher.appliesTo(EquipmentSlotGroup.MAINHAND, EquipmentSlot.HAND));
        assertTrue(AttributeSlotMatcher.appliesTo(EquipmentSlotGroup.HAND, EquipmentSlot.HAND));
        assertTrue(AttributeSlotMatcher.appliesTo(EquipmentSlotGroup.ANY, EquipmentSlot.HAND));
        assertTrue(AttributeSlotMatcher.appliesTo(EquipmentSlotGroup.ARMOR, EquipmentSlot.CHEST));
        assertFalse(AttributeSlotMatcher.appliesTo(EquipmentSlotGroup.OFFHAND, EquipmentSlot.HAND));
        assertFalse(AttributeSlotMatcher.appliesTo(EquipmentSlotGroup.HEAD, EquipmentSlot.CHEST));
    }
}
