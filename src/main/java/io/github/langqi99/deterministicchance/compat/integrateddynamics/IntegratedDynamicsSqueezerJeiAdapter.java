package io.github.langqi99.deterministicchance.compat.integrateddynamics;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.GenericStack;
import io.github.langqi99.deterministicchance.compat.jei.ChanceStack;
import io.github.langqi99.deterministicchance.compat.jei.NativeInputJeiRecipeBatchAdapter;
import io.github.langqi99.deterministicchance.core.ChanceFraction;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import org.cyclops.integrateddynamics.core.recipe.type.RecipeMechanicalSqueezer;
import org.cyclops.integrateddynamics.core.recipe.type.RecipeSqueezer;

/** Native recipe bridge for Integrated Dynamics' automatable Mechanical Squeezer. */
public final class IntegratedDynamicsSqueezerJeiAdapter
        implements NativeInputJeiRecipeBatchAdapter {
    @Override
    public boolean supports(Object recipe) {
        return recipe instanceof RecipeMechanicalSqueezer;
    }

    @Override
    public boolean hasProbabilisticOutputs(Object recipeObject) {
        RecipeMechanicalSqueezer recipe = (RecipeMechanicalSqueezer) recipeObject;
        return recipe.getOutputItems().stream()
                .filter(output -> !output.getIngredientFirst().isEmpty())
                .anyMatch(output -> output.getChance() != 1.0F);
    }

    @Override
    public Optional<String> exactBatchUnsupportedReason(Object recipeObject) {
        RecipeMechanicalSqueezer recipe = (RecipeMechanicalSqueezer) recipeObject;
        for (RecipeSqueezer.IngredientChance output : recipe.getOutputItems()) {
            float chance = output.getChance();
            if (!Float.isFinite(chance) || chance < 0 || chance > 1) {
                return Optional.of("Integrated Dynamics output chance must be finite and between 0 and 1");
            }
        }
        try {
            if (recipe.getClass().getMethod("assemble", ItemStack.class).getDeclaringClass()
                    != RecipeSqueezer.class) {
                return Optional.of(
                        "Mechanical Squeezer recipe overrides input-dependent outputs; exact semantics are unknown");
            }
        } catch (NoSuchMethodException exception) {
            return Optional.of("Integrated Dynamics Squeezer API does not match the supported 1.20.1 line");
        }
        return Optional.empty();
    }

    @Override
    public List<List<GenericStack>> inputs(Object recipeObject) {
        RecipeMechanicalSqueezer recipe = (RecipeMechanicalSqueezer) recipeObject;
        List<GenericStack> alternatives = new ArrayList<>();
        for (ItemStack stack : recipe.getInputIngredient().getItems()) {
            GenericStack generic = GenericStack.fromItemStack(stack);
            if (generic != null && generic.amount() > 0) {
                alternatives.add(generic);
            }
        }
        if (alternatives.isEmpty()) {
            throw new IllegalArgumentException("Mechanical Squeezer input has no concrete alternatives");
        }
        return List.of(List.copyOf(alternatives));
    }

    @Override
    public List<ChanceStack> outputs(Object recipeObject) {
        RecipeMechanicalSqueezer recipe = (RecipeMechanicalSqueezer) recipeObject;
        List<ChanceStack> result = new ArrayList<>();
        for (RecipeSqueezer.IngredientChance output : recipe.getOutputItems()) {
            ItemStack stack = output.getIngredientFirst();
            ChanceFraction chance = ChanceFraction.fromFloat(output.getChance());
            GenericStack generic = GenericStack.fromItemStack(stack);
            if (generic != null && generic.amount() > 0 && !chance.isNever()) {
                result.add(new ChanceStack(generic, chance));
            }
        }
        FluidStack fluid = recipe.getOutputFluid();
        if (fluid != null && !fluid.isEmpty()) {
            result.add(new ChanceStack(
                    new GenericStack(AEFluidKey.of(fluid), fluid.getAmount()),
                    ChanceFraction.ALWAYS));
        }
        return List.copyOf(result);
    }
}
