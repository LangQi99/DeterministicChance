package io.github.langqi99.deterministicchance.mixin.ae2;

import appeng.integration.modules.jei.transfer.EncodePatternTransferHandler;
import appeng.integration.modules.jeirei.EncodingHelper;
import appeng.menu.me.items.PatternEncodingTermMenu;
import io.github.langqi99.deterministicchance.compat.jei.JeiBatchDecision;
import io.github.langqi99.deterministicchance.compat.jei.JeiRecipeBatchAdapterRegistry;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Converts every registered probabilistic recipe into one complete deterministic batch. */
@Mixin(value = EncodePatternTransferHandler.class, remap = false)
abstract class EncodePatternTransferHandlerMixin {
    @Shadow @Final private IRecipeTransferHandlerHelper helper;

    @Inject(
            method = "transferRecipe(Lappeng/menu/me/items/PatternEncodingTermMenu;Ljava/lang/Object;"
                    + "Lmezz/jei/api/gui/ingredient/IRecipeSlotsView;Lnet/minecraft/world/entity/player/Player;ZZ)"
                    + "Lmezz/jei/api/recipe/transfer/IRecipeTransferError;",
            at = @At("HEAD"),
            cancellable = true,
            remap = false)
    private void encodeDeterministicBatch(
            PatternEncodingTermMenu menu,
            Object recipeBase,
            IRecipeSlotsView slotsView,
            Player player,
            boolean maxTransfer,
            boolean doTransfer,
            CallbackInfoReturnable<IRecipeTransferError> callback) {
        JeiBatchDecision decision = JeiRecipeBatchAdapterRegistry.decide(recipeBase, slotsView);
        if (decision.status() == JeiBatchDecision.Status.NOT_APPLICABLE) {
            return;
        }

        if (decision.status() == JeiBatchDecision.Status.RECOGNIZED_BUT_UNSUPPORTED) {
            callback.setReturnValue(helper.createUserErrorWithTooltip(Component.translatable(
                    "gui.deterministic_chance.exact_batch_unsupported",
                    decision.reason())));
            return;
        }

        if (doTransfer) {
            var plan = decision.plan();
            EncodingHelper.encodeProcessingRecipe(menu, plan.inputs(), plan.outputs());
            callback.setReturnValue(null);
        }
    }
}
