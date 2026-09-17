package dev.s3tupw1zard.gearMastery.config;

import dev.s3tupw1zard.gearMastery.level.LevelingCurve;
import org.bukkit.Material;

import java.util.Map;
import java.util.Optional;

public record GearConfiguration(long generation, int maxLevel, String defaultCurveId, Map<String, LevelingCurve> curves,
                                Map<String, ItemProfile> profiles, Map<Material, String> materialOverrides,
                                Map<String, String> profileAliases, Map<Material, Long> blockExperience,
                                boolean excludeCreative) {
    public GearConfiguration {
        curves = Map.copyOf(curves); profiles = Map.copyOf(profiles); materialOverrides = Map.copyOf(materialOverrides);
        profileAliases = Map.copyOf(profileAliases); blockExperience = Map.copyOf(blockExperience);
    }
    public Optional<ItemProfile> findProfile(final String id) {
        String resolved = id;
        for (int i = 0; i < 16 && profileAliases.containsKey(resolved); i++) resolved = profileAliases.get(resolved);
        return Optional.ofNullable(profiles.get(resolved));
    }
    public Optional<ItemProfile> profileFor(final Material material) {
        final String override = materialOverrides.get(material);
        if (override != null) return findProfile(override);
        return profiles.values().stream().filter(profile -> profile.materials().contains(material)).findFirst();
    }
    public LevelingCurve curveFor(final ItemProfile profile) {
        final LevelingCurve curve = curves.get(profile.curveId());
        if (curve == null) throw new IllegalStateException("Unknown curve " + profile.curveId());
        return curve;
    }
}
