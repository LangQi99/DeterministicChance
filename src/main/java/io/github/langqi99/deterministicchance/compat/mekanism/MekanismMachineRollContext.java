package io.github.langqi99.deterministicchance.compat.mekanism;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Associates Mekanism's cached-recipe execution with the machine that owns it. */
public final class MekanismMachineRollContext {
    private static final Map<Object, WeakReference<BlockEntity>> OWNERS =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final ThreadLocal<BlockEntity> ACTIVE_MACHINE = new ThreadLocal<>();

    private MekanismMachineRollContext() {
    }

    public static void associate(Object cachedRecipe, BlockEntity owner) {
        if (cachedRecipe != null && owner != null) {
            OWNERS.put(cachedRecipe, new WeakReference<>(owner));
        }
    }

    public static void runWithOwner(Object cachedRecipe, Runnable action) {
        WeakReference<BlockEntity> reference = OWNERS.get(cachedRecipe);
        BlockEntity owner = reference == null ? null : reference.get();
        if (owner == null) {
            action.run();
            return;
        }

        BlockEntity previous = ACTIVE_MACHINE.get();
        ACTIVE_MACHINE.set(owner);
        try {
            action.run();
        } finally {
            if (previous == null) {
                ACTIVE_MACHINE.remove();
            } else {
                ACTIVE_MACHINE.set(previous);
            }
        }
    }

    public static BlockEntity activeMachine() {
        return ACTIVE_MACHINE.get();
    }
}
