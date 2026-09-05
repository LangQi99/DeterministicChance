package io.github.langqi99.deterministicchance.gametest.productivebees;

import com.mojang.datafixers.util.Pair;
import cy.jdkdigital.productivebees.common.recipe.CentrifugeRecipe;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

final class ProductiveBeesFixture {
    static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(
            "deterministic_chance", "productive_bees_multiple_outputs");
    static final int EXECUTIONS = 30;

    private ProductiveBeesFixture() {
    }

    static CentrifugeRecipe recipe() {
        Map<Ingredient, IntArrayTag> outputs = new LinkedHashMap<>();
        outputs.put(Ingredient.of(Items.GOLD_NUGGET), new IntArrayTag(new int[] {1, 1, 80}));
        outputs.put(Ingredient.of(Items.IRON_NUGGET), new IntArrayTag(new int[] {1, 3, 50}));
        outputs.put(Ingredient.of(Items.DIAMOND), new IntArrayTag(new int[] {1, 1, 0}));
        return new CentrifugeRecipe(
                ID,
                Ingredient.of(Items.COBBLESTONE),
                outputs,
                (Pair<String, Integer>) null,
                1);
    }
}
