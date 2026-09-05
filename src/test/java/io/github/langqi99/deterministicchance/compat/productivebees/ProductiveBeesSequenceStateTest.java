package io.github.langqi99.deterministicchance.compat.productivebees;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class ProductiveBeesSequenceStateTest {
    private static final ResourceLocation RECIPE =
            ResourceLocation.fromNamespaceAndPath("productivebees", "centrifuge/test");

    @Test
    void eightyPercentIsFourSuccessesThenOneFailure() {
        ProductiveBeesSequenceState state = new ProductiveBeesSequenceState();
        for (int operation = 0; operation < 5; operation++) {
            ProductiveBeesSequenceState.Outcome result = state.next(RECIPE, 0, 1, 1, 80);
            assertEquals(operation < 4, result.success());
            assertEquals(operation < 4 ? 1 : 0, result.count());
        }
        assertEquals(0, state.trackedStateCount());
    }

    @Test
    void rangedOutputIsUniformAcrossItsExactCycle() {
        ProductiveBeesSequenceState state = new ProductiveBeesSequenceState();
        int successes = 0;
        int total = 0;
        for (int operation = 0; operation < 15; operation++) {
            ProductiveBeesSequenceState.Outcome result = state.next(RECIPE, 0, 1, 3, 80);
            successes += result.success() ? 1 : 0;
            total += result.count();
        }
        assertEquals(12, successes);
        assertEquals(24, total);
        assertEquals(0, state.trackedStateCount());
    }

    @Test
    void lanesPersistIndependentlyAndZeroPercentNeverSucceeds() {
        ProductiveBeesSequenceState original = new ProductiveBeesSequenceState();
        assertTrue(original.next(RECIPE, 0, 1, 1, 80).success());
        assertTrue(original.next(RECIPE, 1, 1, 3, 50).success());
        assertFalse(original.next(RECIPE, 2, 1, 1, 0).success());

        CompoundTag saved = new CompoundTag();
        original.save(saved);
        ProductiveBeesSequenceState restored = new ProductiveBeesSequenceState();
        restored.load(saved);

        assertEquals(2, restored.trackedStateCount());
        assertTrue(restored.next(RECIPE, 0, 1, 1, 80).success());
        assertEquals(2, restored.next(RECIPE, 1, 1, 3, 50).count());
    }
}
