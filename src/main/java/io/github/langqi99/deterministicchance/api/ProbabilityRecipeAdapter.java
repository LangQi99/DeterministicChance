package io.github.langqi99.deterministicchance.api;

import io.github.langqi99.deterministicchance.core.ProbabilityRecipe;

/**
 * A mod integration describes its native recipe in the common probability
 * model. One central adapter can normally cover all machines sharing that
 * mod's recipe/output implementation.
 */
public interface ProbabilityRecipeAdapter<R, K> {
    boolean supports(R recipe);

    ProbabilityRecipe<K> describe(R recipe);
}
