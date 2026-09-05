package io.github.langqi99.deterministicchance.compat.create;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import com.simibubi.create.content.kinetics.deployer.ItemApplicationRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.foundation.fluid.FluidIngredient;
import io.github.langqi99.deterministicchance.compat.jei.ChanceStack;
import io.github.langqi99.deterministicchance.compat.jei.NativeInputJeiRecipeBatchAdapter;
import io.github.langqi99.deterministicchance.core.ChanceFraction;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.fluids.FluidStack;

/** One adapter covers milling, crushing, splashing, haunting and other ProcessingRecipes. */
public final class CreateProcessingJeiAdapter implements NativeInputJeiRecipeBatchAdapter {
    @Override
    public boolean supports(Object recipe) {
        return recipe instanceof ProcessingRecipe<?>;
    }

    @Override
    public java.util.Optional<String> exactBatchUnsupportedReason(Object recipe) {
        ProcessingRecipe<?> processingRecipe = (ProcessingRecipe<?>) recipe;
        if (!CreateRecipeSupport.isDeterministic(processingRecipe)) {
            return java.util.Optional.of(
                    "this Create recipe previews or rolls outputs outside the supported commit hook");
        }
        return java.util.Optional.empty();
    }

    @Override
    public List<List<GenericStack>> inputs(Object recipeObject) {
        ProcessingRecipe<?> recipe = (ProcessingRecipe<?>) recipeObject;
        List<List<GenericStack>> result = new ArrayList<>(
                recipe.getIngredients().size() + recipe.getFluidIngredients().size());

        for (int index = 0; index < recipe.getIngredients().size(); index++) {
            // A deployer keeps this tool/catalyst installed. JEI displays it as
            // INPUT, but an AE processing pattern must not request one per run.
            if (recipe instanceof ItemApplicationRecipe applicationRecipe
                    && index == 1
                    && applicationRecipe.shouldKeepHeldItem()) {
                continue;
            }
            Ingredient ingredient = recipe.getIngredients().get(index);
            List<GenericStack> alternatives = new ArrayList<>();
            for (ItemStack stack : ingredient.getItems()) {
                GenericStack generic = GenericStack.fromItemStack(stack);
                if (generic != null && generic.amount() > 0) {
                    alternatives.add(generic);
                }
            }
            result.add(requireAlternatives(alternatives, "item"));
        }

        for (FluidIngredient ingredient : recipe.getFluidIngredients()) {
            List<GenericStack> alternatives = new ArrayList<>();
            for (FluidStack stack : ingredient.getMatchingFluidStacks()) {
                if (!stack.isEmpty()) {
                    alternatives.add(new GenericStack(
                            AEFluidKey.of(stack),
                            ingredient.getRequiredAmount()));
                }
            }
            result.add(requireAlternatives(alternatives, "fluid"));
        }
        return List.copyOf(result);
    }

    @Override
    public List<ChanceStack> outputs(Object recipeObject) {
        ProcessingRecipe<?> recipe = (ProcessingRecipe<?>) recipeObject;
        List<ChanceStack> result = new ArrayList<>();
        for (ProcessingOutput output : recipe.getRollableResults()) {
            ItemStack stack = output.getStack();
            ChanceFraction chance = ChanceFraction.fromFloat(output.getChance());
            if (!stack.isEmpty() && !chance.isNever()) {
                result.add(new ChanceStack(
                        new GenericStack(AEItemKey.of(stack), stack.getCount()),
                        chance));
            }
        }
        for (FluidStack stack : recipe.getFluidResults()) {
            if (!stack.isEmpty()) {
                result.add(new ChanceStack(
                        new GenericStack(AEFluidKey.of(stack), stack.getAmount()),
                        ChanceFraction.ALWAYS));
            }
        }
        return result;
    }

    private static List<GenericStack> requireAlternatives(
            List<GenericStack> alternatives,
            String kind) {
        if (alternatives.isEmpty()) {
            throw new IllegalArgumentException("Create recipe has an unresolved " + kind + " input");
        }
        return List.copyOf(alternatives);
    }
}
