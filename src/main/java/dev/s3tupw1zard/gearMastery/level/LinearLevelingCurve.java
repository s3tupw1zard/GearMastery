package dev.s3tupw1zard.gearMastery.level;

public record LinearLevelingCurve(long baseExperience, long growthPerLevel) implements LevelingCurve {
    public LinearLevelingCurve {
        if (baseExperience <= 0 || growthPerLevel < 0) throw new IllegalArgumentException("Invalid linear curve");
    }
    @Override public long experienceForNextLevel(final int currentLevel) {
        return Math.addExact(baseExperience, Math.multiplyExact(growthPerLevel, Math.max(0, currentLevel)));
    }
}
