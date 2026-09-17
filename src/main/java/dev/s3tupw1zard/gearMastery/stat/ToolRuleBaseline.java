package dev.s3tupw1zard.gearMastery.stat;

/** Pure reconciliation rule for externally changed tool speeds. */
final class ToolRuleBaseline {
    private ToolRuleBaseline() { }
    static Float select(final Float visibleSpeed, final Float capturedBaseline, final Float expectedAppliedSpeed) {
        if (capturedBaseline != null && java.util.Objects.equals(visibleSpeed, expectedAppliedSpeed)) return capturedBaseline;
        return visibleSpeed;
    }
}
