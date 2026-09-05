package io.github.langqi99.deterministicchance.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import org.junit.jupiter.api.Test;

class DeterministicRollRegistryTest {
    @Test
    void keepsIndependentPhasesAndSupportsParallelAttempts() {
        var registry = new DeterministicRollRegistry<String>(new HashMap<>());
        ChanceFraction chance = new ChanceFraction(1, 4);

        assertEquals(1, registry.advance("machine-a", chance, 4));
        assertEquals(1, registry.advance("machine-b", chance, 4));
        assertEquals(2, registry.advance("machine-a", chance, 8));
    }

    @Test
    void resetsAKeyWhenItsRecipeChanceChanges() {
        var registry = new DeterministicRollRegistry<String>(new HashMap<>());
        registry.advance("output", new ChanceFraction(1, 4), 2);
        assertEquals(4, registry.advance("output", new ChanceFraction(4, 5), 5));
    }
}
