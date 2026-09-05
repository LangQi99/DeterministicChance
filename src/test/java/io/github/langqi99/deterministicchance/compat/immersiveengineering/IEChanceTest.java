package io.github.langqi99.deterministicchance.compat.immersiveengineering;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.langqi99.deterministicchance.core.ChanceFraction;
import org.junit.jupiter.api.Test;

class IEChanceTest {
    @Test
    void convertsJsonFloatsToSmallExactFractions() {
        assertEquals(new ChanceFraction(1, 10), IEChance.fromRaw(0.1F));
        assertEquals(new ChanceFraction(1, 3), IEChance.fromRaw(1F / 3F));
        assertEquals(new ChanceFraction(4, 5), IEChance.fromRaw(0.8F));
    }

    @Test
    void followsProbabilityComparisonBoundaries() {
        assertEquals(ChanceFraction.NEVER, IEChance.fromRaw(-1));
        assertEquals(ChanceFraction.NEVER, IEChance.fromRaw(0));
        assertEquals(ChanceFraction.ALWAYS, IEChance.fromRaw(1));
        assertEquals(ChanceFraction.ALWAYS, IEChance.fromRaw(2));
        assertThrows(IllegalArgumentException.class, () -> IEChance.fromRaw(Float.NaN));
    }
}
