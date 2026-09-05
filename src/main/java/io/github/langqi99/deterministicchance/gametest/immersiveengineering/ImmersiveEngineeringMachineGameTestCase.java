package io.github.langqi99.deterministicchance.gametest.immersiveengineering;

import blusunrize.immersiveengineering.api.crafting.ArcFurnaceRecipe;
import blusunrize.immersiveengineering.api.crafting.CrusherRecipe;
import blusunrize.immersiveengineering.api.crafting.IngredientWithSize;
import blusunrize.immersiveengineering.api.crafting.StackWithChance;
import blusunrize.immersiveengineering.api.energy.AveragingEnergyStorage;
import blusunrize.immersiveengineering.api.multiblocks.blocks.env.IMultiblockLevel;
import blusunrize.immersiveengineering.api.multiblocks.blocks.util.MultiblockOrientation;
import blusunrize.immersiveengineering.api.multiblocks.blocks.util.RelativeBlockFace;
import blusunrize.immersiveengineering.common.blocks.multiblocks.logic.CrusherLogic;
import blusunrize.immersiveengineering.common.blocks.multiblocks.logic.arcfurnace.ArcFurnaceLogic;
import blusunrize.immersiveengineering.common.blocks.multiblocks.logic.arcfurnace.ArcFurnaceProcess;
import blusunrize.immersiveengineering.common.blocks.multiblocks.process.MultiblockProcessInWorld;
import blusunrize.immersiveengineering.common.blocks.multiblocks.process.ProcessContext.ProcessContextInMachine;
import blusunrize.immersiveengineering.common.blocks.multiblocks.process.ProcessContext.ProcessContextInWorld;
import io.github.langqi99.deterministicchance.compat.immersiveengineering.IESequenceState;
import io.github.langqi99.deterministicchance.compat.immersiveengineering.IESequenceStateAccess;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.Lazy;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.items.ItemStackHandler;

/** Exercises IE's actual central preview/finish methods with persisted state carriers. */
public final class ImmersiveEngineeringMachineGameTestCase {
    private static final ResourceLocation CRUSHER_FIXTURE =
            ResourceLocation.fromNamespaceAndPath("immersiveengineering", "crusher/gravel");
    private static final ResourceLocation ARC_TEST_ID =
            ResourceLocation.fromNamespaceAndPath("deterministic_chance", "arc_probability_semantics");

    private ImmersiveEngineeringMachineGameTestCase() {
    }

    public static void run(GameTestHelper helper) {
        helper.assertTrue(
                IESequenceStateAccess.class.isAssignableFrom(CrusherLogic.State.class),
                "CrusherLogic.State did not receive the deterministic state carrier mixin");
        helper.assertTrue(
                IESequenceStateAccess.class.isAssignableFrom(ArcFurnaceLogic.State.class),
                "ArcFurnaceLogic.State did not receive the deterministic state carrier mixin");

        runCrusherCycle(helper, crusherFixture(helper));
        runArcProbabilityCycle(helper);
        helper.succeed();
    }

    private static void runCrusherCycle(GameTestHelper helper, CrusherRecipe recipe) {
        CrusherTestContext context = new CrusherTestContext();
        TestMultiblockLevel level = new TestMultiblockLevel(helper.getLevel());
        int flint = 0;
        for (int operation = 0; operation < 10; operation++) {
            TestCrusherProcess process = new TestCrusherProcess(recipe);
            helper.assertTrue(process.canProcess(context, helper.getLevel()),
                    "Released IE Crusher recipe could not pass its output preview");
            context.committed.clear();
            process.finish(context, level);
            helper.assertTrue(
                    counts(process.previewed).equals(counts(context.committed)),
                    "Crusher preview did not match the immediately following commit");
            flint += count(context.committed, Items.FLINT);
        }
        helper.assertTrue(flint == 1,
                "IE Crusher 10% secondary produced " + flint + " items in ten commits; expected 1");
    }

    private static void runArcProbabilityCycle(GameTestHelper helper) {
        ArcFurnaceRecipe recipe = new ArcFurnaceRecipe(
                ARC_TEST_ID,
                List.of(Lazy.of(() -> new ItemStack(Items.IRON_INGOT))),
                Lazy.of(() -> ItemStack.EMPTY),
                List.of(new StackWithChance(new ItemStack(Items.GOLD_NUGGET), 0.8F)),
                1,
                1,
                IngredientWithSize.of(new ItemStack(Items.RAW_IRON)));
        ArcTestContext context = new ArcTestContext();
        TestMultiblockLevel level = new TestMultiblockLevel(helper.getLevel());
        int goldNuggets = 0;
        for (int operation = 0; operation < 5; operation++) {
            context.inventory.setStackInSlot(0, new ItemStack(Items.RAW_IRON));
            TestArcProcess process = new TestArcProcess(recipe);
            helper.assertTrue(process.canProcess(context, helper.getLevel()),
                    "IE Arc Furnace test recipe could not pass its output preview");
            process.finish(context, level);
            helper.assertTrue(
                    counts(process.previewed).equals(counts(process.committed)),
                    "Arc Furnace preview did not match the immediately following commit");
            goldNuggets += count(process.committed, Items.GOLD_NUGGET);
        }
        helper.assertTrue(goldNuggets == 4,
                "IE Arc Furnace JEI chance 80% produced " + goldNuggets
                        + " items in five commits; expected 4");
    }

    private static CrusherRecipe crusherFixture(GameTestHelper helper) {
        Recipe<?> recipe = helper.getLevel().getRecipeManager().byKey(CRUSHER_FIXTURE).orElse(null);
        if (recipe instanceof CrusherRecipe crusher) {
            return crusher;
        }
        throw new AssertionError("Missing released IE Crusher fixture " + CRUSHER_FIXTURE);
    }

    private static int count(List<ItemStack> stacks, Item item) {
        return stacks.stream()
                .filter(stack -> stack.is(item))
                .mapToInt(ItemStack::getCount)
                .sum();
    }

    private static Map<Item, Integer> counts(List<ItemStack> stacks) {
        Map<Item, Integer> result = new LinkedHashMap<>();
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty()) {
                result.merge(stack.getItem(), stack.getCount(), Math::addExact);
            }
        }
        return result;
    }

    private static final class CrusherTestContext
            implements ProcessContextInWorld<CrusherRecipe>, IESequenceStateAccess {
        private final AveragingEnergyStorage energy = fullEnergy();
        private final IESequenceState sequenceState = new IESequenceState();
        private final List<ItemStack> committed = new ArrayList<>();

        @Override
        public AveragingEnergyStorage getEnergy() {
            return energy;
        }

        @Override
        public void doProcessOutput(ItemStack result, IMultiblockLevel level) {
            committed.add(result.copy());
        }

        @Override
        public IESequenceState deterministicChance$ieState() {
            return sequenceState;
        }
    }

    private static final class TestCrusherProcess extends MultiblockProcessInWorld<CrusherRecipe> {
        private final List<ItemStack> previewed = new ArrayList<>();

        private TestCrusherProcess(CrusherRecipe recipe) {
            super(recipe, new ItemStack(Items.GRAVEL));
        }

        @Override
        protected boolean canOutputItem(ProcessContextInWorld<CrusherRecipe> context, ItemStack output) {
            previewed.add(output.copy());
            return true;
        }

        private void finish(CrusherTestContext context, IMultiblockLevel level) {
            super.processFinish(context, level);
        }
    }

    private static final class ArcTestContext
            implements ProcessContextInMachine<ArcFurnaceRecipe>, IESequenceStateAccess {
        private final AveragingEnergyStorage energy = fullEnergy();
        private final ItemStackHandler inventory = new ItemStackHandler(16);
        private final IESequenceState sequenceState = new IESequenceState();

        @Override
        public AveragingEnergyStorage getEnergy() {
            return energy;
        }

        @Override
        public ItemStackHandler getInventory() {
            return inventory;
        }

        @Override
        public IESequenceState deterministicChance$ieState() {
            return sequenceState;
        }
    }

    private static final class TestArcProcess extends ArcFurnaceProcess {
        private final List<ItemStack> previewed = new ArrayList<>();
        private final List<ItemStack> committed = new ArrayList<>();

        private TestArcProcess(ArcFurnaceRecipe recipe) {
            super(recipe, 0, 0, 12, 13, 14, 15);
        }

        @Override
        protected boolean canOutputItem(
                ProcessContextInMachine<ArcFurnaceRecipe> context,
                ItemStack output) {
            previewed.add(output.copy());
            return true;
        }

        @Override
        protected void outputItem(
                ProcessContextInMachine<ArcFurnaceRecipe> context,
                ItemStack output,
                IMultiblockLevel level) {
            committed.add(output.copy());
        }

        private void finish(ArcTestContext context, IMultiblockLevel level) {
            super.processFinish(context, level);
        }
    }

    private static AveragingEnergyStorage fullEnergy() {
        AveragingEnergyStorage storage = new AveragingEnergyStorage(1_000_000);
        storage.receiveEnergy(1_000_000, false);
        return storage;
    }

    private record TestMultiblockLevel(Level rawLevel) implements IMultiblockLevel {
        @Override
        public BlockState getBlockState(BlockPos relativePosition) {
            return rawLevel.getBlockState(relativePosition);
        }

        @Override
        public void setBlock(BlockPos relativePosition, BlockState state) {
            rawLevel.setBlock(relativePosition, state, 3);
        }

        @Override
        public BlockEntity getBlockEntity(BlockPos relativePosition) {
            return rawLevel.getBlockEntity(relativePosition);
        }

        @Override
        public BlockEntity forciblyGetBlockEntity(BlockPos relativePosition) {
            return getBlockEntity(relativePosition);
        }

        @Override
        public <T> T getCapabilityValue(
                Capability<T> capability,
                BlockPos relativePosition,
                RelativeBlockFace face) {
            return null;
        }

        @Override
        public boolean shouldTickModulo(int interval) {
            return rawLevel.getGameTime() % interval == 0;
        }

        @Override
        public BlockPos getAbsoluteOrigin() {
            return BlockPos.ZERO;
        }

        @Override
        public MultiblockOrientation getOrientation() {
            return null;
        }

        @Override
        public BlockPos toAbsolute(BlockPos relative) {
            return relative;
        }

        @Override
        public Direction toAbsolute(RelativeBlockFace relative) {
            return null;
        }

        @Override
        public AABB toAbsolute(AABB relative) {
            return relative;
        }

        @Override
        public Vec3 toAbsolute(Vec3 relative) {
            return relative;
        }

        @Override
        public BlockPos toRelative(BlockPos absolute) {
            return absolute;
        }

        @Override
        public RelativeBlockFace toRelative(Direction absolute) {
            return null;
        }

        @Override
        public boolean isThundering() {
            return rawLevel.isThundering();
        }

        @Override
        public boolean isRaining() {
            return rawLevel.isRaining();
        }

        @Override
        public int getMaxBuildHeight() {
            return rawLevel.getMaxBuildHeight();
        }

        @Override
        public Level getRawLevel() {
            return rawLevel;
        }

        @Override
        public void updateNeighbourForOutputSignal(BlockPos posInMultiblock) {
        }
    }
}
