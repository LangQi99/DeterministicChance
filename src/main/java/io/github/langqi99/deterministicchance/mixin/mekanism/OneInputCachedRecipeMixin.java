package io.github.langqi99.deterministicchance.mixin.mekanism;

import io.github.langqi99.deterministicchance.compat.mekanism.MekanismMachineRollContext;
import mekanism.api.recipes.cache.OneInputCachedRecipe;
import mekanism.api.recipes.outputs.IOutputHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Installs the owner context only while a cached recipe commits its output. */
@Mixin(value = OneInputCachedRecipe.class, remap = false)
abstract class OneInputCachedRecipeMixin {
    @Redirect(
            method = "finishProcessing(I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lmekanism/api/recipes/outputs/IOutputHandler;"
                            + "handleOutput(Ljava/lang/Object;I)V",
                    remap = false),
            remap = false)
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void deterministicChance$withMachineOwner(
            IOutputHandler handler,
            Object output,
            int operations) {
        MekanismMachineRollContext.runWithOwner(
                this,
                () -> handler.handleOutput(output, operations));
    }
}
