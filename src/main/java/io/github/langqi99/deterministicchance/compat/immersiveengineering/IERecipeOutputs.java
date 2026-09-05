package io.github.langqi99.deterministicchance.compat.immersiveengineering;

import blusunrize.immersiveengineering.api.crafting.ArcFurnaceRecipe;
import blusunrize.immersiveengineering.api.crafting.CrusherRecipe;
import blusunrize.immersiveengineering.api.crafting.StackWithChance;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemHandlerHelper;

/** Rebuilds the two supported IE output lists using the active machine sequence. */
public final class IERecipeOutputs {
    private IERecipeOutputs() {
    }

    public static NonNullList<ItemStack> crusher(CrusherRecipe recipe) {
        NonNullList<ItemStack> outputs = NonNullList.create();
        ItemStack primary = recipe.output.get();
        if (!primary.isEmpty()) {
            outputs.add(primary.copy());
        }
        for (int lane = 0; lane < recipe.secondaryOutputs.size(); lane++) {
            StackWithChance secondary = recipe.secondaryOutputs.get(lane);
            ItemStack stack = secondary.stack().get();
            if (!stack.isEmpty()
                    && IEProcessRollContext.roll(recipe.getId(), lane, secondary.chance())) {
                outputs.add(stack.copy());
            }
        }
        return outputs;
    }

    public static NonNullList<ItemStack> arcFurnace(ArcFurnaceRecipe recipe) {
        NonNullList<ItemStack> baseOutputs = recipe.getBaseOutputs();
        NonNullList<ItemStack> outputs = NonNullList.withSize(
                baseOutputs.size() + recipe.secondaryOutputs.size(),
                ItemStack.EMPTY);
        for (int index = 0; index < baseOutputs.size(); index++) {
            outputs.set(index, baseOutputs.get(index).copy());
        }

        int nextFree = baseOutputs.size();
        for (int lane = 0; lane < recipe.secondaryOutputs.size(); lane++) {
            StackWithChance secondary = recipe.secondaryOutputs.get(lane);
            ItemStack stack = secondary.stack().get();
            if (stack.isEmpty()
                    || !IEProcessRollContext.roll(recipe.getId(), lane, secondary.chance())) {
                continue;
            }

            ItemStack remaining = stack.copy();
            for (ItemStack existing : outputs) {
                if (ItemHandlerHelper.canItemStacksStack(remaining, existing)) {
                    existing.grow(remaining.getCount());
                    remaining = ItemStack.EMPTY;
                    break;
                }
            }
            if (!remaining.isEmpty()) {
                outputs.set(nextFree++, remaining);
            }
        }
        return outputs;
    }
}
