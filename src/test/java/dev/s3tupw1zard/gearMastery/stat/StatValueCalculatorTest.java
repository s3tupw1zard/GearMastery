package dev.s3tupw1zard.gearMastery.stat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StatValueCalculatorTest {
    @Test void additiveRuleCapsIndependentOfLevel() {
        final StatRule rule = new StatRule(true, StatScaleMode.ADDITIVE, 2.0D, 15.0D);
        assertEquals(14.0D, StatValueCalculator.calculate(4.0D, 5, rule));
        assertEquals(15.0D, StatValueCalculator.calculate(4.0D, 100, rule));
    }
    @Test void multiplicativeRuleCapsTheFactor() {
        final StatRule rule = new StatRule(true, StatScaleMode.MULTIPLICATIVE, 0.01D, 2.0D);
        assertEquals(120.0D, StatValueCalculator.calculate(100.0D, 20, rule));
        assertEquals(200.0D, StatValueCalculator.calculate(100.0D, 500, rule));
    }
    @Test void disabledRuleLeavesBaseUntouched() {
        assertEquals(7.0D, StatValueCalculator.calculate(7.0D, 100, new StatRule(false, StatScaleMode.ADDITIVE, 10.0D, 1.0D)));
    }
}
