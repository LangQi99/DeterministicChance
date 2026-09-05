package io.github.langqi99.deterministicchance.gametest.gtceu;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient;
import io.github.langqi99.deterministicchance.compat.gtceu.GTCEu7RecipeJeiAdapter;
import io.github.langqi99.deterministicchance.compat.jei.JeiBatchPlan;
import io.github.langqi99.deterministicchance.compat.jei.JeiRecipeBatchAdapterRegistry;
import io.github.langqi99.deterministicchance.gametest.AePatternPlanAssertions;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.fluids.FluidStack;

/** Loaded reflectively only when GTCEu 7, JEI and AE2 are all present. */
public final class GTCEuJeiGameTestCase {
    private GTCEuJeiGameTestCase() {
    }

    public static void run(GameTestHelper helper) {
        GTCEuRecipeFixture.Fixture fixture = GTCEuRecipeFixture.find(helper);
        GTRecipe recipe = fixture.recipe();
        GTCEu7RecipeJeiAdapter nativeAdapter = new GTCEu7RecipeJeiAdapter();
        helper.assertTrue(
                nativeAdapter.supports(recipe),
                "Production GTCEu JEI adapter rejected selected real recipe " + recipe.getId());
        helper.assertTrue(
                fixture.targetOutput().tierChanceBoost == 0,
                "GTCEu JEI fixture unexpectedly has a tier-dependent chance");
        List<List<GenericStack>> nativeInputs = nativeAdapter.inputs(recipe);

        // A deliberately bogus viewer input proves the central registry uses
        // GTCEu's actually-consumed item/fluid inputs instead of JEI role slots.
        GenericStack wrongJeiInput = new GenericStack(
                Objects.requireNonNull(AEItemKey.of(new ItemStack(Items.BARRIER))),
                1);
        JeiBatchPlan plan = JeiRecipeBatchAdapterRegistry.plan(
                        recipe,
                        List.of(List.of(wrongJeiInput)))
                .orElseThrow(() -> new AssertionError(
                        "Production registry produced no exact GTCEu batch for " + recipe.getId()));

        helper.assertTrue(
                plan.executions() == fixture.exactBatch(),
                "GTCEu recipe " + recipe.getId() + " planned " + plan.executions()
                        + " executions; expected " + fixture.exactBatch());
        assertScaledNativeInputs(helper, nativeInputs, plan.inputs(), fixture.exactBatch());
        helper.assertFalse(
                plan.inputs().stream().flatMap(List::stream)
                        .anyMatch(stack -> stack.what().equals(wrongJeiInput.what())),
                "GTCEu registry used the synthetic JEI input instead of native recipe inputs");

        Map<AEKey, Long> expectedOutputs = expectedExactOutputs(fixture);
        helper.assertTrue(
                AePatternPlanAssertions.amountsByKey(plan.outputs()).equals(expectedOutputs),
                "GTCEu recipe " + recipe.getId() + " planned outputs " + plan.outputs()
                        + "; expected exact outputs " + expectedOutputs);

        GenericStack[] selectedInputs = plan.inputs().stream()
                .map(alternatives -> alternatives.get(0))
                .toArray(GenericStack[]::new);
        ItemStack encoded = PatternDetailsHelper.encodeProcessingPattern(
                selectedInputs,
                plan.outputs().toArray(GenericStack[]::new));
        helper.assertTrue(
                !encoded.isEmpty() && PatternDetailsHelper.isEncodedPattern(encoded),
                "GTCEu exact batch did not encode as an AE2 processing pattern");

        IPatternDetails decoded = PatternDetailsHelper.decodePattern(encoded, helper.getLevel());
        helper.assertTrue(decoded != null, "AE2 could not decode the GTCEu processing pattern");
        if (decoded != null) {
            helper.assertTrue(
                    decodedInputs(decoded).equals(
                            AePatternPlanAssertions.amountsByKey(List.of(selectedInputs))),
                    "AE2 changed selected GTCEu native inputs during pattern encode/decode");
            helper.assertTrue(
                    AePatternPlanAssertions.amountsByKey(List.of(decoded.getOutputs()))
                            .equals(expectedOutputs),
                    "AE2 changed exact GTCEu outputs during pattern encode/decode");
        }
        helper.succeed();
    }

    private static void assertScaledNativeInputs(
            GameTestHelper helper,
            List<List<GenericStack>> nativeInputs,
            List<List<GenericStack>> plannedInputs,
            long executions) {
        helper.assertTrue(
                plannedInputs.size() == nativeInputs.size(),
                "GTCEu planner changed the number of native input slots");
        for (int slot = 0; slot < nativeInputs.size(); slot++) {
            List<GenericStack> nativeAlternatives = nativeInputs.get(slot);
            List<GenericStack> plannedAlternatives = plannedInputs.get(slot);
            helper.assertTrue(
                    plannedAlternatives.size() == nativeAlternatives.size(),
                    "GTCEu planner changed native alternatives for input slot " + slot);
            for (int alternative = 0; alternative < nativeAlternatives.size(); alternative++) {
                GenericStack nativeStack = nativeAlternatives.get(alternative);
                GenericStack plannedStack = plannedAlternatives.get(alternative);
                helper.assertTrue(
                        plannedStack.what().equals(nativeStack.what()),
                        "GTCEu planner changed a native input key");
                helper.assertTrue(
                        plannedStack.amount() == Math.multiplyExact(nativeStack.amount(), executions),
                        "GTCEu planner did not scale a native input by its exact batch size");
            }
        }
    }

    private static Map<AEKey, Long> expectedExactOutputs(
            GTCEuRecipeFixture.Fixture fixture) {
        Map<AEKey, Long> result = new LinkedHashMap<>();
        appendExpectedOutputs(result, fixture, ItemRecipeCapability.CAP);
        appendExpectedOutputs(result, fixture, FluidRecipeCapability.CAP);
        return result;
    }

    private static void appendExpectedOutputs(
            Map<AEKey, Long> result,
            GTCEuRecipeFixture.Fixture fixture,
            RecipeCapability<?> capability) {
        for (Content content : fixture.recipe().getOutputContents(capability)) {
            GenericStack stack = genericStack(capability, content);
            if (stack == null || content.maxChance <= 0) {
                continue;
            }

            long successes;
            if (content.chance >= content.maxChance) {
                successes = fixture.exactBatch();
            } else if (content.chance <= 0 && content.tierChanceBoost <= 0) {
                continue;
            } else {
                assertNoTierBoost(content);
                successes = exactBaseSuccesses(fixture, content);
            }
            if (successes > 0) {
                result.merge(
                        stack.what(),
                        Math.multiplyExact(stack.amount(), successes),
                        Math::addExact);
            }
        }
    }

    private static long exactBaseSuccesses(
            GTCEuRecipeFixture.Fixture fixture,
            Content content) {
        var chance = GTCEuRecipeFixture.effectiveChance(
                fixture.recipe(),
                content,
                fixture.recipeTier(),
                fixture.recipeTier());
        if (fixture.exactBatch() % chance.denominator() != 0) {
            throw new AssertionError("GTCEu exact batch is not a whole base-chance cycle");
        }
        return Math.multiplyExact(
                fixture.exactBatch() / chance.denominator(),
                (long) chance.numerator());
    }

    private static void assertNoTierBoost(Content content) {
        if (content.tierChanceBoost != 0) {
            throw new AssertionError(
                    "Tier-boosted GTCEu output reached an exact JEI fixture: " + content);
        }
    }

    private static GenericStack genericStack(
            RecipeCapability<?> capability,
            Content content) {
        if (capability == ItemRecipeCapability.CAP) {
            Ingredient ingredient = ItemRecipeCapability.CAP.of(content.content);
            ItemStack[] stacks = ingredient.getItems();
            return stacks.length == 0 || stacks[0].isEmpty()
                    ? null
                    : GenericStack.fromItemStack(stacks[0]);
        }
        if (capability == FluidRecipeCapability.CAP) {
            FluidIngredient ingredient = FluidRecipeCapability.CAP.of(content.content);
            FluidStack[] stacks = ingredient.getStacks();
            return stacks.length == 0 || stacks[0].isEmpty()
                    ? null
                    : GenericStack.fromFluidStack(stacks[0]);
        }
        return null;
    }

    private static Map<AEKey, Long> decodedInputs(IPatternDetails decoded) {
        List<GenericStack> concreteInputs = new ArrayList<>();
        for (IPatternDetails.IInput input : decoded.getInputs()) {
            GenericStack[] alternatives = input.getPossibleInputs();
            if (alternatives.length > 0) {
                GenericStack selected = alternatives[0];
                concreteInputs.add(new GenericStack(
                        selected.what(),
                        Math.multiplyExact(selected.amount(), input.getMultiplier())));
            }
        }
        return AePatternPlanAssertions.amountsByKey(concreteInputs);
    }
}
