package io.github.langqi99.deterministicchance.mixin.ae2;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.integration.modules.jei.GenericEntryStackHelper;
import appeng.integration.modules.jei.transfer.EncodePatternTransferHandler;
import appeng.integration.modules.jeirei.EncodingHelper;
import appeng.menu.me.items.PatternEncodingTermMenu;
import io.github.langqi99.deterministicchance.core.ChanceFraction;
import java.util.List;
import mekanism.api.recipes.SawmillRecipe;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Converts Mekanism's probabilistic JEI display into one complete deterministic batch. */
@Mixin(value = EncodePatternTransferHandler.class, remap = false)
abstract class EncodePatternTransferHandlerMixin {
    @Inject(
            method = "transferRecipe(Lappeng/menu/me/items/PatternEncodingTermMenu;Ljava/lang/Object;"
                    + "Lmezz/jei/api/gui/ingredient/IRecipeSlotsView;Lnet/minecraft/world/entity/player/Player;ZZ)"
                    + "Lmezz/jei/api/recipe/transfer/IRecipeTransferError;",
            at = @At("HEAD"),
            cancellable = true,
            remap = false)
    private void encodeDeterministicSawmillBatch(
            PatternEncodingTermMenu menu,
            Object recipeBase,
            IRecipeSlotsView slotsView,
            Player player,
            boolean maxTransfer,
            boolean doTransfer,
            CallbackInfoReturnable<IRecipeTransferError> callback) {
        if (!doTransfer || !(recipeBase instanceof SawmillRecipe recipe)) {
            return;
        }

        ChanceFraction chance = ChanceFraction.fromDouble(recipe.getSecondaryChance());
        if (chance.numerator() == 0 || chance.isCertain()) {
            return;
        }

        List<List<GenericStack>> inputs = GenericEntryStackHelper.ofInputs(slotsView).stream()
                .map(alternatives -> alternatives.stream()
                        .map(stack -> multiply(stack, chance.denominator()))
                        .toList())
                .toList();

        AEItemKey secondary = AEItemKey.of(recipe.getSecondaryOutputDefinition().get(0));
        List<GenericStack> outputs = GenericEntryStackHelper.ofOutputs(slotsView).stream()
                .map(stack -> multiply(stack,
                        stack.what().equals(secondary) ? chance.numerator() : chance.denominator()))
                .toList();

        EncodingHelper.encodeProcessingRecipe(menu, inputs, outputs);
        callback.setReturnValue(null);
    }

    private static GenericStack multiply(GenericStack stack, long factor) {
        return new GenericStack(stack.what(), Math.multiplyExact(stack.amount(), factor));
    }
}
