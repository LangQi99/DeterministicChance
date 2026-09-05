package io.github.langqi99.deterministicchance.mixin.create;

import com.simibubi.create.content.fluids.spout.FillingBySpout;
import com.simibubi.create.content.fluids.spout.SpoutBlockEntity;
import io.github.langqi99.deterministicchance.compat.create.CreateMachineRollContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Supplies the spout as owner around a committed filling operation. */
@Mixin(value = SpoutBlockEntity.class, remap = false)
abstract class SpoutAssemblyMixin {
    @Redirect(
            method = "whenItemHeld",
            at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/fluids/spout/FillingBySpout;fillItem(Lnet/minecraft/world/level/Level;ILnet/minecraft/world/item/ItemStack;Lnet/minecraftforge/fluids/FluidStack;)Lnet/minecraft/world/item/ItemStack;"),
            remap = false,
            require = 1)
    private ItemStack deterministicChance$fillItem(
            Level level, int requiredAmount, ItemStack stack, FluidStack fluid) {
        return CreateMachineRollContext.withMachine(
                (BlockEntity) (Object) this,
                () -> FillingBySpout.fillItem(level, requiredAmount, stack, fluid));
    }
}
