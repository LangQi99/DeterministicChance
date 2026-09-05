package io.github.langqi99.deterministicchance.gametest;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import io.github.langqi99.deterministicchance.compat.jei.JeiBatchPlan;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;

/** AE2 assertions shared by reflectively loaded JEI compatibility tests. */
public final class AePatternPlanAssertions {
    private AePatternPlanAssertions() {
    }

    public static void assertExactPlanAndEncodedPattern(
            GameTestHelper helper,
            Optional<JeiBatchPlan> candidate,
            long expectedExecutions,
            GenericStack syntheticInput,
            Map<AEKey, Long> expectedOutputs,
            String fixtureDescription) {
        assertExactPlanAndEncodedPattern(
                helper,
                candidate,
                expectedExecutions,
                List.of(List.of(syntheticInput)),
                expectedOutputs,
                fixtureDescription);
    }

    public static void assertExactPlanAndEncodedPattern(
            GameTestHelper helper,
            Optional<JeiBatchPlan> candidate,
            long expectedExecutions,
            List<List<GenericStack>> nativeInputs,
            Map<AEKey, Long> expectedOutputs,
            String fixtureDescription) {
        if (candidate.isEmpty()) {
            throw new AssertionError("No JEI batch plan was produced for " + fixtureDescription);
        }
        JeiBatchPlan plan = candidate.get();
        helper.assertTrue(
                plan.executions() == expectedExecutions,
                fixtureDescription + " planned " + plan.executions()
                        + " executions; expected " + expectedExecutions);

        helper.assertTrue(plan.inputs().size() == nativeInputs.size(),
                fixtureDescription + " changed the native input slot count");
        for (int slot = 0; slot < nativeInputs.size(); slot++) {
            List<GenericStack> expectedAlternatives = nativeInputs.get(slot);
            List<GenericStack> actualAlternatives = plan.inputs().get(slot);
            helper.assertTrue(actualAlternatives.size() == expectedAlternatives.size(),
                    fixtureDescription + " changed alternatives in input slot " + slot);
            for (int alternative = 0; alternative < expectedAlternatives.size(); alternative++) {
                GenericStack expected = expectedAlternatives.get(alternative);
                GenericStack actual = actualAlternatives.get(alternative);
                helper.assertTrue(actual.what().equals(expected.what()),
                        fixtureDescription + " changed a native input key");
                helper.assertTrue(
                        actual.amount() == Math.multiplyExact(expected.amount(), expectedExecutions),
                        fixtureDescription + " did not scale a native input by the exact batch size");
            }
        }

        Map<AEKey, Long> actualOutputs = amountsByKey(plan.outputs());
        helper.assertTrue(
                actualOutputs.equals(expectedOutputs),
                fixtureDescription + " planned AE outputs " + actualOutputs
                        + "; expected " + expectedOutputs);

        // This passes the production registry result through AE2's real pattern codec.
        // It catches invalid slot counts, unsupported keys and lossy amount conversion.
        GenericStack[] selectedInputs = plan.inputs().stream()
                .map(alternatives -> alternatives.get(0))
                .toArray(GenericStack[]::new);
        ItemStack encodedPattern = PatternDetailsHelper.encodeProcessingPattern(
                selectedInputs,
                plan.outputs().toArray(GenericStack[]::new));
        helper.assertTrue(!encodedPattern.isEmpty() && PatternDetailsHelper.isEncodedPattern(encodedPattern),
                fixtureDescription + " did not encode as an AE2 processing pattern");
        IPatternDetails decoded = PatternDetailsHelper.decodePattern(encodedPattern, helper.getLevel());
        helper.assertTrue(decoded != null,
                fixtureDescription + " encoded pattern could not be decoded by AE2");
        if (decoded != null) {
            List<GenericStack> decodedInputStacks = new ArrayList<>();
            for (IPatternDetails.IInput input : decoded.getInputs()) {
                GenericStack[] alternatives = input.getPossibleInputs();
                if (alternatives.length > 0) {
                    GenericStack selected = alternatives[0];
                    decodedInputStacks.add(new GenericStack(
                            selected.what(),
                            Math.multiplyExact(selected.amount(), input.getMultiplier())));
                }
            }
            helper.assertTrue(
                    amountsByKey(decodedInputStacks).equals(amountsByKey(List.of(selectedInputs))),
                    fixtureDescription + " AE2 pattern codec changed the selected inputs");
            helper.assertTrue(
                    amountsByKey(List.of(decoded.getOutputs())).equals(expectedOutputs),
                    fixtureDescription + " AE2 pattern codec changed the exact outputs");
        }
    }

    public static Map<AEKey, Long> amountsByKey(List<GenericStack> stacks) {
        Map<AEKey, Long> result = new LinkedHashMap<>();
        for (GenericStack stack : stacks) {
            result.merge(stack.what(), stack.amount(), Math::addExact);
        }
        return result;
    }
}
