package io.github.langqi99.deterministicchance.compat;

import net.minecraftforge.fml.loading.FMLLoader;

/** Base-Forge-only gate; this class is safe to load when GTCEu is absent. */
public final class GTCEu7Availability {
    private GTCEu7Availability() {}

    public static boolean isLoaded() {
        var loadingModList = FMLLoader.getLoadingModList();
        if (loadingModList == null) {
            return false;
        }
        var file = loadingModList.getModFileById("gtceu");
        if (file == null) {
            return false;
        }
        return file.getMods().stream().anyMatch(mod ->
                mod.getModId().equals("gtceu") && major(mod.getVersion().toString()) == 7);
    }

    private static int major(String version) {
        int end = 0;
        while (end < version.length() && Character.isDigit(version.charAt(end))) {
            end++;
        }
        if (end == 0) {
            return -1;
        }
        try {
            return Integer.parseInt(version.substring(0, end));
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }
}
