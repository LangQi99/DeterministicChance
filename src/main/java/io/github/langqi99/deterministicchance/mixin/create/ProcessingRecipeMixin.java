package io.github.langqi99.deterministicchance.mixin.create;

import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import io.github.langqi99.deterministicchance.compat.create.CreateMachineRollContext;
import io.github.langqi99.deterministicchance.compat.create.CreateRecipeSupport;
import io.github.langqi99.deterministicchance.compat.create.ProcessingOutputSequenceController;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Replaces rolls only inside a supported machine's committed processing pass. */
@Mixin(value = ProcessingRecipe.class, remap = false)
abstract class ProcessingRecipeMixin {
    @Redirect(
            method = "rollResults(Ljava/util/List;)Ljava/util/List;",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/simibubi/create/content/processing/recipe/ProcessingOutput;"
                            + "rollOutput()Lnet/minecraft/world/item/ItemStack;",
                    remap = false),
            remap = false)
    private ItemStack deterministicChance(
            ProcessingOutput output,
            List<ProcessingOutput> activeOutputs) {
        ProcessingRecipe<?> recipe = (ProcessingRecipe<?>) (Object) this;
        BlockEntity machine = CreateMachineRollContext.activeMachine();
        if (machine == null
                || machine.getLevel() == null
                || machine.getLevel().isClientSide
                || !CreateRecipeSupport.isDeterministic(recipe)) {
            return output.rollOutput();
        }
        int count = ProcessingOutputSequenceController.nextOutputCount(
                machine, recipe, activeOutputs, output);
        if (count == 0) {
            return ItemStack.EMPTY;
        }

        ItemStack result = output.getStack().copy();
        result.setCount(count);
        return result;
    }
}
