package dev.s3tupw1zard.gearMastery.stat;

import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AttributeSlotMatcherTest {
    @Test void followsPaperSlotGroupPredicateSemantics() {
        assertTrue(AttributeSlotMatcher.appliesTo(EquipmentSlotGroup.MAINHAND, EquipmentSlot.HAND));
        assertTrue(AttributeSlotMatcher.appliesTo(EquipmentSlotGroup.HAND, EquipmentSlot.HAND));
        assertTrue(AttributeSlotMatcher.appliesTo(EquipmentSlotGroup.ANY, EquipmentSlot.HAND));
        assertTrue(AttributeSlotMatcher.appliesTo(EquipmentSlotGroup.ARMOR, EquipmentSlot.CHEST));
        assertFalse(AttributeSlotMatcher.appliesTo(EquipmentSlotGroup.OFFHAND, EquipmentSlot.HAND));
        assertFalse(AttributeSlotMatcher.appliesTo(EquipmentSlotGroup.HEAD, EquipmentSlot.CHEST));
    }
    @Test void actualEquippableSlotsAreMappedWithoutMaterialNameHeuristics() {
        assertEquals(org.bukkit.inventory.EquipmentSlot.CHEST, AttributeSlotMatcher.targetSlot(org.bukkit.inventory.EquipmentSlot.CHEST).orElseThrow());
        assertEquals(org.bukkit.inventory.EquipmentSlot.HEAD, AttributeSlotMatcher.targetSlot(org.bukkit.inventory.EquipmentSlot.HEAD).orElseThrow());
        assertEquals(org.bukkit.inventory.EquipmentSlot.HAND, AttributeSlotMatcher.targetSlot(org.bukkit.inventory.EquipmentSlot.HAND).orElseThrow());
    }
}
