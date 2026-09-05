package io.github.langqi99.deterministicchance.mixin.create;

import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.content.kinetics.deployer.BeltDeployerCallbacks;
import com.simibubi.create.content.kinetics.deployer.DeployerBlockEntity;
import io.github.langqi99.deterministicchance.compat.create.CreateMachineRollContext;
import net.minecraft.world.item.crafting.Recipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Supplies the deployer as owner around its committed belt application. */
@Mixin(value = BeltDeployerCallbacks.class, remap = false)
abstract class DeployerAssemblyMixin {
    @Redirect(
            method = "whenItemHeld",
            at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/kinetics/deployer/BeltDeployerCallbacks;activate(Lcom/simibubi/create/content/kinetics/belt/transport/TransportedItemStack;Lcom/simibubi/create/content/kinetics/belt/behaviour/TransportedItemStackHandlerBehaviour;Lcom/simibubi/create/content/kinetics/deployer/DeployerBlockEntity;Lnet/minecraft/world/item/crafting/Recipe;)V"),
            remap = false,
            require = 1)
    private static void deterministicChance$activate(
            TransportedItemStack transported,
            TransportedItemStackHandlerBehaviour handler,
            DeployerBlockEntity deployer,
            Recipe<?> recipe) {
        CreateMachineRollContext.withMachine(deployer, () -> {
            BeltDeployerCallbacks.activate(transported, handler, deployer, recipe);
            return null;
        });
    }
}
