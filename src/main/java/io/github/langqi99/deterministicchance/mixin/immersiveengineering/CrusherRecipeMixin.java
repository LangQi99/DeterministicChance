package io.github.langqi99.deterministicchance.mixin.immersiveengineering;

import blusunrize.immersiveengineering.api.crafting.CrusherRecipe;
import io.github.langqi99.deterministicchance.compat.immersiveengineering.IEProcessRollContext;
import io.github.langqi99.deterministicchance.compat.immersiveengineering.IERecipeOutputs;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Replaces Crusher secondary random rolls only while an actual process is previewed/committed. */
@Mixin(value = CrusherRecipe.class, remap = false)
abstract class CrusherRecipeMixin {
    @Inject(
            method = "getActualItemOutputs",
            at = @At("HEAD"),
            cancellable = true,
            remap = false,
            require = 1)
    private void deterministicChance$exactCrusherOutputs(
            CallbackInfoReturnable<NonNullList<ItemStack>> callback) {
        CrusherRecipe recipe = (CrusherRecipe) (Object) this;
        if (IEProcessRollContext.isActiveFor(recipe.getId())) {
            callback.setReturnValue(IERecipeOutputs.crusher(recipe));
        }
    }
}
