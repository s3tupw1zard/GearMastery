package dev.s3tupw1zard.gearMastery.stat;

/** Rejects values Paper cannot represent in a native attribute modifier. */
final class AttributeModifierValue {
    private AttributeModifierValue() { }
    static double requireFinite(final double value, final StatType type) {
        if (!Double.isFinite(value)) throw new IllegalArgumentException("Invalid non-finite attribute delta for " + type + ": " + value);
        return value;
    }
}
