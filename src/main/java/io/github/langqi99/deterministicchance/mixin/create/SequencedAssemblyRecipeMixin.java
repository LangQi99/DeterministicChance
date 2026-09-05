package io.github.langqi99.deterministicchance.mixin.create;

import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import io.github.langqi99.deterministicchance.compat.create.CreateMachineRollContext;
import io.github.langqi99.deterministicchance.compat.create.SequencedAssemblySequenceController;
import java.util.List;
import java.util.Random;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Replaces Create's global weighted roll only while a real assembly machine commits its result. */
@Mixin(value = SequencedAssemblyRecipe.class, remap = false)
abstract class SequencedAssemblyRecipeMixin {
    @Shadow public List<ProcessingOutput> resultPool;

    @Redirect(
            method = "rollResult",
            at = @At(value = "INVOKE", target = "Ljava/util/Random;nextFloat()F"),
            remap = false,
            require = 1)
    private float deterministicChance$weightedResult(Random random) {
        var machine = CreateMachineRollContext.activeMachine();
        if (machine == null) {
            return random.nextFloat();
        }
        return SequencedAssemblySequenceController.nextRoll(
                machine,
                (SequencedAssemblyRecipe) (Object) this,
                resultPool,
                random);
    }
}
