package io.github.langqi99.deterministicchance.core;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class DeterministicSequenceTest {
    @Test
    void eightyPercentIsExactlyFourOfEveryFive() {
        var sequence = new DeterministicSequence(new ChanceFraction(4, 5));
        boolean[] actual = new boolean[10];
        for (int i = 0; i < actual.length; i++) {
            actual[i] = sequence.next();
        }
        assertArrayEquals(
                new boolean[] { true, true, true, true, false, true, true, true, true, false },
                actual);
    }

    @Test
    void advancePreservesPhaseAndCountsFullCycles() {
        var sequence = new DeterministicSequence(new ChanceFraction(4, 5));
        assertEquals(8, sequence.advance(10));
        assertEquals(0, sequence.position());
    }

    @Test
    void convertsRecipeDecimalChanceToReducedFraction() {
        assertEquals(new ChanceFraction(1, 4), ChanceFraction.fromDouble(0.25));
        assertEquals(new ChanceFraction(4, 5), ChanceFraction.fromDouble(0.8));
        assertEquals(new ChanceFraction(1, 3), ChanceFraction.fromDouble(1.0 / 3.0));
        assertEquals(new ChanceFraction(333, 1_000), ChanceFraction.fromDouble(0.333));
        assertEquals(new ChanceFraction(1, 10), ChanceFraction.fromFloat(0.1F));
        assertEquals(new ChanceFraction(1, 3), ChanceFraction.fromFloat(1F / 3F));
        assertEquals(new ChanceFraction(333, 1_000), ChanceFraction.fromFloat(0.333F));
    }
}
