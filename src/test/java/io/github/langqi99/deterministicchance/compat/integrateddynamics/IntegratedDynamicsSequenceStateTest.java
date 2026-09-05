package io.github.langqi99.deterministicchance.compat.integrateddynamics;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.langqi99.deterministicchance.core.ChanceFraction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class IntegratedDynamicsSequenceStateTest {
    private static final ResourceLocation RECIPE = ResourceLocation.fromNamespaceAndPath(
            "integrateddynamics", "mechanical_squeezer/ore/redstone");

    @Test
    void cyclesAndRestoresEachOutputLane() {
        IntegratedDynamicsSequenceState original = new IntegratedDynamicsSequenceState();
        ChanceFraction threeQuarters = new ChanceFraction(3, 4);
        assertEquals(true, original.next(RECIPE, 0, threeQuarters));
        assertEquals(true, original.next(RECIPE, 1, threeQuarters));

        CompoundTag saved = new CompoundTag();
        original.save(saved);
        IntegratedDynamicsSequenceState restored = new IntegratedDynamicsSequenceState();
        restored.load(saved);
        assertEquals(2, restored.trackedStateCount());

        assertEquals(true, restored.next(RECIPE, 0, threeQuarters));
        assertEquals(true, restored.next(RECIPE, 0, threeQuarters));
        assertEquals(false, restored.next(RECIPE, 0, threeQuarters));
        assertEquals(1, restored.trackedStateCount());
    }
}
