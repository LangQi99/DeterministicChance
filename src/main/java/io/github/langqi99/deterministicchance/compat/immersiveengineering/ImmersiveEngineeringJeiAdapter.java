package io.github.langqi99.deterministicchance.compat.immersiveengineering;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import blusunrize.immersiveengineering.api.crafting.ArcFurnaceRecipe;
import blusunrize.immersiveengineering.api.crafting.CrusherRecipe;
import blusunrize.immersiveengineering.api.crafting.IngredientWithSize;
import blusunrize.immersiveengineering.api.crafting.MultiblockRecipe;
import blusunrize.immersiveengineering.api.crafting.StackWithChance;
import io.github.langqi99.deterministicchance.compat.jei.ChanceStack;
import io.github.langqi99.deterministicchance.compat.jei.NativeInputJeiRecipeBatchAdapter;
import io.github.langqi99.deterministicchance.core.ChanceFraction;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;

/** Native recipe bridge for IE 10.2's Crusher and Arc Furnace JEI recipes. */
public final class ImmersiveEngineeringJeiAdapter implements NativeInputJeiRecipeBatchAdapter {
    @Override
    public boolean supports(Object recipe) {
        return recipe instanceof CrusherRecipe || recipe instanceof ArcFurnaceRecipe;
    }

    @Override
    public boolean hasProbabilisticOutputs(Object recipe) {
        boolean arcFurnace = recipe instanceof ArcFurnaceRecipe;
        for (StackWithChance output : chanceOutputs(recipe)) {
            float chance = output.chance();
            if (!output.stack().get().isEmpty()
                    && chanceNeedsTakeover(chance, arcFurnace)) {
                return true;
            }
        }
        return false;
    }

    static boolean chanceNeedsTakeover(float chance, boolean arcFurnace) {
        return !Float.isFinite(chance)
                // IE's Arc JEI category displays every secondary, including
                // NEVER outputs. Take those recipes over so AE does not encode
                // chance <= 0 as guaranteed.
                || (arcFurnace ? chance < 1 : chance > 0 && chance < 1);
    }

    @Override
    public Optional<String> exactBatchUnsupportedReason(Object recipe) {
        for (StackWithChance output : chanceOutputs(recipe)) {
            if (!output.stack().get().isEmpty() && !Float.isFinite(output.chance())) {
                return Optional.of(
                        "Immersive Engineering recipe contains a non-finite output chance");
            }
        }
        try {
            if (recipe instanceof CrusherRecipe
                    && recipe.getClass().getMethod("getActualItemOutputs").getDeclaringClass()
                            != CrusherRecipe.class) {
                return Optional.of(
                        "Crusher recipe overrides its output generator; exact chance semantics are unknown");
            }
            if (recipe instanceof ArcFurnaceRecipe
                    && recipe.getClass().getMethod(
                                    "generateActualOutput",
                                    ItemStack.class,
                                    NonNullList.class,
                                    long.class)
                            .getDeclaringClass() != ArcFurnaceRecipe.class) {
                return Optional.of(
                        "Arc Furnace recipe overrides its output generator; exact chance semantics are unknown");
            }
        } catch (NoSuchMethodException exception) {
            return Optional.of("Immersive Engineering output API does not match 10.2.0-183");
        }
        return Optional.empty();
    }

    @Override
    public List<List<GenericStack>> inputs(Object recipeObject) {
        MultiblockRecipe recipe = (MultiblockRecipe) recipeObject;
        List<IngredientWithSize> nativeInputs = recipe.getItemInputs();
        if (nativeInputs == null || nativeInputs.isEmpty()) {
            throw new IllegalArgumentException("IE recipe has no consumed item inputs");
        }

        List<List<GenericStack>> result = new ArrayList<>(nativeInputs.size());
        for (IngredientWithSize ingredient : nativeInputs) {
            List<GenericStack> alternatives = new ArrayList<>();
            for (ItemStack stack : ingredient.getMatchingStacks()) {
                GenericStack generic = GenericStack.fromItemStack(stack);
                if (generic != null && generic.amount() > 0) {
                    alternatives.add(generic);
                }
            }
            if (alternatives.isEmpty()) {
                throw new IllegalArgumentException("IE recipe has an unresolved item input");
            }
            result.add(List.copyOf(alternatives));
        }
        return List.copyOf(result);
    }

    @Override
    public List<ChanceStack> outputs(Object recipeObject) {
        List<ChanceStack> result = new ArrayList<>();
        if (recipeObject instanceof CrusherRecipe recipe) {
            add(result, recipe.output.get(), ChanceFraction.ALWAYS);
            appendChanceOutputs(result, recipe.secondaryOutputs);
        } else if (recipeObject instanceof ArcFurnaceRecipe recipe) {
            for (ItemStack output : recipe.getBaseOutputs()) {
                add(result, output, ChanceFraction.ALWAYS);
            }
            appendChanceOutputs(result, recipe.secondaryOutputs);
            add(result, recipe.slag.get(), ChanceFraction.ALWAYS);
        } else {
            throw new IllegalArgumentException("Unsupported IE recipe type: " + recipeObject);
        }
        return List.copyOf(result);
    }

    private static List<StackWithChance> chanceOutputs(Object recipe) {
        if (recipe instanceof CrusherRecipe crusher) {
            return crusher.secondaryOutputs;
        }
        return ((ArcFurnaceRecipe) recipe).secondaryOutputs;
    }

    private static void appendChanceOutputs(
            List<ChanceStack> result,
            List<StackWithChance> outputs) {
        for (StackWithChance output : outputs) {
            add(result, output.stack().get(), IEChance.fromRaw(output.chance()));
        }
    }

    private static void add(
            List<ChanceStack> result,
            ItemStack stack,
            ChanceFraction chance) {
        if (stack.isEmpty() || chance.isNever()) {
            return;
        }
        result.add(new ChanceStack(
                new GenericStack(Objects.requireNonNull(AEItemKey.of(stack)), stack.getCount()),
                chance));
    }
}
