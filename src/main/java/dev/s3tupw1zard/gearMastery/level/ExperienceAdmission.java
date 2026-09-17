package dev.s3tupw1zard.gearMastery.level;

import dev.s3tupw1zard.gearMastery.config.GearConfiguration;
import dev.s3tupw1zard.gearMastery.config.ItemProfile;
import dev.s3tupw1zard.gearMastery.data.GearItemData;
import org.bukkit.Material;

import java.util.Optional;

/** Resolves whether an XP attempt can proceed without mutating the item. */
record ExperienceAdmission(Optional<GearItemData> existingData, ItemProfile profile) {
    static Optional<ExperienceAdmission> resolve(final GearConfiguration configuration, final Optional<GearItemData> existingData,
                                                 final Material material, final String sourceId) {
        final Optional<ItemProfile> profile = existingData.isPresent()
            ? configuration.findProfile(existingData.get().profileId())
            : configuration.profileFor(material);
        if (profile.isEmpty() || (!"admin".equals(sourceId) && !profile.get().supportsSource(sourceId))) return Optional.empty();
        return Optional.of(new ExperienceAdmission(existingData, profile.get()));
    }

    static boolean shouldMutate(final boolean cancelled, final long amount) {
        return !cancelled && amount > 0;
    }
}
