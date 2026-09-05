package io.github.langqi99.deterministicchance.compat.thermal;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import cofh.lib.common.fluid.FluidIngredient;
import cofh.thermal.lib.util.recipes.ThermalRecipe;
import io.github.langqi99.deterministicchance.compat.jei.ChanceStack;
import io.github.langqi99.deterministicchance.compat.jei.NativeInputJeiRecipeBatchAdapter;
import io.github.langqi99.deterministicchance.core.ChanceFraction;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.fluids.FluidStack;

/**
 * Covers every Thermal machine recipe shown by JEI. Inputs come from the
 * native recipe so optional catalysts displayed by JEI are not encoded as
 * consumed processing-pattern inputs.
 */
public final class ThermalRecipeJeiAdapter implements NativeInputJeiRecipeBatchAdapter {
    @Override
    public boolean supports(Object recipe) {
        return recipe instanceof ThermalRecipe;
    }

    @Override
    public boolean hasProbabilisticOutputs(Object recipeObject) {
        ThermalRecipe recipe = (ThermalRecipe) recipeObject;
        // Even a base chance of exactly 1 can become 1.5 in an augmented
        // machine. JEI has no target-machine profile, so every boostable recipe
        // must be taken over and rejected rather than encoded optimistically.
        return needsTakeover(recipe.isCatalyzable(), recipe.getOutputItemChances());
    }

    static boolean needsTakeover(boolean catalyzable, List<Float> outputChances) {
        if (catalyzable) {
            return true;
        }
        // Locked values use their magnitude as an output multiplier. AE2's
        // ordinary JEI transfer cannot express either a fractional locked
        // output or an integer multiplier such as -2, even though both are
        // deterministic at the target machine.
        return outputChances.stream().anyMatch(rawChance ->
                rawChance == null
                        || !Float.isFinite(rawChance)
                        || Math.abs(rawChance) != 1.0F);
    }

    @Override
    public java.util.Optional<String> exactBatchUnsupportedReason(Object recipeObject) {
        ThermalRecipe recipe = (ThermalRecipe) recipeObject;
        if (recipe.isCatalyzable()) {
            return java.util.Optional.of(
                    "Thermal output augments or catalysts can change this recipe's chance; use a locked-chance recipe");
        }
        return java.util.Optional.empty();
    }

    @Override
    public List<List<GenericStack>> inputs(Object recipeObject) {
        ThermalRecipe recipe = (ThermalRecipe) recipeObject;
        List<List<GenericStack>> result = new ArrayList<>(
                recipe.getInputItems().size() + recipe.getInputFluids().size());

        for (Ingredient ingredient : recipe.getInputItems()) {
            List<GenericStack> alternatives = new ArrayList<>();
            for (ItemStack stack : ingredient.getItems()) {
                if (!stack.isEmpty()) {
                    alternatives.add(new GenericStack(AEItemKey.of(stack), stack.getCount()));
                }
            }
            result.add(requireAlternatives(alternatives, "item"));
        }
        for (FluidIngredient ingredient : recipe.getInputFluids()) {
            List<GenericStack> alternatives = new ArrayList<>();
            for (FluidStack stack : ingredient.getFluids()) {
                if (!stack.isEmpty()) {
                    alternatives.add(new GenericStack(AEFluidKey.of(stack), stack.getAmount()));
                }
            }
            result.add(requireAlternatives(alternatives, "fluid"));
        }
        return List.copyOf(result);
    }

    @Override
    public List<ChanceStack> outputs(Object recipeObject) {
        ThermalRecipe recipe = (ThermalRecipe) recipeObject;
        List<ItemStack> itemOutputs = recipe.getOutputItems();
        List<Float> itemChances = recipe.getOutputItemChances();
        List<ChanceStack> result = new ArrayList<>(itemOutputs.size() * 2 + recipe.getOutputFluids().size());

        for (int index = 0; index < itemOutputs.size(); index++) {
            ItemStack stack = itemOutputs.get(index);
            if (stack.isEmpty()) {
                continue;
            }

            // Thermal fills absent entries with -1 (a locked, guaranteed output),
            // but retaining this fallback also handles third-party recipe subclasses.
            float rawChance = index < itemChances.size() ? itemChances.get(index) : -1.0F;
            ThermalChance chance = ThermalChance.fromRaw(rawChance);
            if (chance.guaranteedCopies() > 0) {
                long amount = Math.multiplyExact((long) stack.getCount(), chance.guaranteedCopies());
                result.add(new ChanceStack(
                        new GenericStack(AEItemKey.of(stack), amount),
                        ChanceFraction.ALWAYS));
            }
            if (!chance.fractionalChance().isNever()) {
                result.add(new ChanceStack(
                        new GenericStack(AEItemKey.of(stack), stack.getCount()),
                        chance.fractionalChance()));
            }
        }

        // Thermal fluid outputs have no chance field and are always produced.
        for (FluidStack stack : recipe.getOutputFluids()) {
            if (!stack.isEmpty()) {
                result.add(new ChanceStack(
                        new GenericStack(AEFluidKey.of(stack), stack.getAmount()),
                        ChanceFraction.ALWAYS));
            }
        }
        return List.copyOf(result);
    }

    private static List<GenericStack> requireAlternatives(
            List<GenericStack> alternatives,
            String kind) {
        if (alternatives.isEmpty()) {
            throw new IllegalArgumentException("Thermal recipe has an unresolved " + kind + " input");
        }
        return List.copyOf(alternatives);
    }
}
