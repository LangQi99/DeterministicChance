package io.github.langqi99.deterministicchance.compat.gtceu;

/**
 * Pure arithmetic for GTCEu 7's independent (OR) chance outputs.
 *
 * <p>The fraction is reduced before the cursor is interpreted. For example,
 * {@code 8000 / 10000} is a five-step cycle with four successes, rather than a
 * ten-thousand-step cycle. The implementation is deliberately independent of
 * Minecraft and GTCEu so it can be unit-tested without launching Forge.</p>
 */
public final class GTCEu7ExactOrCycle {
    private GTCEu7ExactOrCycle() {}

    public record Fraction(int numerator, int denominator) {
        public Fraction {
            if (denominator <= 0) {
                throw new IllegalArgumentException("denominator must be positive");
            }
            if (numerator < 0 || numerator > denominator) {
                throw new IllegalArgumentException("chance must be in [0, denominator]");
            }
            int gcd = gcd(numerator, denominator);
            numerator /= gcd;
            denominator /= gcd;
        }
    }

    public record Advance(int successes, int nextPosition) {}

    public static Fraction fraction(int chance, int maxChance) {
        return new Fraction(chance, maxChance);
    }

    /**
     * Advances a cyclic schedule by {@code attempts} without iterating once per
     * attempt. Positions {@code [0, numerator)} are successes.
     */
    public static Advance advance(Fraction chance, int position, int attempts) {
        if (attempts < 0) {
            throw new IllegalArgumentException("attempts must not be negative");
        }

        int denominator = chance.denominator();
        int numerator = chance.numerator();
        int normalizedPosition = Math.floorMod(position, denominator);

        long fullCycles = attempts / (long) denominator;
        int remainder = attempts % denominator;
        long successes = Math.multiplyExact(fullCycles, (long) numerator);

        int beforeWrap = Math.min(remainder, denominator - normalizedPosition);
        successes += overlapWithSuccessPrefix(normalizedPosition, beforeWrap, numerator);

        int afterWrap = remainder - beforeWrap;
        successes += Math.min(afterWrap, numerator);

        int nextPosition = (int) ((normalizedPosition + (long) attempts) % denominator);
        return new Advance(Math.toIntExact(successes), nextPosition);
    }

    private static int overlapWithSuccessPrefix(int start, int length, int prefixLength) {
        int end = start + length;
        return Math.max(0, Math.min(end, prefixLength) - start);
    }

    private static int gcd(int a, int b) {
        while (b != 0) {
            int next = a % b;
            a = b;
            b = next;
        }
        return a == 0 ? 1 : Math.abs(a);
    }
}
