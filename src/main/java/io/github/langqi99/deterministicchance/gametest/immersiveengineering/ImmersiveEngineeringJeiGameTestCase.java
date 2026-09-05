package io.github.langqi99.deterministicchance.gametest.immersiveengineering;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import blusunrize.immersiveengineering.api.crafting.ArcFurnaceRecipe;
import blusunrize.immersiveengineering.api.crafting.CrusherRecipe;
import blusunrize.immersiveengineering.api.crafting.StackWithChance;
import io.github.langqi99.deterministicchance.compat.immersiveengineering.IEChance;
import io.github.langqi99.deterministicchance.compat.immersiveengineering.ImmersiveEngineeringJeiAdapter;
import io.github.langqi99.deterministicchance.compat.jei.JeiBatchPlan;
import io.github.langqi99.deterministicchance.compat.jei.JeiRecipeBatchAdapterRegistry;
import io.github.langqi99.deterministicchance.core.ChanceFraction;
import io.github.langqi99.deterministicchance.gametest.AePatternPlanAssertions;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;

/** Real released IE recipes through the production registry and AE2 pattern codec. */
public final class ImmersiveEngineeringJeiGameTestCase {
    private static final ResourceLocation CRUSHER_FIXTURE =
            ResourceLocation.fromNamespaceAndPath("immersiveengineering", "crusher/gravel");
    private static final ResourceLocation ARC_FIXTURE =
            ResourceLocation.fromNamespaceAndPath("immersiveengineering", "arcfurnace/raw_ore_iron");

    private ImmersiveEngineeringJeiGameTestCase() {
    }

    public static void run(GameTestHelper helper) {
        verify(helper, recipe(helper, CRUSHER_FIXTURE, CrusherRecipe.class), 10);
        verify(helper, recipe(helper, ARC_FIXTURE, ArcFurnaceRecipe.class), 2);
        helper.succeed();
    }

    private static void verify(GameTestHelper helper, Object recipe, long expectedExecutions) {
        ImmersiveEngineeringJeiAdapter adapter = new ImmersiveEngineeringJeiAdapter();
        helper.assertTrue(adapter.supports(recipe),
                "Production IE adapter rejected released recipe " + id(recipe));
        List<List<GenericStack>> nativeInputs = adapter.inputs(recipe);
        helper.assertFalse(nativeInputs.isEmpty(),
                "Released IE recipe exposed no native consumed inputs: " + id(recipe));

        GenericStack wrongViewerInput = new GenericStack(
                Objects.requireNonNull(AEItemKey.of(new ItemStack(Items.BARRIER))),
                1);
        JeiBatchPlan plan = JeiRecipeBatchAdapterRegistry.plan(
                        recipe,
                        List.of(List.of(wrongViewerInput)))
                .orElseThrow(() -> new AssertionError(
                        "Production registry produced no exact IE batch for " + id(recipe)));

        helper.assertTrue(plan.executions() == expectedExecutions,
                "IE recipe " + id(recipe) + " planned " + plan.executions()
                        + " executions; expected " + expectedExecutions);
        assertScaledNativeInputs(helper, nativeInputs, plan.inputs(), expectedExecutions, id(recipe));
        helper.assertFalse(
                plan.inputs().stream().flatMap(List::stream)
                        .anyMatch(stack -> stack.what().equals(wrongViewerInput.what())),
                "IE registry encoded a synthetic JEI role slot instead of native inputs");

        Map<AEKey, Long> expectedOutputs = expectedOutputs(recipe, expectedExecutions);
        helper.assertTrue(
                AePatternPlanAssertions.amountsByKey(plan.outputs()).equals(expectedOutputs),
                "IE recipe " + id(recipe) + " planned unexpected outputs " + plan.outputs());

        GenericStack[] selectedInputs = plan.inputs().stream()
                .map(alternatives -> alternatives.get(0))
                .toArray(GenericStack[]::new);
        ItemStack encoded = PatternDetailsHelper.encodeProcessingPattern(
                selectedInputs,
                plan.outputs().toArray(GenericStack[]::new));
        helper.assertTrue(!encoded.isEmpty() && PatternDetailsHelper.isEncodedPattern(encoded),
                "IE exact batch did not encode as an AE2 processing pattern: " + id(recipe));
        IPatternDetails decoded = PatternDetailsHelper.decodePattern(encoded, helper.getLevel());
        helper.assertTrue(decoded != null,
                "AE2 could not decode the IE processing pattern: " + id(recipe));
        if (decoded != null) {
            helper.assertTrue(
                    decodedInputs(decoded).equals(
                            AePatternPlanAssertions.amountsByKey(List.of(selectedInputs))),
                    "AE2 changed selected IE native inputs during pattern encode/decode");
            helper.assertTrue(
                    AePatternPlanAssertions.amountsByKey(List.of(decoded.getOutputs()))
                            .equals(expectedOutputs),
                    "AE2 changed IE outputs during pattern encode/decode");
        }
    }

    private static Map<AEKey, Long> expectedOutputs(Object recipe, long executions) {
        Map<AEKey, Long> result = new LinkedHashMap<>();
        if (recipe instanceof CrusherRecipe crusher) {
            add(result, crusher.output.get(), executions);
            appendChanceOutputs(result, crusher.secondaryOutputs, executions);
        } else {
            ArcFurnaceRecipe arc = (ArcFurnaceRecipe) recipe;
            for (ItemStack output : arc.getBaseOutputs()) {
                add(result, output, executions);
            }
            appendChanceOutputs(result, arc.secondaryOutputs, executions);
            add(result, arc.slag.get(), executions);
        }
        return result;
    }

    private static void appendChanceOutputs(
            Map<AEKey, Long> result,
            List<StackWithChance> outputs,
            long executions) {
        for (StackWithChance output : outputs) {
            ChanceFraction chance = IEChance.fromRaw(output.chance());
            long successes = Math.multiplyExact(
                    executions / chance.denominator(),
                    chance.numerator());
            add(result, output.stack().get(), successes);
        }
    }

    private static void add(Map<AEKey, Long> result, ItemStack stack, long copies) {
        if (!stack.isEmpty() && copies > 0) {
            result.merge(
                    Objects.requireNonNull(AEItemKey.of(stack)),
                    Math.multiplyExact(stack.getCount(), copies),
                    Math::addExact);
        }
    }

    private static void assertScaledNativeInputs(
            GameTestHelper helper,
            List<List<GenericStack>> nativeInputs,
            List<List<GenericStack>> plannedInputs,
            long executions,
            ResourceLocation recipeId) {
        helper.assertTrue(plannedInputs.size() == nativeInputs.size(),
                "IE planner changed native input slot count for " + recipeId);
        for (int slot = 0; slot < nativeInputs.size(); slot++) {
            List<GenericStack> nativeAlternatives = nativeInputs.get(slot);
            List<GenericStack> plannedAlternatives = plannedInputs.get(slot);
            helper.assertTrue(plannedAlternatives.size() == nativeAlternatives.size(),
                    "IE planner changed native alternatives for " + recipeId);
            for (int alternative = 0; alternative < nativeAlternatives.size(); alternative++) {
                GenericStack nativeStack = nativeAlternatives.get(alternative);
                GenericStack plannedStack = plannedAlternatives.get(alternative);
                helper.assertTrue(plannedStack.what().equals(nativeStack.what()),
                        "IE planner changed a native input key for " + recipeId);
                helper.assertTrue(
                        plannedStack.amount() == Math.multiplyExact(nativeStack.amount(), executions),
                        "IE planner did not scale a native input for " + recipeId);
            }
        }
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

    private static ResourceLocation id(Object recipe) {
        return ((Recipe<?>) recipe).getId();
    }

    private static <T> T recipe(
            GameTestHelper helper,
            ResourceLocation id,
            Class<T> type) {
        Recipe<?> recipe = helper.getLevel().getRecipeManager().byKey(id).orElse(null);
        if (type.isInstance(recipe)) {
            return type.cast(recipe);
        }
        throw new AssertionError("Missing released IE recipe fixture " + id);
    }
}
