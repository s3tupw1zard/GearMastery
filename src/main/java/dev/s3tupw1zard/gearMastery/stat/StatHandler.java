package dev.s3tupw1zard.gearMastery.stat;

/** Applies one supported statistic to the concrete item passed by the central service. */
public interface StatHandler {
    StatType type();
    void apply(StatApplicationContext context);
}
