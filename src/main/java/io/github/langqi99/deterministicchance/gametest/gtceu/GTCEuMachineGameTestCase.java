package io.github.langqi99.deterministicchance.gametest.gtceu;

import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient;
import io.github.langqi99.deterministicchance.compat.gtceu.GTCEu7OrChanceRoller;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;

/** Loaded reflectively only when GTCEu 7 is present. */
public final class GTCEuMachineGameTestCase {
    private static final String ROOT_KEY = "deterministic_chance";
    private static final String INTEGRATION_KEY = "gtceu_7";
    private static final String STATES_KEY = "states";

    private GTCEuMachineGameTestCase() {
    }

    public static void run(GameTestHelper helper) {
        GTCEuRecipeFixture.Fixture fixture = GTCEuRecipeFixture.find(helper);
        int executions = Math.toIntExact(fixture.exactBatch());
        ChestBlockEntity original = new ChestBlockEntity(
                BlockPos.ZERO,
                Blocks.CHEST.defaultBlockState());

        Map<OutputKey, Long> actual = new LinkedHashMap<>();
        merge(actual, amounts(
                fixture.targetCapability(),
                GTCEu7OrChanceRoller.roll(
                        original,
                        fixture.recipe(),
                        fixture.targetCapability(),
                        fixture.targetCapabilityOutputs(),
                        fixture.recipe().getType().getChanceFunction(),
                        fixture.recipeTier(),
                        fixture.recipeTier(),
                        1)));
        helper.assertTrue(
                persistedTargetPosition(original, fixture) == 1,
                "GTCEu recipe " + fixture.recipe().getId()
                        + " did not advance its persisted chance phase after one operation");

        CompoundTag saved = original.saveWithoutMetadata();
        ChestBlockEntity restored = new ChestBlockEntity(
                BlockPos.ZERO,
                Blocks.CHEST.defaultBlockState());
        restored.load(saved);
        helper.assertTrue(
                persistedTargetPosition(restored, fixture) == 1,
                "GTCEu chance phase did not survive BlockEntity save/load");

        merge(actual, amounts(
                fixture.targetCapability(),
                GTCEu7OrChanceRoller.roll(
                        restored,
                        fixture.recipe(),
                        fixture.targetCapability(),
                        fixture.targetCapabilityOutputs(),
                        fixture.recipe().getType().getChanceFunction(),
                        fixture.recipeTier(),
                        fixture.recipeTier(),
                        executions - 1)));

        Map<OutputKey, Long> expected = expectedAmounts(fixture);
        helper.assertTrue(
                actual.equals(expected),
                "GTCEu exact batch for " + fixture.recipe().getId()
                        + " produced " + actual + "; expected " + expected);
        helper.assertTrue(
                persistedTargetPosition(restored, fixture) == -1,
                "GTCEu recipe " + fixture.recipe().getId()
                        + " retained stale state after its full exact batch of " + executions);
        helper.succeed();
    }

    private static Map<OutputKey, Long> expectedAmounts(
            GTCEuRecipeFixture.Fixture fixture) {
        Map<OutputKey, Long> result = new LinkedHashMap<>();
        for (Content content : fixture.targetCapabilityOutputs()) {
            var chance = GTCEuRecipeFixture.effectiveChance(
                    fixture.recipe(),
                    content,
                    fixture.recipeTier(),
                    fixture.recipeTier());
            long successes = Math.multiplyExact(
                    fixture.exactBatch() / chance.denominator(),
                    (long) chance.numerator());
            long amount = GTCEuRecipeFixture.concreteAmount(
                    fixture.targetCapability(),
                    content);
            if (amount > 0 && successes > 0) {
                result.merge(
                        outputKey(fixture.targetCapability(), content),
                        Math.multiplyExact(amount, successes),
                        Math::addExact);
            }
        }
        return result;
    }

    private static Map<OutputKey, Long> amounts(
            RecipeCapability<?> capability,
            List<Content> contents) {
        Map<OutputKey, Long> result = new LinkedHashMap<>();
        for (Content content : contents) {
            long amount = GTCEuRecipeFixture.concreteAmount(capability, content);
            if (amount <= 0) {
                throw new AssertionError("GTCEu roller returned a non-concrete output: " + content);
            }
            result.merge(outputKey(capability, content), amount, Math::addExact);
        }
        return result;
    }

    private static OutputKey outputKey(
            RecipeCapability<?> capability,
            Content content) {
        if (capability == ItemRecipeCapability.CAP) {
            Ingredient ingredient = ItemRecipeCapability.CAP.of(content.content);
            ItemStack[] stacks = ingredient.getItems();
            if (stacks.length == 0 || stacks[0].isEmpty()) {
                throw new AssertionError("GTCEu item output has no concrete stack: " + content);
            }
            ItemStack stack = stacks[0];
            ResourceLocation id = Objects.requireNonNull(
                    ForgeRegistries.ITEMS.getKey(stack.getItem()),
                    "registered GTCEu item output");
            return new OutputKey("item", id, copyTag(stack.getTag()));
        }
        if (capability == FluidRecipeCapability.CAP) {
            FluidIngredient ingredient = FluidRecipeCapability.CAP.of(content.content);
            FluidStack[] stacks = ingredient.getStacks();
            if (stacks.length == 0 || stacks[0].isEmpty()) {
                throw new AssertionError("GTCEu fluid output has no concrete stack: " + content);
            }
            FluidStack stack = stacks[0];
            ResourceLocation id = Objects.requireNonNull(
                    ForgeRegistries.FLUIDS.getKey(stack.getFluid()),
                    "registered GTCEu fluid output");
            return new OutputKey("fluid", id, copyTag(stack.getTag()));
        }
        throw new AssertionError("Unexpected GTCEu output capability " + capability.name);
    }

    private static CompoundTag copyTag(CompoundTag tag) {
        return tag == null ? null : tag.copy();
    }

    private static int persistedTargetPosition(
            BlockEntity owner,
            GTCEuRecipeFixture.Fixture fixture) {
        CompoundTag root = owner.getPersistentData().getCompound(ROOT_KEY);
        CompoundTag integration = root.getCompound(INTEGRATION_KEY);
        ListTag states = integration.getList(STATES_KEY, Tag.TAG_COMPOUND);
        Tag targetIdentity = fixture.targetCapability()
                .contentToNbt(fixture.targetOutput().content);
        for (int index = 0; index < states.size(); index++) {
            CompoundTag state = states.getCompound(index);
            if (state.getString("recipe").equals(fixture.recipe().getId().toString())
                    && state.getString("capability").equals(fixture.targetCapability().name)
                    && state.getInt("ordinal") == fixture.targetDuplicateOrdinal()
                    && Objects.equals(state.get("content"), targetIdentity)) {
                return state.getInt("position");
            }
        }
        return -1;
    }

    private static void merge(
            Map<OutputKey, Long> destination,
            Map<OutputKey, Long> source) {
        source.forEach((key, amount) -> destination.merge(key, amount, Math::addExact));
    }

    private record OutputKey(
            String capability,
            ResourceLocation id,
            CompoundTag tag) {
    }
}
