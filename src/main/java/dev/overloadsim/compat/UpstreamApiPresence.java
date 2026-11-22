package dev.overloadsim.compat;

import java.util.List;
import java.util.function.Predicate;

/** Shared selection rule, usable before Minecraft or the addon is initialized. */
public final class UpstreamApiPresence {
    public static final List<String> REQUIRED=List.of(
            "com.moakiee.ae2lt.api.lightning.collector.CollectorCrystalApi",
            "com.moakiee.ae2lt.api.device.DeviceWorkbenchApi",
            "com.moakiee.ae2lt.api.lightning.LightningApi");
    private UpstreamApiPresence() {}
    public static boolean available(Predicate<String> exists) { return REQUIRED.stream().allMatch(exists); }
}
