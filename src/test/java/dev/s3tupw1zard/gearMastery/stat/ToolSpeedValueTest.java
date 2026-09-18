package dev.s3tupw1zard.gearMastery.stat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ToolSpeedValueTest {
    @Test void rejectsInvalidValuesBeforeFloatNarrowing() {
        assertEquals(Float.MAX_VALUE, ToolSpeedValue.requireValid(Float.MAX_VALUE, "test"));
        assertThrows(IllegalArgumentException.class, () -> ToolSpeedValue.requireValid(Math.nextUp((double) Float.MAX_VALUE), "test"));
        assertThrows(IllegalArgumentException.class, () -> ToolSpeedValue.requireValid(Double.POSITIVE_INFINITY, "test"));
        assertThrows(IllegalArgumentException.class, () -> ToolSpeedValue.requireValid(Double.NaN, "test"));
    }
}
