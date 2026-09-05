package io.github.langqi99.deterministicchance.mixin.immersiveengineering;

import blusunrize.immersiveengineering.common.blocks.multiblocks.process.MultiblockProcess;
import blusunrize.immersiveengineering.common.blocks.multiblocks.process.ProcessContext;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Accessor used by the preview/commit redirect to call IE's original virtual output method. */
@Mixin(value = MultiblockProcess.class, remap = false)
public interface MultiblockProcessInvoker {
    @Invoker("getRecipeItemOutputs")
    List<ItemStack> deterministicChance$invokeRecipeItemOutputs(
            Level level,
            ProcessContext<?> context);
}
