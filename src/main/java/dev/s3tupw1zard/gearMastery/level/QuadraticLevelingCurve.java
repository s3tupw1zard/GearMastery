package dev.s3tupw1zard.gearMastery.level;

public record QuadraticLevelingCurve(long baseExperience, long growth) implements LevelingCurve {
    public QuadraticLevelingCurve {
        if (baseExperience <= 0 || growth < 0) throw new IllegalArgumentException("Invalid quadratic curve");
    }
    @Override public long experienceForNextLevel(final int currentLevel) {
        final long level = Math.max(0, currentLevel);
        return Math.addExact(baseExperience, Math.multiplyExact(growth, Math.multiplyExact(level, level)));
    }
}
