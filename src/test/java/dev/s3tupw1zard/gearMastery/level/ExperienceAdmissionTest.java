package dev.s3tupw1zard.gearMastery.level;

import dev.s3tupw1zard.gearMastery.config.GearConfiguration;
import dev.s3tupw1zard.gearMastery.config.ItemProfile;
import dev.s3tupw1zard.gearMastery.level.LinearLevelingCurve;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExperienceAdmissionTest {
    @Test void uninitializedItemsAreOnlyAdmittedBeforeTheGainEventAcceptsThem() {
        final ItemProfile profile = new ItemProfile("pickaxes", Set.of(Material.DIAMOND_PICKAXE), "standard", Set.of("block_break"), Map.of());
        final GearConfiguration configuration = new GearConfiguration(1, 100, "standard", Map.of("standard", new LinearLevelingCurve(10, 0)),
            Map.of("pickaxes", profile), Map.of(), Map.of(Material.DIAMOND_PICKAXE, "pickaxes"), Map.of(), Map.of(), true);

        assertTrue(ExperienceAdmission.resolve(configuration, java.util.Optional.empty(), Material.DIAMOND_PICKAXE, "block_break").isPresent());
        assertFalse(ExperienceAdmission.shouldMutate(true, 25));
        assertFalse(ExperienceAdmission.shouldMutate(false, 0));
        assertFalse(ExperienceAdmission.shouldMutate(false, -1));
        assertTrue(ExperienceAdmission.shouldMutate(false, 25));
    }
}
