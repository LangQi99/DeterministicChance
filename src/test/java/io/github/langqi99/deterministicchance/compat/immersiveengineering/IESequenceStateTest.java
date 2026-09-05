package io.github.langqi99.deterministicchance.compat.immersiveengineering;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.langqi99.deterministicchance.core.ChanceFraction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class IESequenceStateTest {
    private static final ResourceLocation CRUSHER_RECIPE =
            ResourceLocation.fromNamespaceAndPath("immersiveengineering", "crusher/gravel");

    @Test
    void previewsTheCommitResultWithoutAdvancing() {
        IESequenceState state = new IESequenceState();
        ChanceFraction chance = new ChanceFraction(4, 5);
        int successes = 0;
        for (int operation = 0; operation < 10; operation++) {
            boolean firstPreview = state.roll(CRUSHER_RECIPE, 0, chance, false);
            boolean secondPreview = state.roll(CRUSHER_RECIPE, 0, chance, false);
            boolean committed = state.roll(CRUSHER_RECIPE, 0, chance, true);
            assertEquals(firstPreview, secondPreview);
            assertEquals(firstPreview, committed);
            successes += committed ? 1 : 0;
        }
        assertEquals(8, successes);
    }

    @Test
    void savesIndependentRecipeAndOutputLanePositions() {
        IESequenceState original = new IESequenceState();
        ChanceFraction quarter = new ChanceFraction(1, 4);
        ResourceLocation arcRecipe =
                ResourceLocation.fromNamespaceAndPath("immersiveengineering", "arcfurnace/raw_ore_iron");

        assertEquals(true, original.roll(CRUSHER_RECIPE, 0, quarter, true));
        assertEquals(true, original.roll(CRUSHER_RECIPE, 1, quarter, true));
        assertEquals(true, original.roll(arcRecipe, 0, quarter, true));

        CompoundTag saved = new CompoundTag();
        original.save(saved);
        IESequenceState restored = new IESequenceState();
        restored.load(saved);

        assertEquals(3, restored.trackedStateCount());
        assertEquals(false, restored.roll(CRUSHER_RECIPE, 0, quarter, false));
        assertEquals(false, restored.roll(CRUSHER_RECIPE, 1, quarter, true));
        assertEquals(false, restored.roll(arcRecipe, 0, quarter, true));
    }

    @Test
    void recipeChanceChangesRestartOnlyThatLane() {
        IESequenceState state = new IESequenceState();
        state.roll(CRUSHER_RECIPE, 0, new ChanceFraction(1, 4), true);

        assertEquals(
                true,
                state.roll(CRUSHER_RECIPE, 0, new ChanceFraction(4, 5), false));
        assertEquals(
                true,
                state.roll(CRUSHER_RECIPE, 0, new ChanceFraction(4, 5), true));
    }

    @Test
    void dropsCompletedCyclesFromPersistentState() {
        IESequenceState state = new IESequenceState();
        ChanceFraction chance = new ChanceFraction(4, 5);
        for (int operation = 0; operation < 5; operation++) {
            state.roll(CRUSHER_RECIPE, 0, chance, true);
        }
        assertEquals(0, state.trackedStateCount());

        CompoundTag saved = new CompoundTag();
        state.save(saved);
        assertEquals(false, saved.contains(IESequenceState.ROOT_TAG));
    }
}
