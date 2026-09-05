package io.github.langqi99.deterministicchance.mixin;

import io.github.langqi99.deterministicchance.compat.GTCEu7Availability;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/**
 * Keeps every compatibility hook safe when its target mod is absent. Class
 * resources are checked without initializing any optional-mod class.
 */
public final class DeterministicChanceMixinPlugin implements IMixinConfigPlugin {
    private static final String GTCEU_7_MIXIN =
            "io.github.langqi99.deterministicchance.mixin.gtceu.GTCEu7RecipeRunnerMixin";
    private static final Map<String, List<String>> REQUIREMENTS = Map.ofEntries(
            Map.entry(
                    "io.github.langqi99.deterministicchance.mixin.mekanism.SawmillChanceOutputMixin",
                    List.of("mekanism.api.recipes.SawmillRecipe$ChanceOutput")),
            Map.entry(
                    "io.github.langqi99.deterministicchance.mixin.mekanism.PrecisionSawmillCachedRecipeMixin",
                    List.of("mekanism.common.tile.machine.TileEntityPrecisionSawmill")),
            Map.entry(
                    "io.github.langqi99.deterministicchance.mixin.mekanism.SawingFactoryCachedRecipeMixin",
                    List.of("mekanism.common.tile.factory.TileEntitySawingFactory")),
            Map.entry(
                    "io.github.langqi99.deterministicchance.mixin.mekanism.OneInputCachedRecipeMixin",
                    List.of("mekanism.api.recipes.cache.OneInputCachedRecipe")),
            Map.entry(
                    "io.github.langqi99.deterministicchance.mixin.create.ProcessingRecipeMixin",
                    List.of("com.simibubi.create.content.processing.recipe.ProcessingRecipe")),
            Map.entry(
                    "io.github.langqi99.deterministicchance.mixin.thermal.MachineBlockEntityMixin",
                    List.of("cofh.thermal.lib.common.block.entity.MachineBlockEntity")),
            Map.entry(
                    "io.github.langqi99.deterministicchance.mixin.productivebees.CentrifugeBlockEntityMixin",
                    List.of(
                            "cy.jdkdigital.productivebees.common.block.entity.CentrifugeBlockEntity",
                            "cy.jdkdigital.productivebees.common.recipe.CentrifugeRecipe")),
            Map.entry(
                    "io.github.langqi99.deterministicchance.mixin.productivebees.CentrifugeBlockEntityInvoker",
                    List.of("cy.jdkdigital.productivebees.common.block.entity.CentrifugeBlockEntity")),
            Map.entry(
                    "io.github.langqi99.deterministicchance.mixin.integrateddynamics.MechanicalSqueezerBlockEntityMixin",
                    List.of(
                            "org.cyclops.integrateddynamics.blockentity.BlockEntityMechanicalSqueezer",
                            "org.cyclops.integrateddynamics.core.recipe.type.RecipeMechanicalSqueezer")),
            Map.entry(
                    "io.github.langqi99.deterministicchance.mixin.integrateddynamics.MechanicalSqueezerInvoker",
                    List.of("org.cyclops.integrateddynamics.blockentity.BlockEntityMechanicalSqueezer")),
            Map.entry(
                    GTCEU_7_MIXIN,
                    List.of(
                            "com.gregtechceu.gtceu.api.recipe.RecipeRunner",
                            "com.gregtechceu.gtceu.api.machine.trait.RecipeLogic")),
            Map.entry(
                    "io.github.langqi99.deterministicchance.mixin.ae2.EncodePatternTransferHandlerMixin",
                    List.of(
                            "appeng.integration.modules.jei.transfer.EncodePatternTransferHandler",
                            "mezz.jei.api.gui.ingredient.IRecipeSlotsView")));

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (GTCEU_7_MIXIN.equals(mixinClassName) && !GTCEu7Availability.isLoaded()) {
            return false;
        }
        List<String> requirements = REQUIREMENTS.getOrDefault(mixinClassName, List.of(targetClassName));
        ClassLoader loader = DeterministicChanceMixinPlugin.class.getClassLoader();
        return requirements.stream().allMatch(name ->
                loader.getResource(name.replace('.', '/') + ".class") != null);
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(
            String targetClassName,
            ClassNode targetClass,
            String mixinClassName,
            IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(
            String targetClassName,
            ClassNode targetClass,
            String mixinClassName,
            IMixinInfo mixinInfo) {
    }
}
