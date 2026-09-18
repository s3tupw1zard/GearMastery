package dev.s3tupw1zard.gearMastery.stat;

/** Calculates the additive native modifier required to reach a scaled baseline. */
public final class StatDeltaCalculator {
    private StatDeltaCalculator() { }
    public static double delta(final double baseline, final int level, final StatRule rule) {
        return StatValueCalculator.calculate(baseline, level, rule) - baseline;
    }
}
