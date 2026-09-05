package io.github.langqi99.deterministicchance.compat.gtceu;

import java.util.Collection;
import java.util.List;

/** Pure LCM/minimum-output arithmetic shared by the AE2 pattern adapter. */
public final class GTCEu7ExactBatchMath {
    private GTCEu7ExactBatchMath() {}

    /**
     * Finds a batch length that is a whole number of cycles for every output at
     * every enumerated machine tier.
     */
    public static long exactBatch(
            List<? extends Collection<GTCEu7ExactOrCycle.Fraction>> outputChances,
            long maxExecutions) {
        if (maxExecutions <= 0) {
            throw new IllegalArgumentException("maxExecutions must be positive");
        }

        long executions = 1;
        for (Collection<GTCEu7ExactOrCycle.Fraction> tiers : outputChances) {
            if (tiers.isEmpty()) {
                throw new IllegalArgumentException("every output needs at least one effective chance");
            }
            for (GTCEu7ExactOrCycle.Fraction chance : tiers) {
                executions = lcm(executions, chance.denominator());
                if (executions > maxExecutions) {
                    throw new IllegalArgumentException(
                            "exact batch requires " + executions + " executions; limit is " + maxExecutions);
                }
            }
        }
        return executions;
    }

    /**
     * Returns the conservative output count advertised to AE2. Because the
     * batch is a whole cycle at every tier, a real machine can never produce
     * less than this count; tier-boosted excess is harmless.
     */
    public static long minimumSuccesses(
            long executions,
            Collection<GTCEu7ExactOrCycle.Fraction> effectiveChances) {
        if (executions < 0) {
            throw new IllegalArgumentException("executions must not be negative");
        }
        if (effectiveChances.isEmpty()) {
            throw new IllegalArgumentException("effectiveChances must not be empty");
        }

        long minimum = Long.MAX_VALUE;
        for (GTCEu7ExactOrCycle.Fraction chance : effectiveChances) {
            if (executions % chance.denominator() != 0) {
                throw new IllegalArgumentException("executions must contain whole chance cycles");
            }
            long successes = Math.multiplyExact(
                    executions / chance.denominator(),
                    (long) chance.numerator());
            minimum = Math.min(minimum, successes);
        }
        return minimum;
    }

    private static long lcm(long left, long right) {
        return Math.multiplyExact(left / gcd(left, right), right);
    }

    private static long gcd(long left, long right) {
        while (right != 0) {
            long next = left % right;
            left = right;
            right = next;
        }
        return Math.abs(left);
    }
}
