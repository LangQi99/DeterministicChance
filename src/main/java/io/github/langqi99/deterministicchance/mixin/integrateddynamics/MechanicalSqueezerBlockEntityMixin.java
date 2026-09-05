package io.github.langqi99.deterministicchance.mixin.integrateddynamics;

import io.github.langqi99.deterministicchance.compat.integrateddynamics.IntegratedDynamicsSequenceState;
import io.github.langqi99.deterministicchance.compat.integrateddynamics.IntegratedDynamicsSequenceStateAccess;
import io.github.langqi99.deterministicchance.core.ChanceFraction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import org.cyclops.integrateddynamics.blockentity.BlockEntityMechanicalSqueezer;
import org.cyclops.integrateddynamics.core.recipe.type.RecipeMechanicalSqueezer;
import org.cyclops.integrateddynamics.core.recipe.type.RecipeSqueezer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Determinizes the Mechanical Squeezer only on its non-simulated commit pass. */
@Mixin(value = BlockEntityMechanicalSqueezer.class, remap = false)
abstract class MechanicalSqueezerBlockEntityMixin implements IntegratedDynamicsSequenceStateAccess {
    @Unique private IntegratedDynamicsSequenceState deterministicChance$integratedDynamicsState;
    @Unique private int deterministicChance$outputLane;

    @Override
    public IntegratedDynamicsSequenceState deterministicChance$integratedDynamicsState() {
        if (deterministicChance$integratedDynamicsState == null) {
            deterministicChance$integratedDynamicsState = new IntegratedDynamicsSequenceState();
        }
        return deterministicChance$integratedDynamicsState;
    }

    @Inject(method = "finalizeRecipe", at = @At("HEAD"), remap = false, require = 1)
    private void deterministicChance$beginOutputs(
            RecipeMechanicalSqueezer recipe,
            boolean simulate,
            CallbackInfoReturnable<Boolean> callback) {
        deterministicChance$outputLane = 0;
    }

    @Redirect(
            method = "finalizeRecipe",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/RandomSource;nextFloat()F"),
            remap = false,
            require = 1)
    private float deterministicChance$rollOutput(
            RandomSource random,
            RecipeMechanicalSqueezer recipe,
            boolean simulate) {
        while (deterministicChance$outputLane < recipe.getOutputItems().size()) {
            int lane = deterministicChance$outputLane++;
            RecipeSqueezer.IngredientChance output = recipe.getOutputItems().get(lane);
            if (output.getIngredientFirst().isEmpty() || output.getChance() == 1.0F) {
                continue;
            }
            if (!Float.isFinite(output.getChance())
                    || output.getChance() < 0.0F
                    || output.getChance() > 1.0F) {
                return random.nextFloat();
            }
            ChanceFraction chance = ChanceFraction.fromFloat(output.getChance());
            boolean success = deterministicChance$integratedDynamicsState().next(
                    recipe.getId(), lane, chance);
            return success ? 0.0F : 1.0F;
        }
        return random.nextFloat();
    }

    @Inject(method = "saveAdditional", at = @At("TAIL"), remap = false, require = 1)
    private void deterministicChance$saveSequence(CompoundTag tag, CallbackInfo callback) {
        deterministicChance$integratedDynamicsState().save(tag);
    }

    @Inject(method = "read", at = @At("TAIL"), remap = false, require = 1)
    private void deterministicChance$loadSequence(CompoundTag tag, CallbackInfo callback) {
        deterministicChance$integratedDynamicsState().load(tag);
    }
}
