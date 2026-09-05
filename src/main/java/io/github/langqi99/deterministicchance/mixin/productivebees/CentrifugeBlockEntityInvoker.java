package io.github.langqi99.deterministicchance.mixin.productivebees;

import cy.jdkdigital.productivebees.common.block.entity.CentrifugeBlockEntity;
import cy.jdkdigital.productivebees.common.recipe.CentrifugeRecipe;
import net.minecraft.util.RandomSource;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = CentrifugeBlockEntity.class, remap = false)
public interface CentrifugeBlockEntityInvoker {
    @Invoker("completeRecipeProcessing")
    void deterministicChance$completeRecipeProcessing(
            CentrifugeRecipe recipe,
            IItemHandlerModifiable inventory,
            RandomSource random,
            boolean stripWax);
}
