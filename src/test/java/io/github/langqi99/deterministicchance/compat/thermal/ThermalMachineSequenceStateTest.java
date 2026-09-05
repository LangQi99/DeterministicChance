package io.github.langqi99.deterministicchance.compat.thermal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

class ThermalMachineSequenceStateTest {
    @Test
    void keepsIndependentOutputLanesAndPreservesGreaterThanOneCounts() {
        ThermalMachineSequenceState state = new ThermalMachineSequenceState();
        int firstLaneTotal = 0;
        int secondLaneTotal = 0;

        for (int operation = 0; operation < 4; operation++) {
            state.beginOutputPass("recipe-a");
            firstLaneTotal += state.nextOutputCopies(1.25F);
            secondLaneTotal += state.nextOutputCopies(0.5F);
        }

        assertEquals(5, firstLaneTotal);
        assertEquals(2, secondLaneTotal);
        assertEquals(0, state.trackedLaneCount());
    }

    @Test
    void savesAndRestoresTheNextRollPosition() {
        ThermalMachineSequenceState original = new ThermalMachineSequenceState();
        original.beginOutputPass("recipe-a");
        assertEquals(1, original.nextOutputCopies(0.25F));

        CompoundTag serialized = new CompoundTag();
        original.save(serialized);

        ThermalMachineSequenceState restored = new ThermalMachineSequenceState();
        restored.load(serialized);
        for (int operation = 0; operation < 3; operation++) {
            restored.beginOutputPass("recipe-a");
            assertEquals(0, restored.nextOutputCopies(0.25F));
        }
        restored.beginOutputPass("recipe-a");
        assertEquals(1, restored.nextOutputCopies(0.25F));
    }

    @Test
    void resetsOneLaneWhenItsEffectiveMachineChanceChanges() {
        ThermalMachineSequenceState state = new ThermalMachineSequenceState();
        state.beginOutputPass("recipe-a");
        state.nextOutputCopies(0.25F);

        state.beginOutputPass("recipe-a");
        assertEquals(1, state.nextOutputCopies(0.8F));
    }

    @Test
    void switchingRecipesDoesNotResetOrShareTheirPartialCycles() {
        ThermalMachineSequenceState state = new ThermalMachineSequenceState();

        state.beginOutputPass("recipe-a");
        assertEquals(1, state.nextOutputCopies(0.25F));
        state.beginOutputPass("recipe-b");
        assertEquals(1, state.nextOutputCopies(0.25F));

        state.beginOutputPass("recipe-a");
        assertEquals(0, state.nextOutputCopies(0.25F));
        state.beginOutputPass("recipe-b");
        assertEquals(0, state.nextOutputCopies(0.25F));
        assertEquals(2, state.trackedLaneCount());
    }
}
