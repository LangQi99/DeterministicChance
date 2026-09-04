package io.github.langqi99.deterministicchance.core;

import java.util.List;

public record ProbabilityRecipe<K>(
        String recipeId,
        List<IngredientAmount<K>> inputs,
        List<ProbabilityOutput<K>> outputs) {
    public ProbabilityRecipe {
        inputs = List.copyOf(inputs);
        outputs = List.copyOf(outputs);
        if (recipeId == null || recipeId.isBlank()) {
            throw new IllegalArgumentException("recipeId must not be blank");
        }
        if (inputs.isEmpty() || outputs.isEmpty()) {
            throw new IllegalArgumentException("recipe must have inputs and outputs");
        }
    }
}
