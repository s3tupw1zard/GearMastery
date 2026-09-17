package dev.s3tupw1zard.gearMastery.level;

/** Calculates the XP needed to advance from a level to the next one. */
@FunctionalInterface
public interface LevelingCurve {
    long experienceForNextLevel(int currentLevel);
}
