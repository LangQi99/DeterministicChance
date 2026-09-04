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

    /** Converts the finite decimal probabilities used by recipe JSON into a reduced fraction. */
    public static ChanceFraction fromDouble(double chance) {
        if (!Double.isFinite(chance) || chance < 0 || chance > 1) {
            throw new IllegalArgumentException("chance must be finite and between 0 and 1");
        }
        BigDecimal decimal = BigDecimal.valueOf(chance).stripTrailingZeros();
        BigInteger denominator = BigInteger.TEN.pow(Math.max(0, decimal.scale()));
        BigInteger numerator = decimal.movePointRight(Math.max(0, decimal.scale())).toBigIntegerExact();
        return new ChanceFraction(numerator.longValueExact(), denominator.longValueExact());
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
