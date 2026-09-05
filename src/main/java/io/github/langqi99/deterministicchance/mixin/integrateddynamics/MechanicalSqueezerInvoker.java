package io.github.langqi99.deterministicchance.mixin.integrateddynamics;

import org.cyclops.integrateddynamics.blockentity.BlockEntityMechanicalSqueezer;
import org.cyclops.integrateddynamics.core.recipe.type.RecipeMechanicalSqueezer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = BlockEntityMechanicalSqueezer.class, remap = false)
public interface MechanicalSqueezerInvoker {
    @Invoker("finalizeRecipe")
    boolean deterministicChance$finalizeRecipe(RecipeMechanicalSqueezer recipe, boolean simulate);
}
