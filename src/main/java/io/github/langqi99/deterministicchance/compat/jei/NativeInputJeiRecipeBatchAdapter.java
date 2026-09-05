package io.github.langqi99.deterministicchance.compat.jei;

import appeng.api.stacks.GenericStack;
import java.util.List;

/**
 * A JEI adapter whose native recipe exposes the inputs that are actually
 * consumed by the machine.
 *
 * <p>This matters for recipe viewers that display catalysts or tools with the
 * input role even though the machine does not consume them. The central
 * registry should prefer these inputs over the generic JEI slot view.</p>
 */
public interface NativeInputJeiRecipeBatchAdapter extends JeiRecipeBatchAdapter {
    List<List<GenericStack>> inputs(Object recipe);

    default JeiBatchPlan plan(Object recipe) {
        return JeiBatchPlanner.plan(inputs(recipe), outputs(recipe));
    }
}
