package io.github.langqi99.deterministicchance.gametest.productivebees;

import cy.jdkdigital.productivebees.common.block.entity.CentrifugeBlockEntity;
import cy.jdkdigital.productivebees.common.recipe.CentrifugeRecipe;
import cy.jdkdigital.productivebees.init.ModBlocks;
import io.github.langqi99.deterministicchance.compat.productivebees.ProductiveBeesSequenceState;
import io.github.langqi99.deterministicchance.compat.productivebees.ProductiveBeesSequenceStateAccess;
import io.github.langqi99.deterministicchance.mixin.productivebees.CentrifugeBlockEntityInvoker;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandlerModifiable;

/** Executes the actual centrifuge commit method, including inventory insertion and persistence. */
public final class ProductiveBeesMachineGameTestCase {
    private ProductiveBeesMachineGameTestCase() {
    }

    public static void run(GameTestHelper helper) {
        BlockPos position = helper.absolutePos(new BlockPos(1, 2, 1));
        helper.getLevel().setBlock(position, ModBlocks.CENTRIFUGE.get().defaultBlockState(), 3);
        CentrifugeBlockEntity machine = (CentrifugeBlockEntity) helper.getLevel().getBlockEntity(position);
        helper.assertTrue((Object) machine instanceof ProductiveBeesSequenceStateAccess,
                "Productive Bees centrifuge did not receive the deterministic state mixin");

        CentrifugeRecipe recipe = ProductiveBeesFixture.recipe();
        IItemHandlerModifiable inventory = inventory(machine);
        inventory.setStackInSlot(1, new ItemStack(Items.COBBLESTONE, ProductiveBeesFixture.EXECUTIONS));
        finish(machine, recipe, inventory);

        CompoundTag saved = ((BlockEntity) (Object) machine).saveWithoutMetadata();
        helper.assertTrue(saved.contains(ProductiveBeesSequenceState.ROOT_TAG),
                "Centrifuge did not persist partial output schedules");
        helper.getLevel().setBlock(position, Blocks.AIR.defaultBlockState(), 3);
        helper.getLevel().setBlock(position, ModBlocks.CENTRIFUGE.get().defaultBlockState(), 3);
        CentrifugeBlockEntity restored =
                (CentrifugeBlockEntity) helper.getLevel().getBlockEntity(position);
        ((BlockEntity) (Object) restored).load(saved);
        inventory = inventory(restored);

        for (int operation = 1; operation < ProductiveBeesFixture.EXECUTIONS; operation++) {
            finish(restored, recipe, inventory);
        }

        helper.assertTrue(count(inventory, Items.GOLD_NUGGET) == 24,
                "80% centrifuge output was not exactly 24/30");
        helper.assertTrue(count(inventory, Items.IRON_NUGGET) == 30,
                "50% ranged 1-3 centrifuge output did not total exactly 30/30");
        helper.assertTrue(count(inventory, Items.DIAMOND) == 0,
                "0% centrifuge output reproduced Productive Bees' <= off-by-one roll");
        helper.assertTrue(inventory.getStackInSlot(1).isEmpty(),
                "Centrifuge commit path did not consume all exact-batch inputs");
        helper.succeed();
    }

    private static IItemHandlerModifiable inventory(CentrifugeBlockEntity machine) {
        return (IItemHandlerModifiable) ((BlockEntity) (Object) machine)
                .getCapability(ForgeCapabilities.ITEM_HANDLER)
                .orElseThrow(() -> new AssertionError("Centrifuge has no item capability"));
    }

    private static void finish(
            CentrifugeBlockEntity machine,
            CentrifugeRecipe recipe,
            IItemHandlerModifiable inventory) {
        ((CentrifugeBlockEntityInvoker) (Object) machine).deterministicChance$completeRecipeProcessing(
                recipe, inventory, RandomSource.create(), false);
    }

    private static int count(IItemHandlerModifiable inventory, Item item) {
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
