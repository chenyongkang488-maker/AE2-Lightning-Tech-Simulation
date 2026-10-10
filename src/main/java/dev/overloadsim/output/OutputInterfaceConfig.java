package dev.overloadsim.output;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Server-side routing limits. Zero range means unlimited same-dimension range. */
public final class OutputInterfaceConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue RANGE;
    public static final ModConfigSpec.BooleanValue CROSS_DIMENSION;
    static {
        var builder = new ModConfigSpec.Builder();
        RANGE = builder.translation("overload_sim.configuration.outputInterface.range")
                .comment("Maximum same-dimension wireless range in blocks; 0 means unlimited. Target chunks are never forced to load.")
                .defineInRange("wirelessRange", 128, 0, Integer.MAX_VALUE);
        CROSS_DIMENSION = builder.translation("overload_sim.configuration.outputInterface.crossDimension")
                .comment("Allow wireless output to other dimensions. Only loaded target chunks are eligible.")
                .define("allowCrossDimension", true);
        SPEC = builder.build();
    }
    private OutputInterfaceConfig() {}
}
