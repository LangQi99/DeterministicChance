package io.github.langqi99.deterministicchance.compat.productivebees;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class ProductiveBeesCentrifugeJeiAdapterTest {
    @Test
    void rangedProbabilityProducesAnIntegralExactCycle() {
        ProductiveBeesSequenceState.Profile profile =
                ProductiveBeesSequenceState.Profile.create(1, 3, 80);
        assertTrue(profile.cycleLength() == 15);
        assertTrue(profile.totalCountPerCycle() == 24);
    }
}
