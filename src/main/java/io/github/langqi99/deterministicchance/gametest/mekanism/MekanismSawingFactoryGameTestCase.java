package io.github.langqi99.deterministicchance.gametest.mekanism;

import io.github.langqi99.deterministicchance.compat.mekanism.MekanismMachineRollContext;
import io.github.langqi99.deterministicchance.compat.mekanism.SawmillSequenceController;
import io.github.langqi99.deterministicchance.core.ChanceFraction;
import java.util.List;
import mekanism.api.recipes.SawmillRecipe;
import mekanism.api.recipes.cache.CachedRecipe;
import mekanism.common.content.blocktype.FactoryType;
import mekanism.common.registries.MekanismBlocks;
import mekanism.common.tier.FactoryTier;
import mekanism.common.tile.factory.TileEntitySawingFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;

/** Loaded reflectively only in a Mekanism runtime. */
public final class MekanismSawingFactoryGameTestCase {
    private static final String ROOT_TAG = "DeterministicChanceMekanismSawmill";
    private static final String STATES_TAG = "States";

    private MekanismSawingFactoryGameTestCase() {
    }

    public static void run(GameTestHelper helper) {
        SawmillRecipe recipe = findFixture(helper);
        ChanceFraction chance = ChanceFraction.fromDouble(recipe.getSecondaryChance());

        TileEntitySawingFactory first = newFactory(BlockPos.ZERO);
        CachedRecipe<SawmillRecipe> firstCache = first.createNewCachedRecipe(recipe, 0);
        helper.assertTrue(
                next(firstCache, recipe),
                "A fresh Mekanism sawing factory did not start at the success prefix");
        assertPosition(helper, first, recipe, 1, "first factory after one roll");

        CompoundTag saved = first.saveWithoutMetadata();
        TileEntitySawingFactory restored = newFactory(BlockPos.ZERO);
        restored.load(saved);
        assertPosition(helper, restored, recipe, 1, "restored factory");
        CachedRecipe<SawmillRecipe> restoredCache = restored.createNewCachedRecipe(recipe, 0);

        // Move A to the first failure. A second factory must still begin at its
        // own position zero rather than sharing A's recipe-global cursor.
        for (long operation = 1; operation < chance.numerator(); operation++) {
            helper.assertTrue(
                    next(restoredCache, recipe),
                    "Restored Mekanism factory lost the success prefix at operation " + operation);
        }
        assertPosition(
                helper,
                restored,
                recipe,
                chance.numerator(),
                "restored factory at the success/failure boundary");

        TileEntitySawingFactory independent = newFactory(new BlockPos(1, 0, 0));
        CachedRecipe<SawmillRecipe> independentCache = independent.createNewCachedRecipe(recipe, 0);
        helper.assertTrue(
                next(independentCache, recipe),
                "A second Mekanism sawing factory inherited another machine's phase");
        assertPosition(helper, independent, recipe, 1, "independent factory after one roll");

        for (long operation = chance.numerator(); operation < chance.denominator(); operation++) {
            helper.assertFalse(
                    next(restoredCache, recipe),
                    "Mekanism factory emitted an extra secondary output at operation " + operation);
        }
        assertNoPosition(helper, restored, recipe, "restored factory after a complete cycle");
        helper.succeed();
    }

    private static boolean next(
            CachedRecipe<SawmillRecipe> cachedRecipe,
            SawmillRecipe recipe) {
        boolean[] result = new boolean[1];
        MekanismMachineRollContext.runWithOwner(
                cachedRecipe,
                () -> result[0] = SawmillSequenceController.next(recipe));
        return result[0];
    }

    private static void assertPosition(
            GameTestHelper helper,
            TileEntitySawingFactory machine,
            SawmillRecipe recipe,
            long expected,
            String description) {
        CompoundTag root = machine.getPersistentData().getCompound(ROOT_TAG);
        CompoundTag states = root.getCompound(STATES_TAG);
        String recipeId = recipe.getId().toString();
        helper.assertTrue(
                states.contains(recipeId),
                description + " has no machine-scoped state for " + recipeId
                        + "; the cached recipe was probably not associated with its factory owner");
        helper.assertTrue(
                states.getCompound(recipeId).getLong("Position") == expected,
                description + " persisted position "
                        + states.getCompound(recipeId).getLong("Position")
                        + "; expected " + expected);
    }

    private static void assertNoPosition(
            GameTestHelper helper,
            TileEntitySawingFactory machine,
            SawmillRecipe recipe,
            String description) {
        CompoundTag states = machine.getPersistentData()
                .getCompound(ROOT_TAG)
                .getCompound(STATES_TAG);
        helper.assertFalse(
                states.contains(recipe.getId().toString()),
                description + " retained a completed deterministic cursor");
    }

    private static TileEntitySawingFactory newFactory(BlockPos position) {
        var factoryBlock = MekanismBlocks.getFactory(FactoryTier.BASIC, FactoryType.SAWING);
        return new TileEntitySawingFactory(
                factoryBlock,
                position,
                factoryBlock.getBlock().defaultBlockState());
    }

    private static SawmillRecipe findFixture(GameTestHelper helper) {
        for (Recipe<?> candidate : helper.getLevel().getRecipeManager().getRecipes()) {
            if (!(candidate instanceof SawmillRecipe recipe)) {
                continue;
            }
            ChanceFraction chance = ChanceFraction.fromDouble(recipe.getSecondaryChance());
            List<ItemStack> inputs = recipe.getInput().getRepresentations();
            if (!chance.isNever()
                    && !chance.isCertain()
                    && chance.denominator() <= 1_000
                    && !inputs.isEmpty()
                    && !inputs.get(0).isEmpty()
                    && !recipe.getMainOutputDefinition().isEmpty()
                    && !recipe.getSecondaryOutputDefinition().isEmpty()) {
                return recipe;
            }
        }
        throw new AssertionError("Mekanism loaded without a probabilistic SawmillRecipe fixture");
    }
}
