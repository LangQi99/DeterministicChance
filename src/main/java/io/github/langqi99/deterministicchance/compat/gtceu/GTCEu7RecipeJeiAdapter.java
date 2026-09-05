package io.github.langqi99.deterministicchance.compat.gtceu;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
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
import io.github.langqi99.deterministicchance.compat.GTCEu7Availability;
import io.github.langqi99.deterministicchance.compat.jei.ChanceStack;
import io.github.langqi99.deterministicchance.compat.jei.JeiBatchPlanner;
import io.github.langqi99.deterministicchance.compat.jei.NativeInputJeiRecipeBatchAdapter;
import io.github.langqi99.deterministicchance.core.ChanceFraction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.fluids.FluidStack;

/**
 * GTCEu Modern 7.x recipe-to-AE2 adapter.
 *
 * <p>Only independent, non-tick {@link ChanceLogic#OR OR} outputs are accepted.
 * AND, XOR, FIRST and arbitrary addon chance logics are deliberately left to
 * vanilla transfer. Ranged quantities and probabilistic/non-consumed inputs
 * are also rejected because they are a different random model.</p>
 *
 * <p>Tier-boosted chance outputs are rejected. A JEI recipe does not identify
 * the target machine tier, so there is no single output count that is exact at
 * every tier.</p>
 */
public final class GTCEu7RecipeJeiAdapter implements NativeInputJeiRecipeBatchAdapter {
    private static final int MAX_INPUT_SLOTS = 81;
    private static final int MAX_OUTPUT_SLOTS = 27;

    @Override
    public boolean supports(Object recipe) {
        // Keep accidental registration against GTCEu 8+ harmless.  The adapter
        // itself is loaded reflectively only after its v7-only class gate, but
        // this second check also protects direct callers and future registries.
        return GTCEu7Availability.isLoaded() && recipe instanceof GTRecipe;
    }

    @Override
    public boolean hasProbabilisticOutputs(Object recipeObject) {
        GTRecipe recipe = (GTRecipe) recipeObject;
        return hasProbability(recipe.getOutputContents(ItemRecipeCapability.CAP))
                || hasProbability(recipe.getOutputContents(FluidRecipeCapability.CAP));
    }

    @Override
    public java.util.Optional<String> exactBatchUnsupportedReason(Object recipe) {
        try {
            describe((GTRecipe) recipe);
            return java.util.Optional.empty();
        } catch (IllegalArgumentException | ArithmeticException exception) {
            return java.util.Optional.ofNullable(exception.getMessage());
        }
    }

    @Override
    public List<List<GenericStack>> inputs(Object recipe) {
        return describe((GTRecipe) recipe).inputs();
    }

    @Override
    public List<ChanceStack> outputs(Object recipe) {
        return describe((GTRecipe) recipe).outputs();
    }

    private static Description describe(GTRecipe recipe) {
        if (recipe.getId() == null) {
            throw unsupported("recipe has no stable id");
        }
        rejectMaterialTickIO(recipe);
        rejectUnknownNonTickCapabilities(recipe.inputs, "input");
        rejectUnknownNonTickCapabilities(recipe.outputs, "output");

        List<List<GenericStack>> inputs = readInputs(recipe);
        if (inputs.isEmpty()) {
            throw unsupported("recipe has no AE-compatible inputs");
        }
        if (inputs.size() > MAX_INPUT_SLOTS) {
            throw unsupported("recipe exceeds AE2's processing input slot limit");
        }

        List<NativeOutput> nativeOutputs = readOutputs(recipe);
        if (nativeOutputs.isEmpty()) {
            throw unsupported("recipe has no AE-compatible outputs");
        }

        List<Collection<GTCEu7ExactOrCycle.Fraction>> probabilisticSchedules = nativeOutputs.stream()
                .filter(NativeOutput::probabilistic)
                .map(NativeOutput::effectiveChances)
                .map(chances -> (Collection<GTCEu7ExactOrCycle.Fraction>) chances)
                .toList();
        long executions = GTCEu7ExactBatchMath.exactBatch(
                probabilisticSchedules,
                JeiBatchPlanner.DEFAULT_MAX_EXECUTIONS);

        Map<AEKey, Long> totals = new LinkedHashMap<>();
        for (NativeOutput output : nativeOutputs) {
            long successes = output.probabilistic()
                    ? GTCEu7ExactBatchMath.minimumSuccesses(executions, output.effectiveChances())
                    : executions;
            long amount = Math.multiplyExact(output.amountPerSuccess(), successes);
            if (amount > 0) {
                totals.merge(output.key(), amount, Math::addExact);
            }
        }
        if (totals.isEmpty()) {
            throw unsupported("exact batch has no guaranteed output");
        }
        if (totals.size() > MAX_OUTPUT_SLOTS) {
            throw unsupported("recipe exceeds AE2's processing output slot limit");
        }

        // The common planner needs to choose the same global GT batch. Compress
        // each already-computed batch total into one 1/executions ChanceStack.
        ChanceFraction encodedChance = executions == 1
                ? ChanceFraction.ALWAYS
                : new ChanceFraction(1, executions);
        List<ChanceStack> outputs = new ArrayList<>(totals.size());
        totals.forEach((key, amount) -> outputs.add(
                new ChanceStack(new GenericStack(key, amount), encodedChance)));
        return new Description(List.copyOf(inputs), List.copyOf(outputs));
    }

    private static List<List<GenericStack>> readInputs(GTRecipe recipe) {
        List<List<GenericStack>> result = new ArrayList<>();
        appendItemInputs(result, recipe.getInputContents(ItemRecipeCapability.CAP));
        appendFluidInputs(result, recipe.getInputContents(FluidRecipeCapability.CAP));
        return result;
    }

    private static void appendItemInputs(List<List<GenericStack>> result, List<Content> contents) {
        for (Content content : contents) {
            requireCertainConsumedInput(content);
            Ingredient ingredient = ItemRecipeCapability.CAP.of(content.content);
            if (containsRangedAmount(ingredient)) {
                throw unsupported("ranged item input amount is not supported");
            }

            List<GenericStack> alternatives = new ArrayList<>();
            for (ItemStack stack : ingredient.getItems()) {
                GenericStack generic = GenericStack.fromItemStack(stack);
                if (generic != null && generic.amount() > 0) {
                    alternatives.add(generic);
                }
            }
            if (alternatives.isEmpty()) {
                throw unsupported("item input has no concrete alternatives");
            }
            result.add(List.copyOf(alternatives));
        }
    }

    private static void appendFluidInputs(List<List<GenericStack>> result, List<Content> contents) {
        for (Content content : contents) {
            requireCertainConsumedInput(content);
            FluidIngredient ingredient = FluidRecipeCapability.CAP.of(content.content);
            if (ingredient instanceof IntProviderFluidIngredient) {
                throw unsupported("ranged fluid input amount is not supported");
            }

            List<GenericStack> alternatives = new ArrayList<>();
            for (FluidStack stack : ingredient.getStacks()) {
                GenericStack generic = GenericStack.fromFluidStack(stack);
                if (generic != null && generic.amount() > 0) {
                    alternatives.add(generic);
                }
            }
            if (alternatives.isEmpty()) {
                throw unsupported("fluid input has no concrete alternatives");
            }
            result.add(List.copyOf(alternatives));
        }
    }

    private static List<NativeOutput> readOutputs(GTRecipe recipe) {
        List<NativeOutput> result = new ArrayList<>();
        appendItemOutputs(result, recipe, recipe.getOutputContents(ItemRecipeCapability.CAP));
        appendFluidOutputs(result, recipe, recipe.getOutputContents(FluidRecipeCapability.CAP));
        return result;
    }

    private static void appendItemOutputs(
            List<NativeOutput> result,
            GTRecipe recipe,
            List<Content> contents) {
        requireIndependentOr(recipe, ItemRecipeCapability.CAP, contents);
        for (Content content : contents) {
            Ingredient ingredient = ItemRecipeCapability.CAP.of(content.content);
            if (containsRangedAmount(ingredient)) {
                throw unsupported("ranged item output amount is not supported");
            }
            ItemStack[] stacks = ingredient.getItems();
            if (stacks.length == 0 || stacks[0].isEmpty()) {
                continue;
            }
            GenericStack generic = GenericStack.fromItemStack(stacks[0]);
            if (generic != null) {
                addOutput(result, recipe, content, generic);
            }
        }
    }

    private static void appendFluidOutputs(
            List<NativeOutput> result,
            GTRecipe recipe,
            List<Content> contents) {
        requireIndependentOr(recipe, FluidRecipeCapability.CAP, contents);
        for (Content content : contents) {
            FluidIngredient ingredient = FluidRecipeCapability.CAP.of(content.content);
            if (ingredient instanceof IntProviderFluidIngredient) {
                throw unsupported("ranged fluid output amount is not supported");
            }
            FluidStack[] stacks = ingredient.getStacks();
            if (stacks.length == 0 || stacks[0].isEmpty()) {
                continue;
            }
            GenericStack generic = GenericStack.fromFluidStack(stacks[0]);
            if (generic != null) {
                addOutput(result, recipe, content, generic);
            }
        }
    }

    private static void addOutput(
            List<NativeOutput> result,
            GTRecipe recipe,
            Content content,
            GenericStack stack) {
        if (content.maxChance <= 0) {
            throw unsupported("output has an invalid maximum chance");
        }
        if (content.chance >= content.maxChance) {
            result.add(new NativeOutput(stack.what(), stack.amount(), false, List.of()));
            return;
        }
        if (content.chance <= 0 && content.tierChanceBoost <= 0) {
            return;
        }
        if (content.tierChanceBoost != 0) {
            throw unsupported("tier-boosted chance has no single exact JEI batch");
        }

        int recipeTier = RecipeHelper.getPreOCRecipeEuTier(recipe);
        int effectiveChance = recipe.getType().getChanceFunction()
                .getBoostedChance(content, recipeTier, recipeTier);
        effectiveChance = Math.max(0, Math.min(content.maxChance, effectiveChance));
        for (int chanceTier = recipeTier + 1; chanceTier <= Math.max(recipeTier, GTValues.MAX); chanceTier++) {
            int atTier = recipe.getType().getChanceFunction()
                    .getBoostedChance(content, recipeTier, chanceTier);
            atTier = Math.max(0, Math.min(content.maxChance, atTier));
            if (atTier != effectiveChance) {
                throw unsupported("machine tier changes output chance, so JEI has no single exact batch");
            }
        }
        List<GTCEu7ExactOrCycle.Fraction> effective = List.of(
                GTCEu7ExactOrCycle.fraction(effectiveChance, content.maxChance));
        result.add(new NativeOutput(stack.what(), stack.amount(), true, List.copyOf(effective)));
    }

    private static void requireIndependentOr(
            GTRecipe recipe,
            RecipeCapability<?> capability,
            List<Content> contents) {
        boolean hasChance = contents.stream().anyMatch(content ->
                content.chance < content.maxChance
                        && (content.chance > 0 || content.tierChanceBoost > 0));
        if (hasChance && recipe.getChanceLogicForCapability(capability, IO.OUT, false) != ChanceLogic.OR) {
            throw unsupported("only independent OR chance outputs are supported");
        }
    }

    private static void requireCertainConsumedInput(Content content) {
        if (content.maxChance <= 0 || content.chance < content.maxChance) {
            throw unsupported("probabilistic or non-consumed inputs are not supported");
        }
    }

    private static boolean hasProbability(List<Content> contents) {
        return contents.stream().anyMatch(content ->
                content.maxChance > 0
                        && content.chance < content.maxChance
                        && (content.chance > 0 || content.tierChanceBoost > 0));
    }

    private static boolean containsRangedAmount(Ingredient ingredient) {
        if (ingredient instanceof IntProviderIngredient) {
            return true;
        }
        return ingredient instanceof SizedIngredient sized
                && containsRangedAmount(sized.getInner());
    }

    private static void rejectMaterialTickIO(GTRecipe recipe) {
        if (!recipe.getTickInputContents(ItemRecipeCapability.CAP).isEmpty()
                || !recipe.getTickOutputContents(ItemRecipeCapability.CAP).isEmpty()
                || !recipe.getTickInputContents(FluidRecipeCapability.CAP).isEmpty()
                || !recipe.getTickOutputContents(FluidRecipeCapability.CAP).isEmpty()) {
            throw unsupported("per-tick item/fluid IO cannot be represented by an AE2 processing pattern");
        }
    }

    private static void rejectUnknownNonTickCapabilities(
            Map<RecipeCapability<?>, List<Content>> contents,
            String side) {
        for (var entry : contents.entrySet()) {
            if (!entry.getValue().isEmpty()
                    && entry.getKey() != ItemRecipeCapability.CAP
                    && entry.getKey() != FluidRecipeCapability.CAP) {
                throw unsupported("non-item/fluid " + side + " capability is not supported: "
                        + entry.getKey().name);
            }
        }
    }

    private static IllegalArgumentException unsupported(String reason) {
        return new IllegalArgumentException("Unsupported GTCEu 7 recipe: " + reason);
    }

    private record Description(
            List<List<GenericStack>> inputs,
            List<ChanceStack> outputs) {}

    private record NativeOutput(
            AEKey key,
            long amountPerSuccess,
            boolean probabilistic,
            List<GTCEu7ExactOrCycle.Fraction> effectiveChances) {}
}
