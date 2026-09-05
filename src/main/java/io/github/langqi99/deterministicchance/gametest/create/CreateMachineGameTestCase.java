package io.github.langqi99.deterministicchance.gametest.create;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.kinetics.crusher.CrushingRecipe;
import com.simibubi.create.content.kinetics.crusher.CrushingWheelControllerBlockEntity;
import com.simibubi.create.content.kinetics.millstone.MillingRecipe;
import com.simibubi.create.content.kinetics.millstone.MillstoneBlockEntity;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import io.github.langqi99.deterministicchance.compat.create.ProcessingOutputSequenceController;
import io.github.langqi99.deterministicchance.core.ChanceFraction;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.wrapper.RecipeWrapper;

/** Loaded reflectively only in a Create runtime. */
public final class CreateMachineGameTestCase {
    private static final Method PROCESS = processMethod();
    private static final Method APPLY_CRUSHING_RECIPE = crushingMethod();

    private CreateMachineGameTestCase() {}

    public static void run(GameTestHelper helper) {
        Fixture fixture = findFixture(helper);
        BlockPos position = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.getLevel().setBlock(position, AllBlocks.MILLSTONE.getDefaultState(), 3);
        MillstoneBlockEntity original = requireMillstone(helper, position);

        ItemStack input = fixture.input().copy();
        input.setCount(fixture.executions());
        original.inputInv.setStackInSlot(0, input);
        process(original);

        int expectedPosition = Math.floorMod(
                fixture.target().getStack().getCount(),
                Math.toIntExact(fixture.targetChance().denominator()));
        helper.assertTrue(
                persistedPosition(original, fixture) == expectedPosition,
                "Create millstone did not persist its target output phase after one operation");

        CompoundTag saved = original.saveWithoutMetadata();
        helper.getLevel().setBlock(position, Blocks.AIR.defaultBlockState(), 3);
        helper.getLevel().setBlock(position, AllBlocks.MILLSTONE.getDefaultState(), 3);
        MillstoneBlockEntity restored = requireMillstone(helper, position);
        restored.load(saved);
        helper.assertTrue(
                persistedPosition(restored, fixture) == expectedPosition,
                "Create millstone output phase did not survive BlockEntity save/load");

        for (int execution = 1; execution < fixture.executions(); execution++) {
            process(restored);
        }

        long actualItems = countMatching(restored, fixture.target().getStack());
        long expectedItems = expectedMatchingItems(fixture);
        helper.assertTrue(
                actualItems == expectedItems,
                "Create millstone recipe " + fixture.recipe().getId() + " produced "
                        + actualItems + " target items in one exact batch; expected "
                        + expectedItems);
        helper.assertTrue(
                !restored.getPersistentData().contains(
                        ProcessingOutputSequenceController.ROOT_TAG,
                        Tag.TAG_COMPOUND),
                "Create millstone retained stale sequence state after a complete exact batch");

        verifyIndependentMachineStartsFresh(helper, fixture);
        verifyCrushingWheelCommitPath(helper);
        helper.succeed();
    }

    private static Fixture findFixture(GameTestHelper helper) {
        for (Recipe<?> candidate : helper.getLevel().getRecipeManager().getRecipes()) {
            if (!(candidate instanceof MillingRecipe recipe)) {
                continue;
            }
            ItemStack input = firstInput(recipe);
            if (input.isEmpty() || !input.getCraftingRemainingItem().isEmpty()) {
                continue;
            }

            MillingRecipe resolved = resolveMilling(helper, input).orElse(null);
            if (resolved == null || !resolved.getId().equals(recipe.getId())) {
                continue;
            }

            long executions = 1;
            ProcessingOutput target = null;
            ChanceFraction targetChance = null;
            List<ProcessingOutput> outputs = recipe.getRollableResults();
            for (ProcessingOutput output : outputs) {
                if (output.getStack().isEmpty()) {
                    continue;
                }
                ChanceFraction chance = ChanceFraction.fromFloat(output.getChance());
                if (!chance.isNever() && !chance.isCertain()) {
                    executions = lcm(executions, chance.denominator());
                    if (target == null
                            && Math.floorMod(output.getStack().getCount(), chance.denominator()) != 0) {
                        target = output;
                        targetChance = chance;
                    }
                }
            }
            if (target == null
                    || executions < 2
                    || executions > Math.min(64, input.getMaxStackSize())) {
                continue;
            }

            int targetIndex = outputs.indexOf(target);
            long expected = expectedMatchingItems(
                    recipe, target.getStack(), Math.toIntExact(executions));
            if (expected <= 9L * target.getStack().getMaxStackSize()) {
                return new Fixture(
                        recipe,
                        input,
                        target,
                        targetChance,
                        targetIndex,
                        Math.toIntExact(executions));
            }
        }
        throw new AssertionError(
                "Create loaded without a compact probabilistic MillingRecipe fixture");
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

    private static Optional<MillingRecipe> resolveMilling(
            GameTestHelper helper,
            ItemStack input) {
        ItemStackHandler inventory = new ItemStackHandler(1);
        inventory.setStackInSlot(0, input.copy());
        return AllRecipeTypes.MILLING.find(new RecipeWrapper(inventory), helper.getLevel());
    }

    private static MillstoneBlockEntity requireMillstone(
            GameTestHelper helper,
            BlockPos position) {
        if (helper.getLevel().getBlockEntity(position) instanceof MillstoneBlockEntity millstone) {
            return millstone;
        }
        throw new AssertionError("Create did not create a MillstoneBlockEntity for the test block");
    }

    private static void process(MillstoneBlockEntity millstone) {
        try {
            PROCESS.invoke(millstone);
        } catch (IllegalAccessException | InvocationTargetException exception) {
            Throwable cause = exception instanceof InvocationTargetException invocation
                    && invocation.getCause() != null
                    ? invocation.getCause()
                    : exception;
            throw new AssertionError("Create millstone process call failed", cause);
        }
    }

    private static void applyRecipe(CrushingWheelControllerBlockEntity controller) {
        try {
            APPLY_CRUSHING_RECIPE.invoke(controller);
        } catch (IllegalAccessException | InvocationTargetException exception) {
            Throwable cause = exception instanceof InvocationTargetException invocation
                    && invocation.getCause() != null
                    ? invocation.getCause()
                    : exception;
            throw new AssertionError("Create crushing-wheel applyRecipe call failed", cause);
        }
    }

    private static Method processMethod() {
        try {
            Method method = MillstoneBlockEntity.class.getDeclaredMethod("process");
            method.setAccessible(true);
            return method;
        } catch (ReflectiveOperationException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    private static Method crushingMethod() {
        try {
            Method method = CrushingWheelControllerBlockEntity.class
                    .getDeclaredMethod("applyRecipe");
            method.setAccessible(true);
            return method;
        } catch (ReflectiveOperationException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    private static void verifyIndependentMachineStartsFresh(
            GameTestHelper helper,
            Fixture fixture) {
        BlockPos position = helper.absolutePos(new BlockPos(2, 1, 1));
        helper.getLevel().setBlock(position, AllBlocks.MILLSTONE.getDefaultState(), 3);
        MillstoneBlockEntity independent = requireMillstone(helper, position);
        ItemStack input = fixture.input().copy();
        input.setCount(1);
        independent.inputInv.setStackInSlot(0, input);
        process(independent);
        helper.assertTrue(
                persistedPosition(independent, fixture)
                        == Math.floorMod(
                                fixture.target().getStack().getCount(),
                                Math.toIntExact(fixture.targetChance().denominator())),
                "A second Create millstone inherited another machine's output phase");
    }

    private static void verifyCrushingWheelCommitPath(GameTestHelper helper) {
        CrushingFixture fixture = findCrushingFixture(helper);
        BlockPos position = helper.absolutePos(new BlockPos(1, 1, 2));
        helper.getLevel().setBlock(
                position,
                AllBlocks.CRUSHING_WHEEL_CONTROLLER.getDefaultState(),
                3);
        if (!(helper.getLevel().getBlockEntity(position)
                instanceof CrushingWheelControllerBlockEntity controller)) {
            throw new AssertionError(
                    "Create did not create a CrushingWheelControllerBlockEntity for the test block");
        }

        ItemStack input = fixture.input().copy();
        input.setCount(fixture.executions());
        controller.inventory.setStackInSlot(0, input);
        applyRecipe(controller);

        long actual = 0;
        for (int slot = 0; slot < controller.inventory.getSlots(); slot++) {
            ItemStack stack = controller.inventory.getStackInSlot(slot);
            if (ItemStack.isSameItemSameTags(stack, fixture.target().getStack())) {
                actual += stack.getCount();
            }
        }
        long expected = expectedMatchingItems(
                fixture.recipe(), fixture.target().getStack(), fixture.executions());
        helper.assertTrue(
                actual == expected,
                "Create crushing-wheel recipe " + fixture.recipe().getId() + " produced "
                        + actual + " target items in one exact batch; expected " + expected);
        helper.assertTrue(
                !controller.getPersistentData().contains(
                        ProcessingOutputSequenceController.ROOT_TAG,
                        Tag.TAG_COMPOUND),
                "Create crushing wheel retained stale state after a complete exact batch");
    }

    private static CrushingFixture findCrushingFixture(GameTestHelper helper) {
        for (Recipe<?> candidate : helper.getLevel().getRecipeManager().getRecipes()) {
            if (!(candidate instanceof CrushingRecipe recipe)) {
                continue;
            }
            ItemStack input = firstInput(recipe);
            if (input.isEmpty()
                    || !input.getCraftingRemainingItem().isEmpty()
                    || !resolveCrushing(helper, input)
                            .map(resolved -> resolved.getId().equals(recipe.getId()))
                            .orElse(false)) {
                continue;
            }
            ProcessingFixture processing = processingFixture(recipe, input);
            if (processing != null) {
                return new CrushingFixture(
                        recipe,
                        input,
                        processing.target(),
                        processing.executions());
            }
        }
        throw new AssertionError(
                "Create loaded without a compact probabilistic CrushingRecipe fixture");
    }

    private static Optional<CrushingRecipe> resolveCrushing(
            GameTestHelper helper,
            ItemStack input) {
        ItemStackHandler inventory = new ItemStackHandler(1);
        inventory.setStackInSlot(0, input.copy());
        return AllRecipeTypes.CRUSHING.find(new RecipeWrapper(inventory), helper.getLevel());
    }

    private static ProcessingFixture processingFixture(
            ProcessingRecipe<?> recipe,
            ItemStack input) {
        long executions = 1;
        ProcessingOutput target = null;
        for (ProcessingOutput output : recipe.getRollableResults()) {
            if (output.getStack().isEmpty()) {
                continue;
            }
            ChanceFraction chance = ChanceFraction.fromFloat(output.getChance());
            if (!chance.isNever() && !chance.isCertain()) {
                executions = lcm(executions, chance.denominator());
                if (target == null) {
                    target = output;
                }
            }
        }
        if (target == null
                || executions < 2
                || executions > Math.min(64, input.getMaxStackSize())) {
            return null;
        }
        long expected = expectedMatchingItems(
                recipe, target.getStack(), Math.toIntExact(executions));
        return expected <= 15L * target.getStack().getMaxStackSize()
                ? new ProcessingFixture(target, Math.toIntExact(executions))
                : null;
    }

    private static int persistedPosition(
            MillstoneBlockEntity machine,
            Fixture fixture) {
        CompoundTag root = machine.getPersistentData()
                .getCompound(ProcessingOutputSequenceController.ROOT_TAG);
        ListTag states = root.getList("States", Tag.TAG_COMPOUND);
        for (int index = 0; index < states.size(); index++) {
            CompoundTag state = states.getCompound(index);
            if (state.getString("Recipe").equals(fixture.recipe().getId().toString())
                    && state.getInt("Output") == fixture.targetIndex()) {
                return Math.toIntExact(state.getLong("Position"));
            }
        }
        return -1;
    }

    private static long countMatching(
            MillstoneBlockEntity machine,
            ItemStack target) {
        long result = 0;
        for (int slot = 0; slot < machine.outputInv.getSlots(); slot++) {
            ItemStack stack = machine.outputInv.getStackInSlot(slot);
            if (ItemStack.isSameItemSameTags(stack, target)) {
                result += stack.getCount();
            }
        }
        return result;
    }

    private static long expectedMatchingItems(Fixture fixture) {
        return expectedMatchingItems(
                fixture.recipe(), fixture.target().getStack(), fixture.executions());
    }

    private static long expectedMatchingItems(
            ProcessingRecipe<?> recipe,
            ItemStack target,
            int executions) {
        long expected = 0;
        for (ProcessingOutput output : recipe.getRollableResults()) {
            if (!ItemStack.isSameItemSameTags(output.getStack(), target)) {
                continue;
            }
            ChanceFraction chance = ChanceFraction.fromFloat(output.getChance());
            long successes = Math.multiplyExact(
                    chance.numerator(), executions / chance.denominator());
            expected = Math.addExact(
                    expected,
                    Math.multiplyExact(successes, output.getStack().getCount()));
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

    private record Fixture(
            MillingRecipe recipe,
            ItemStack input,
            ProcessingOutput target,
            ChanceFraction targetChance,
            int targetIndex,
            int executions) {}

    private record ProcessingFixture(ProcessingOutput target, int executions) {}

    private record CrushingFixture(
            CrushingRecipe recipe,
            ItemStack input,
            ProcessingOutput target,
            int executions) {}
}
