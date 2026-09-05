package io.github.langqi99.deterministicchance.compat.create;

import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import io.github.langqi99.deterministicchance.core.ChanceFraction;
import io.github.langqi99.deterministicchance.core.DeterministicSequence;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Persistent, machine-scoped replacement for Create's {@link ProcessingOutput} roll. */
public final class ProcessingOutputSequenceController {
    public static final String ROOT_TAG = "DeterministicChanceCreate";
    private static final String STATES_TAG = "States";
    private static final int MAX_PERSISTED_STATES = 256;

    private ProcessingOutputSequenceController() {
    }

    /** Create performs one independent roll for every item in the output stack. */
    public static int nextOutputCount(
            BlockEntity machine,
            ProcessingRecipe<?> recipe,
            List<ProcessingOutput> activeOutputs,
            ProcessingOutput output) {
        if (recipe.getId() == null) {
            return output.rollOutput().getCount();
        }
        int outputIndex = recipe.getRollableResults().indexOf(output);
        if (outputIndex < 0) {
            outputIndex = activeOutputs.indexOf(output);
        }
        if (outputIndex < 0) {
            return output.rollOutput().getCount();
        }

        CompoundTag persistentData = machine.getPersistentData();
        CompoundTag root = persistentData.getCompound(ROOT_TAG);
        ListTag states = root.getList(STATES_TAG, Tag.TAG_COMPOUND);
        String recipeId = recipe.getId().toString();
        int stateIndex = find(states, recipeId, outputIndex);
        CompoundTag state = stateIndex < 0 ? null : states.getCompound(stateIndex);

        ChanceFraction chance = ChanceFraction.fromFloat(output.getChance());
        if (chance.isCertain() || chance.isNever()) {
            if (stateIndex >= 0) {
                states.remove(stateIndex);
                save(machine, persistentData, root, states);
            }
            return chance.isCertain() ? output.getStack().getCount() : 0;
        }

        long position = 0;
        if (state != null
                && state.getLong("Numerator") == chance.numerator()
                && state.getLong("Denominator") == chance.denominator()) {
            position = state.getLong("Position");
        }

        DeterministicSequence sequence = new DeterministicSequence(chance, position);
        long successes = sequence.advance(output.getStack().getCount());
        if (sequence.position() == 0) {
            if (stateIndex >= 0) {
                states.remove(stateIndex);
            }
        } else {
            if (state == null) {
                if (states.size() >= MAX_PERSISTED_STATES) {
                    states.remove(0);
                }
                state = new CompoundTag();
                state.putString("Recipe", recipeId);
                state.putInt("Output", outputIndex);
                states.add(state);
            }
            state.putLong("Numerator", chance.numerator());
            state.putLong("Denominator", chance.denominator());
            state.putLong("Position", sequence.position());
        }

        save(machine, persistentData, root, states);
        return Math.toIntExact(successes);
    }

    private static void save(
            BlockEntity machine,
            CompoundTag persistentData,
            CompoundTag root,
            ListTag states) {
        if (states.isEmpty()) {
            persistentData.remove(ROOT_TAG);
        } else {
            root.put(STATES_TAG, states);
            persistentData.put(ROOT_TAG, root);
        }
        machine.setChanged();
    }

    private static int find(ListTag states, String recipeId, int outputIndex) {
        for (int index = 0; index < states.size(); index++) {
            CompoundTag state = states.getCompound(index);
            if (state.getInt("Output") == outputIndex
                    && state.getString("Recipe").equals(recipeId)) {
                return index;
            }
        }
        return -1;
    }
}
