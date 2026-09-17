package dev.s3tupw1zard.gearMastery.stat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ToolRuleBaselineTest {
    @Test void unchangedGearMasteryRuleUsesItsCapturedBaseline() {
        assertEquals(8.0F, ToolRuleBaseline.select(10.0F, 8.0F, 10.0F));
    }
    @Test void insertedOrReorderedRulesCannotReceiveAnotherRulesBaseline() {
        assertEquals(20.0F, ToolRuleBaseline.select(20.0F, null, null));
        assertEquals(6.0F, ToolRuleBaseline.select(7.5F, 6.0F, 7.5F));
    }
    @Test void externallyChangedSpeedIsRecapturedInsteadOfRescaled() {
        assertEquals(13.0F, ToolRuleBaseline.select(13.0F, 8.0F, 10.0F));
        assertNull(ToolRuleBaseline.select(null, null, null));
    }
    @Test void uniquePreviousAppliedSpeedSurvivesAStructuralChangeWithoutDoubleScaling() {
        final var previous = java.util.List.of(new ToolRuleBaseline.Snapshot("stone", 8.0F, 10.0F));
        assertEquals(8.0F, ToolRuleBaseline.selectStructural(10.0F, previous));
        assertEquals(15.0F, ToolRuleBaseline.selectStructural(15.0F, previous));
    }
}
