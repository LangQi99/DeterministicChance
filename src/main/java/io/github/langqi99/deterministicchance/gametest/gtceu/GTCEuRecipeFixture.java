package io.github.langqi99.deterministicchance.gametest.gtceu;

import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.chance.logic.ChanceLogic;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient;
import com.gregtechceu.gtceu.api.recipe.ingredient.IntProviderFluidIngredient;
import com.gregtechceu.gtceu.api.recipe.ingredient.IntProviderIngredient;
import com.gregtechceu.gtceu.api.recipe.ingredient.SizedIngredient;
import io.github.langqi99.deterministicchance.compat.gtceu.GTCEu7ExactBatchMath;
import io.github.langqi99.deterministicchance.compat.gtceu.GTCEu7ExactOrCycle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.fluids.FluidStack;

/**
 * Selects one released GTCEu recipe that both runtime and JEI tests can use.
 *
 * <p>This class deliberately has no AE2 or JEI references, so the machine test
 * remains loadable in a GTCEu-only dependency profile.</p>
 */
final class GTCEuRecipeFixture {
    private static final int LEGACY_AE_INPUT_SLOTS = 9;
    private static final int LEGACY_AE_OUTPUT_SLOTS = 3;
    private static final long MAX_EXACT_EXECUTIONS = 10_000;

    private GTCEuRecipeFixture() {
    }

    static Fixture find(GameTestHelper helper) {
        List<GTRecipe> recipes = helper.getLevel().getRecipeManager().getRecipes().stream()
                .filter(GTRecipe.class::isInstance)
                .map(GTRecipe.class::cast)
                .sorted(Comparator.comparing(recipe -> recipe.getId() == null
                        ? ""
                        : recipe.getId().toString()))
                .toList();
        for (GTRecipe recipe : recipes) {
            try {
                Fixture fixture = analyze(recipe);
                if (fixture != null) {
                    return fixture;
                }
            } catch (IllegalArgumentException | ArithmeticException ignored) {
                // The production adapter rejects the same non-exact shapes.
            }
        }
        throw new AssertionError(
                "GTCEu loaded without a real, AE-encodable independent OR chance recipe; scanned "
                        + recipes.size() + " GT recipes");
    }

    private static Fixture analyze(GTRecipe recipe) {
        if (recipe.getId() == null
                || hasMaterialTickIo(recipe)
                || hasUnknownNonTickCapability(recipe.inputs)
                || hasUnknownNonTickCapability(recipe.outputs)
                || !validInputs(recipe)) {
            return null;
        }

        int recipeTier = RecipeHelper.getPreOCRecipeEuTier(recipe);
        List<OutputInfo> outputs = new ArrayList<>();
        if (!appendOutputs(outputs, recipe, ItemRecipeCapability.CAP, recipeTier)
                || !appendOutputs(outputs, recipe, FluidRecipeCapability.CAP, recipeTier)) {
            return null;
        }

        List<List<GTCEu7ExactOrCycle.Fraction>> schedules = outputs.stream()
                .filter(OutputInfo::probabilistic)
                .map(output -> List.of(output.effectiveChance()))
                .toList();
        if (schedules.isEmpty()) {
            return null;
        }
        long exactBatch = GTCEu7ExactBatchMath.exactBatch(
                schedules,
                MAX_EXACT_EXECUTIONS);
        if (exactBatch <= 1 || exactBatch > Integer.MAX_VALUE) {
            return null;
        }

        int guaranteedOutputSlots = 0;
        OutputInfo target = null;
        for (OutputInfo output : outputs) {
            long successes = output.probabilistic()
                    ? Math.multiplyExact(
                            exactBatch / output.effectiveChance().denominator(),
                            (long) output.effectiveChance().numerator())
                    : exactBatch;
            if (successes > 0 && output.amountPerSuccess() > 0) {
                guaranteedOutputSlots++;
            }
            if (target == null && output.probabilistic()) {
                GTCEu7ExactOrCycle.Fraction baseChance = output.effectiveChance();
                if (baseChance.numerator() > 0
                        && baseChance.numerator() < baseChance.denominator()) {
                    target = output;
                }
            }
        }
        if (target == null
                || guaranteedOutputSlots == 0
                || guaranteedOutputSlots > LEGACY_AE_OUTPUT_SLOTS) {
            return null;
        }

        List<Content> capabilityOutputs = List.copyOf(
                recipe.getOutputContents(target.capability()));
        int targetIndex = indexByIdentity(
                capabilityOutputs,
                target.content(),
                target.capability());
        if (targetIndex < 0) {
            return null;
        }
        int duplicateOrdinal = duplicateOrdinal(
                capabilityOutputs,
                targetIndex,
                target.capability());
        return new Fixture(
                recipe,
                target.capability(),
                capabilityOutputs,
                target.content(),
                duplicateOrdinal,
                recipeTier,
                target.effectiveChance(),
                exactBatch);
    }

    private static boolean validInputs(GTRecipe recipe) {
        List<Content> itemInputs = recipe.getInputContents(ItemRecipeCapability.CAP);
        List<Content> fluidInputs = recipe.getInputContents(FluidRecipeCapability.CAP);
        int slots = itemInputs.size() + fluidInputs.size();
        if (slots == 0 || slots > LEGACY_AE_INPUT_SLOTS) {
            return false;
        }
        for (Content content : itemInputs) {
            if (!certainConsumed(content)) {
                return false;
            }
            Ingredient ingredient = ItemRecipeCapability.CAP.of(content.content);
            if (containsRangedAmount(ingredient) || firstConcreteItem(ingredient).isEmpty()) {
                return false;
            }
        }
        for (Content content : fluidInputs) {
            if (!certainConsumed(content)) {
                return false;
            }
            FluidIngredient ingredient = FluidRecipeCapability.CAP.of(content.content);
            if (ingredient instanceof IntProviderFluidIngredient
                    || firstConcreteFluid(ingredient).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static boolean appendOutputs(
            List<OutputInfo> destination,
            GTRecipe recipe,
            RecipeCapability<?> capability,
            int recipeTier) {
        List<Content> contents = recipe.getOutputContents(capability);
        boolean hasChance = contents.stream().anyMatch(content ->
                content.chance < content.maxChance
                        && (content.chance > 0 || content.tierChanceBoost > 0));
        if (hasChance
                && recipe.getChanceLogicForCapability(capability, IO.OUT, false) != ChanceLogic.OR) {
            return false;
        }

        for (Content content : contents) {
            long amount = concreteAmount(capability, content);
            if (amount < 0) {
                return false;
            }
            if (amount == 0) {
                continue;
            }
            if (content.maxChance <= 0) {
                return false;
            }
            if (content.chance >= content.maxChance) {
                destination.add(new OutputInfo(capability, content, amount, false, null));
                continue;
            }
            if (content.chance <= 0 && content.tierChanceBoost <= 0) {
                continue;
            }
            if (content.tierChanceBoost != 0) {
                return false;
            }
            GTCEu7ExactOrCycle.Fraction baseChance =
                    effectiveChance(recipe, content, recipeTier, recipeTier);
            for (int chanceTier = recipeTier + 1;
                    chanceTier <= Math.max(recipeTier, GTValues.MAX);
                    chanceTier++) {
                if (!effectiveChance(recipe, content, recipeTier, chanceTier).equals(baseChance)) {
                    return false;
                }
            }
            destination.add(new OutputInfo(
                    capability,
                    content,
                    amount,
                    true,
                    baseChance));
        }
        return true;
    }

    static GTCEu7ExactOrCycle.Fraction effectiveChance(
            GTRecipe recipe,
            Content content,
            int recipeTier,
            int chanceTier) {
        int chance = recipe.getType().getChanceFunction()
                .getBoostedChance(content, recipeTier, chanceTier);
        chance = Math.max(0, Math.min(content.maxChance, chance));
        return GTCEu7ExactOrCycle.fraction(chance, content.maxChance);
    }

    static long concreteAmount(RecipeCapability<?> capability, Content content) {
        if (capability == ItemRecipeCapability.CAP) {
            Ingredient ingredient = ItemRecipeCapability.CAP.of(content.content);
            if (containsRangedAmount(ingredient)) {
                return -1;
            }
            ItemStack stack = firstConcreteItem(ingredient);
            return stack.isEmpty() ? 0 : stack.getCount();
        }
        if (capability == FluidRecipeCapability.CAP) {
            FluidIngredient ingredient = FluidRecipeCapability.CAP.of(content.content);
            if (ingredient instanceof IntProviderFluidIngredient) {
                return -1;
            }
            FluidStack stack = firstConcreteFluid(ingredient);
            return stack.isEmpty() ? 0 : stack.getAmount();
        }
        return -1;
    }

    private static ItemStack firstConcreteItem(Ingredient ingredient) {
        ItemStack[] stacks = ingredient.getItems();
        return stacks.length == 0 ? ItemStack.EMPTY : stacks[0];
    }

    private static FluidStack firstConcreteFluid(FluidIngredient ingredient) {
        FluidStack[] stacks = ingredient.getStacks();
        return stacks.length == 0 ? FluidStack.EMPTY : stacks[0];
    }

    private static boolean certainConsumed(Content content) {
        return content.maxChance > 0 && content.chance >= content.maxChance;
    }

    private static boolean containsRangedAmount(Ingredient ingredient) {
        if (ingredient instanceof IntProviderIngredient) {
            return true;
        }
        return ingredient instanceof SizedIngredient sized
                && containsRangedAmount(sized.getInner());
    }

    private static boolean hasMaterialTickIo(GTRecipe recipe) {
        return !recipe.getTickInputContents(ItemRecipeCapability.CAP).isEmpty()
                || !recipe.getTickOutputContents(ItemRecipeCapability.CAP).isEmpty()
                || !recipe.getTickInputContents(FluidRecipeCapability.CAP).isEmpty()
                || !recipe.getTickOutputContents(FluidRecipeCapability.CAP).isEmpty();
    }

    private static boolean hasUnknownNonTickCapability(
            Map<RecipeCapability<?>, List<Content>> contents) {
        return contents.entrySet().stream().anyMatch(entry ->
                !entry.getValue().isEmpty()
                        && entry.getKey() != ItemRecipeCapability.CAP
                        && entry.getKey() != FluidRecipeCapability.CAP);
    }

    private static int indexByIdentity(
            List<Content> entries,
            Content target,
            RecipeCapability<?> capability) {
        Tag targetIdentity = capability.contentToNbt(target.content);
        for (int index = 0; index < entries.size(); index++) {
            if (Objects.equals(
                    capability.contentToNbt(entries.get(index).content),
                    targetIdentity)) {
                return index;
            }
        }
        return -1;
    }

    private static int duplicateOrdinal(
            List<Content> entries,
            int targetIndex,
            RecipeCapability<?> capability) {
        Tag targetIdentity = capability.contentToNbt(entries.get(targetIndex).content);
        int ordinal = 0;
        for (int index = 0; index < targetIndex; index++) {
            if (Objects.equals(
                    capability.contentToNbt(entries.get(index).content),
                    targetIdentity)) {
                ordinal++;
            }
        }
        return ordinal;
    }

    record Fixture(
            GTRecipe recipe,
            RecipeCapability<?> targetCapability,
            List<Content> targetCapabilityOutputs,
            Content targetOutput,
            int targetDuplicateOrdinal,
            int recipeTier,
            GTCEu7ExactOrCycle.Fraction targetBaseChance,
            long exactBatch) {
    }

    private record OutputInfo(
            RecipeCapability<?> capability,
            Content content,
            long amountPerSuccess,
            boolean probabilistic,
            GTCEu7ExactOrCycle.Fraction effectiveChance) {
    }
}
