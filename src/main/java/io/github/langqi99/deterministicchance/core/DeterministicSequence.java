package io.github.langqi99.deterministicchance.core;

/**
 * Replaces independent random rolls with a repeatable cycle that yields exactly
 * numerator successes in every denominator attempts.
 */
public final class DeterministicSequence {
    private final ChanceFraction chance;
    private long position;

    public DeterministicSequence(ChanceFraction chance) {
        this(chance, 0);
    }

    public DeterministicSequence(ChanceFraction chance, long position) {
        this.chance = chance;
        setPosition(position);
    }

    public boolean next() {
        boolean success = position < chance.numerator();
        position = (position + 1) % chance.denominator();
        return success;
    }

    public long advance(long attempts) {
        if (attempts < 0) {
            throw new IllegalArgumentException("attempts must not be negative");
        }
        if (attempts == 0 || chance.numerator() == 0) {
            return 0;
        }

        long fullCycles = attempts / chance.denominator();
        long remainderAttempts = attempts % chance.denominator();
        long successes = Math.multiplyExact(fullCycles, chance.numerator());

        for (long i = 0; i < remainderAttempts; i++) {
            if (next()) {
                successes++;
            }
        }

        if (fullCycles > 0) {
            // Whole cycles do not change the phase.
            position %= chance.denominator();
        }
        return successes;
    }

    public ChanceFraction chance() {
        return chance;
    }

    public long position() {
        return position;
    }

    public void setPosition(long position) {
        this.position = Math.floorMod(position, chance.denominator());
    }
}
