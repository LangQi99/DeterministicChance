package io.github.langqi99.deterministicchance.compat.jei;

import appeng.api.stacks.GenericStack;
import java.util.List;

/** Exact integer batch ready for AE2's processing-pattern encoder. */
public record JeiBatchPlan(
        long executions,
        List<List<GenericStack>> inputs,
        List<GenericStack> outputs) {
    public JeiBatchPlan {
        if (executions <= 0) {
            throw new IllegalArgumentException("executions must be positive");
        }
        inputs = inputs.stream().map(List::copyOf).toList();
        outputs = List.copyOf(outputs);
    }
}
