package io.github.langqi99.deterministicchance.mixin.mekanism;

import io.github.langqi99.deterministicchance.compat.mekanism.MekanismMachineRollContext;
import io.github.langqi99.deterministicchance.compat.mekanism.SawmillSequenceController;
import mekanism.api.recipes.SawmillRecipe;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = SawmillRecipe.ChanceOutput.class, remap = false)
abstract class SawmillChanceOutputMixin {
    @Unique
    private SawmillRecipe deterministicChance$recipe;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void captureRecipe(SawmillRecipe recipe, double randomValue, CallbackInfo callback) {
        deterministicChance$recipe = recipe;
    }

    @Inject(method = {"getSecondaryOutput", "nextSecondaryOutput"}, at = @At("HEAD"), cancellable = true)
    private void deterministicChance(CallbackInfoReturnable<ItemStack> callback) {
        // getSecondaryOutput is also an idempotent preview API. Only replace it
        // while a cached recipe is committing output to its owning machine.
        if (MekanismMachineRollContext.activeMachine() == null) {
            return;
        }
        callback.setReturnValue(SawmillSequenceController.next(deterministicChance$recipe)
                ? deterministicChance$recipe.getSecondaryOutputDefinition().get(0).copy()
                : ItemStack.EMPTY);
    }
}
