package io.github.langqi99.deterministicchance.compat.thermal;

import io.github.langqi99.deterministicchance.core.ChanceFraction;
import io.github.langqi99.deterministicchance.core.DeterministicSequence;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/** Per-block-entity deterministic phases for Thermal item output lanes. */
public final class ThermalMachineSequenceState {
    public static final String ROOT_TAG = "DeterministicChanceThermal";
    private static final String LANES_TAG = "States";
    private static final String RECIPE_TAG = "Recipe";
    private static final String LANE_TAG = "Lane";
    private static final String GUARANTEED_TAG = "Guaranteed";
    private static final String NUMERATOR_TAG = "Numerator";
    private static final String DENOMINATOR_TAG = "Denominator";
    private static final String POSITION_TAG = "Position";
    private static final int MAX_PERSISTED_STATES = 256;

    private final Map<StateKey, LaneState> lanes = new HashMap<>();
    private String activeRecipe = "";
    private int nextLane;

    public void beginOutputPass(String recipeKey) {
        if (recipeKey == null || recipeKey.isBlank()) {
            throw new IllegalArgumentException("Thermal recipe key must not be blank");
        }
        activeRecipe = recipeKey;
        nextLane = 0;
    }

    /** Returns the integer output multiplier Thermal should use for this pass. */
    public int nextOutputCopies(float rawChance) {
        if (activeRecipe.isEmpty()) {
            throw new IllegalStateException("beginOutputPass must be called before resolving outputs");
        }
        int lane = nextLane++;
        StateKey key = new StateKey(activeRecipe, lane);
        ThermalChance chance = ThermalChance.fromRaw(rawChance);
        ChanceFraction fractional = chance.fractionalChance();
        if (fractional.isNever()) {
            lanes.remove(key);
            return chance.guaranteedCopies();
        }

        LaneState laneState = lanes.get(key);
        if (laneState == null || !laneState.chance().equals(chance)) {
            laneState = new LaneState(chance, new DeterministicSequence(fractional));
            lanes.put(key, laneState);
        }
        int copies = Math.addExact(
                chance.guaranteedCopies(),
                laneState.sequence().next() ? 1 : 0);
        if (laneState.sequence().position() == 0) {
            // A completed cycle has no cursor to preserve. Removing it keeps
            // machine NBT bounded naturally as recipes are changed over time.
            lanes.remove(key);
        }
        return copies;
    }

    public void load(CompoundTag root) {
        lanes.clear();
        activeRecipe = "";
        nextLane = 0;
        if (!root.contains(ROOT_TAG, Tag.TAG_COMPOUND)) {
            return;
        }

        ListTag serializedLanes = root.getCompound(ROOT_TAG).getList(LANES_TAG, Tag.TAG_COMPOUND);
        int count = Math.min(serializedLanes.size(), MAX_PERSISTED_STATES);
        for (int index = 0; index < count; index++) {
            CompoundTag serialized = serializedLanes.getCompound(index);
            String recipe = serialized.getString(RECIPE_TAG);
            int lane = serialized.getInt(LANE_TAG);
            int guaranteed = serialized.getInt(GUARANTEED_TAG);
            long numerator = serialized.getLong(NUMERATOR_TAG);
            long denominator = serialized.getLong(DENOMINATOR_TAG);
            long position = serialized.getLong(POSITION_TAG);
            if (recipe.isBlank() || lane < 0 || lane >= MAX_PERSISTED_STATES || guaranteed < 0
                    || numerator <= 0 || denominator <= 0 || numerator >= denominator) {
                continue;
            }
            try {
                ChanceFraction fraction = new ChanceFraction(numerator, denominator);
                ThermalChance chance = new ThermalChance(guaranteed, fraction);
                lanes.put(
                        new StateKey(recipe, lane),
                        new LaneState(chance, new DeterministicSequence(fraction, position)));
            } catch (IllegalArgumentException ignored) {
                // Ignore malformed or stale optional-mod state without breaking the chunk.
            }
        }
    }

    public void save(CompoundTag root) {
        if (lanes.isEmpty()) {
            root.remove(ROOT_TAG);
            return;
        }

        ListTag serializedLanes = new ListTag();
        lanes.entrySet().stream()
                .filter(entry -> entry.getKey().lane() >= 0
                        && entry.getKey().lane() < MAX_PERSISTED_STATES)
                .sorted(Map.Entry.comparingByKey())
                .limit(MAX_PERSISTED_STATES)
                .forEach(entry -> {
                    ThermalChance chance = entry.getValue().chance();
                    CompoundTag serialized = new CompoundTag();
                    serialized.putString(RECIPE_TAG, entry.getKey().recipe());
                    serialized.putInt(LANE_TAG, entry.getKey().lane());
                    serialized.putInt(GUARANTEED_TAG, chance.guaranteedCopies());
                    serialized.putLong(NUMERATOR_TAG, chance.fractionalChance().numerator());
                    serialized.putLong(DENOMINATOR_TAG, chance.fractionalChance().denominator());
                    serialized.putLong(POSITION_TAG, entry.getValue().sequence().position());
                    serializedLanes.add(serialized);
                });

        CompoundTag serializedState = new CompoundTag();
        serializedState.put(LANES_TAG, serializedLanes);
        root.put(ROOT_TAG, serializedState);
    }

    int trackedLaneCount() {
        return lanes.size();
    }

    private record StateKey(String recipe, int lane) implements Comparable<StateKey> {
        @Override
        public int compareTo(StateKey other) {
            int byRecipe = recipe.compareTo(other.recipe);
            return byRecipe != 0 ? byRecipe : Integer.compare(lane, other.lane);
        }
    }

    private record LaneState(ThermalChance chance, DeterministicSequence sequence) {
    }
}
