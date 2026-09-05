package io.github.langqi99.deterministicchance.compat.gtceu;

import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.chance.boost.ChanceBoostFunction;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Runtime bridge for GTCEu 7's independent OR chance logic. */
public final class GTCEu7OrChanceRoller {
    private GTCEu7OrChanceRoller() {}

    public static List<Content> roll(
            BlockEntity owner,
            GTRecipe recipe,
            RecipeCapability<?> capability,
            List<Content> entries,
            ChanceBoostFunction boostFunction,
            int recipeTier,
            int chanceTier,
            int times) {
        if (times < 0) {
            throw new IllegalArgumentException("times must not be negative");
        }

        List<Content> produced = new ArrayList<>(entries.size());
        List<Tag> identities = new ArrayList<>(entries.size());
        for (Content entry : entries) {
            Tag identity = capability.contentToNbt(entry.content);
            int duplicateOrdinal = 0;
            for (Tag previous : identities) {
                if (Objects.equals(previous, identity)) {
                    duplicateOrdinal++;
                }
            }
            identities.add(identity);

            int effectiveChance = Math.max(
                    0,
                    Math.min(entry.maxChance, boostFunction.getBoostedChance(entry, recipeTier, chanceTier)));
            var result = GTCEu7MachineSequenceState.advance(
                    owner,
                    recipe,
                    capability,
                    entry,
                    duplicateOrdinal,
                    effectiveChance,
                    times);
            if (result.successes() > 0) {
                produced.add(entry.copyChanced(
                        capability,
                        ContentModifier.multiplier(result.successes())));
            }
        }
        return List.copyOf(produced);
    }
}
