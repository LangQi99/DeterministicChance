package io.github.langqi99.deterministicchance.mixin.gtceu;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.IRecipeCapabilityHolder;
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.RecipeRunner;
import com.gregtechceu.gtceu.api.recipe.chance.boost.ChanceBoostFunction;
import com.gregtechceu.gtceu.api.recipe.chance.logic.ChanceLogic;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import io.github.langqi99.deterministicchance.compat.gtceu.GTCEu7OrChanceRoller;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Replaces only GTCEu 7's committed, non-tick output OR roll. Chanced inputs
 * and correlated AND/XOR/FIRST/custom logics always delegate to GTCEu.
 */
@Mixin(value = RecipeRunner.class, remap = false)
abstract class GTCEu7RecipeRunnerMixin {
    @Shadow @Final private GTRecipe recipe;
    @Shadow @Final private IO io;
    @Shadow @Final private boolean isTick;
    @Shadow @Final private boolean simulated;

    @Unique
    private IRecipeCapabilityHolder deterministicChance$holder;

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void deterministicChance$captureHolder(
            GTRecipe recipe,
            IO io,
            boolean isTick,
            IRecipeCapabilityHolder holder,
            Map<RecipeCapability<?>, Object2IntMap<?>> chanceCaches,
            boolean simulated,
            CallbackInfo callback) {
        deterministicChance$holder = holder;
    }

    @Redirect(
            method = "fillContentMatchList",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/gregtechceu/gtceu/api/recipe/chance/logic/ChanceLogic;"
                            + "roll(Lcom/gregtechceu/gtceu/api/capability/recipe/RecipeCapability;"
                            + "Ljava/util/List;"
                            + "Lcom/gregtechceu/gtceu/api/recipe/chance/boost/ChanceBoostFunction;"
                            + "IILit/unimi/dsi/fastutil/objects/Object2IntMap;I)Ljava/util/List;"),
            remap = false,
            require = 1)
    private List<Content> deterministicChance$rollIndependentOutput(
            ChanceLogic logic,
            RecipeCapability<?> capability,
            List<Content> entries,
            ChanceBoostFunction boostFunction,
            int recipeTier,
            int chanceTier,
            Object2IntMap<?> cache,
            int times) {
        if (simulated
                || io != IO.OUT
                || isTick
                || logic != ChanceLogic.OR
                || recipe.getId() == null
                || !(deterministicChance$holder instanceof IRecipeLogicMachine machine)) {
            return logic.roll(
                    capability,
                    entries,
                    boostFunction,
                    recipeTier,
                    chanceTier,
                    cache,
                    times);
        }

        BlockEntity owner = machine.self().getHolder().self();
        if (owner.getLevel() == null || owner.getLevel().isClientSide()) {
            return logic.roll(
                    capability,
                    entries,
                    boostFunction,
                    recipeTier,
                    chanceTier,
                    cache,
                    times);
        }
        return GTCEu7OrChanceRoller.roll(
                owner,
                recipe,
                capability,
                entries,
                boostFunction,
                recipeTier,
                chanceTier,
                times);
    }
}
