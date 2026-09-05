package io.github.langqi99.deterministicchance.compat.create;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import com.simibubi.create.content.kinetics.deployer.ItemApplicationRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import com.simibubi.create.content.processing.sequenced.SequencedRecipe;
import com.simibubi.create.foundation.fluid.FluidIngredient;
import io.github.langqi99.deterministicchance.compat.jei.ChanceStack;
import io.github.langqi99.deterministicchance.compat.jei.NativeInputJeiRecipeBatchAdapter;
import io.github.langqi99.deterministicchance.core.WeightedChoiceCycle;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.fluids.FluidStack;

/** Exact weighted-result and consumable-input model for native and KubeJS-authored assemblies. */
public final class CreateSequencedAssemblyJeiAdapter implements NativeInputJeiRecipeBatchAdapter {
    @Override
    public boolean supports(Object recipe) {
        return recipe instanceof SequencedAssemblyRecipe;
    }

    @Override
    public boolean hasProbabilisticOutputs(Object recipeObject) {
        SequencedAssemblyRecipe recipe = (SequencedAssemblyRecipe) recipeObject;
        return recipe.resultPool.stream().filter(output -> output.getChance() > 0).count() > 1;
    }

    @Override
    public Optional<String> exactBatchUnsupportedReason(Object recipeObject) {
        SequencedAssemblyRecipe recipe = (SequencedAssemblyRecipe) recipeObject;
        try {
            SequencedAssemblySequenceController.cycle(recipe.resultPool);
            if (recipe.getLoops() <= 0 || recipe.getSequence().isEmpty()) {
                return Optional.of("Create sequenced assembly must have positive loops and at least one step");
            }
            return Optional.empty();
        } catch (IllegalArgumentException | ArithmeticException exception) {
            return Optional.of(exception.getMessage());
        }
    }

    @Override
    public List<List<GenericStack>> inputs(Object recipeObject) {
        SequencedAssemblyRecipe recipe = (SequencedAssemblyRecipe) recipeObject;
        List<List<GenericStack>> result = new ArrayList<>();
        result.add(itemAlternatives(recipe.getIngredient(), 1));
        for (SequencedRecipe<?> sequenced : recipe.getSequence()) {
            ProcessingRecipe<?> step = sequenced.getRecipe();
            for (int index = 1; index < step.getIngredients().size(); index++) {
                if (step instanceof ItemApplicationRecipe application
                        && index == 1
                        && application.shouldKeepHeldItem()) {
                    continue;
                }
                result.add(itemAlternatives(step.getIngredients().get(index), recipe.getLoops()));
            }
            for (FluidIngredient fluid : step.getFluidIngredients()) {
                List<GenericStack> alternatives = new ArrayList<>();
                for (FluidStack stack : fluid.getMatchingFluidStacks()) {
                    if (!stack.isEmpty()) {
                        alternatives.add(new GenericStack(
                                AEFluidKey.of(stack),
                                Math.multiplyExact(fluid.getRequiredAmount(), recipe.getLoops())));
                    }
                }
                result.add(requireAlternatives(alternatives, "fluid"));
            }
        }
        return List.copyOf(result);
    }

    @Override
    public List<ChanceStack> outputs(Object recipeObject) {
        SequencedAssemblyRecipe recipe = (SequencedAssemblyRecipe) recipeObject;
        WeightedChoiceCycle cycle = SequencedAssemblySequenceController.cycle(recipe.resultPool);
        List<ChanceStack> result = new ArrayList<>();
        for (int index = 0; index < recipe.resultPool.size(); index++) {
            ItemStack stack = recipe.resultPool.get(index).getStack();
            if (!stack.isEmpty() && cycle.weight(index) > 0) {
                result.add(new ChanceStack(
                        new GenericStack(AEItemKey.of(stack), stack.getCount()),
                        cycle.chance(index)));
            }
        }
        return List.copyOf(result);
    }

    private static List<GenericStack> itemAlternatives(Ingredient ingredient, int factor) {
        List<GenericStack> alternatives = new ArrayList<>();
        for (ItemStack stack : ingredient.getItems()) {
            if (!stack.isEmpty()) {
                alternatives.add(new GenericStack(
                        AEItemKey.of(stack),
                        Math.multiplyExact(stack.getCount(), factor)));
            }
        }
        return requireAlternatives(alternatives, "item");
    }

    private static List<GenericStack> requireAlternatives(List<GenericStack> alternatives, String kind) {
        if (alternatives.isEmpty()) {
            throw new IllegalArgumentException("Create assembly has an unresolved " + kind + " input");
        }
        return List.copyOf(alternatives);
    }
}
