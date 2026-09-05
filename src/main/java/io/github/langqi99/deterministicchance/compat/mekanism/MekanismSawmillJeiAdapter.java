package io.github.langqi99.deterministicchance.compat.mekanism;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import io.github.langqi99.deterministicchance.compat.jei.ChanceStack;
import io.github.langqi99.deterministicchance.compat.jei.JeiRecipeBatchAdapter;
import io.github.langqi99.deterministicchance.core.ChanceFraction;
import java.util.ArrayList;
import java.util.List;
import mekanism.api.recipes.SawmillRecipe;
import net.minecraft.world.item.ItemStack;

public final class MekanismSawmillJeiAdapter implements JeiRecipeBatchAdapter {
    @Override
    public boolean supports(Object recipe) {
        return recipe instanceof SawmillRecipe;
    }

    @Override
    public List<ChanceStack> outputs(Object recipeObject) {
        SawmillRecipe recipe = (SawmillRecipe) recipeObject;
        List<ChanceStack> result = new ArrayList<>(2);
        addFirst(result, recipe.getMainOutputDefinition(), ChanceFraction.ALWAYS);
        addFirst(
                result,
                recipe.getSecondaryOutputDefinition(),
                ChanceFraction.fromDouble(recipe.getSecondaryChance()));
        return result;
    }

    private static void addFirst(
            List<ChanceStack> result,
            List<ItemStack> definitions,
            ChanceFraction chance) {
        if (definitions.isEmpty() || definitions.get(0).isEmpty() || chance.isNever()) {
            return;
        }
        ItemStack stack = definitions.get(0);
        result.add(new ChanceStack(
                new GenericStack(AEItemKey.of(stack), stack.getCount()),
                chance));
    }
}
