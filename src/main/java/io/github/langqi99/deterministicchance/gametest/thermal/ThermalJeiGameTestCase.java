package io.github.langqi99.deterministicchance.gametest.thermal;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import cofh.thermal.core.util.recipes.machine.SawmillRecipe;
import cofh.thermal.core.util.recipes.machine.SmelterRecipe;
import io.github.langqi99.deterministicchance.compat.jei.JeiBatchDecision;
import io.github.langqi99.deterministicchance.compat.jei.JeiBatchPlan;
import io.github.langqi99.deterministicchance.compat.jei.JeiRecipeBatchAdapterRegistry;
import io.github.langqi99.deterministicchance.compat.thermal.ThermalRecipeJeiAdapter;
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

/** Loaded reflectively only when Thermal, JEI and AE2 are all present. */
public final class ThermalJeiGameTestCase {
    private static final ResourceLocation LOCKED_FIXTURE_ID =
            ResourceLocation.fromNamespaceAndPath(
                    "thermal",
                    "machines/smelter/smelter_ancient_debris");
    private static final ResourceLocation CATALYZABLE_FIXTURE_ID =
            ResourceLocation.fromNamespaceAndPath(
                    "thermal",
                    "machines/sawmill/sawmill_oak_logs");
    private static final long EXECUTIONS = 5;

    private ThermalJeiGameTestCase() {
    }

    public static void run(GameTestHelper helper) {
        SmelterRecipe lockedRecipe = lockedFixture(helper);
        assertStableLockedFixture(lockedRecipe);
        SawmillRecipe catalyzableRecipe = catalyzableFixture(helper);
        assertStableCatalyzableFixture(catalyzableRecipe);

        GenericStack wrongJeiInput = new GenericStack(
                Objects.requireNonNull(AEItemKey.of(new ItemStack(Items.BARRIER))),
                1);

        // Recognized probabilistic recipes with a catalyst-sensitive profile
        // must not fall through to AE2's ordinary one-run pattern transfer.
        JeiBatchDecision catalyzableDecision = JeiRecipeBatchAdapterRegistry.decide(
                catalyzableRecipe,
                List.of(List.of(wrongJeiInput)));
        helper.assertTrue(
                catalyzableDecision.status()
                        == JeiBatchDecision.Status.RECOGNIZED_BUT_UNSUPPORTED,
                "Catalyzable Thermal oak sawmill decision was " + catalyzableDecision.status()
                        + "; expected RECOGNIZED_BUT_UNSUPPORTED");
        helper.assertTrue(
                catalyzableDecision.exactPlan().isEmpty(),
                "Catalyzable Thermal oak sawmill unexpectedly exposed an exact plan");
        helper.assertTrue(
                !catalyzableDecision.reason().isBlank(),
                "Unsupported Thermal decision did not explain why exact transfer is unsafe");

        ThermalRecipeJeiAdapter nativeAdapter = new ThermalRecipeJeiAdapter();
        List<List<GenericStack>> nativeInputs = nativeAdapter.inputs(lockedRecipe);
        helper.assertTrue(
                nativeInputs.size() == 1 && !nativeInputs.get(0).isEmpty(),
                "Locked Thermal ancient-debris fixture did not expose one native input");

        // A deliberately wrong JEI fallback proves that the production registry
        // uses Thermal's consumed inputs instead of viewer-only role slots.
        JeiBatchDecision exactDecision = JeiRecipeBatchAdapterRegistry.decide(
                lockedRecipe,
                List.of(List.of(wrongJeiInput)));
        helper.assertTrue(
                exactDecision.status() == JeiBatchDecision.Status.EXACT_PLAN,
                "Locked Thermal ancient-debris decision was " + exactDecision.status()
                        + ": " + exactDecision.reason());
        JeiBatchPlan plan = exactDecision.exactPlan().orElseThrow(() ->
                new AssertionError("Production registry returned no exact plan for " + LOCKED_FIXTURE_ID));

        helper.assertTrue(
                plan.executions() == EXECUTIONS,
                "Locked Thermal ancient-debris recipe planned " + plan.executions()
                        + " executions; expected 5");
        assertScaledNativeInputs(helper, nativeInputs, plan.inputs());

        Map<AEKey, Long> expectedOutputs = expectedOutputs(lockedRecipe);
        helper.assertTrue(
                AePatternPlanAssertions.amountsByKey(plan.outputs()).equals(expectedOutputs),
                "Locked Thermal ancient-debris recipe planned unexpected outputs: "
                        + plan.outputs());

        // AE2's encoded processing pattern stores the concrete alternative the
        // terminal selected. Pick the first native alternative from every slot,
        // then exercise AE2's real encoder and decoder.
        GenericStack[] selectedInputs = plan.inputs().stream()
                .map(alternatives -> alternatives.get(0))
                .toArray(GenericStack[]::new);
        ItemStack encoded = PatternDetailsHelper.encodeProcessingPattern(
                selectedInputs,
                plan.outputs().toArray(GenericStack[]::new));
        helper.assertTrue(
                !encoded.isEmpty() && PatternDetailsHelper.isEncodedPattern(encoded),
                "Locked Thermal exact batch did not encode as an AE2 processing pattern");

        IPatternDetails decoded = PatternDetailsHelper.decodePattern(encoded, helper.getLevel());
        helper.assertTrue(decoded != null,
                "AE2 could not decode the Thermal processing pattern");
        if (decoded != null) {
            helper.assertTrue(
                    decodedInputs(decoded).equals(AePatternPlanAssertions.amountsByKey(List.of(selectedInputs))),
                    "AE2 changed the selected Thermal inputs during pattern encode/decode");
            helper.assertTrue(
                    AePatternPlanAssertions.amountsByKey(List.of(decoded.getOutputs())).equals(expectedOutputs),
                    "AE2 changed the Thermal outputs during pattern encode/decode");
        }
        helper.succeed();
    }

    private static SmelterRecipe lockedFixture(GameTestHelper helper) {
        Recipe<?> recipe = helper.getLevel().getRecipeManager().byKey(LOCKED_FIXTURE_ID).orElse(null);
        if (recipe instanceof SmelterRecipe smelterRecipe) {
            return smelterRecipe;
        }
        throw new AssertionError("Missing released Thermal recipe fixture " + LOCKED_FIXTURE_ID);
    }

    private static SawmillRecipe catalyzableFixture(GameTestHelper helper) {
        Recipe<?> recipe = helper.getLevel().getRecipeManager()
                .byKey(CATALYZABLE_FIXTURE_ID)
                .orElse(null);
        if (recipe instanceof SawmillRecipe sawmillRecipe) {
            return sawmillRecipe;
        }
        throw new AssertionError("Missing released Thermal recipe fixture " + CATALYZABLE_FIXTURE_ID);
    }

    private static void assertStableLockedFixture(SmelterRecipe recipe) {
        if (recipe.isCatalyzable()
                || recipe.getInputItems().size() != 1
                || recipe.getInputItems().get(0).getItems().length == 0
                || recipe.getOutputItems().size() != 2
                || recipe.getOutputItemChances().size() != 2
                || Math.abs(recipe.getOutputItemChances().get(0)) != 1.0F
                || Math.abs(recipe.getOutputItemChances().get(1)) != 0.2F
                || recipe.getOutputItemChances().get(0) >= 0.0F
                || recipe.getOutputItemChances().get(1) >= 0.0F) {
            throw new AssertionError(
                    "Thermal released ancient-debris smelter fixture no longer has locked 1 + 20% semantics");
        }
    }

    private static void assertStableCatalyzableFixture(SawmillRecipe recipe) {
        if (!recipe.isCatalyzable()
                || recipe.getInputItems().size() != 1
                || recipe.getOutputItems().size() != 2
                || recipe.getOutputItemChances().size() != 2
                || Math.abs(recipe.getOutputItemChances().get(0)) != 1.0F
                || Math.abs(recipe.getOutputItemChances().get(1)) != 1.25F) {
            throw new AssertionError(
                    "Thermal released oak sawmill fixture no longer has catalyzable 1 + 1.25 semantics");
        }
    }

    private static void assertScaledNativeInputs(
            GameTestHelper helper,
            List<List<GenericStack>> nativeInputs,
            List<List<GenericStack>> plannedInputs) {
        helper.assertTrue(plannedInputs.size() == nativeInputs.size(),
                "Thermal planner changed the number of native input slots");
        for (int slot = 0; slot < nativeInputs.size(); slot++) {
            List<GenericStack> nativeAlternatives = nativeInputs.get(slot);
            List<GenericStack> plannedAlternatives = plannedInputs.get(slot);
            helper.assertTrue(plannedAlternatives.size() == nativeAlternatives.size(),
                    "Thermal planner changed native alternatives for input slot " + slot);
            for (int alternative = 0; alternative < nativeAlternatives.size(); alternative++) {
                GenericStack nativeStack = nativeAlternatives.get(alternative);
                GenericStack plannedStack = plannedAlternatives.get(alternative);
                helper.assertTrue(plannedStack.what().equals(nativeStack.what()),
                        "Thermal planner changed a native input key");
                helper.assertTrue(
                        plannedStack.amount() == Math.multiplyExact(nativeStack.amount(), EXECUTIONS),
                        "Thermal planner did not scale a native input by five executions");
            }
        }
    }

    private static Map<AEKey, Long> expectedOutputs(SmelterRecipe recipe) {
        Map<AEKey, Long> result = new LinkedHashMap<>();
        add(result, recipe.getOutputItems().get(0), EXECUTIONS);
        add(result, recipe.getOutputItems().get(1), 1);
        return result;
    }

    private static void add(Map<AEKey, Long> result, ItemStack stack, long copies) {
        AEItemKey key = Objects.requireNonNull(AEItemKey.of(stack));
        result.merge(key, Math.multiplyExact(stack.getCount(), copies), Math::addExact);
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
