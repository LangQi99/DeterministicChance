package io.github.langqi99.deterministicchance.gametest.thermal;

import cofh.lib.common.inventory.ItemStorageCoFH;
import cofh.thermal.core.util.recipes.machine.SawmillRecipe;
import cofh.thermal.expansion.common.block.entity.machine.MachineSawmillBlockEntity;
import io.github.langqi99.deterministicchance.compat.thermal.ThermalMachineSequenceState;
import io.github.langqi99.deterministicchance.compat.thermal.ThermalSequenceStateAccess;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;

/** Loaded reflectively only in a Thermal Expansion runtime. */
public final class ThermalMachineGameTestCase {
    private static final ResourceLocation FIXTURE_ID =
            ResourceLocation.fromNamespaceAndPath("thermal", "machines/sawmill/sawmill_oak_logs");
    private static final ResourceLocation MACHINE_ID =
            ResourceLocation.fromNamespaceAndPath("thermal", "machine_sawmill");
    private static final int EXECUTIONS = 16;

    private ThermalMachineGameTestCase() {
    }

    public static void run(GameTestHelper helper) {
        SawmillRecipe recipe = fixture(helper);
        assertStableFixture(recipe);
        Block machineBlock = ForgeRegistries.BLOCKS.getValue(MACHINE_ID);
        if (machineBlock == null) {
            throw new AssertionError("Missing Thermal machine block " + MACHINE_ID);
        }

        ItemStack input = recipe.getInputItems().get(0).getItems()[0].copy();
        input.setCount(Math.multiplyExact(input.getCount(), EXECUTIONS));
        ExposedSawmill firstMachine = new ExposedSawmill(
                BlockPos.ZERO,
                machineBlock.defaultBlockState());
        firstMachine.setInput(input);
        helper.assertTrue((Object) firstMachine instanceof ThermalSequenceStateAccess,
                "Thermal MachineBlockEntity mixin was not applied");

        Map<ResourceLocation, Long> totals = new LinkedHashMap<>();
        firstMachine.finishOne();
        Map<ResourceLocation, Long> firstOutput = firstMachine.drainOutputs();
        assertOneOperation(helper, recipe, firstOutput, 0);
        merge(totals, firstOutput);

        // Keep a second partial profile beside the real oak-recipe profile.
        // It must survive the save and must not consume or reset oak's lane.
        ThermalMachineSequenceState firstState =
                ((ThermalSequenceStateAccess) (Object) firstMachine)
                        .deterministicChance$thermalState();
        firstState.beginOutputPass("gametest:alternate-machine-profile");
        helper.assertTrue(
                firstState.nextOutputCopies(0.25F) == 1,
                "Thermal alternate profile did not begin at its own success position");

        // Save immediately after the first extra-product success. Losing the
        // phase would make operation 1 succeed again instead of continuing
        // through the three guaranteed-only operations.
        CompoundTag saved = firstMachine.saveWithoutMetadata();
        helper.assertTrue(saved.contains(ThermalMachineSequenceState.ROOT_TAG),
                "Thermal machine did not persist its deterministic output phase");

        ExposedSawmill restoredMachine = new ExposedSawmill(
                BlockPos.ZERO,
                machineBlock.defaultBlockState());
        restoredMachine.load(saved);
        for (int operation = 1; operation < EXECUTIONS; operation++) {
            restoredMachine.finishOne();
            Map<ResourceLocation, Long> actual = restoredMachine.drainOutputs();
            assertOneOperation(helper, recipe, actual, operation);
            merge(totals, actual);
        }

        Map<ResourceLocation, Long> expectedTotals = new LinkedHashMap<>();
        add(expectedTotals, recipe.getOutputItems().get(0), EXECUTIONS);
        add(expectedTotals, recipe.getOutputItems().get(1), 20);
        helper.assertTrue(totals.equals(expectedTotals),
                "Thermal resolveOutputs produced " + totals + "; expected " + expectedTotals);
        helper.assertTrue(restoredMachine.remainingInputCount() == 0,
                "Thermal processFinish did not consume all 16 native recipe inputs");

        ThermalMachineSequenceState restoredState =
                ((ThermalSequenceStateAccess) (Object) restoredMachine)
                        .deterministicChance$thermalState();
        restoredState.beginOutputPass("gametest:alternate-machine-profile");
        helper.assertTrue(
                restoredState.nextOutputCopies(0.25F) == 0,
                "Thermal recipe/profile switching reset or lost the alternate persisted phase");
        helper.succeed();
    }

    private static SawmillRecipe fixture(GameTestHelper helper) {
        Recipe<?> recipe = helper.getLevel().getRecipeManager().byKey(FIXTURE_ID).orElse(null);
        if (recipe instanceof SawmillRecipe sawmillRecipe) {
            return sawmillRecipe;
        }
        throw new AssertionError("Missing released Thermal recipe fixture " + FIXTURE_ID);
    }

    private static void assertStableFixture(SawmillRecipe recipe) {
        if (recipe.getInputItems().size() != 1
                || recipe.getInputItems().get(0).getItems().length == 0
                || recipe.getOutputItems().size() != 2
                || recipe.getOutputItemChances().size() != 2
                || Math.abs(recipe.getOutputItemChances().get(0)) != 1.0F
                || Math.abs(recipe.getOutputItemChances().get(1)) != 1.25F) {
            throw new AssertionError(
                    "Thermal released oak sawmill fixture no longer has 1 + 1.25 output semantics");
        }
    }

    private static void assertOneOperation(
            GameTestHelper helper,
            SawmillRecipe recipe,
            Map<ResourceLocation, Long> actual,
            int operation) {
        Map<ResourceLocation, Long> expected = expectedOneOperation(recipe, operation);
        helper.assertTrue(actual.equals(expected),
                "Thermal operation " + operation + " produced " + actual + "; expected " + expected);
    }

    private static Map<ResourceLocation, Long> expectedOneOperation(
            SawmillRecipe recipe,
            int operation) {
        Map<ResourceLocation, Long> expected = new LinkedHashMap<>();
        add(expected, recipe.getOutputItems().get(0), 1);
        add(expected, recipe.getOutputItems().get(1), operation % 4 == 0 ? 2 : 1);
        return expected;
    }

    private static void add(
            Map<ResourceLocation, Long> result,
            ItemStack stack,
            long copies) {
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (key == null) {
            throw new AssertionError("Unregistered Thermal output " + stack);
        }
        result.merge(key, Math.multiplyExact(stack.getCount(), copies), Math::addExact);
    }

    private static void merge(
            Map<ResourceLocation, Long> destination,
            Map<ResourceLocation, Long> source) {
        source.forEach((key, amount) -> destination.merge(key, amount, Math::addExact));
    }

    private static final class ExposedSawmill extends MachineSawmillBlockEntity {
        private ExposedSawmill(BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
            super(pos, state);
        }

        private void setInput(ItemStack stack) {
            inputSlot.setItemStack(stack);
        }

        private void finishOne() {
            processFinish();
        }

        private int remainingInputCount() {
            return inputSlot.getCount();
        }

        private Map<ResourceLocation, Long> drainOutputs() {
            Map<ResourceLocation, Long> result = new LinkedHashMap<>();
            for (ItemStorageCoFH output : outputSlots()) {
                ItemStack stack = output.getItemStack();
                if (!stack.isEmpty()) {
                    ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
                    if (key == null) {
                        throw new AssertionError("Unregistered Thermal machine output " + stack);
                    }
                    result.merge(key, (long) stack.getCount(), Math::addExact);
                    output.clear();
                }
            }
            return result;
        }
    }
}
