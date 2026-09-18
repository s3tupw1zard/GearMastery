package dev.s3tupw1zard.gearMastery.stat;

/** Preserves relative remaining durability while a max-damage value changes. */
public final class DurabilityRescaler {
    private DurabilityRescaler() { }
    public static int rescaleDamage(final int oldMaximum, final int oldDamage, final int newMaximum) {
        if (oldMaximum <= 0 || newMaximum <= 0) return 0;
        final double remaining = Math.clamp((oldMaximum - Math.clamp(oldDamage, 0, oldMaximum)) / (double) oldMaximum, 0.0D, 1.0D);
        return Math.clamp((int) Math.round(newMaximum * (1.0D - remaining)), 0, Math.max(0, newMaximum - 1));
    }
}
