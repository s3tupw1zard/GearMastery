package dev.s3tupw1zard.gearMastery.stat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RuntimeStatMathTest {
    @Test void deltaUsesStableBaselineInsteadOfPreviousAppliedValue() {
        final StatRule rule = new StatRule(true, StatScaleMode.MULTIPLICATIVE, 0.01D, 2.0D);
        assertEquals(2.0D, StatDeltaCalculator.delta(10.0D, 20, rule));
        assertEquals(2.0D, StatDeltaCalculator.delta(10.0D, 20, rule));
    }
    @Test void additiveAndMultiplicativeDeltasRespectCaps() {
        assertEquals(4.0D, StatDeltaCalculator.delta(6.0D, 10, new StatRule(true, StatScaleMode.ADDITIVE, 1.0D, 10.0D)));
        assertEquals(10.0D, StatDeltaCalculator.delta(10.0D, 100, new StatRule(true, StatScaleMode.MULTIPLICATIVE, 0.01D, 2.0D)));
    }
    @Test void multiplicativeZeroBaselineDoesNotInventAValue() {
        assertEquals(0.0D, StatDeltaCalculator.delta(0.0D, 100, new StatRule(true, StatScaleMode.MULTIPLICATIVE, 0.01D, 2.0D)));
    }
    @Test void durabilityRescalePreservesRelativeRemainingState() {
        assertEquals(750, DurabilityRescaler.rescaleDamage(1000, 500, 1500));
        assertEquals(1, DurabilityRescaler.rescaleDamage(3, 1, 2));
        assertEquals(0, DurabilityRescaler.rescaleDamage(1000, 0, 1500));
    }
    @Test void statRulesRejectNonFiniteConfigurationValues() {
        assertThrows(IllegalArgumentException.class, () -> new StatRule(true, StatScaleMode.ADDITIVE, Double.NaN, 1.0D));
        assertThrows(IllegalArgumentException.class, () -> new StatRule(true, StatScaleMode.ADDITIVE, 1.0D, Double.POSITIVE_INFINITY));
    }
}
