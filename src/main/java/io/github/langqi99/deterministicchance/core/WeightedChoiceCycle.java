package io.github.langqi99.deterministicchance.core;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/** Exact finite cycle for one-of-many weighted outcomes. */
public final class WeightedChoiceCycle {
    private final List<Long> weights;
    private final long totalWeight;
    private final long stride;

    private WeightedChoiceCycle(List<Long> weights, long totalWeight) {
        this.weights = weights;
        this.totalWeight = totalWeight;
        this.stride = coprimeStride(totalWeight);
    }

    public static WeightedChoiceCycle fromFloatWeights(List<Float> source, long maxCycle) {
        if (source.isEmpty()) {
            throw new IllegalArgumentException("weighted outcome pool must not be empty");
        }
        List<BigDecimal> decimals = source.stream().map(weight -> {
            if (weight == null || !Float.isFinite(weight) || weight < 0) {
                throw new IllegalArgumentException("outcome weights must be finite and non-negative");
            }
            return new BigDecimal(Float.toString(weight)).stripTrailingZeros();
        }).toList();
        int scale = decimals.stream().mapToInt(BigDecimal::scale).map(value -> Math.max(0, value)).max().orElse(0);
        List<BigInteger> integers = decimals.stream()
                .map(value -> value.movePointRight(scale).toBigIntegerExact())
                .toList();
        BigInteger gcd = integers.stream()
                .filter(value -> value.signum() != 0)
                .reduce(BigInteger.ZERO, BigInteger::gcd);
        if (gcd.signum() == 0) {
            throw new IllegalArgumentException("weighted outcome pool has no positive result");
        }
        List<Long> weights = new ArrayList<>(integers.size());
        long total = 0;
        for (BigInteger integer : integers) {
            long weight = integer.divide(gcd).longValueExact();
            weights.add(weight);
            total = Math.addExact(total, weight);
        }
        if (total > maxCycle) {
            throw new IllegalArgumentException(
                    "exact weighted cycle requires " + total + " executions; limit is " + maxCycle);
        }
        return new WeightedChoiceCycle(List.copyOf(weights), total);
    }

    public int choice(long position) {
        long ticket = Math.floorMod(Math.multiplyExact(Math.floorMod(position, totalWeight), stride), totalWeight);
        long cumulative = 0;
        for (int index = 0; index < weights.size(); index++) {
            cumulative += weights.get(index);
            if (ticket < cumulative) {
                return index;
            }
        }
        throw new IllegalStateException("weighted choice fell outside its cycle");
    }

    public long weight(int index) {
        return weights.get(index);
    }

    public long totalWeight() {
        return totalWeight;
    }

    public ChanceFraction chance(int index) {
        return new ChanceFraction(weight(index), totalWeight);
    }

    private static long coprimeStride(long total) {
        if (total == 1) {
            return 1;
        }
        for (long candidate = total / 2 + 1; candidate < total; candidate++) {
            if (gcd(candidate, total) == 1) {
                return candidate;
            }
        }
        return 1;
    }

    private static long gcd(long a, long b) {
        while (b != 0) {
            long next = a % b;
            a = b;
            b = next;
        }
        return Math.abs(a);
    }
}
