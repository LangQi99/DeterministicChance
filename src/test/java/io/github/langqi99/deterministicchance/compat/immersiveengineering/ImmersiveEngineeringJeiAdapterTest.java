package io.github.langqi99.deterministicchance.compat.immersiveengineering;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ImmersiveEngineeringJeiAdapterTest {
    @Test
    void takesOverArcNeverOutputsThatJeiStillDisplays() {
        assertTrue(ImmersiveEngineeringJeiAdapter.chanceNeedsTakeover(0, true));
        assertTrue(ImmersiveEngineeringJeiAdapter.chanceNeedsTakeover(-0.25F, true));
        assertFalse(ImmersiveEngineeringJeiAdapter.chanceNeedsTakeover(1, true));
    }

    @Test
    void crusherOnlyTakesOverFractionalPositiveChance() {
        assertFalse(ImmersiveEngineeringJeiAdapter.chanceNeedsTakeover(0, false));
        assertTrue(ImmersiveEngineeringJeiAdapter.chanceNeedsTakeover(0.5F, false));
        assertFalse(ImmersiveEngineeringJeiAdapter.chanceNeedsTakeover(1, false));
    }
}
