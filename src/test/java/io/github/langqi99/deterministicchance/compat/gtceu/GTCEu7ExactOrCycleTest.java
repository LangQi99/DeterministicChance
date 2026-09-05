package io.github.langqi99.deterministicchance.compat.gtceu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class GTCEu7ExactOrCycleTest {
    @Test
    void reducesEightyPercentToFourOutOfFive() {
        assertEquals(new GTCEu7ExactOrCycle.Fraction(4, 5),
                GTCEu7ExactOrCycle.fraction(8_000, 10_000));
    }

    @Test
    void everyWholeCycleIsExactFromEveryPersistedPhase() {
        var chance = GTCEu7ExactOrCycle.fraction(8_000, 10_000);
        for (int phase = 0; phase < chance.denominator(); phase++) {
            var result = GTCEu7ExactOrCycle.advance(chance, phase, 5);
            assertEquals(4, result.successes(), "phase " + phase);
            assertEquals(phase, result.nextPosition(), "phase " + phase);
        }
    }

    @Test
    void aParallelRunCountsSuccessesAndAdvancesOncePerOperation() {
        var result = GTCEu7ExactOrCycle.advance(
                GTCEu7ExactOrCycle.fraction(1, 4),
                3,
                6);
        assertEquals(2, result.successes());
        assertEquals(1, result.nextPosition());
    }

    @Test
    void zeroAndCertainChancesRemainWellDefined() {
        assertEquals(new GTCEu7ExactOrCycle.Advance(0, 0),
                GTCEu7ExactOrCycle.advance(GTCEu7ExactOrCycle.fraction(0, 10_000), 123, 100));
        assertEquals(new GTCEu7ExactOrCycle.Advance(100, 0),
                GTCEu7ExactOrCycle.advance(GTCEu7ExactOrCycle.fraction(10_000, 10_000), 123, 100));
    }

    @Test
    void rejectsInvalidNativeChances() {
        assertThrows(IllegalArgumentException.class, () -> GTCEu7ExactOrCycle.fraction(-1, 10_000));
        assertThrows(IllegalArgumentException.class, () -> GTCEu7ExactOrCycle.fraction(10_001, 10_000));
        assertThrows(IllegalArgumentException.class, () -> GTCEu7ExactOrCycle.fraction(1, 0));
    }
}
