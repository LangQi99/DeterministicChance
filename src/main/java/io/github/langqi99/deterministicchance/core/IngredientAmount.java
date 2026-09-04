package io.github.langqi99.deterministicchance.core;

import java.util.Objects;

public record IngredientAmount<K>(K ingredient, long amount) {
    public IngredientAmount {
        Objects.requireNonNull(ingredient, "ingredient");
        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
    }
}
