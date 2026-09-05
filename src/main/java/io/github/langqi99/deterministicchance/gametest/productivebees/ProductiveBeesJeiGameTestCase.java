package io.github.langqi99.deterministicchance.gametest.productivebees;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import cy.jdkdigital.productivebees.common.recipe.CentrifugeRecipe;
import io.github.langqi99.deterministicchance.compat.jei.JeiRecipeBatchAdapterRegistry;
import io.github.langqi99.deterministicchance.compat.productivebees.ProductiveBeesCentrifugeJeiAdapter;
import io.github.langqi99.deterministicchance.gametest.AePatternPlanAssertions;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Runs the native adapter through the common planner and AE2's real processing-pattern codec. */
public final class ProductiveBeesJeiGameTestCase {
    private ProductiveBeesJeiGameTestCase() {
    }

    public static void run(GameTestHelper helper) {
        CentrifugeRecipe recipe = ProductiveBeesFixture.recipe();
        ProductiveBeesCentrifugeJeiAdapter adapter = new ProductiveBeesCentrifugeJeiAdapter();
        Map<appeng.api.stacks.AEKey, Long> expectedOutputs = new LinkedHashMap<>();
        expectedOutputs.put(Objects.requireNonNull(AEItemKey.of(new ItemStack(Items.GOLD_NUGGET))), 24L);
        expectedOutputs.put(Objects.requireNonNull(AEItemKey.of(new ItemStack(Items.IRON_NUGGET))), 30L);

        AePatternPlanAssertions.assertExactPlanAndEncodedPattern(
                helper,
                JeiRecipeBatchAdapterRegistry.plan(recipe, adapter.inputs(recipe)),
                ProductiveBeesFixture.EXECUTIONS,
                new GenericStack(
                        Objects.requireNonNull(AEItemKey.of(new ItemStack(Items.COBBLESTONE))),
                        1),
                expectedOutputs,
                "Productive Bees 80% plus ranged multi-output centrifuge recipe");
        helper.succeed();
    }
}
