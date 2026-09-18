package dev.s3tupw1zard.gearMastery.stat;

/** Validates the finite non-negative float values accepted by the native TOOL component. */
final class ToolSpeedValue {
    private ToolSpeedValue() { }
    static float requireValid(final double value, final String context) {
        if (!Double.isFinite(value) || value < 0.0D || value > Float.MAX_VALUE) {
            throw new IllegalArgumentException("Invalid tool speed for " + context + ": " + value);
        }
        return (float) value;
    }
}
