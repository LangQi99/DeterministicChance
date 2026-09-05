package io.github.langqi99.deterministicchance.compat.jei;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import io.github.langqi99.deterministicchance.core.DeterministicBatchPlanner;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Shared JEI-to-AE2 arithmetic; native adapters only expose their chance outputs. */
public final class JeiBatchPlanner {
    public static final long DEFAULT_MAX_EXECUTIONS = 10_000;
    public static final int MAX_INPUT_SLOTS = 81;
    public static final int MAX_OUTPUT_SLOTS = 27;

    private JeiBatchPlanner() {
    }

    public static JeiBatchPlan plan(
            List<List<GenericStack>> inputAlternatives,
            List<ChanceStack> nativeOutputs) {
        return plan(inputAlternatives, nativeOutputs, DEFAULT_MAX_EXECUTIONS);
    }

    public static JeiBatchPlan plan(
            List<List<GenericStack>> inputAlternatives,
            List<ChanceStack> nativeOutputs,
            long maxExecutions) {
        if (inputAlternatives.isEmpty()) {
            throw new IllegalArgumentException("recipe must have at least one input");
        }
        if (inputAlternatives.size() > MAX_INPUT_SLOTS) {
            throw new IllegalArgumentException("exact batch exceeds AE2's processing input slot limit");
        }
        if (nativeOutputs.isEmpty()) {
            throw new IllegalArgumentException("recipe must have at least one output");
        }

        long executions = DeterministicBatchPlanner.minimumExecutions(
                nativeOutputs.stream().map(ChanceStack::chance).toList(), maxExecutions);

        List<List<GenericStack>> inputs = inputAlternatives.stream()
                .map(alternatives -> {
                    if (alternatives.isEmpty()) {
                        throw new IllegalArgumentException("every input slot needs an alternative");
                    }
                    return alternatives.stream()
                            .map(stack -> multiply(stack, executions))
                            .toList();
                })
                .toList();

        Map<AEKey, Long> outputAmounts = new LinkedHashMap<>();
        for (ChanceStack output : nativeOutputs) {
            long successes = Math.multiplyExact(
                    executions / output.chance().denominator(),
                    output.chance().numerator());
            long amount = Math.multiplyExact(output.stack().amount(), successes);
            if (amount > 0) {
                outputAmounts.merge(output.stack().what(), amount, Math::addExact);
            }
        }

        List<GenericStack> outputs = new ArrayList<>(outputAmounts.size());
        outputAmounts.forEach((what, amount) -> outputs.add(new GenericStack(what, amount)));
        if (outputs.isEmpty()) {
            throw new IllegalArgumentException("exact batch has no outputs");
        }
        if (outputs.size() > MAX_OUTPUT_SLOTS) {
            throw new IllegalArgumentException("exact batch exceeds AE2's processing output slot limit");
        }
        return new JeiBatchPlan(executions, inputs, outputs);
    }

    private static GenericStack multiply(GenericStack stack, long factor) {
        if (stack == null || stack.amount() <= 0) {
            throw new IllegalArgumentException("input amount must be positive");
        }
        return new GenericStack(stack.what(), Math.multiplyExact(stack.amount(), factor));
    }
}
