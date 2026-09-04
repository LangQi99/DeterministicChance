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
}
