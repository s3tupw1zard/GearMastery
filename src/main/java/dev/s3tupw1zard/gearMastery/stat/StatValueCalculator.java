package dev.s3tupw1zard.gearMastery.stat;

/** Pure scaling calculation; concrete handlers decide how to apply the result in Paper. */
public final class StatValueCalculator {
    private StatValueCalculator() { }
    public static double calculate(final double base, final int level, final StatRule rule) {
        if (!rule.enabled()) return base;
        if (rule.mode() == StatScaleMode.ADDITIVE) {
            return Math.min(base + (rule.perLevel() * Math.max(0, level)), rule.cap());
        }
        final double factor = Math.min(1.0D + (rule.perLevel() * Math.max(0, level)), rule.cap());
        return base * factor;
    }
}
