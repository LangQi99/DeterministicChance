package io.github.langqi99.deterministicchance.compat.productivebees;

import io.github.langqi99.deterministicchance.core.ChanceFraction;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

/** Persistent, independent schedules for Productive Bees centrifuge outputs. */
public final class ProductiveBeesSequenceState {
    public static final String ROOT_TAG = "DeterministicChanceProductiveBees";
    private static final String STATES_TAG = "States";
    private static final int MAX_PERSISTED_STATES = 4096;

    private final Map<Key, LaneState> states = new HashMap<>();

    public Outcome next(ResourceLocation recipeId, int lane, int minimum, int maximum, int percent) {
        Profile profile = Profile.create(minimum, maximum, percent);
        Key key = new Key(recipeId, lane);
        if (profile.chance().isNever()) {
            states.remove(key);
            return new Outcome(false, 0);
        }

        LaneState previous = states.get(key);
        long position = previous != null && previous.profile().equals(profile)
                ? previous.position()
                : 0;
        long successSlots = Math.multiplyExact(profile.chance().numerator(), profile.rangeSize());
        boolean success = position < successSlots;
        int count = success
                ? Math.toIntExact(minimum + position / profile.chance().numerator())
                : 0;

        long nextPosition = (position + 1) % profile.cycleLength();
        if (nextPosition == 0) {
            states.remove(key);
        } else {
            states.put(key, new LaneState(profile, nextPosition));
        }
        return new Outcome(success, count);
    }

    public void load(CompoundTag root) {
        states.clear();
        if (!root.contains(ROOT_TAG, Tag.TAG_COMPOUND)) {
            return;
        }
        ListTag serialized = root.getCompound(ROOT_TAG).getList(STATES_TAG, Tag.TAG_COMPOUND);
        for (int index = 0; index < Math.min(serialized.size(), MAX_PERSISTED_STATES); index++) {
            CompoundTag entry = serialized.getCompound(index);
            ResourceLocation recipeId = ResourceLocation.tryParse(entry.getString("Recipe"));
            int lane = entry.getInt("Lane");
            int minimum = entry.getInt("Minimum");
            int maximum = entry.getInt("Maximum");
            int percent = entry.getInt("Percent");
            long position = entry.getLong("Position");
            try {
                Profile profile = Profile.create(minimum, maximum, percent);
                if (recipeId != null && lane >= 0 && position > 0 && position < profile.cycleLength()) {
                    states.put(new Key(recipeId, lane), new LaneState(profile, position));
                }
            } catch (IllegalArgumentException | ArithmeticException ignored) {
                // Malformed optional-mod state must not prevent the machine from loading.
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
                    tag.putString("Recipe", entry.getKey().recipeId().toString());
                    tag.putInt("Lane", entry.getKey().lane());
                    tag.putInt("Minimum", entry.getValue().profile().minimum());
                    tag.putInt("Maximum", entry.getValue().profile().maximum());
                    tag.putInt("Percent", entry.getValue().profile().percent());
                    tag.putLong("Position", entry.getValue().position());
                    serialized.add(tag);
                });
        CompoundTag state = new CompoundTag();
        state.put(STATES_TAG, serialized);
        root.put(ROOT_TAG, state);
    }

    int trackedStateCount() {
        return states.size();
    }

    public record Outcome(boolean success, int count) {
    }

    public record Profile(
            int minimum,
            int maximum,
            int percent,
            ChanceFraction chance,
            long rangeSize,
            long cycleLength) {
        public static Profile create(int minimum, int maximum, int percent) {
            if (minimum < 0 || maximum < minimum) {
                throw new IllegalArgumentException("centrifuge output range must satisfy 0 <= min <= max");
            }
            if (percent < 0 || percent > 100) {
                throw new IllegalArgumentException("centrifuge chance must be between 0 and 100 percent");
            }
            ChanceFraction chance = ChanceFraction.percent(percent);
            long rangeSize = Math.addExact(Math.subtractExact((long) maximum, minimum), 1);
            long cycleLength = chance.isNever()
                    ? 1
                    : Math.multiplyExact(chance.denominator(), rangeSize);
            return new Profile(minimum, maximum, percent, chance, rangeSize, cycleLength);
        }

        public long totalCountPerCycle() {
            if (chance.isNever()) {
                return 0;
            }
            long rangeSum = Math.multiplyExact(
                    rangeSize,
                    Math.addExact((long) minimum, maximum)) / 2;
            return Math.multiplyExact(chance.numerator(), rangeSum);
        }
    }

    private record Key(ResourceLocation recipeId, int lane) implements Comparable<Key> {
        @Override
        public int compareTo(Key other) {
            int recipeComparison = recipeId.toString().compareTo(other.recipeId.toString());
            return recipeComparison != 0 ? recipeComparison : Integer.compare(lane, other.lane);
        }
    }

    private record LaneState(Profile profile, long position) {
    }
}
