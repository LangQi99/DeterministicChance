package io.github.langqi99.deterministicchance.compat.thermal;

import cofh.thermal.lib.util.recipes.internal.IMachineRecipe;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;

/** Small bridge kept separate from the bytecode hook for direct testing. */
public final class ThermalMachineSequenceController {
    private ThermalMachineSequenceController() {
    }

    public static void beginOutputPass(
            ThermalSequenceStateAccess machine,
            IMachineRecipe recipe,
            List<ItemStack> outputItems,
            List<FluidStack> outputFluids,
            List<Float> outputChances) {
        machine.deterministicChance$thermalState().beginOutputPass(
                stableRecipeKey(recipe, outputItems, outputFluids, outputChances));
    }

    public static float nextOutputMultiplier(
            ThermalSequenceStateAccess machine,
            float nativeChance) {
        return machine.deterministicChance$thermalState().nextOutputCopies(nativeChance);
    }

    public static void load(ThermalSequenceStateAccess machine, CompoundTag tag) {
        machine.deterministicChance$thermalState().load(tag);
    }

    public static void save(ThermalSequenceStateAccess machine, CompoundTag tag) {
        machine.deterministicChance$thermalState().save(tag);
    }

    /** Thermal's runtime recipe wrapper has no id, so persist a content fingerprint. */
    static String stableRecipeKey(
            IMachineRecipe recipe,
            List<ItemStack> outputItems,
            List<FluidStack> outputFluids,
            List<Float> outputChances) {
        if (recipe == null) {
            throw new IllegalStateException("Thermal machine has no active recipe");
        }
        StringBuilder canonical = new StringBuilder(recipe.getClass().getName());
        appendItems(canonical, 'I', recipe.getInputItems());
        appendFluids(canonical, 'F', recipe.getInputFluids());
        // Some IMachineRecipe implementations resolve output identity and
        // amount from the inventory. Keeping those actual lists in the key
        // prevents one dynamic result from borrowing another result's phase.
        appendItems(canonical, 'O', outputItems);
        appendFluids(canonical, 'R', outputFluids);
        // A catalyst or output augment creates a distinct deterministic
        // profile. Switching profiles therefore cannot reset or steal another
        // profile's partial cycle.
        canonical.append("|C:");
        for (float chance : outputChances) {
            canonical.append(Float.floatToIntBits(chance)).append(';');
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonical.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("JVM has no SHA-256 implementation", impossible);
        }
    }

    private static void appendItems(StringBuilder target, char marker, List<ItemStack> stacks) {
        target.append('|').append(marker).append(':').append(stacks.size());
        for (ItemStack stack : stacks) {
            target.append(';')
                    .append(ForgeRegistries.ITEMS.getKey(stack.getItem()))
                    .append('@').append(stack.getCount())
                    .append('#').append(stack.getTag());
        }
    }

    private static void appendFluids(StringBuilder target, char marker, List<FluidStack> stacks) {
        target.append('|').append(marker).append(':').append(stacks.size());
        for (FluidStack stack : stacks) {
            target.append(';')
                    .append(ForgeRegistries.FLUIDS.getKey(stack.getFluid()))
                    .append('@').append(stack.getAmount())
                    .append('#').append(stack.getTag());
        }
    }
}
