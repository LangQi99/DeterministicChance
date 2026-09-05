package io.github.langqi99.deterministicchance.gametest;

import io.github.langqi99.deterministicchance.DeterministicChance;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.fml.ModList;

/**
 * Stable GameTest discovery point that deliberately has no references to optional-mod classes.
 *
 * <p>Forge loads this class in every dependency profile. Each implementation is linked only
 * after every mod needed by that particular test has been confirmed as loaded.</p>
 */
@GameTestHolder(DeterministicChance.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CompatibilityGameTests {
    private static final String EXPECTED_MODS_PROPERTY = "deterministicchance.expectedTestMods";
    private static final String FORBIDDEN_MODS_PROPERTY = "deterministicchance.forbiddenTestMods";
    private static final String CREATE_MACHINE_TEST =
            "io.github.langqi99.deterministicchance.gametest.create.CreateMachineGameTestCase";
    private static final String CREATE_JEI_TEST =
            "io.github.langqi99.deterministicchance.gametest.create.CreateJeiGameTestCase";
    private static final String MEKANISM_MACHINE_TEST =
            "io.github.langqi99.deterministicchance.gametest.mekanism.MekanismMachineGameTestCase";
    private static final String MEKANISM_FACTORY_TEST =
            "io.github.langqi99.deterministicchance.gametest.mekanism.MekanismSawingFactoryGameTestCase";
    private static final String MEKANISM_JEI_TEST =
            "io.github.langqi99.deterministicchance.gametest.mekanism.MekanismJeiGameTestCase";
    private static final String THERMAL_MACHINE_TEST =
            "io.github.langqi99.deterministicchance.gametest.thermal.ThermalMachineGameTestCase";
    private static final String THERMAL_JEI_TEST =
            "io.github.langqi99.deterministicchance.gametest.thermal.ThermalJeiGameTestCase";
    private static final String GTCEU_MACHINE_TEST =
            "io.github.langqi99.deterministicchance.gametest.gtceu.GTCEuMachineGameTestCase";
    private static final String GTCEU_JEI_TEST =
            "io.github.langqi99.deterministicchance.gametest.gtceu.GTCEuJeiGameTestCase";
    private static final String IE_MACHINE_TEST =
            "io.github.langqi99.deterministicchance.gametest.immersiveengineering."
                    + "ImmersiveEngineeringMachineGameTestCase";
    private static final String IE_JEI_TEST =
            "io.github.langqi99.deterministicchance.gametest.immersiveengineering."
                    + "ImmersiveEngineeringJeiGameTestCase";

    private CompatibilityGameTests() {
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void dependencyProfileMatchesRequestedMatrix(GameTestHelper helper) {
        for (String modId : configuredModIds(EXPECTED_MODS_PROPERTY)) {
            helper.assertTrue(
                    ModList.get().isLoaded(modId),
                    "Dependency matrix expected mod '" + modId + "' but it was not loaded");
        }
        for (String modId : configuredModIds(FORBIDDEN_MODS_PROPERTY)) {
            helper.assertFalse(
                    ModList.get().isLoaded(modId),
                    "Dependency matrix forbids mod '" + modId + "' but it was loaded");
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void createActualRecipeUsesExactSequence(GameTestHelper helper) {
        runWhenLoaded(helper, new String[] {"create"}, CREATE_MACHINE_TEST, "run");
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void createJeiTransferBuildsExactAePattern(GameTestHelper helper) {
        runWhenLoaded(helper, new String[] {"create", "ae2", "jei"}, CREATE_JEI_TEST, "run");
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void mekanismActualRecipeUsesExactSequence(GameTestHelper helper) {
        runWhenLoaded(helper, new String[] {"mekanism"}, MEKANISM_MACHINE_TEST, "run");
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void mekanismSawingFactoryPersistsIndependentSequence(GameTestHelper helper) {
        runWhenLoaded(helper, new String[] {"mekanism"}, MEKANISM_FACTORY_TEST, "run");
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void mekanismJeiTransferBuildsExactAePattern(GameTestHelper helper) {
        runWhenLoaded(helper, new String[] {"mekanism", "ae2", "jei"}, MEKANISM_JEI_TEST, "run");
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void thermalActualMachineUsesExactPersistentSequence(GameTestHelper helper) {
        runWhenLoaded(
                helper,
                new String[] {"thermal", "thermal_expansion"},
                THERMAL_MACHINE_TEST,
                "run");
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void thermalJeiTransferBuildsExactAePattern(GameTestHelper helper) {
        runWhenLoaded(
                helper,
                new String[] {"thermal", "thermal_expansion", "ae2", "jei"},
                THERMAL_JEI_TEST,
                "run");
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void gtceuActualRecipeUsesExactPersistentSequence(GameTestHelper helper) {
        runWhenLoaded(helper, new String[] {"gtceu"}, GTCEU_MACHINE_TEST, "run");
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void gtceuJeiTransferBuildsExactAePattern(GameTestHelper helper) {
        runWhenLoaded(
                helper,
                new String[] {"gtceu", "ae2", "jei"},
                GTCEU_JEI_TEST,
                "run");
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void immersiveEngineeringPreviewAndCommitUseExactSequence(GameTestHelper helper) {
        runWhenLoaded(
                helper,
                new String[] {"immersiveengineering"},
                IE_MACHINE_TEST,
                "run");
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void immersiveEngineeringJeiTransferBuildsExactAePattern(GameTestHelper helper) {
        runWhenLoaded(
                helper,
                new String[] {"immersiveengineering", "ae2", "jei"},
                IE_JEI_TEST,
                "run");
    }

    private static void runWhenLoaded(
            GameTestHelper helper,
            String[] requiredMods,
            String implementationClass,
            String methodName) {
        for (String modId : requiredMods) {
            if (!ModList.get().isLoaded(modId)) {
                DeterministicChance.LOGGER.info(
                        "Skipping GameTest {} because optional mod {} is not loaded",
                        methodName,
                        modId);
                helper.succeed();
                return;
            }
        }

        try {
            Class<?> implementation = Class.forName(
                    implementationClass,
                    true,
                    CompatibilityGameTests.class.getClassLoader());
            Method method = implementation.getMethod(methodName, GameTestHelper.class);
            if (!Modifier.isStatic(method.getModifiers())) {
                helper.fail("Optional GameTest entry is not static: " + implementationClass + "#" + methodName);
                return;
            }
            method.invoke(null, helper);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause() == null ? exception : exception.getCause();
            helper.fail("Optional GameTest failed for " + Arrays.toString(requiredMods) + ": " + cause);
        } catch (ReflectiveOperationException | LinkageError exception) {
            helper.fail("Could not load optional GameTest for " + Arrays.toString(requiredMods)
                    + " (" + implementationClass + "): " + exception);
        }
    }

    private static List<String> configuredModIds(String property) {
        return Arrays.stream(System.getProperty(property, "").split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .distinct()
                .toList();
    }
}
