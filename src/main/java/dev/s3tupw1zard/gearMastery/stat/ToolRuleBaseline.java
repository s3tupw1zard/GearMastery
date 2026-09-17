package dev.s3tupw1zard.gearMastery.stat;

/** Pure reconciliation rule for externally changed tool speeds. */
final class ToolRuleBaseline {
    private ToolRuleBaseline() { }
    static Float select(final Float visibleSpeed, final Float capturedBaseline, final Float expectedAppliedSpeed) {
        if (capturedBaseline != null && java.util.Objects.equals(visibleSpeed, expectedAppliedSpeed)) return capturedBaseline;
        return visibleSpeed;
    }
    static Float selectStructural(final Float visibleSpeed, final java.util.List<Snapshot> snapshots) {
        if (visibleSpeed == null) return null;
        final java.util.List<Snapshot> matches = snapshots.stream().filter(snapshot -> Float.compare(snapshot.expectedSpeed(), visibleSpeed) == 0).toList();
        return matches.size() == 1 ? matches.getFirst().baselineSpeed() : visibleSpeed;
    }
    record Snapshot(String identity, float baselineSpeed, float expectedSpeed) { }
}
