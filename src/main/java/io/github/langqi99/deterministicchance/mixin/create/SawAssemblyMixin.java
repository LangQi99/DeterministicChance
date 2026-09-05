package io.github.langqi99.deterministicchance.mixin.create;

import com.simibubi.create.content.kinetics.saw.CuttingRecipe;
import com.simibubi.create.content.kinetics.saw.SawBlockEntity;
import io.github.langqi99.deterministicchance.compat.create.CreateMachineRollContext;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Supplies the mechanical saw as owner for a cutting assembly's final result. */
@Mixin(value = SawBlockEntity.class, remap = false)
abstract class SawAssemblyMixin {
    @Redirect(
            method = "applyRecipe",
            at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/kinetics/saw/CuttingRecipe;rollResults()Ljava/util/List;"),
            remap = false,
            require = 1)
    private List<ItemStack> deterministicChance$rollResults(CuttingRecipe recipe) {
        return CreateMachineRollContext.withMachine(
                (BlockEntity) (Object) this,
                recipe::rollResults);
    }
}
