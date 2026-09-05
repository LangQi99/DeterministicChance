package io.github.langqi99.deterministicchance.compat.create;

import com.simibubi.create.content.kinetics.crusher.AbstractCrushingRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;

/** Central compatibility boundary shared by Create execution and JEI adapters. */
public final class CreateRecipeSupport {
    private CreateRecipeSupport() {
    }

    public static boolean isDeterministic(ProcessingRecipe<?> recipe) {
        // Milling and crushing have one unambiguous commit point owned by a
        // persistent block entity. Other ProcessingRecipes are deliberately not
        // accepted here: fans, basins, deployers and hand tools either preview
        // outputs or use a static application path with no stable machine owner.
        return recipe instanceof AbstractCrushingRecipe;
    }
}
