package io.github.langqi99.deterministicchance.mixin.create;

import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.content.kinetics.press.MechanicalPressBlockEntity;
import com.simibubi.create.foundation.recipe.RecipeApplier;
import io.github.langqi99.deterministicchance.compat.create.CreateMachineRollContext;
import java.util.List;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Supplies the press as owner for an assembly result committed on belts, depots, or in-world. */
@Mixin(value = MechanicalPressBlockEntity.class, remap = false)
abstract class MechanicalPressAssemblyMixin {
    @Redirect(
            method = {"tryProcessOnBelt", "tryProcessInWorld"},
            at = @At(value = "INVOKE", target = "Lcom/simibubi/create/foundation/recipe/RecipeApplier;applyRecipeOn(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/crafting/Recipe;Z)Ljava/util/List;"),
            remap = false)
    private List<ItemStack> deterministicChance$applyOnStack(
            Level level, ItemStack stack, Recipe<?> recipe, boolean keepHeldItem) {
        return CreateMachineRollContext.withMachine(
                (BlockEntity) (Object) this,
                () -> RecipeApplier.applyRecipeOn(level, stack, recipe, keepHeldItem));
    }

    @Redirect(
            method = "tryProcessInWorld",
            at = @At(value = "INVOKE", target = "Lcom/simibubi/create/foundation/recipe/RecipeApplier;applyRecipeOn(Lnet/minecraft/world/entity/item/ItemEntity;Lnet/minecraft/world/item/crafting/Recipe;Z)V"),
            remap = false,
            require = 1)
    private void deterministicChance$applyOnEntity(
            ItemEntity entity, Recipe<?> recipe, boolean keepHeldItem) {
        CreateMachineRollContext.withMachine(
                (BlockEntity) (Object) this,
                () -> {
                    RecipeApplier.applyRecipeOn(entity, recipe, keepHeldItem);
                    return null;
                });
    }
}
