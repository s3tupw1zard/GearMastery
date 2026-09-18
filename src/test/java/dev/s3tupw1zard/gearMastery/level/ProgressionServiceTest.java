package dev.s3tupw1zard.gearMastery.level;

import dev.s3tupw1zard.gearMastery.config.GearConfiguration;
import dev.s3tupw1zard.gearMastery.config.ItemProfile;
import dev.s3tupw1zard.gearMastery.data.GearItemData;
import dev.s3tupw1zard.gearMastery.stat.StatRule;
import dev.s3tupw1zard.gearMastery.stat.StatScaleMode;
import dev.s3tupw1zard.gearMastery.stat.StatType;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class ProgressionServiceTest {
    private static final ItemProfile PROFILE = new ItemProfile("pickaxes", Set.of(), "standard", Set.of("block_break"),
        Map.of(StatType.DURABILITY, new StatRule(true, StatScaleMode.MULTIPLICATIVE, 0.01D, 2.0D)));

    @Test void statValueUsesTheEffectiveRuntimeLevel() {
        assertEquals(150.0D, ProgressionService.calculateStatValue(100.0D, data(100), StatType.DURABILITY, PROFILE, configuration(50)).orElseThrow());
        assertEquals(120.0D, ProgressionService.calculateStatValue(100.0D, data(20), StatType.DURABILITY, PROFILE, configuration(50)).orElseThrow());
    }

    @Test void postEventInitializationIsUsedAsTheExperienceMutationBase() {
        final GearItemData listenerData = data(10);
        assertSame(listenerData, ProgressionService.mutationBase(Optional.of(listenerData), PROFILE));
    }

    private static GearItemData data(final int level) { return new GearItemData(1, UUID.randomUUID(), "pickaxes", level, 0, 0); }
    private static GearConfiguration configuration(final int maxLevel) {
        return new GearConfiguration(1, 1, maxLevel, "standard", Map.of("standard", new LinearLevelingCurve(10, 0)), Map.of("pickaxes", PROFILE), Map.of(), Map.of(), Map.of(), Map.of(), false);
    }
}
