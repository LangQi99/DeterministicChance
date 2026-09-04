package io.github.langqi99.deterministicchance.core;

import java.util.LinkedHashMap;
import java.util.Map;

/** Converts a probabilistic recipe into the smallest exact integer batch. */
public final class DeterministicBatchPlanner {
    private DeterministicBatchPlanner() {
    }

    public static <K> DeterministicBatchPlan<K> plan(ProbabilityRecipe<K> recipe, long maxExecutions) {
        if (maxExecutions <= 0) {
            throw new IllegalArgumentException("maxExecutions must be positive");
        }

        long executions = 1;
        for (var output : recipe.outputs()) {
            executions = lcm(executions, output.chance().denominator());
            if (executions > maxExecutions) {
                throw new IllegalArgumentException(
                        "exact batch requires " + executions + " executions; limit is " + maxExecutions);
            }
        }

        Map<K, Long> inputs = new LinkedHashMap<>();
        for (var input : recipe.inputs()) {
            merge(inputs, input.ingredient(), Math.multiplyExact(input.amount(), executions));
        }

        Map<K, Long> outputs = new LinkedHashMap<>();
        for (var output : recipe.outputs()) {
            long successes = Math.multiplyExact(
                    executions / output.chance().denominator(),
                    output.chance().numerator());
            long amount = Math.multiplyExact(output.amount(), successes);
            if (amount > 0) {
                merge(outputs, output.ingredient(), amount);
            }
        }

        return new DeterministicBatchPlan<>(recipe.recipeId(), executions, inputs, outputs);
    }

    private static <K> void merge(Map<K, Long> values, K key, long amount) {
        values.merge(key, amount, Math::addExact);
    }

    private static long lcm(long a, long b) {
        return Math.multiplyExact(a / gcd(a, b), b);
    }

    private static long gcd(long a, long b) {
        while (b != 0) {
            long next = a % b;
            a = b;
            b = next;
        }
        return Math.abs(a);
    }
}
