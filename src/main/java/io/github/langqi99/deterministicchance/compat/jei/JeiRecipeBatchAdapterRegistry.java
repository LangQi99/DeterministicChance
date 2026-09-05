package io.github.langqi99.deterministicchance.compat.jei;

import appeng.api.stacks.GenericStack;
import appeng.integration.modules.jei.GenericEntryStackHelper;
import io.github.langqi99.deterministicchance.DeterministicChance;
import io.github.langqi99.deterministicchance.compat.GTCEu7Availability;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;

/** Lazy string-based registry: optional-mod adapter classes are never linked when absent. */
public final class JeiRecipeBatchAdapterRegistry {
    private static final List<Registration> REGISTRATIONS = List.of(
            new Registration(
                    List.of("mekanism.api.recipes.SawmillRecipe"),
                    "io.github.langqi99.deterministicchance.compat.mekanism.MekanismSawmillJeiAdapter"),
            new Registration(
                    List.of("com.simibubi.create.content.processing.recipe.ProcessingRecipe"),
                    "io.github.langqi99.deterministicchance.compat.create.CreateProcessingJeiAdapter"),
            new Registration(
                    List.of("cofh.thermal.lib.util.recipes.ThermalRecipe"),
                    "io.github.langqi99.deterministicchance.compat.thermal.ThermalRecipeJeiAdapter"),
            new Registration(
                    List.of(
                            "com.gregtechceu.gtceu.api.recipe.GTRecipe",
                            "com.gregtechceu.gtceu.api.machine.trait.RecipeLogic"),
                    "io.github.langqi99.deterministicchance.compat.gtceu.GTCEu7RecipeJeiAdapter"),
            new Registration(
                    List.of(
                            "blusunrize.immersiveengineering.api.crafting.CrusherRecipe",
                            "blusunrize.immersiveengineering.api.crafting.ArcFurnaceRecipe"),
                    "io.github.langqi99.deterministicchance.compat.immersiveengineering."
                            + "ImmersiveEngineeringJeiAdapter"),
            new Registration(
                    List.of("cy.jdkdigital.productivebees.common.recipe.CentrifugeRecipe"),
                    "io.github.langqi99.deterministicchance.compat.productivebees."
                            + "ProductiveBeesCentrifugeJeiAdapter"),
            new Registration(
                    List.of("org.cyclops.integrateddynamics.core.recipe.type.RecipeMechanicalSqueezer"),
                    "io.github.langqi99.deterministicchance.compat.integrateddynamics."
                            + "IntegratedDynamicsSqueezerJeiAdapter"));

    private static volatile List<JeiRecipeBatchAdapter> loadedAdapters;

    private JeiRecipeBatchAdapterRegistry() {
    }

    public static Optional<JeiBatchPlan> plan(Object recipe, IRecipeSlotsView slotsView) {
        return decide(recipe, slotsView).exactPlan();
    }

    /** Public overload used by GameTests to exercise the real native adapter without a GUI. */
    public static Optional<JeiBatchPlan> plan(
            Object recipe,
            List<List<GenericStack>> inputs) {
        return decide(recipe, inputs).exactPlan();
    }

    public static JeiBatchDecision decide(Object recipe, IRecipeSlotsView slotsView) {
        List<List<GenericStack>> inputs = GenericEntryStackHelper.ofInputs(slotsView);
        for (JeiRecipeBatchAdapter adapter : adapters()) {
            try {
                if (!adapter.supports(recipe)) {
                    continue;
                }
                if (!adapter.hasProbabilisticOutputs(recipe, slotsView)) {
                    return JeiBatchDecision.notApplicable();
                }
                Optional<String> unsupportedReason =
                        adapter.exactBatchUnsupportedReason(recipe, slotsView);
                if (unsupportedReason.isPresent()) {
                    return JeiBatchDecision.unsupported(unsupportedReason.get());
                }
                List<List<GenericStack>> effectiveInputs =
                        adapter instanceof NativeInputJeiRecipeBatchAdapter nativeInputs
                                ? nativeInputs.inputs(recipe)
                                : inputs;
                return JeiBatchDecision.exact(JeiBatchPlanner.plan(
                        effectiveInputs,
                        adapter.outputs(recipe, slotsView)));
            } catch (IllegalArgumentException | ArithmeticException exception) {
                return unsupported(recipe, adapter, exception);
            } catch (LinkageError error) {
                return incompatible(adapter, error);
            }
        }
        return JeiBatchDecision.notApplicable();
    }

    /** Public overload used by GameTests to inspect exact/unsupported behavior. */
    public static JeiBatchDecision decide(
            Object recipe,
            List<List<GenericStack>> inputs) {
        for (JeiRecipeBatchAdapter adapter : adapters()) {
            try {
                if (!adapter.supports(recipe)) {
                    continue;
                }

                if (!adapter.hasProbabilisticOutputs(recipe)) {
                    return JeiBatchDecision.notApplicable();
                }
                Optional<String> unsupportedReason = adapter.exactBatchUnsupportedReason(recipe);
                if (unsupportedReason.isPresent()) {
                    return JeiBatchDecision.unsupported(unsupportedReason.get());
                }
                List<ChanceStack> outputs = adapter.outputs(recipe);
                List<List<GenericStack>> effectiveInputs = adapter instanceof NativeInputJeiRecipeBatchAdapter nativeInputs
                        ? nativeInputs.inputs(recipe)
                        : inputs;
                return JeiBatchDecision.exact(JeiBatchPlanner.plan(effectiveInputs, outputs));
            } catch (IllegalArgumentException | ArithmeticException exception) {
                return unsupported(recipe, adapter, exception);
            } catch (LinkageError error) {
                return incompatible(adapter, error);
            }
        }
        return JeiBatchDecision.notApplicable();
    }

    private static JeiBatchDecision unsupported(
            Object recipe,
            JeiRecipeBatchAdapter adapter,
            RuntimeException exception) {
        DeterministicChance.LOGGER.warn(
                "Cannot create an exact AE2 batch for recipe {} with adapter {}: {}",
                recipe,
                adapter.getClass().getName(),
                exception.getMessage());
        return JeiBatchDecision.unsupported(exception.getMessage());
    }

    private static JeiBatchDecision incompatible(JeiRecipeBatchAdapter adapter, LinkageError error) {
        DeterministicChance.LOGGER.error(
                "Optional recipe adapter {} is incompatible with the loaded mod version",
                adapter.getClass().getName(),
                error);
        return JeiBatchDecision.unsupported(
                "the loaded optional mod version is incompatible with this adapter");
    }

    public static List<String> loadedAdapterNames() {
        return adapters().stream().map(adapter -> adapter.getClass().getName()).toList();
    }

    private static List<JeiRecipeBatchAdapter> adapters() {
        List<JeiRecipeBatchAdapter> result = loadedAdapters;
        if (result != null) {
            return result;
        }
        synchronized (JeiRecipeBatchAdapterRegistry.class) {
            if (loadedAdapters == null) {
                loadedAdapters = loadAdapters();
            }
            return loadedAdapters;
        }
    }

    private static List<JeiRecipeBatchAdapter> loadAdapters() {
        ClassLoader loader = JeiRecipeBatchAdapterRegistry.class.getClassLoader();
        List<JeiRecipeBatchAdapter> result = new ArrayList<>();
        for (Registration registration : REGISTRATIONS) {
            if (registration.adapterClass().endsWith("GTCEu7RecipeJeiAdapter")
                    && !GTCEu7Availability.isLoaded()) {
                continue;
            }
            if (!registration.requirements().stream().allMatch(name -> classExists(loader, name))) {
                continue;
            }
            try {
                Class<?> adapterClass = Class.forName(registration.adapterClass(), true, loader);
                result.add((JeiRecipeBatchAdapter) adapterClass.getDeclaredConstructor().newInstance());
            } catch (ClassNotFoundException
                    | NoSuchMethodException
                    | InstantiationException
                    | IllegalAccessException
                    | InvocationTargetException
                    | LinkageError exception) {
                DeterministicChance.LOGGER.error(
                        "Could not load probability recipe adapter {}",
                        registration.adapterClass(),
                        exception);
            }
        }
        return List.copyOf(result);
    }

    private static boolean classExists(ClassLoader loader, String className) {
        return loader.getResource(className.replace('.', '/') + ".class") != null;
    }

    private record Registration(List<String> requirements, String adapterClass) {
    }
}
