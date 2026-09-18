package dev.s3tupw1zard.gearMastery.stat;

/** Defines the level used by all runtime stat calculations. */
public final class EffectiveStatLevel {
    private EffectiveStatLevel() { }

    public static int of(final int storedLevel, final int configuredMaximum) { return Math.min(storedLevel, configuredMaximum); }
}
