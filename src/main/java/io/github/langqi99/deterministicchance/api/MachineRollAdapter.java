package io.github.langqi99.deterministicchance.api;

import io.github.langqi99.deterministicchance.core.ChanceFraction;

/**
 * Integration point used at the exact native chance-roll call site. Machine
 * adapters must provide stable keys so sequence state can be saved per machine,
 * recipe and output slot.
 */
public interface MachineRollAdapter<M> {
    String machineKey(M machine);

    String recipeKey(M machine);

    boolean deterministicRoll(M machine, int outputSlot, ChanceFraction chance);
}
