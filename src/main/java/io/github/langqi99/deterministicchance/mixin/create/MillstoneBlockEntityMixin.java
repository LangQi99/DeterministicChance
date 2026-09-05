package io.github.langqi99.deterministicchance.mixin.create;

import com.simibubi.create.content.kinetics.millstone.MillingRecipe;
import com.simibubi.create.content.kinetics.millstone.MillstoneBlockEntity;
import io.github.langqi99.deterministicchance.compat.create.CreateMachineRollContext;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Establishes the persistent millstone owner around its committed recipe roll. */
@Mixin(value = MillstoneBlockEntity.class, remap = false)
abstract class MillstoneBlockEntityMixin {
    @Redirect(
            method = "process",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/simibubi/create/content/kinetics/millstone/MillingRecipe;"
                            + "rollResults()Ljava/util/List;",
                    remap = false),
            remap = false)
    private List<ItemStack> deterministicChance$rollResults(MillingRecipe recipe) {
        return CreateMachineRollContext.withMachine(
                (BlockEntity) (Object) this,
                recipe::rollResults);
    }
}
