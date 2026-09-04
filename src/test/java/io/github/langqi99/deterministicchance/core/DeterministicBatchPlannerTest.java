package io.github.langqi99.deterministicchance.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DeterministicBatchPlannerTest {
    @Test
    void convertsEightyPercentIntoFiveToFour() {
        var recipe = new ProbabilityRecipe<>(
                "test:a_to_b",
                List.of(new IngredientAmount<>("A", 1)),
                List.of(new ProbabilityOutput<>("B", 1, new ChanceFraction(4, 5))));

        var plan = DeterministicBatchPlanner.plan(recipe, 64);
        assertEquals(5, plan.executions());
        assertEquals(Map.of("A", 5L), plan.inputs());
        assertEquals(Map.of("B", 4L), plan.outputs());
    }

    @Test
    void combinesGuaranteedProductsByproductsAndDifferentDenominators() {
        var recipe = new ProbabilityRecipe<>(
                "test:multi",
                List.of(new IngredientAmount<>("ore", 1)),
                List.of(
                        ProbabilityOutput.guaranteed("ingot", 1),
                        new ProbabilityOutput<>("dust", 2, new ChanceFraction(1, 2)),
                        new ProbabilityOutput<>("gem", 1, new ChanceFraction(1, 3))));

        var plan = DeterministicBatchPlanner.plan(recipe, 64);
        assertEquals(6, plan.executions());
        assertEquals(Map.of("ore", 6L), plan.inputs());
        assertEquals(Map.of("ingot", 6L, "dust", 6L, "gem", 2L), plan.outputs());
    }

    @Test
    void rejectsUnreasonablyLargeExactBatch() {
        var recipe = new ProbabilityRecipe<>(
                "test:large_lcm",
                List.of(new IngredientAmount<>("A", 1)),
                List.of(
                        new ProbabilityOutput<>("B", 1, new ChanceFraction(1, 97)),
                        new ProbabilityOutput<>("C", 1, new ChanceFraction(1, 89))));

        assertThrows(IllegalArgumentException.class, () -> DeterministicBatchPlanner.plan(recipe, 1024));
    }
}
