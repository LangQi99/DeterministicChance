package io.github.langqi99.deterministicchance.compat.create;

import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import io.github.langqi99.deterministicchance.compat.jei.JeiBatchPlanner;
import io.github.langqi99.deterministicchance.core.WeightedChoiceCycle;
import java.util.List;
import java.util.Random;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Persistent exact one-of-many result selection for Create sequenced assembly. */
public final class SequencedAssemblySequenceController {
    public static final String ROOT_TAG = "DeterministicChanceCreateAssembly";
    private static final String STATES_TAG = "States";
    private static final int MAX_PERSISTED_STATES = 256;

    private SequencedAssemblySequenceController() {}

    public static float nextRoll(
            BlockEntity machine,
            SequencedAssemblyRecipe recipe,
            List<ProcessingOutput> pool,
            Random fallback) {
        if (recipe.getId() == null || machine.getLevel() == null || machine.getLevel().isClientSide) {
            return fallback.nextFloat();
        }
        final WeightedChoiceCycle cycle;
        try {
            cycle = cycle(pool);
        } catch (IllegalArgumentException | ArithmeticException exception) {
            return fallback.nextFloat();
        }

        String recipeId = recipe.getId().toString();
        String signature = signature(pool, cycle);
        CompoundTag persistent = machine.getPersistentData();
        CompoundTag root = persistent.getCompound(ROOT_TAG);
        ListTag states = root.getList(STATES_TAG, Tag.TAG_COMPOUND);
        int stateIndex = find(states, recipeId);
        CompoundTag state = stateIndex < 0 ? null : states.getCompound(stateIndex);
        long position = state != null && signature.equals(state.getString("Signature"))
                ? Math.floorMod(state.getLong("Position"), cycle.totalWeight())
                : 0;
        int choice = cycle.choice(position);
        long next = (position + 1) % cycle.totalWeight();

        if (next == 0) {
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
                states.add(state);
            }
            state.putString("Signature", signature);
            state.putLong("Position", next);
        }
        save(machine, persistent, root, states);

        long before = 0;
        for (int index = 0; index < choice; index++) {
            before += cycle.weight(index);
        }
        return (float) ((before + cycle.weight(choice) / 2.0D) / cycle.totalWeight());
    }

    public static WeightedChoiceCycle cycle(List<ProcessingOutput> pool) {
        return WeightedChoiceCycle.fromFloatWeights(
                pool.stream().map(ProcessingOutput::getChance).toList(),
                JeiBatchPlanner.DEFAULT_MAX_EXECUTIONS);
    }

    private static String signature(List<ProcessingOutput> pool, WeightedChoiceCycle cycle) {
        StringBuilder result = new StringBuilder();
        for (int index = 0; index < pool.size(); index++) {
            ProcessingOutput output = pool.get(index);
            result.append(BuiltInRegistries.ITEM.getKey(output.getStack().getItem()))
                    .append('#').append(output.getStack().getCount())
                    .append('#').append(output.getStack().getTag())
                    .append('@').append(cycle.weight(index)).append(';');
        }
        return result.toString();
    }

    private static int find(ListTag states, String recipeId) {
        for (int index = 0; index < states.size(); index++) {
            if (recipeId.equals(states.getCompound(index).getString("Recipe"))) {
                return index;
            }
        }
        return -1;
    }

    private static void save(
            BlockEntity machine,
            CompoundTag persistent,
            CompoundTag root,
            ListTag states) {
        if (states.isEmpty()) {
            persistent.remove(ROOT_TAG);
        } else {
            root.put(STATES_TAG, states);
            persistent.put(ROOT_TAG, root);
        }
        machine.setChanged();
    }
}
