package io.github.langqi99.deterministicchance.mixin.immersiveengineering;

import blusunrize.immersiveengineering.common.blocks.multiblocks.logic.CrusherLogic;
import blusunrize.immersiveengineering.common.blocks.multiblocks.logic.arcfurnace.ArcFurnaceLogic;
import io.github.langqi99.deterministicchance.compat.immersiveengineering.IESequenceState;
import io.github.langqi99.deterministicchance.compat.immersiveengineering.IESequenceStateAccess;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds persisted probability cursors to both supported IE multiblock states. */
@Mixin(
        value = {CrusherLogic.State.class, ArcFurnaceLogic.State.class},
        remap = false)
abstract class MultiblockStateMixin implements IESequenceStateAccess {
    @Unique
    private IESequenceState deterministicChance$ieSequenceState;

    @Override
    public IESequenceState deterministicChance$ieState() {
        if (deterministicChance$ieSequenceState == null) {
            deterministicChance$ieSequenceState = new IESequenceState();
        }
        return deterministicChance$ieSequenceState;
    }

    @Inject(method = "writeSaveNBT", at = @At("TAIL"), remap = false, require = 1)
    private void deterministicChance$saveSequence(CompoundTag tag, CallbackInfo callback) {
        deterministicChance$ieState().save(tag);
    }

    @Inject(method = "readSaveNBT", at = @At("TAIL"), remap = false, require = 1)
    private void deterministicChance$loadSequence(CompoundTag tag, CallbackInfo callback) {
        deterministicChance$ieState().load(tag);
    }
}
