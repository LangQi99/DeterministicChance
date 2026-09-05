package io.github.langqi99.deterministicchance.mixin.immersiveengineering;

import blusunrize.immersiveengineering.api.crafting.ArcFurnaceRecipe;
import io.github.langqi99.deterministicchance.compat.immersiveengineering.IEProcessRollContext;
import io.github.langqi99.deterministicchance.compat.immersiveengineering.IERecipeOutputs;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Uses the JSON/JEI probability p, fixing IE 10.2's reversed Arc Furnace comparison. */
@Mixin(value = ArcFurnaceRecipe.class, remap = false)
abstract class ArcFurnaceRecipeMixin {
    @Inject(
            method = "generateActualOutput",
            at = @At("HEAD"),
            cancellable = true,
            remap = false,
            require = 1)
    private void deterministicChance$exactArcFurnaceOutputs(
            ItemStack input,
            NonNullList<ItemStack> additives,
            long seed,
            CallbackInfoReturnable<NonNullList<ItemStack>> callback) {
        ArcFurnaceRecipe recipe = (ArcFurnaceRecipe) (Object) this;
        if (IEProcessRollContext.isActiveFor(recipe.getId())) {
            callback.setReturnValue(IERecipeOutputs.arcFurnace(recipe));
        }
    }
}
