package io.github.langqi99.deterministicchance.core;

import java.math.BigDecimal;
import java.math.BigInteger;

/** A reduced probability fraction in the closed interval [0, 1]. */
public record ChanceFraction(long numerator, long denominator) {
    public static final ChanceFraction NEVER = new ChanceFraction(0, 1);
    public static final ChanceFraction ALWAYS = new ChanceFraction(1, 1);

    public ChanceFraction {
        if (denominator <= 0) {
            throw new IllegalArgumentException("denominator must be positive");
        }
        if (numerator < 0 || numerator > denominator) {
            throw new IllegalArgumentException("chance must be between 0 and 1");
        }

        long gcd = gcd(numerator, denominator);
        numerator /= gcd;
        denominator /= gcd;
    }

    public static ChanceFraction percent(long percent) {
        return new ChanceFraction(percent, 100);
    }

    /** Converts recipe probabilities while recovering simple authored fractions such as 1/3. */
    public static ChanceFraction fromDouble(double chance) {
        if (!Double.isFinite(chance) || chance < 0 || chance > 1) {
            throw new IllegalArgumentException("chance must be finite and between 0 and 1");
        }
        return approximate(chance, 10_000);
    }

    /**
     * Converts a recipe probability stored as a float without exposing its binary
     * representation (for example, {@code 0.8F} remains {@code 4/5}).
     */
    public static ChanceFraction fromFloat(float chance) {
        if (!Float.isFinite(chance) || chance < 0 || chance > 1) {
            throw new IllegalArgumentException("chance must be finite and between 0 and 1");
        }
        return approximate(chance, 10_000);
    }

    /**
     * Recovers the smallest rational that rounds to the same float. This turns
     * addon-authored {@code 1 / 3F} back into {@code 1/3}, while a literal
     * {@code 0.333F} remains {@code 333/1000}.
     */
    public static ChanceFraction approximate(float chance, int maxDenominator) {
        if (!Float.isFinite(chance) || chance < 0 || chance > 1) {
            throw new IllegalArgumentException("chance must be finite and between 0 and 1");
        }
        if (maxDenominator <= 0) {
            throw new IllegalArgumentException("maxDenominator must be positive");
        }
        if (chance == 0) {
            return NEVER;
        }
        if (chance == 1) {
            return ALWAYS;
        }

        double tolerance = Math.ulp(chance) / 2.0;
        for (long denominator = 1; denominator <= maxDenominator; denominator++) {
            long numerator = Math.round((double) chance * denominator);
            if (numerator < 0 || numerator > denominator) {
                continue;
            }
            double candidate = (double) numerator / denominator;
            if (Math.abs(candidate - chance) <= tolerance) {
                return new ChanceFraction(numerator, denominator);
            }
        }
        return fromDecimal(new BigDecimal(Float.toString(chance)));
    }

    /** Double-precision counterpart used by recipe APIs such as Mekanism. */
    public static ChanceFraction approximate(double chance, int maxDenominator) {
        if (!Double.isFinite(chance) || chance < 0 || chance > 1) {
            throw new IllegalArgumentException("chance must be finite and between 0 and 1");
        }
        if (maxDenominator <= 0) {
            throw new IllegalArgumentException("maxDenominator must be positive");
        }
        if (chance == 0) {
            return NEVER;
        }
        if (chance == 1) {
            return ALWAYS;
        }

        double tolerance = Math.ulp(chance) / 2.0;
        for (long denominator = 1; denominator <= maxDenominator; denominator++) {
            long numerator = Math.round(chance * denominator);
            if (numerator < 0 || numerator > denominator) {
                continue;
            }
            double candidate = (double) numerator / denominator;
            if (Math.abs(candidate - chance) <= tolerance) {
                return new ChanceFraction(numerator, denominator);
            }
        }
        return fromDecimal(BigDecimal.valueOf(chance));
    }

    private static ChanceFraction fromDecimal(BigDecimal value) {
        BigDecimal decimal = value.stripTrailingZeros();
        BigInteger denominator = BigInteger.TEN.pow(Math.max(0, decimal.scale()));
        BigInteger numerator = decimal.movePointRight(Math.max(0, decimal.scale())).toBigIntegerExact();
        return new ChanceFraction(numerator.longValueExact(), denominator.longValueExact());
    }

    public boolean isNever() {
        return numerator == 0;
    }

    public boolean isCertain() {
        return numerator == denominator;
    }

    private static long gcd(long a, long b) {
        while (b != 0) {
            long next = a % b;
            a = b;
            b = next;
        }
        return a == 0 ? 1 : Math.abs(a);
    }
}
