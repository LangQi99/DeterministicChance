package io.github.langqi99.deterministicchance.core;

import java.util.Map;

public record DeterministicBatchPlan<K>(
        String recipeId,
        long executions,
        Map<K, Long> inputs,
        Map<K, Long> outputs) {
    public DeterministicBatchPlan {
        inputs = Map.copyOf(inputs);
        outputs = Map.copyOf(outputs);
    }
}
