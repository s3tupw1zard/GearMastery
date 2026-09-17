package dev.s3tupw1zard.gearMastery.stat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DurabilityValueTest {
    @Test void validatesBeforeNarrowingToTheComponentIntegerRange() {
        assertEquals(Integer.MAX_VALUE, DurabilityValue.toMaxDamage(Integer.MAX_VALUE));
        assertThrows(IllegalArgumentException.class, () -> DurabilityValue.toMaxDamage((double) Integer.MAX_VALUE + 1.0D));
        assertThrows(IllegalArgumentException.class, () -> DurabilityValue.toMaxDamage(Double.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class, () -> DurabilityValue.toMaxDamage(Double.NaN));
    }
}
