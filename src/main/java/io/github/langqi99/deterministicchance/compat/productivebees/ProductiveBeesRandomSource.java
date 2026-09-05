package io.github.langqi99.deterministicchance.compat.productivebees;

import cy.jdkdigital.productivebees.common.recipe.CentrifugeRecipe;
import cy.jdkdigital.productivebees.init.ModTags;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;

/** Delegates unrelated randomness while replacing one centrifuge output batch with exact schedules. */
public final class ProductiveBeesRandomSource implements RandomSource {
    private final RandomSource delegate;
    private final ProductiveBeesSequenceState state;
    private final CentrifugeRecipe recipe;
    private final List<IntArrayTag> profiles;
    private int lane;
    private ProductiveBeesSequenceState.Outcome pending;
    private int pendingMinimum;
    private int pendingBound;

    public ProductiveBeesRandomSource(
            RandomSource delegate,
            ProductiveBeesSequenceState state,
            CentrifugeRecipe recipe,
            boolean stripWax) {
        this.delegate = delegate;
        this.state = state;
        this.recipe = recipe;
        this.profiles = new ArrayList<>();
        recipe.getRecipeOutputs().forEach((stack, values) -> {
            if (!stripWax || !stack.is(ModTags.Forge.WAX)) {
                profiles.add(values);
            }
        });
    }

    @Override
    public int nextInt(int bound) {
        if (pending != null && pending.success() && pendingBound == bound) {
            int offset = pending.count() - pendingMinimum;
            pending = null;
            pendingBound = 0;
            return offset;
        }
        if (bound == 100 && lane < profiles.size()) {
            IntArrayTag values = profiles.get(lane);
            pendingMinimum = values.get(0).getAsInt();
            pending = state.next(
                    recipe.getId(),
                    lane++,
                    pendingMinimum,
                    values.get(1).getAsInt(),
                    values.get(2).getAsInt());
            pendingBound = values.get(1).getAsInt() - pendingMinimum + 1;
            if (!pending.success() || pendingBound == 1) {
                pendingBound = 0;
            }
            // Productive Bees compares with <=. Returning 100 is an intentional
            // sentinel that makes authored 0% actually NEVER, while 100% stays certain.
            return pending.success() ? 0 : 100;
        }
        return delegate.nextInt(bound);
    }

    @Override public RandomSource fork() { return delegate.fork(); }
    @Override public PositionalRandomFactory forkPositional() { return delegate.forkPositional(); }
    @Override public void setSeed(long seed) { delegate.setSeed(seed); }
    @Override public int nextInt() { return delegate.nextInt(); }
    @Override public long nextLong() { return delegate.nextLong(); }
    @Override public boolean nextBoolean() { return delegate.nextBoolean(); }
    @Override public float nextFloat() { return delegate.nextFloat(); }
    @Override public double nextDouble() { return delegate.nextDouble(); }
    @Override public double nextGaussian() { return delegate.nextGaussian(); }
}
