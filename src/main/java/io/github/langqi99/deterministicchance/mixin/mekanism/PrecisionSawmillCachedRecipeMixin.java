package io.github.langqi99.deterministicchance.mixin.mekanism;

import io.github.langqi99.deterministicchance.compat.mekanism.MekanismMachineRollContext;
import mekanism.api.recipes.SawmillRecipe;
import mekanism.api.recipes.cache.CachedRecipe;
import mekanism.common.tile.machine.TileEntityPrecisionSawmill;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Captures the owning sawmill for Mekanism's otherwise owner-less recipe cache. */
@Mixin(value = TileEntityPrecisionSawmill.class, remap = false)
abstract class PrecisionSawmillCachedRecipeMixin {
    @Inject(
            method = "createNewCachedRecipe(Lmekanism/api/recipes/SawmillRecipe;I)"
                    + "Lmekanism/api/recipes/cache/CachedRecipe;",
            at = @At("RETURN"),
            remap = false)
    private void deterministicChance$captureOwner(
            SawmillRecipe recipe,
            int cacheIndex,
            CallbackInfoReturnable<CachedRecipe<SawmillRecipe>> callback) {
        MekanismMachineRollContext.associate(
                callback.getReturnValue(),
                (BlockEntity) (Object) this);
    }
}
