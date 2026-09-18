package dev.s3tupw1zard.gearMastery.stat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AttributeModifierValueTest {
    @Test void acceptsFinitePositiveAndNegativeDeltasAndRejectsNonFiniteValues() {
        assertEquals(2.5D, AttributeModifierValue.requireFinite(2.5D, StatType.ARMOR));
        assertEquals(-2.5D, AttributeModifierValue.requireFinite(-2.5D, StatType.ATTACK_SPEED));
        assertThrows(IllegalArgumentException.class, () -> AttributeModifierValue.requireFinite(Double.NaN, StatType.ARMOR));
        assertThrows(IllegalArgumentException.class, () -> AttributeModifierValue.requireFinite(Double.POSITIVE_INFINITY, StatType.ARMOR));
        assertThrows(IllegalArgumentException.class, () -> AttributeModifierValue.requireFinite(Double.NEGATIVE_INFINITY, StatType.ATTACK_SPEED));
    }
    @Test void rejectsOverflowFromOtherwiseFiniteScalingInputs() {
        final StatRule rule = new StatRule(true, StatScaleMode.MULTIPLICATIVE, Double.MAX_VALUE, Double.MAX_VALUE);
        final double overflow = StatDeltaCalculator.delta(2.0D, 2, rule);
        assertThrows(IllegalArgumentException.class, () -> AttributeModifierValue.requireFinite(overflow, StatType.ATTACK_DAMAGE));
    }
}
