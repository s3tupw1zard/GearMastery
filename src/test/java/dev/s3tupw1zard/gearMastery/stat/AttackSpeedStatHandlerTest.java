package dev.s3tupw1zard.gearMastery.stat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AttackSpeedStatHandlerTest {
    @Test void multiplicativeScalingImprovesVanillaEffectiveSwordSpeed() {
        final StatRule rule = new StatRule(true, StatScaleMode.MULTIPLICATIVE, 0.01D, 2.0D);
        final double swordEffectiveSpeed = AttackSpeedStatHandler.VANILLA_PLAYER_BASE_ATTACK_SPEED - 2.4D;
        final double bonus = AttackSpeedStatHandler.effectiveBonus(swordEffectiveSpeed, 10, rule);
        assertEquals(1.76D, swordEffectiveSpeed + bonus, 0.0000001D);
        assertEquals(0.16D, bonus, 0.0000001D);
    }

    @Test void additiveAndDisabledRulesKeepTheExpectedEffectiveSemantics() {
        assertEquals(0.5D, AttackSpeedStatHandler.effectiveBonus(2.0D, 5, new StatRule(true, StatScaleMode.ADDITIVE, 0.1D, 3.0D)));
        assertEquals(0.0D, AttackSpeedStatHandler.effectiveBonus(1.6D, 10, new StatRule(false, StatScaleMode.MULTIPLICATIVE, 0.01D, 2.0D)));
    }
}
