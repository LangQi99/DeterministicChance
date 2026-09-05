package io.github.langqi99.deterministicchance.mixin.thermal;

import cofh.thermal.lib.common.block.entity.MachineBlockEntity;
import cofh.thermal.lib.util.recipes.internal.IMachineRecipe;
import io.github.langqi99.deterministicchance.compat.thermal.ThermalMachineSequenceController;
import io.github.langqi99.deterministicchance.compat.thermal.ThermalMachineSequenceState;
import io.github.langqi99.deterministicchance.compat.thermal.ThermalSequenceStateAccess;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

/**
 * One hook covers every standard Thermal processing machine. The original
 * method still performs all slot selection and insertion; only its local
 * chance multiplier is replaced with the next deterministic integer.
 */
@Mixin(value = MachineBlockEntity.class, remap = false)
abstract class MachineBlockEntityMixin implements ThermalSequenceStateAccess {
    @org.spongepowered.asm.mixin.Shadow protected IMachineRecipe curRecipe;

    @Unique
    private ThermalMachineSequenceState deterministicChance$thermalState;

    @Unique
    private boolean deterministicChance$outputPassStarted;

    @Override
    public ThermalMachineSequenceState deterministicChance$thermalState() {
        if (deterministicChance$thermalState == null) {
            deterministicChance$thermalState = new ThermalMachineSequenceState();
        }
        return deterministicChance$thermalState;
    }

    @Inject(
            method = "resolveOutputs()V",
            at = @At("HEAD"),
            remap = false)
    private void deterministicChance$resetOutputPass(CallbackInfo callback) {
        deterministicChance$outputPassStarted = false;
    }

    @Inject(
            method = "resolveOutputs()V",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/List;size()I",
                    ordinal = 0),
            locals = LocalCapture.CAPTURE_FAILHARD,
            remap = false)
    private void deterministicChance$beginOutputPass(
            CallbackInfo callback,
            List<ItemStack> recipeOutputItems,
            List<FluidStack> recipeOutputFluids,
            List<Float> recipeOutputChances) {
        // List.size() is the item-loop condition and is visited once per
        // output. The first visit is the earliest point where all three lists
        // are available as locals, so explicitly run this setup only once.
        if (deterministicChance$outputPassStarted) {
            return;
        }
        deterministicChance$outputPassStarted = true;
        // Capture the three lists resolveOutputs already resolved. Calling any
        // of these getters again is observably wrong for dynamic recipes and,
        // in particular, can roll a catalyst-dependent chance profile twice.
        ThermalMachineSequenceController.beginOutputPass(
                this,
                curRecipe,
                recipeOutputItems,
                recipeOutputFluids,
                recipeOutputChances);
    }

    @ModifyVariable(
            method = "resolveOutputs()V",
            at = @At(value = "STORE", ordinal = 0),
            ordinal = 0,
            remap = false)
    private float deterministicChance$replaceOutputChance(float nativeChance) {
        return ThermalMachineSequenceController.nextOutputMultiplier(this, nativeChance);
    }

    @Inject(
            method = "load(Lnet/minecraft/nbt/CompoundTag;)V",
            at = @At("TAIL"),
            remap = true)
    private void deterministicChance$loadState(CompoundTag tag, CallbackInfo callback) {
        ThermalMachineSequenceController.load(this, tag);
    }

    @Inject(
            method = "saveAdditional(Lnet/minecraft/nbt/CompoundTag;)V",
            at = @At("TAIL"),
            remap = true)
    private void deterministicChance$saveState(CompoundTag tag, CallbackInfo callback) {
        ThermalMachineSequenceController.save(this, tag);
    }
}
