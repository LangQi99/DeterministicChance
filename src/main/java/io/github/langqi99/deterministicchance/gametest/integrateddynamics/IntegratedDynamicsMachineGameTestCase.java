package io.github.langqi99.deterministicchance.gametest.integrateddynamics;

import io.github.langqi99.deterministicchance.compat.integrateddynamics.IntegratedDynamicsSequenceState;
import io.github.langqi99.deterministicchance.compat.integrateddynamics.IntegratedDynamicsSequenceStateAccess;
import io.github.langqi99.deterministicchance.mixin.integrateddynamics.MechanicalSqueezerInvoker;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.cyclops.integrateddynamics.RegistryEntries;
import org.cyclops.integrateddynamics.blockentity.BlockEntityMechanicalSqueezer;
import org.cyclops.integrateddynamics.core.recipe.type.RecipeMechanicalSqueezer;

/** Uses the released redstone recipe through the Mechanical Squeezer's final commit method. */
public final class IntegratedDynamicsMachineGameTestCase {
    static final ResourceLocation FIXTURE_ID = ResourceLocation.fromNamespaceAndPath(
            "integrateddynamics", "mechanical_squeezer/ore/redstone");

    private IntegratedDynamicsMachineGameTestCase() {
    }

    public static void run(GameTestHelper helper) {
        RecipeMechanicalSqueezer recipe = fixture(helper);
        BlockPos position = helper.absolutePos(new BlockPos(1, 2, 1));
        helper.getLevel().setBlock(
                position, RegistryEntries.BLOCK_MECHANICAL_SQUEEZER.defaultBlockState(), 3);
        BlockEntityMechanicalSqueezer machine =
                (BlockEntityMechanicalSqueezer) helper.getLevel().getBlockEntity(position);
        helper.assertTrue((Object) machine instanceof IntegratedDynamicsSequenceStateAccess,
                "Mechanical Squeezer did not receive the deterministic state mixin");
        IItemHandlerModifiable inventory = inventory(machine);
        ItemStack input = recipe.getInputIngredient().getItems()[0].copy();
        input.setCount(2);
        inventory.setStackInSlot(0, input);

        helper.assertTrue(finalize(machine, recipe, true),
                "Mechanical Squeezer rejected its released recipe simulation");
        helper.assertTrue(finalize(machine, recipe, false),
                "Mechanical Squeezer rejected its released recipe commit");
        CompoundTag saved = ((BlockEntity) (Object) machine).saveWithoutMetadata();
        helper.assertTrue(saved.contains(IntegratedDynamicsSequenceState.ROOT_TAG),
                "Mechanical Squeezer did not persist its half-cycle");

        helper.getLevel().setBlock(position, Blocks.AIR.defaultBlockState(), 3);
        helper.getLevel().setBlock(
                position, RegistryEntries.BLOCK_MECHANICAL_SQUEEZER.defaultBlockState(), 3);
        BlockEntityMechanicalSqueezer restored =
                (BlockEntityMechanicalSqueezer) helper.getLevel().getBlockEntity(position);
        ((BlockEntity) (Object) restored).load(saved);
        helper.assertTrue(finalize(restored, recipe, true),
                "Restored Mechanical Squeezer rejected its simulation");
        helper.assertTrue(finalize(restored, recipe, false),
                "Restored Mechanical Squeezer rejected its commit");

        inventory = inventory(restored);
        helper.assertTrue(count(inventory, Items.REDSTONE) == 26,
                "Released 12 + 50% x 2 redstone recipe did not produce exact two-run total 26");
        helper.assertTrue(inventory.getStackInSlot(0).isEmpty(),
                "Mechanical Squeezer did not consume both inputs");
        helper.succeed();
    }

    static RecipeMechanicalSqueezer fixture(GameTestHelper helper) {
        Recipe<?> recipe = helper.getLevel().getRecipeManager().byKey(FIXTURE_ID).orElse(null);
        if (recipe instanceof RecipeMechanicalSqueezer squeezer
                && squeezer.getOutputItems().size() == 2
                && squeezer.getOutputItems().get(0).getIngredientFirst().getCount() == 12
                && squeezer.getOutputItems().get(1).getIngredientFirst().getCount() == 2
                && squeezer.getOutputItems().get(1).getChance() == 0.5F) {
            return squeezer;
        }
        throw new AssertionError("Missing or changed released Integrated Dynamics fixture " + FIXTURE_ID);
    }

    private static IItemHandlerModifiable inventory(BlockEntityMechanicalSqueezer machine) {
        IItemHandler handler = ((BlockEntity) (Object) machine)
                .getCapability(ForgeCapabilities.ITEM_HANDLER)
                .orElseThrow(() -> new AssertionError("Mechanical Squeezer has no item capability"));
        if (handler instanceof IItemHandlerModifiable modifiable) {
            return modifiable;
        }
        throw new AssertionError("Mechanical Squeezer item capability is not modifiable in GameTest");
    }

    private static boolean finalize(
            BlockEntityMechanicalSqueezer machine,
            RecipeMechanicalSqueezer recipe,
            boolean simulate) {
        return ((MechanicalSqueezerInvoker) (Object) machine)
                .deterministicChance$finalizeRecipe(recipe, simulate);
    }

    private static int count(IItemHandler inventory, net.minecraft.world.item.Item item) {
        int total = 0;
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (stack.is(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }
}
