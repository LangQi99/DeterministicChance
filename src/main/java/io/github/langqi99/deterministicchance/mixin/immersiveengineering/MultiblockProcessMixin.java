package io.github.langqi99.deterministicchance.mixin.immersiveengineering;

import blusunrize.immersiveengineering.api.crafting.MultiblockRecipe;
import blusunrize.immersiveengineering.common.blocks.multiblocks.process.MultiblockProcess;
import blusunrize.immersiveengineering.common.blocks.multiblocks.process.ProcessContext;
import io.github.langqi99.deterministicchance.compat.immersiveengineering.IEProcessRollContext;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Marks IE's central output-space check as preview and final insertion as commit. */
@Mixin(value = MultiblockProcess.class, remap = false)
abstract class MultiblockProcessMixin<R extends MultiblockRecipe, C extends ProcessContext<R>> {
    private static final String OUTPUT_CALL =
            "Lblusunrize/immersiveengineering/common/blocks/multiblocks/process/MultiblockProcess;"
                    + "getRecipeItemOutputs(Lnet/minecraft/world/level/Level;"
                    + "Lblusunrize/immersiveengineering/common/blocks/multiblocks/process/ProcessContext;)"
                    + "Ljava/util/List;";

    @Redirect(
            method = "canProcess",
            at = @At(value = "INVOKE", target = OUTPUT_CALL),
            remap = false,
            require = 1)
    private List<ItemStack> deterministicChance$previewOutputs(
            MultiblockProcess<?, ?> process,
            Level level,
            ProcessContext<?> context) {
        return IEProcessRollContext.call(
                context,
                process.getRecipeId(),
                false,
                () -> ((MultiblockProcessInvoker) process)
                        .deterministicChance$invokeRecipeItemOutputs(level, context));
    }

    @Redirect(
            method = "processFinish",
            at = @At(value = "INVOKE", target = OUTPUT_CALL),
            remap = false,
            require = 1)
    private List<ItemStack> deterministicChance$commitOutputs(
            MultiblockProcess<?, ?> process,
            Level level,
            ProcessContext<?> context) {
        return IEProcessRollContext.call(
                context,
                process.getRecipeId(),
                true,
                () -> ((MultiblockProcessInvoker) process)
                        .deterministicChance$invokeRecipeItemOutputs(level, context));
    }
}
