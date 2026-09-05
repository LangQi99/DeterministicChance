package io.github.langqi99.deterministicchance.compat.create;

import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Scopes Create output rolls to the machine that is committing the recipe. */
public final class CreateMachineRollContext {
    private static final ThreadLocal<BlockEntity> ACTIVE_MACHINE = new ThreadLocal<>();

    private CreateMachineRollContext() {}

    public static <T> T withMachine(BlockEntity machine, Supplier<T> action) {
        Objects.requireNonNull(machine, "machine");
        Objects.requireNonNull(action, "action");
        BlockEntity previous = ACTIVE_MACHINE.get();
        ACTIVE_MACHINE.set(machine);
        try {
            return action.get();
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
