package io.github.langqi99.deterministicchance.core;

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
