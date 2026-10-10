package dev.overloadsim.output;

import appeng.menu.SlotSemantic;
import appeng.menu.SlotSemantics;

/** Each page keeps stable slot ids while sharing the original two-row interface layout. */
public final class OutputSlotSemantics {
    public static final SlotSemantic[] CONFIG = {
            SlotSemantics.register("LTS_OUTPUT_CONFIG_0", false),
            SlotSemantics.register("LTS_OUTPUT_CONFIG_1", false),
            SlotSemantics.register("LTS_OUTPUT_CONFIG_2", false),
            SlotSemantics.register("LTS_OUTPUT_CONFIG_3", false)
    };
    public static final SlotSemantic[] STORAGE = {
            SlotSemantics.register("LTS_OUTPUT_STORAGE_0", false),
            SlotSemantics.register("LTS_OUTPUT_STORAGE_1", false),
            SlotSemantics.register("LTS_OUTPUT_STORAGE_2", false),
            SlotSemantics.register("LTS_OUTPUT_STORAGE_3", false)
    };
    private OutputSlotSemantics() {}
}
