package io.github.langqi99.deterministicchance;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod(DeterministicChance.MOD_ID)
public final class DeterministicChance {
    public static final String MOD_ID = "deterministic_chance";
    public static final Logger LOGGER = LogUtils.getLogger();

    public DeterministicChance() {
        LOGGER.info("确定的概率 loaded");
    }
}
