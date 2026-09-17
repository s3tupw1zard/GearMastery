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
    @Test void snapshotSerializationPreservesDuplicateIdentitiesAndOrder() {
        final var original = java.util.List.of(new ToolRuleBaseline.Snapshot("stone|true", 8.0F, 10.0F), new ToolRuleBaseline.Snapshot("stone|true", 12.0F, 15.0F));
        assertEquals(original, MiningSpeedStatHandler.snapshots(MiningSpeedStatHandler.serialize(original)));
    }
    @Test void structuralMatchingDoesNotReuseAnAmbiguousDuplicate() {
        final var duplicate = java.util.List.of(new ToolRuleBaseline.Snapshot("a", 8.0F, 10.0F), new ToolRuleBaseline.Snapshot("a", 12.0F, 10.0F));
        assertEquals(10.0F, ToolRuleBaseline.selectStructural(10.0F, duplicate));
    }
    @Test void identicalExpectedDuplicateSnapshotsAreConsumedByOccurrence() {
        final var prior = java.util.List.of(new ToolRuleBaseline.Snapshot("stone|true", 2.0F, 2.2F), new ToolRuleBaseline.Snapshot("stone|true", 2.0F, 2.2F), new ToolRuleBaseline.Snapshot("stone|true", 2.0F, 2.2F));
        final var consumed = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<ToolRuleBaseline.Snapshot, Boolean>());
        for (int occurrence = 0; occurrence < 3; occurrence++) {
            final ToolRuleBaseline.Snapshot matched = MiningSpeedStatHandler.match("stone|true", 2.2F, prior, consumed);
            assertEquals(2.0F, ToolRuleBaseline.select(2.2F, matched.baselineSpeed(), matched.expectedSpeed()));
        }
        assertEquals(3, consumed.size());
    }
    @Test void consumedExactSnapshotCannotBeUsedByStructuralFallback() {
        final var prior = java.util.List.of(new ToolRuleBaseline.Snapshot("a", 8.0F, 10.0F));
        final var consumed = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<ToolRuleBaseline.Snapshot, Boolean>());
        assertEquals(8.0F, MiningSpeedStatHandler.match("a", 10.0F, prior, consumed).baselineSpeed());
        assertNull(ToolRuleBaseline.structuralMatch(10.0F, prior.stream().filter(snapshot -> !consumed.contains(snapshot)).toList()));
    }
}
