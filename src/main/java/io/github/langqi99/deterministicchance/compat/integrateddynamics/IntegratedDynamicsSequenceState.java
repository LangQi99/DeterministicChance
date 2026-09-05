package io.github.langqi99.deterministicchance.compat.integrateddynamics;

import io.github.langqi99.deterministicchance.core.ChanceFraction;
import io.github.langqi99.deterministicchance.core.DeterministicSequence;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

/** Per-mechanical-squeezer, per-recipe, per-output persisted chance cursors. */
public final class IntegratedDynamicsSequenceState {
    public static final String ROOT_TAG = "DeterministicChanceIntegratedDynamics";
    private static final int MAX_PERSISTED_STATES = 4096;
    private final Map<Key, LaneState> states = new HashMap<>();

    public boolean next(ResourceLocation recipeId, int lane, ChanceFraction chance) {
        Key key = new Key(recipeId, lane);
        if (chance.isNever()) {
            states.remove(key);
            return false;
        }
        if (chance.isCertain()) {
            states.remove(key);
            return true;
        }
        LaneState previous = states.get(key);
        long position = previous != null && previous.chance().equals(chance)
                ? previous.sequence().position()
                : 0;
        DeterministicSequence sequence = new DeterministicSequence(chance, position);
        boolean result = sequence.next();
        if (sequence.position() == 0) {
            states.remove(key);
        } else {
            states.put(key, new LaneState(chance, sequence));
        }
        return result;
    }

    public void load(CompoundTag root) {
        states.clear();
        if (!root.contains(ROOT_TAG, Tag.TAG_COMPOUND)) {
            return;
        }
        ListTag list = root.getCompound(ROOT_TAG).getList("States", Tag.TAG_COMPOUND);
        for (int index = 0; index < Math.min(list.size(), MAX_PERSISTED_STATES); index++) {
            CompoundTag entry = list.getCompound(index);
            ResourceLocation recipeId = ResourceLocation.tryParse(entry.getString("Recipe"));
            int lane = entry.getInt("Lane");
            long numerator = entry.getLong("Numerator");
            long denominator = entry.getLong("Denominator");
            long position = entry.getLong("Position");
            try {
                ChanceFraction chance = new ChanceFraction(numerator, denominator);
                if (recipeId != null && lane >= 0 && !chance.isNever() && !chance.isCertain()) {
                    states.put(
                            new Key(recipeId, lane),
                            new LaneState(chance, new DeterministicSequence(chance, position)));
                }
            } catch (IllegalArgumentException ignored) {
                // Ignore malformed optional compatibility state.
            }
        }
    }

    public void save(CompoundTag root) {
        if (states.isEmpty()) {
            root.remove(ROOT_TAG);
            return;
        }
        ListTag list = new ListTag();
        states.entrySet().stream()
                .sorted(Map.Entry.<Key, LaneState>comparingByKey())
                .limit(MAX_PERSISTED_STATES)
                .forEach(entry -> {
                    CompoundTag tag = new CompoundTag();
                    tag.putString("Recipe", entry.getKey().recipeId().toString());
                    tag.putInt("Lane", entry.getKey().lane());
                    tag.putLong("Numerator", entry.getValue().chance().numerator());
                    tag.putLong("Denominator", entry.getValue().chance().denominator());
                    tag.putLong("Position", entry.getValue().sequence().position());
                    list.add(tag);
                });
        CompoundTag state = new CompoundTag();
        state.put("States", list);
        root.put(ROOT_TAG, state);
    }

    int trackedStateCount() {
        return states.size();
    }

    private record Key(ResourceLocation recipeId, int lane) implements Comparable<Key> {
        @Override
        public int compareTo(Key other) {
            int recipeComparison = recipeId.toString().compareTo(other.recipeId.toString());
            return recipeComparison != 0 ? recipeComparison : Integer.compare(lane, other.lane);
        }
    }

    private record LaneState(ChanceFraction chance, DeterministicSequence sequence) {
    }
}
