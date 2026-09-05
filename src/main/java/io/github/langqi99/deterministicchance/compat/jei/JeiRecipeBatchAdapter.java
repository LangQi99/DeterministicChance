package io.github.langqi99.deterministicchance.compat.jei;

import java.util.List;
import java.util.Optional;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;

/** Thin optional-mod bridge from one native recipe family to the common planner. */
public interface JeiRecipeBatchAdapter {
    /** Returns true for every recipe owned by this native recipe family. */
    boolean supports(Object recipe);

    /**
     * Cheaply determines whether this feature should take ownership of the
     * transfer. Adapters with complex validation should override this so a
     * deterministic recipe can still fall through to AE2's normal handler.
     */
    default boolean hasProbabilisticOutputs(Object recipe) {
        return outputs(recipe).stream().anyMatch(output -> !output.chance().isCertain());
    }

    /** Context-aware counterpart for recipe families whose JEI categories expose different outputs. */
    default boolean hasProbabilisticOutputs(Object recipe, IRecipeSlotsView slotsView) {
        return outputs(recipe, slotsView).stream().anyMatch(output -> !output.chance().isCertain());
    }

    /**
     * Explains why a recognized recipe cannot be represented as one exact AE2
     * batch. The registry turns this into a visible transfer error instead of
     * letting AE2 encode a misleading ordinary processing pattern.
     */
    default Optional<String> exactBatchUnsupportedReason(Object recipe) {
        return Optional.empty();
    }

    default Optional<String> exactBatchUnsupportedReason(Object recipe, IRecipeSlotsView slotsView) {
        return exactBatchUnsupportedReason(recipe);
    }

    List<ChanceStack> outputs(Object recipe);

    default List<ChanceStack> outputs(Object recipe, IRecipeSlotsView slotsView) {
        return outputs(recipe);
    }
}
