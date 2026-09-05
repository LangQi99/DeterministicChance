package io.github.langqi99.deterministicchance.compat.gtceu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class GTCEu7ExactBatchMathTest {
    @Test
    void independentOutputsUseTheirLeastCommonWholeCycle() {
        var eightyPercent = List.of(GTCEu7ExactOrCycle.fraction(80, 100));
        var thirtyPercent = List.of(GTCEu7ExactOrCycle.fraction(30, 100));

        long batch = GTCEu7ExactBatchMath.exactBatch(
                List.of(eightyPercent, thirtyPercent),
                10_000);

        assertEquals(10, batch);
        assertEquals(8, GTCEu7ExactBatchMath.minimumSuccesses(batch, eightyPercent));
        assertEquals(3, GTCEu7ExactBatchMath.minimumSuccesses(batch, thirtyPercent));
    }

    @Test
    void tierBoostedPatternAdvertisesTheMinimumButCoversEveryTierCycle() {
        var tierChances = List.of(
                GTCEu7ExactOrCycle.fraction(20, 100),
                GTCEu7ExactOrCycle.fraction(40, 100),
                GTCEu7ExactOrCycle.fraction(50, 100));

        long batch = GTCEu7ExactBatchMath.exactBatch(List.of(tierChances), 10_000);

        assertEquals(10, batch);
        assertEquals(2, GTCEu7ExactBatchMath.minimumSuccesses(batch, tierChances));
    }

    @Test
    void rejectsAPlanBeyondTheConfiguredSafetyLimit() {
        var first = List.of(GTCEu7ExactOrCycle.fraction(1, 97));
        var second = List.of(GTCEu7ExactOrCycle.fraction(1, 89));
        assertThrows(IllegalArgumentException.class,
                () -> GTCEu7ExactBatchMath.exactBatch(List.of(first, second), 1_000));
    }

    @Test
    void refusesToReportCountsForAPartialCycle() {
        assertThrows(IllegalArgumentException.class, () ->
                GTCEu7ExactBatchMath.minimumSuccesses(
                        4,
                        List.of(GTCEu7ExactOrCycle.fraction(4, 5))));
    }
}
