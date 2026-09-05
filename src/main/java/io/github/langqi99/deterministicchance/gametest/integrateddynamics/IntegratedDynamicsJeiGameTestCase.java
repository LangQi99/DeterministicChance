package io.github.langqi99.deterministicchance.gametest.integrateddynamics;

import appeng.api.stacks.AEItemKey;
import io.github.langqi99.deterministicchance.compat.integrateddynamics.IntegratedDynamicsSqueezerJeiAdapter;
import io.github.langqi99.deterministicchance.compat.jei.JeiRecipeBatchAdapterRegistry;
import io.github.langqi99.deterministicchance.gametest.AePatternPlanAssertions;
import java.util.Map;
import java.util.Objects;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.cyclops.integrateddynamics.core.recipe.type.RecipeMechanicalSqueezer;

public final class IntegratedDynamicsJeiGameTestCase {
    private IntegratedDynamicsJeiGameTestCase() {
    }

    public static void run(GameTestHelper helper) {
        RecipeMechanicalSqueezer recipe = IntegratedDynamicsMachineGameTestCase.fixture(helper);
        IntegratedDynamicsSqueezerJeiAdapter adapter = new IntegratedDynamicsSqueezerJeiAdapter();
        AePatternPlanAssertions.assertExactPlanAndEncodedPattern(
                helper,
                JeiRecipeBatchAdapterRegistry.plan(recipe, adapter.inputs(recipe)),
                2,
                adapter.inputs(recipe),
                Map.of(Objects.requireNonNull(AEItemKey.of(new ItemStack(Items.REDSTONE))), 26L),
                "Integrated Dynamics released Mechanical Squeezer redstone recipe");
        helper.succeed();
    }
}
