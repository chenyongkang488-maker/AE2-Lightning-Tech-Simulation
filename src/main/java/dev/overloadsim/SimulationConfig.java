package dev.overloadsim;
import net.neoforged.neoforge.common.ModConfigSpec;
public final class SimulationConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue MATRIX_LIMIT,CARD_LIMIT,EJECT_INTERVAL,NETWORK_FE_PER_TICK;
    static {var b=new ModConfigSpec.Builder();MATRIX_LIMIT=b.comment("Pack limit; hard maximum is 32 (128 parallel).").defineInRange("matrixLimit",32,1,32);CARD_LIMIT=b.defineInRange("accelerationCardLimit",4,0,4);EJECT_INTERVAL=b.defineInRange("autoEjectIntervalTicks",10,1,200);NETWORK_FE_PER_TICK=b.comment("Maximum FE drawn from an online ME grid per server tick, including while idle. 0 disables ME charging.").defineInRange("networkFeChargePerTick",10000,0,2_000_000);SPEC=b.build();}
}
