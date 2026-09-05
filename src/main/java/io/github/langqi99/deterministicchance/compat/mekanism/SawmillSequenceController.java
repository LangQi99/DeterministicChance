package io.github.langqi99.deterministicchance.compat.mekanism;

import io.github.langqi99.deterministicchance.core.ChanceFraction;
import io.github.langqi99.deterministicchance.core.DeterministicSequence;
import mekanism.api.recipes.SawmillRecipe;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Owns the deterministic phase for each sawmill machine and recipe.
 *
 * <p>The cached-recipe mixins establish the current machine only around committed output
 * insertion. The runtime path is therefore machine-scoped and persisted in ForgeData;
 * calls outside that commit context retain Mekanism's native preview semantics.</p>
 */
public final class SawmillSequenceController {
    private static final String ROOT_TAG = "DeterministicChanceMekanismSawmill";
    private static final String STATES_TAG = "States";

    private SawmillSequenceController() {
    }

    public static boolean next(SawmillRecipe recipe) {
        ChanceFraction chance = ChanceFraction.fromDouble(recipe.getSecondaryChance());
        BlockEntity machine = MekanismMachineRollContext.activeMachine();
        if (machine == null) {
            throw new IllegalStateException("Mekanism chance roll has no active machine owner");
        }
        if (recipe.getId() == null) {
            throw new IllegalArgumentException("Mekanism sawmill recipe has no id");
        }

        CompoundTag persistentData = machine.getPersistentData();
        CompoundTag root = persistentData.getCompound(ROOT_TAG);
        CompoundTag states = root.getCompound(STATES_TAG);
        String recipeId = recipe.getId().toString();
        CompoundTag state = states.getCompound(recipeId);

        long position = 0;
        if (state.getLong("Numerator") == chance.numerator()
                && state.getLong("Denominator") == chance.denominator()) {
            position = state.getLong("Position");
        }

        DeterministicSequence sequence = new DeterministicSequence(chance, position);
        boolean result = sequence.next();
        if (sequence.position() == 0) {
            states.remove(recipeId);
        } else {
            state.putLong("Numerator", chance.numerator());
            state.putLong("Denominator", chance.denominator());
            state.putLong("Position", sequence.position());
            states.put(recipeId, state);
        }
        if (states.isEmpty()) {
            persistentData.remove(ROOT_TAG);
        } else {
            root.put(STATES_TAG, states);
            persistentData.put(ROOT_TAG, root);
        }
        machine.setChanged();
        return result;
    }
}
