package io.github.langqi99.deterministicchance.compat.productivebees;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import com.mojang.datafixers.util.Pair;
import cy.jdkdigital.productivebees.common.recipe.CentrifugeRecipe;
import cy.jdkdigital.productivebees.init.ModTags;
import io.github.langqi99.deterministicchance.compat.jei.ChanceStack;
import io.github.langqi99.deterministicchance.compat.jei.NativeInputJeiRecipeBatchAdapter;
import io.github.langqi99.deterministicchance.core.ChanceFraction;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;

/** Exact JEI-to-AE2 batches for Productive Bees centrifuge recipes. */
public final class ProductiveBeesCentrifugeJeiAdapter implements NativeInputJeiRecipeBatchAdapter {
    @Override
    public boolean supports(Object recipe) {
        return recipe instanceof CentrifugeRecipe;
    }

    @Override
    public boolean hasProbabilisticOutputs(Object recipe) {
        return needsTakeover((CentrifugeRecipe) recipe, false);
    }

    @Override
    public boolean hasProbabilisticOutputs(Object recipe, IRecipeSlotsView slotsView) {
        CentrifugeRecipe centrifugeRecipe = (CentrifugeRecipe) recipe;
        return needsTakeover(centrifugeRecipe, isHeatedCategory(centrifugeRecipe, slotsView));
    }

    @Override
    public List<List<GenericStack>> inputs(Object recipeObject) {
        CentrifugeRecipe recipe = (CentrifugeRecipe) recipeObject;
        List<GenericStack> alternatives = new ArrayList<>();
        for (ItemStack stack : recipe.ingredient.getItems()) {
            GenericStack generic = GenericStack.fromItemStack(stack);
            if (generic != null && generic.amount() > 0) {
                alternatives.add(generic);
            }
        }
        if (alternatives.isEmpty()) {
            throw new IllegalArgumentException("Productive Bees centrifuge input has no concrete alternatives");
        }
        return List.of(List.copyOf(alternatives));
    }

    @Override
    public List<ChanceStack> outputs(Object recipe) {
        return outputsForMode((CentrifugeRecipe) recipe, false);
    }

    @Override
    public List<ChanceStack> outputs(Object recipe, IRecipeSlotsView slotsView) {
        CentrifugeRecipe centrifugeRecipe = (CentrifugeRecipe) recipe;
        return outputsForMode(centrifugeRecipe, isHeatedCategory(centrifugeRecipe, slotsView));
    }

    static boolean needsTakeover(CentrifugeRecipe recipe, boolean stripWax) {
        for (var entry : recipe.getRecipeOutputs().entrySet()) {
            if (stripWax && entry.getKey().is(ModTags.Forge.WAX)) {
                continue;
            }
            IntArrayTag values = entry.getValue();
            if (values.size() < 3
                    || values.get(0).getAsInt() != values.get(1).getAsInt()
                    || values.get(2).getAsInt() != 100) {
                return true;
            }
        }
        return false;
    }

    static List<ChanceStack> outputsForMode(CentrifugeRecipe recipe, boolean stripWax) {
        List<ChanceStack> result = new ArrayList<>();
        recipe.getRecipeOutputs().forEach((stack, values) -> {
            if (stripWax && stack.is(ModTags.Forge.WAX)) {
                return;
            }
            if (values.size() < 3) {
                throw new IllegalArgumentException("Productive Bees centrifuge output profile is incomplete");
            }
            ProductiveBeesSequenceState.Profile profile = ProductiveBeesSequenceState.Profile.create(
                    values.get(0).getAsInt(),
                    values.get(1).getAsInt(),
                    values.get(2).getAsInt());
            long total = profile.totalCountPerCycle();
            if (stack.isEmpty() || total == 0) {
                return;
            }
            ChanceFraction encodedChance = profile.cycleLength() == 1
                    ? ChanceFraction.ALWAYS
                    : new ChanceFraction(1, profile.cycleLength());
            result.add(new ChanceStack(
                    new GenericStack(Objects.requireNonNull(AEItemKey.of(stack)), total),
                    encodedChance));
        });

        Pair<Fluid, Integer> fluidOutput = recipe.getFluidOutputs();
        if (fluidOutput != null && fluidOutput.getSecond() > 0) {
            FluidStack stack = new FluidStack(fluidOutput.getFirst(), fluidOutput.getSecond());
            result.add(new ChanceStack(
                    new GenericStack(Objects.requireNonNull(AEFluidKey.of(stack)), stack.getAmount()),
                    ChanceFraction.ALWAYS));
        }
        return List.copyOf(result);
    }

    /** Heated JEI omits wax while the ordinary category exposes it. */
    private static boolean isHeatedCategory(CentrifugeRecipe recipe, IRecipeSlotsView slotsView) {
        List<ItemStack> waxOutputs = recipe.getRecipeOutputs().keySet().stream()
                .filter(stack -> stack.is(ModTags.Forge.WAX))
                .toList();
        if (waxOutputs.isEmpty()) {
            return false;
        }
        List<ItemStack> shownOutputs = slotsView.getSlotViews(RecipeIngredientRole.OUTPUT).stream()
                .flatMap(slot -> slot.getItemStacks())
                .toList();
        return isHeatedCategory(waxOutputs, shownOutputs);
    }

    static boolean isHeatedCategory(List<ItemStack> waxOutputs, List<ItemStack> shownOutputs) {
        return shownOutputs.stream()
                .noneMatch(shown -> waxOutputs.stream()
                        .anyMatch(wax -> ItemStack.isSameItemSameTags(shown, wax)));
    }
}
