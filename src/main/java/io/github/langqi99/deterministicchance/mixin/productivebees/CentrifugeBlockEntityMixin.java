package io.github.langqi99.deterministicchance.mixin.productivebees;

import cy.jdkdigital.productivebees.common.block.entity.CentrifugeBlockEntity;
import cy.jdkdigital.productivebees.common.recipe.CentrifugeRecipe;
import io.github.langqi99.deterministicchance.compat.productivebees.ProductiveBeesRandomSource;
import io.github.langqi99.deterministicchance.compat.productivebees.ProductiveBeesSequenceState;
import io.github.langqi99.deterministicchance.compat.productivebees.ProductiveBeesSequenceStateAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Uses the centrifuge's real commit method, so failed capacity checks never advance a schedule. */
@Mixin(value = CentrifugeBlockEntity.class, remap = false)
abstract class CentrifugeBlockEntityMixin implements ProductiveBeesSequenceStateAccess {
    @Unique
    private ProductiveBeesSequenceState deterministicChance$productiveBeesState;

    @Override
    public ProductiveBeesSequenceState deterministicChance$productiveBeesState() {
        if (deterministicChance$productiveBeesState == null) {
            deterministicChance$productiveBeesState = new ProductiveBeesSequenceState();
        }
        return deterministicChance$productiveBeesState;
    }

    @ModifyVariable(
            method = "completeRecipeProcessing(Lcy/jdkdigital/productivebees/common/recipe/CentrifugeRecipe;"
                    + "Lnet/minecraftforge/items/IItemHandlerModifiable;Lnet/minecraft/util/RandomSource;Z)V",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0,
            remap = false,
            require = 1)
    private RandomSource deterministicChance$wrapRandom(
            RandomSource random,
            CentrifugeRecipe recipe,
            net.minecraftforge.items.IItemHandlerModifiable inventory,
            RandomSource originalRandom,
            boolean stripWax) {
        return new ProductiveBeesRandomSource(
                random,
                deterministicChance$productiveBeesState(),
                recipe,
                stripWax);
    }

    @Inject(method = "savePacketNBT", at = @At("TAIL"), remap = false, require = 1)
    private void deterministicChance$saveSequence(CompoundTag tag, CallbackInfo callback) {
        deterministicChance$productiveBeesState().save(tag);
    }

    @Inject(method = "loadPacketNBT", at = @At("TAIL"), remap = false, require = 1)
    private void deterministicChance$loadSequence(CompoundTag tag, CallbackInfo callback) {
        deterministicChance$productiveBeesState().load(tag);
    }
}
