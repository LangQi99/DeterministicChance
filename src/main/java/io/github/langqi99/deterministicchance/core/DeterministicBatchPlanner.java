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

        long executions = minimumExecutions(
                recipe.outputs().stream().map(ProbabilityOutput::chance).toList(), maxExecutions);

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

    /** Returns the smallest number of executions that makes every expected output integral. */
    public static long minimumExecutions(Iterable<ChanceFraction> chances, long maxExecutions) {
        if (maxExecutions <= 0) {
            throw new IllegalArgumentException("maxExecutions must be positive");
        }

        long executions = 1;
        for (ChanceFraction chance : chances) {
            executions = lcm(executions, chance.denominator());
            if (executions > maxExecutions) {
                throw new IllegalArgumentException(
                        "exact batch requires " + executions + " executions; limit is " + maxExecutions);
            }
        }
        return executions;
    }

    private static <K> void merge(Map<K, Long> values, K key, long amount) {
        values.merge(key, amount, Math::addExact);
    }

    public static long lcm(long a, long b) {
        if (a <= 0 || b <= 0) {
            throw new IllegalArgumentException("lcm operands must be positive");
        }
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
