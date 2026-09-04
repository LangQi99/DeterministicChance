package io.github.langqi99.deterministicchance.core;

import java.util.Objects;

public record ProbabilityOutput<K>(K ingredient, long amount, ChanceFraction chance) {
    public ProbabilityOutput {
        Objects.requireNonNull(ingredient, "ingredient");
        Objects.requireNonNull(chance, "chance");
        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
    }

    public static <K> ProbabilityOutput<K> guaranteed(K ingredient, long amount) {
        return new ProbabilityOutput<>(ingredient, amount, ChanceFraction.ALWAYS);
    }
}
