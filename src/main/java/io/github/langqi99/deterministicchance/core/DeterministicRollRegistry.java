package io.github.langqi99.deterministicchance.core;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

/**
 * Shared phase storage for adapters whose native chance object is stable while a
 * recipe is loaded. A changed probability starts a new sequence for that key.
 */
public final class DeterministicRollRegistry<K> {
    private final Map<K, DeterministicSequence> sequences;

    public DeterministicRollRegistry(Map<K, DeterministicSequence> sequences) {
        this.sequences = Objects.requireNonNull(sequences, "sequences");
    }

    public static <K> DeterministicRollRegistry<K> weakKeys() {
        return new DeterministicRollRegistry<>(Collections.synchronizedMap(new WeakHashMap<>()));
    }

    public boolean next(K key, ChanceFraction chance) {
        return advance(key, chance, 1) == 1;
    }

    public long advance(K key, ChanceFraction chance, long attempts) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(chance, "chance");
        if (attempts < 0) {
            throw new IllegalArgumentException("attempts must not be negative");
        }
        synchronized (sequences) {
            DeterministicSequence sequence = sequences.get(key);
            if (sequence == null || !sequence.chance().equals(chance)) {
                sequence = new DeterministicSequence(chance);
                sequences.put(key, sequence);
            }
            return sequence.advance(attempts);
        }
    }

    public void clear() {
        sequences.clear();
    }
}
