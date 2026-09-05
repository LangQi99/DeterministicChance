package io.github.langqi99.deterministicchance.compat.thermal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class ThermalRecipeJeiAdapterTest {
    @Test
    void leavesOrdinaryLockedGuaranteedOutputToAe2() {
        assertFalse(ThermalRecipeJeiAdapter.needsTakeover(false, List.of(-1.0F)));
    }

    @Test
    void takesOverEveryCatalyzableChanceProfile() {
        assertTrue(ThermalRecipeJeiAdapter.needsTakeover(true, List.of(0.0F)));
        assertTrue(ThermalRecipeJeiAdapter.needsTakeover(true, List.of(1.0F)));
    }

    @Test
    void takesOverLockedFractionsAndIntegerMultipliers() {
        assertTrue(ThermalRecipeJeiAdapter.needsTakeover(false, List.of(-0.8F)));
        assertTrue(ThermalRecipeJeiAdapter.needsTakeover(false, List.of(-2.0F)));
    }
}
