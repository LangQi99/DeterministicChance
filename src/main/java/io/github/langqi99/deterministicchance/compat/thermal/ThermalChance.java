package io.github.langqi99.deterministicchance.compat.thermal;

import io.github.langqi99.deterministicchance.core.ChanceFraction;
import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Thermal represents an output multiplier as one float. Its integer part is
 * guaranteed and its fractional part is the chance of one additional copy.
 * Negative values mean that the chance is locked against machine modifiers;
 * they do not mean a negative output.
 */
public record ThermalChance(int guaranteedCopies, ChanceFraction fractionalChance) {
    private static final int MAX_FRIENDLY_DENOMINATOR = 10_000;

    public ThermalChance {
        if (guaranteedCopies < 0) {
            throw new IllegalArgumentException("guaranteed copies must not be negative");
        }
        if (fractionalChance == null) {
            throw new NullPointerException("fractionalChance");
        }
        if (fractionalChance.isCertain()) {
            throw new IllegalArgumentException("fractional chance must be less than one");
        }
    }

    public static ThermalChance fromRaw(float rawChance) {
        if (!Float.isFinite(rawChance)) {
            throw new IllegalArgumentException("Thermal chance must be finite");
        }

        float chance = Math.abs(rawChance);
        if (chance > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Thermal chance is too large: " + rawChance);
        }

        int guaranteed = (int) Math.floor(chance);
        if (chance == guaranteed) {
            return new ThermalChance(guaranteed, ChanceFraction.NEVER);
        }

        ChanceFraction fraction = findFriendlyFraction(chance, guaranteed);
        if (fraction == null) {
            fraction = exactDecimalFraction(chance, guaranteed);
        }
        return new ThermalChance(guaranteed, fraction);
    }

    /** Number of base output stacks promised by an exact batch. */
    public long copiesInBatch(long executions) {
        if (executions < 0) {
            throw new IllegalArgumentException("executions must not be negative");
        }
        if (executions % fractionalChance.denominator() != 0) {
            throw new IllegalArgumentException("executions must contain complete chance cycles");
        }
        long guaranteed = Math.multiplyExact(executions, guaranteedCopies);
        long probabilistic = Math.multiplyExact(
                executions / fractionalChance.denominator(),
                fractionalChance.numerator());
        return Math.addExact(guaranteed, probabilistic);
    }

    /**
     * Finds the smallest human-authored rational that rounds back to the same
     * complete Thermal float. Testing the complete value is important for
     * values such as {@code 4 / 3F}, where subtracting the integer part first
     * loses another bit of precision.
     */
    private static ChanceFraction findFriendlyFraction(float chance, int guaranteed) {
        double fractional = (double) chance - guaranteed;
        int chanceBits = Float.floatToIntBits(chance);
        for (long denominator = 1; denominator <= MAX_FRIENDLY_DENOMINATOR; denominator++) {
            long numerator = Math.round(fractional * denominator);
            if (numerator <= 0 || numerator >= denominator) {
                continue;
            }
            float reconstructed = (float) (guaranteed + (double) numerator / denominator);
            if (Float.floatToIntBits(reconstructed) == chanceBits) {
                return new ChanceFraction(numerator, denominator);
            }
        }
        return null;
    }

    private static ChanceFraction exactDecimalFraction(float chance, int guaranteed) {
        BigDecimal value = new BigDecimal(Float.toString(chance));
        BigDecimal fractional = value.subtract(BigDecimal.valueOf(guaranteed)).stripTrailingZeros();
        int scale = Math.max(0, fractional.scale());
        BigInteger denominator = BigInteger.TEN.pow(scale);
        BigInteger numerator = fractional.movePointRight(scale).toBigIntegerExact();
        BigInteger gcd = numerator.gcd(denominator);
        try {
            return new ChanceFraction(
                    numerator.divide(gcd).longValueExact(),
                    denominator.divide(gcd).longValueExact());
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException(
                    "Thermal chance cannot be represented by the exact batch planner: " + chance,
                    exception);
        }
    }
}
