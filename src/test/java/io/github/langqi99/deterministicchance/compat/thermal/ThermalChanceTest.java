package io.github.langqi99.deterministicchance.compat.thermal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.langqi99.deterministicchance.core.ChanceFraction;
import org.junit.jupiter.api.Test;

class ThermalChanceTest {
    @Test
    void treatsLockedNegativeChanceAsItsAbsoluteMultiplier() {
        assertEquals(
                new ThermalChance(1, new ChanceFraction(1, 4)),
                ThermalChance.fromRaw(-1.25F));
    }

    @Test
    void supportsOrdinaryAndGreaterThanOneChances() {
        assertEquals(new ThermalChance(0, ChanceFraction.NEVER), ThermalChance.fromRaw(0.0F));
        assertEquals(new ThermalChance(1, ChanceFraction.NEVER), ThermalChance.fromRaw(1.0F));
        assertEquals(
                new ThermalChance(0, new ChanceFraction(4, 5)),
                ThermalChance.fromRaw(0.8F));
        ThermalChance oneAndAQuarter = ThermalChance.fromRaw(1.25F);
        assertEquals(new ThermalChance(1, new ChanceFraction(1, 4)), oneAndAQuarter);
        assertEquals(5, oneAndAQuarter.copiesInBatch(4));
    }

    @Test
    void recoversFriendlyFractionFromTheCompleteFloat() {
        assertEquals(
                new ThermalChance(1, new ChanceFraction(1, 3)),
                ThermalChance.fromRaw(4.0F / 3.0F));
    }

    @Test
    void rejectsNonFiniteValuesAndIncompleteBatches() {
        assertThrows(IllegalArgumentException.class, () -> ThermalChance.fromRaw(Float.NaN));
        assertThrows(
                IllegalArgumentException.class,
                () -> ThermalChance.fromRaw(1.25F).copiesInBatch(3));
    }
}
