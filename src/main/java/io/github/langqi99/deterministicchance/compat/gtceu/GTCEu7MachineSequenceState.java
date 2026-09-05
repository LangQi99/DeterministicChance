package io.github.langqi99.deterministicchance.compat.gtceu;

import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import java.util.Objects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * ForgeData-backed, per-block/per-recipe/per-output cursor storage.
 *
 * <p>This intentionally does not use GTCEu's chance cache. GTCEu clears that
 * cache when a machine changes recipe, which would let recipe switching reset
 * a deterministic sequence. The content's serialized tag plus a duplicate
 * ordinal gives stable identity without relying on Java object identity.</p>
 */
final class GTCEu7MachineSequenceState {
    private static final String ROOT_KEY = "deterministic_chance";
    private static final String INTEGRATION_KEY = "gtceu_7";
    private static final String STATES_KEY = "states";

    private GTCEu7MachineSequenceState() {}

    static GTCEu7ExactOrCycle.Advance advance(
            BlockEntity owner,
            GTRecipe recipe,
            RecipeCapability<?> capability,
            Content content,
            int duplicateOrdinal,
            int effectiveChance,
            int attempts) {
        Objects.requireNonNull(recipe.getId(), "GTCEu recipe id");

        var fraction = GTCEu7ExactOrCycle.fraction(effectiveChance, content.maxChance);
        Tag contentIdentity = capability.contentToNbt(content.content);

        CompoundTag forgeData = owner.getPersistentData();
        CompoundTag root = forgeData.getCompound(ROOT_KEY);
        CompoundTag integration = root.getCompound(INTEGRATION_KEY);
        ListTag states = integration.getList(STATES_KEY, Tag.TAG_COMPOUND);

        int stateIndex = find(
                states,
                recipe.getId().toString(),
                capability.name,
                contentIdentity,
                duplicateOrdinal);
        CompoundTag state = stateIndex < 0 ? null : states.getCompound(stateIndex);

        int position = 0;
        if (state != null
                && state.getInt("numerator") == fraction.numerator()
                && state.getInt("denominator") == fraction.denominator()) {
            position = state.getInt("position");
        }

        var result = GTCEu7ExactOrCycle.advance(fraction, position, attempts);
        if (result.nextPosition() == 0) {
            // A completed cycle needs no persisted cursor. This naturally
            // prunes recipes and output lanes instead of growing machine NBT
            // forever as the player changes recipes.
            if (stateIndex >= 0) {
                states.remove(stateIndex);
            }
        } else {
            if (state == null) {
                state = new CompoundTag();
                state.putString("recipe", recipe.getId().toString());
                state.putString("capability", capability.name);
                state.put("content", contentIdentity.copy());
                state.putInt("ordinal", duplicateOrdinal);
                states.add(state);
            }
            state.putInt("numerator", fraction.numerator());
            state.putInt("denominator", fraction.denominator());
            state.putInt("position", result.nextPosition());
        }

        integration.put(STATES_KEY, states);
        root.put(INTEGRATION_KEY, integration);
        forgeData.put(ROOT_KEY, root);
        owner.setChanged();
        return result;
    }

    private static int find(
            ListTag states,
            String recipeId,
            String capability,
            Tag content,
            int ordinal) {
        for (int index = 0; index < states.size(); index++) {
            CompoundTag state = states.getCompound(index);
            if (state.getInt("ordinal") == ordinal
                    && state.getString("recipe").equals(recipeId)
                    && state.getString("capability").equals(capability)
                    && Objects.equals(state.get("content"), content)) {
                return index;
            }
        }
        return -1;
    }
}
