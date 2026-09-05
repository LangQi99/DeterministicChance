package io.github.langqi99.deterministicchance.gametest.mekanism;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import io.github.langqi99.deterministicchance.compat.jei.JeiRecipeBatchAdapterRegistry;
import io.github.langqi99.deterministicchance.core.ChanceFraction;
import io.github.langqi99.deterministicchance.gametest.AePatternPlanAssertions;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import mekanism.api.recipes.SawmillRecipe;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;

/** Loaded reflectively only when Mekanism, JEI and AE2 are all present. */
public final class MekanismJeiGameTestCase {
    private MekanismJeiGameTestCase() {
    }

    public static void run(GameTestHelper helper) {
        SawmillRecipe recipe = findFixture(helper);
        ChanceFraction chance = ChanceFraction.fromDouble(recipe.getSecondaryChance());
        ItemStack input = recipe.getInput().getRepresentations().get(0);
        GenericStack syntheticInput = new GenericStack(
                Objects.requireNonNull(AEItemKey.of(input)),
                input.getCount());

        Map<AEKey, Long> expectedOutputs = new LinkedHashMap<>();
        addScaled(
                expectedOutputs,
                recipe.getMainOutputDefinition().get(0),
                chance.denominator());
        addScaled(
                expectedOutputs,
                recipe.getSecondaryOutputDefinition().get(0),
                chance.numerator());

        AePatternPlanAssertions.assertExactPlanAndEncodedPattern(
                helper,
                JeiRecipeBatchAdapterRegistry.plan(
                        recipe,
                        List.of(List.of(syntheticInput))),
                chance.denominator(),
                syntheticInput,
                expectedOutputs,
                "Mekanism sawmill recipe " + recipe.getId());
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
                    && chance.denominator() <= 10_000
                    && !inputs.isEmpty()
                    && !inputs.get(0).isEmpty()
                    && !recipe.getMainOutputDefinition().isEmpty()
                    && !recipe.getSecondaryOutputDefinition().isEmpty()) {
                return recipe;
            }
        }
        throw new AssertionError("Mekanism loaded without a probabilistic SawmillRecipe fixture");
    }

    private static void addScaled(Map<AEKey, Long> output, ItemStack stack, long factor) {
        if (!stack.isEmpty() && factor > 0) {
            output.merge(
                    Objects.requireNonNull(AEItemKey.of(stack)),
                    Math.multiplyExact(stack.getCount(), factor),
                    Math::addExact);
        }
    }
}
