package dev.s3tupw1zard.gearMastery.level;

public record ExponentialLevelingCurve(long baseExperience, double growth) implements LevelingCurve {
    public ExponentialLevelingCurve {
        if (baseExperience <= 0 || growth < 1.0D) throw new IllegalArgumentException("Invalid exponential curve");
    }
    @Override public long experienceForNextLevel(final int currentLevel) {
        final double value = baseExperience * Math.pow(growth, Math.max(0, currentLevel));
        return value >= Long.MAX_VALUE ? Long.MAX_VALUE : Math.max(1L, Math.round(value));
    }
}
