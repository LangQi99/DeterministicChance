package io.github.langqi99.deterministicchance.compat.mekanism;

import io.github.langqi99.deterministicchance.core.ChanceFraction;
import io.github.langqi99.deterministicchance.core.DeterministicSequence;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import mekanism.api.recipes.SawmillRecipe;

/**
 * Owns the deterministic phase for each loaded sawmill recipe.
 *
 * <p>Mekanism creates one recipe object per recipe id and calls the chance-output methods only
 * when an operation is actually committed, so this does not consume the sequence during output
 * space simulation. A later persistence layer will move the phase from recipe scope to machine
 * scope; recipe scope is intentionally sufficient for the single-machine integration world.</p>
 */
public final class SawmillSequenceController {
    private static final Map<SawmillRecipe, DeterministicSequence> SEQUENCES =
            Collections.synchronizedMap(new WeakHashMap<>());

    private SawmillSequenceController() {
    }

    public static boolean next(SawmillRecipe recipe) {
        double chance = recipe.getSecondaryChance();
        if (chance <= 0) {
            return false;
        }
        if (chance >= 1) {
            return true;
        }
        DeterministicSequence sequence = SEQUENCES.computeIfAbsent(
                recipe, ignored -> new DeterministicSequence(ChanceFraction.fromDouble(chance)));
        return sequence.next();
    }

    static void resetForTests() {
        SEQUENCES.clear();
    }
}
