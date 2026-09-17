package dev.s3tupw1zard.gearMastery.stat;

/** Validates a calculated MAX_DAMAGE value before narrowing it for the data component. */
final class DurabilityValue {
    private DurabilityValue() { }
    static int toMaxDamage(final double value) {
        if (!Double.isFinite(value) || value < 1.0D || value > Integer.MAX_VALUE) throw new IllegalArgumentException("Calculated durability is outside the supported MAX_DAMAGE range: " + value);
        final long rounded = Math.round(value);
        if (rounded < 1L || rounded > Integer.MAX_VALUE) throw new IllegalArgumentException("Calculated durability is outside the supported MAX_DAMAGE range: " + value);
        return (int) rounded;
    }
}
