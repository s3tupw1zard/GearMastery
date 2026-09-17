package dev.s3tupw1zard.gearMastery.level;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LevelingCurveTest {
    @Test void linearCurveIncreasesByConfiguredGrowth() {
        final LevelingCurve curve = new LinearLevelingCurve(100, 25);
        assertEquals(100, curve.experienceForNextLevel(0));
        assertEquals(175, curve.experienceForNextLevel(3));
    }
    @Test void quadraticCurveUsesCurrentLevelSquared() {
        final LevelingCurve curve = new QuadraticLevelingCurve(100, 10);
        assertEquals(100, curve.experienceForNextLevel(0));
        assertEquals(190, curve.experienceForNextLevel(3));
    }
    @Test void exponentialCurveRoundsAndGrows() {
        final LevelingCurve curve = new ExponentialLevelingCurve(100, 1.5D);
        assertEquals(100, curve.experienceForNextLevel(0));
        assertEquals(225, curve.experienceForNextLevel(2));
    }
}
