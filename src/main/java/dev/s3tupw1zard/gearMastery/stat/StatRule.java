package dev.s3tupw1zard.gearMastery.stat;

public record StatRule(boolean enabled, StatScaleMode mode, double perLevel, double cap) {
    public StatRule {
        if (perLevel < 0 || cap < 0) throw new IllegalArgumentException("Stat values must be non-negative");
    }
}
