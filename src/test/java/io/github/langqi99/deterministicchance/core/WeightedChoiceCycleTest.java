package io.github.langqi99.deterministicchance.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class WeightedChoiceCycleTest {
    @Test
    void reducesAndEmitsEveryWeightedOutcomeExactly() {
        WeightedChoiceCycle cycle = WeightedChoiceCycle.fromFloatWeights(
                List.of(80.0F, 20.0F, 0.0F), 10_000);
        int[] counts = new int[3];
        for (int position = 0; position < cycle.totalWeight(); position++) {
            counts[cycle.choice(position)]++;
        }
        assertEquals(5, cycle.totalWeight());
        assertEquals(4, counts[0]);
        assertEquals(1, counts[1]);
        assertEquals(0, counts[2]);
    }

    @Test
    void preservesDecimalKubeJsWeightsAsAnExactRatio() {
        WeightedChoiceCycle cycle = WeightedChoiceCycle.fromFloatWeights(
                List.of(0.13F, 0.008F, 0.002F), 10_000);
        assertEquals(70, cycle.totalWeight());
        assertEquals(new ChanceFraction(65, 70), cycle.chance(0));
        assertEquals(new ChanceFraction(4, 70), cycle.chance(1));
        assertEquals(new ChanceFraction(1, 70), cycle.chance(2));
    }

    @Test
    void rejectsInvalidOrUnboundedPools() {
        assertThrows(IllegalArgumentException.class,
                () -> WeightedChoiceCycle.fromFloatWeights(List.of(0.0F, 0.0F), 10_000));
        assertThrows(IllegalArgumentException.class,
                () -> WeightedChoiceCycle.fromFloatWeights(List.of(1.0F, Float.NaN), 10_000));
        assertThrows(IllegalArgumentException.class,
                () -> WeightedChoiceCycle.fromFloatWeights(List.of(1.0F, 10_001.0F), 10_000));
    }
}
