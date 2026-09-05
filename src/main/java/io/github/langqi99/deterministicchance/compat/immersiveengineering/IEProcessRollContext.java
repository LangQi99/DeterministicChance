package io.github.langqi99.deterministicchance.compat.immersiveengineering;

import io.github.langqi99.deterministicchance.core.ChanceFraction;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;

/** Thread-local bridge from IE's central process preview/commit calls to recipe output rolls. */
public final class IEProcessRollContext {
    private static final ThreadLocal<Deque<Frame>> FRAMES = ThreadLocal.withInitial(ArrayDeque::new);

    private IEProcessRollContext() {
    }

    public static void begin(Object owner, ResourceLocation recipeId, boolean commit) {
        IESequenceState state = owner instanceof IESequenceStateAccess access
                ? access.deterministicChance$ieState()
                : null;
        FRAMES.get().push(new Frame(state, recipeId, commit));
    }

    public static void end() {
        Deque<Frame> frames = FRAMES.get();
        if (!frames.isEmpty()) {
            frames.pop();
        }
        if (frames.isEmpty()) {
            FRAMES.remove();
        }
    }

    public static <T> T call(
            Object owner,
            ResourceLocation recipeId,
            boolean commit,
            Supplier<T> operation) {
        begin(owner, recipeId, commit);
        try {
            return operation.get();
        } finally {
            end();
        }
    }

    public static boolean isActiveFor(ResourceLocation recipeId) {
        Frame frame = current();
        return frame != null && frame.state() != null && frame.recipeId().equals(recipeId);
    }

    public static boolean roll(ResourceLocation recipeId, int lane, float rawChance) {
        Frame frame = current();
        if (frame == null || frame.state() == null || !frame.recipeId().equals(recipeId)) {
            throw new IllegalStateException("No active Immersive Engineering process roll context");
        }
        ChanceFraction chance = IEChance.fromRaw(rawChance);
        return frame.state().roll(recipeId, lane, chance, frame.commit());
    }

    private static Frame current() {
        Deque<Frame> frames = FRAMES.get();
        return frames.isEmpty() ? null : frames.peek();
    }

    private record Frame(IESequenceState state, ResourceLocation recipeId, boolean commit) {
    }
}
