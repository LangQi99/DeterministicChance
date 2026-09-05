package io.github.langqi99.deterministicchance.compat.jei;

import appeng.api.stacks.GenericStack;
import io.github.langqi99.deterministicchance.core.ChanceFraction;
import java.util.Objects;

/** One native recipe output and the independent chance applied to each unit. */
public record ChanceStack(GenericStack stack, ChanceFraction chance) {
    public ChanceStack {
        Objects.requireNonNull(stack, "stack");
        Objects.requireNonNull(chance, "chance");
        if (stack.amount() <= 0) {
            throw new IllegalArgumentException("output amount must be positive");
        }
    }
}
