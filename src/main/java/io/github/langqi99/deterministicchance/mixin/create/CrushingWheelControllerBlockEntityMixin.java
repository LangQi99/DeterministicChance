package io.github.langqi99.deterministicchance.mixin.create;

import com.simibubi.create.content.kinetics.crusher.CrushingWheelControllerBlockEntity;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import io.github.langqi99.deterministicchance.compat.create.CreateMachineRollContext;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Establishes the crushing-wheel controller around each committed recipe roll. */
@Mixin(value = CrushingWheelControllerBlockEntity.class, remap = false)
abstract class CrushingWheelControllerBlockEntityMixin {
    @Redirect(
            method = "applyRecipe",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/simibubi/create/content/processing/recipe/ProcessingRecipe;"
                            + "rollResults()Ljava/util/List;",
                    remap = false),
            remap = false)
    private List<ItemStack> deterministicChance$rollResults(ProcessingRecipe<?> recipe) {
        return CreateMachineRollContext.withMachine(
                (BlockEntity) (Object) this,
                recipe::rollResults);
    }
}
