package io.github.langqi99.deterministicchance.gametest.create;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import io.github.langqi99.deterministicchance.compat.create.CreateSequencedAssemblyJeiAdapter;
import io.github.langqi99.deterministicchance.compat.create.SequencedAssemblySequenceController;
import io.github.langqi99.deterministicchance.compat.create.CreateProcessingJeiAdapter;
import io.github.langqi99.deterministicchance.compat.create.CreateRecipeSupport;
import io.github.langqi99.deterministicchance.compat.jei.JeiRecipeBatchAdapterRegistry;
import io.github.langqi99.deterministicchance.core.ChanceFraction;
import io.github.langqi99.deterministicchance.gametest.AePatternPlanAssertions;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraftforge.fluids.FluidStack;

/** Loaded reflectively only when Create, JEI and AE2 are all present. */
public final class CreateJeiGameTestCase {
    private CreateJeiGameTestCase() {
    }

    public static void run(GameTestHelper helper) {
        Fixture fixture = findFixture(helper);
        List<List<GenericStack>> nativeInputs = new CreateProcessingJeiAdapter().inputs(fixture.recipe());
        GenericStack wrongJeiInput = new GenericStack(
                Objects.requireNonNull(AEItemKey.of(new ItemStack(net.minecraft.world.item.Items.BARRIER))),
                1);

        Map<AEKey, Long> expectedOutputs = expectedOutputs(
                fixture.recipe(), fixture.executions());
        AePatternPlanAssertions.assertExactPlanAndEncodedPattern(
                helper,
                JeiRecipeBatchAdapterRegistry.plan(
                        fixture.recipe(),
                        List.of(List.of(wrongJeiInput))),
                fixture.executions(),
                nativeInputs,
                expectedOutputs,
                "Create recipe " + fixture.recipe().getId());
        verifySequencedAssembly(helper);
        helper.succeed();
    }

    private static void verifySequencedAssembly(GameTestHelper helper) {
        SequencedAssemblyRecipe recipe = helper.getLevel().getRecipeManager().getRecipes().stream()
                .filter(SequencedAssemblyRecipe.class::isInstance)
                .map(SequencedAssemblyRecipe.class::cast)
                .filter(candidate -> candidate.resultPool.size() > 1)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Create has no weighted sequenced assembly fixture"));
        var adapter = new CreateSequencedAssemblyJeiAdapter();
        var cycle = SequencedAssemblySequenceController.cycle(recipe.resultPool);
        Map<AEKey, Long> expected = new LinkedHashMap<>();
        for (int index = 0; index < recipe.resultPool.size(); index++) {
            ItemStack stack = recipe.resultPool.get(index).getStack();
            long amount = Math.multiplyExact(stack.getCount(), cycle.weight(index));
            if (!stack.isEmpty() && amount > 0) {
                expected.merge(Objects.requireNonNull(AEItemKey.of(stack)), amount, Math::addExact);
            }
        }
        AePatternPlanAssertions.assertExactPlanAndEncodedPattern(
                helper,
                JeiRecipeBatchAdapterRegistry.plan(recipe, List.of()),
                cycle.totalWeight(),
                adapter.inputs(recipe),
                expected,
                "Create sequenced assembly " + recipe.getId());
    }

    private static Fixture findFixture(GameTestHelper helper) {
        for (Recipe<?> candidate : helper.getLevel().getRecipeManager().getRecipes()) {
            if (!(candidate instanceof ProcessingRecipe<?> recipe)) {
                continue;
            }
            if (!CreateRecipeSupport.isDeterministic(recipe)) {
                continue;
            }
            ItemStack input = firstInput(recipe);
            if (input.isEmpty()) {
                continue;
            }

            boolean probabilistic = false;
            long executions = 1;
            for (ProcessingOutput output : recipe.getRollableResults()) {
                ChanceFraction chance = ChanceFraction.fromFloat(output.getChance());
                if (output.getStack().isEmpty() || chance.isNever()) {
                    continue;
                }
                probabilistic |= !chance.isCertain();
                executions = lcm(executions, chance.denominator());
            }
            if (!probabilistic || executions > 10_000) {
                continue;
            }

            Map<AEKey, Long> outputs = expectedOutputs(recipe, executions);
            if (!outputs.isEmpty() && outputs.size() <= 3) {
                return new Fixture(recipe, input, executions);
            }
        }
        throw new AssertionError(
                "Create loaded without an encodable probabilistic ProcessingRecipe fixture");
    }

    private static ItemStack firstInput(ProcessingRecipe<?> recipe) {
        for (Ingredient ingredient : recipe.getIngredients()) {
            ItemStack[] candidates = ingredient.getItems();
            if (candidates.length > 0 && !candidates[0].isEmpty()) {
                return candidates[0].copy();
            }
        }
        return ItemStack.EMPTY;
    }

    private static Map<AEKey, Long> expectedOutputs(
            ProcessingRecipe<?> recipe,
            long executions) {
        Map<AEKey, Long> expected = new LinkedHashMap<>();
        for (ProcessingOutput output : recipe.getRollableResults()) {
            ItemStack stack = output.getStack();
            ChanceFraction chance = ChanceFraction.fromFloat(output.getChance());
            if (stack.isEmpty() || chance.isNever()) {
                continue;
            }
            long successfulItems = Math.multiplyExact(
                    chance.numerator(), executions / chance.denominator());
            long amount = Math.multiplyExact(stack.getCount(), successfulItems);
            expected.merge(Objects.requireNonNull(AEItemKey.of(stack)), amount, Math::addExact);
        }
        for (FluidStack stack : recipe.getFluidResults()) {
            if (!stack.isEmpty()) {
                long amount = Math.multiplyExact(stack.getAmount(), executions);
                expected.merge(Objects.requireNonNull(AEFluidKey.of(stack)), amount, Math::addExact);
            }
        }
        return expected;
    }

    private static long lcm(long first, long second) {
        return Math.multiplyExact(first / gcd(first, second), second);
    }

    private static long gcd(long first, long second) {
        while (second != 0) {
            long remainder = first % second;
            first = second;
            second = remainder;
        }
        return Math.abs(first);
    }

    private record Fixture(ProcessingRecipe<?> recipe, ItemStack input, long executions) {
    }
}
