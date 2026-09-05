package io.github.langqi99.deterministicchance.gametest.mekanism;

import io.github.langqi99.deterministicchance.core.ChanceFraction;
import java.util.List;
import mekanism.api.recipes.SawmillRecipe;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;

/** Loaded reflectively only in a Mekanism runtime. */
public final class MekanismMachineGameTestCase {
    private MekanismMachineGameTestCase() {
    }

    public static void run(GameTestHelper helper) {
        SawmillRecipe recipe = findFixture(helper);
        ItemStack input = recipe.getInput().getRepresentations().get(0).copy();
        SawmillRecipe.ChanceOutput output = recipe.getOutput(input);

        ItemStack main = output.getMainOutput();
        helper.assertTrue(
                ItemStack.isSameItemSameTags(main, recipe.getMainOutputDefinition().get(0)),
                "Mekanism sawmill fixture did not return its declared main output");

        ItemStack firstPreview = output.getSecondaryOutput();
        ItemStack repeatedPreview = output.getSecondaryOutput();
        helper.assertTrue(
                ItemStack.matches(firstPreview, repeatedPreview),
                "Reading Mekanism ChanceOutput outside a machine commit changed its cached preview");
        helper.succeed();
    }

    private static SawmillRecipe findFixture(GameTestHelper helper) {
        for (Recipe<?> candidate : helper.getLevel().getRecipeManager().getRecipes()) {
            if (!(candidate instanceof SawmillRecipe recipe)) {
                continue;
            }
            ChanceFraction chance = ChanceFraction.fromDouble(recipe.getSecondaryChance());
            List<ItemStack> inputs = recipe.getInput().getRepresentations();
            if (!chance.isNever()
                    && !chance.isCertain()
                    && chance.denominator() <= 1_000
                    && !inputs.isEmpty()
                    && !inputs.get(0).isEmpty()
                    && !recipe.getMainOutputDefinition().isEmpty()
                    && !recipe.getSecondaryOutputDefinition().isEmpty()) {
                return recipe;
            }
        }
        throw new AssertionError("Mekanism loaded without a probabilistic SawmillRecipe fixture");
    }
}
