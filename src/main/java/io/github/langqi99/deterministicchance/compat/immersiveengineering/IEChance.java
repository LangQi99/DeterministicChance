package io.github.langqi99.deterministicchance.compat.immersiveengineering;

import io.github.langqi99.deterministicchance.core.ChanceFraction;

/** Immersive Engineering's JSON/JEI probability semantics, normalized to [0, 1]. */
public final class IEChance {
    private IEChance() {
    }

    public static ChanceFraction fromRaw(float rawChance) {
        if (!Float.isFinite(rawChance)) {
            throw new IllegalArgumentException("Immersive Engineering chance must be finite");
        }
        if (rawChance <= 0) {
            return ChanceFraction.NEVER;
        }
        if (rawChance >= 1) {
            return ChanceFraction.ALWAYS;
        }
        return ChanceFraction.fromFloat(rawChance);
    }
}
