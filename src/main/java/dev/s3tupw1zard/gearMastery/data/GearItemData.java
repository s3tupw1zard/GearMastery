package dev.s3tupw1zard.gearMastery.data;

import java.util.UUID;

/** Persistent, per-item GearMastery state. */
public record GearItemData(int schemaVersion, UUID gearId, String profileId, int level, long experience,
                           long lifetimeExperience) {
    public GearItemData {
        if (schemaVersion < 1 || level < 0 || experience < 0 || lifetimeExperience < 0) {
            throw new IllegalArgumentException("Invalid GearMastery item data");
        }
    }
}
