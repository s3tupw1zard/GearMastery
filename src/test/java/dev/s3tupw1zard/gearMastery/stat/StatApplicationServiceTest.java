package dev.s3tupw1zard.gearMastery.stat;

import dev.s3tupw1zard.gearMastery.config.ItemProfile;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatApplicationServiceTest {
    private static final ItemProfile PROFILE = new ItemProfile("test", Set.of(), "standard", Set.of(), Map.of());
    @Test void clampsOnlyTheRuntimeStatLevelToTheConfiguredMaximum() {
        assertEquals(50, StatApplicationService.effectiveStatLevel(100, 50));
        assertEquals(80, StatApplicationService.effectiveStatLevel(100, 80));
        assertEquals(100, StatApplicationService.effectiveStatLevel(100, 100));
        assertEquals(20, StatApplicationService.effectiveStatLevel(20, 50));
    }
    @Test void aFailurePreventsCompletionAndTheNextAttemptCanSucceed() {
        final AtomicBoolean broken = new AtomicBoolean(true);
        final StatHandler handler = new StatHandler() {
            @Override public StatType type() { return StatType.DURABILITY; }
            @Override public void apply(final StatApplicationContext context) { if (broken.get()) throw new IllegalStateException("broken"); }
        };
        assertFalse(StatApplicationService.applyHandlers(List.of(handler), PROFILE, null, 1, null, (ignored, exception) -> { }));
        broken.set(false);
        assertTrue(StatApplicationService.applyHandlers(List.of(handler), PROFILE, null, 1, null, (ignored, exception) -> { }));
    }
    @Test void allSuccessfulHandlersReportCompletion() {
        final StatHandler handler = new StatHandler() {
            @Override public StatType type() { return StatType.DURABILITY; }
            @Override public void apply(final StatApplicationContext context) { }
        };
        assertTrue(StatApplicationService.applyHandlers(List.of(handler), PROFILE, null, 1, null, (ignored, exception) -> { }));
    }
    @Test void missingProfileCleanupUsesDisabledRules() {
        final AtomicBoolean disabled = new AtomicBoolean(false);
        final StatHandler handler = new StatHandler() {
            @Override public StatType type() { return StatType.DURABILITY; }
            @Override public void apply(final StatApplicationContext context) { disabled.set(!context.rule().enabled()); }
        };
        final ItemProfile missing = new ItemProfile("_missing_profile", Set.of(), "", Set.of(), Map.of());
        assertTrue(StatApplicationService.applyHandlers(List.of(handler), missing, null, 0, null, (ignored, exception) -> { }));
        assertTrue(disabled.get());
    }
}
