package io.github.langqi99.deterministicchance.compat.immersiveengineering;

import io.github.langqi99.deterministicchance.core.ChanceFraction;
import io.github.langqi99.deterministicchance.core.DeterministicSequence;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

/** Per-multiblock, per-recipe, per-output deterministic cursor storage. */
public final class IESequenceState {
    public static final String ROOT_TAG = "DeterministicChanceIE";
    private static final String STATES_TAG = "States";
    private static final String RECIPE_TAG = "Recipe";
    private static final String LANE_TAG = "Lane";
    private static final String NUMERATOR_TAG = "Numerator";
    private static final String DENOMINATOR_TAG = "Denominator";
    private static final String POSITION_TAG = "Position";
    private static final int MAX_PERSISTED_STATES = 4096;

    private final Map<Key, LaneState> states = new HashMap<>();

    /**
     * Returns the next result. A preview observes the same result as a later
     * commit without creating state or consuming the cursor.
     */
    public boolean roll(
            ResourceLocation recipeId,
            int lane,
            ChanceFraction chance,
            boolean commit) {
        if (lane < 0) {
            throw new IllegalArgumentException("lane must not be negative");
        }
        Key key = new Key(recipeId, lane);
        if (chance.isNever()) {
            if (commit) {
                states.remove(key);
            }
            return false;
        }
        if (chance.isCertain()) {
            if (commit) {
                states.remove(key);
            }
            return true;
        }

        LaneState previous = states.get(key);
        long position = previous != null && previous.chance().equals(chance)
                ? previous.sequence().position()
                : 0;
        boolean success = position < chance.numerator();
        if (commit) {
            DeterministicSequence sequence = new DeterministicSequence(chance, position);
            sequence.next();
            if (sequence.position() == 0) {
                states.remove(key);
            } else {
                states.put(key, new LaneState(chance, sequence));
            }
        }
        return success;
    }

    public void load(CompoundTag root) {
        states.clear();
        if (!root.contains(ROOT_TAG, Tag.TAG_COMPOUND)) {
            return;
        }
        ListTag serialized = root.getCompound(ROOT_TAG).getList(STATES_TAG, Tag.TAG_COMPOUND);
        int count = Math.min(serialized.size(), MAX_PERSISTED_STATES);
        for (int index = 0; index < count; index++) {
            CompoundTag entry = serialized.getCompound(index);
            ResourceLocation recipeId = ResourceLocation.tryParse(entry.getString(RECIPE_TAG));
            int lane = entry.getInt(LANE_TAG);
            long numerator = entry.getLong(NUMERATOR_TAG);
            long denominator = entry.getLong(DENOMINATOR_TAG);
            long position = entry.getLong(POSITION_TAG);
            if (recipeId == null || lane < 0 || numerator <= 0 || denominator <= 0
                    || numerator >= denominator) {
                continue;
            }
            try {
                ChanceFraction chance = new ChanceFraction(numerator, denominator);
                states.put(
                        new Key(recipeId, lane),
                        new LaneState(chance, new DeterministicSequence(chance, position)));
            } catch (IllegalArgumentException ignored) {
                // Ignore malformed optional-mod state rather than breaking the multiblock.
            }
        }
    }

    public void save(CompoundTag root) {
        if (states.isEmpty()) {
            root.remove(ROOT_TAG);
            return;
        }

        ListTag serialized = new ListTag();
        states.entrySet().stream()
                .sorted(Map.Entry.<Key, LaneState>comparingByKey())
                .limit(MAX_PERSISTED_STATES)
                .forEach(entry -> {
                    CompoundTag tag = new CompoundTag();
                    tag.putString(RECIPE_TAG, entry.getKey().recipeId().toString());
                    tag.putInt(LANE_TAG, entry.getKey().lane());
                    tag.putLong(NUMERATOR_TAG, entry.getValue().chance().numerator());
                    tag.putLong(DENOMINATOR_TAG, entry.getValue().chance().denominator());
                    tag.putLong(POSITION_TAG, entry.getValue().sequence().position());
                    serialized.add(tag);
                });
        CompoundTag state = new CompoundTag();
        state.put(STATES_TAG, serialized);
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
