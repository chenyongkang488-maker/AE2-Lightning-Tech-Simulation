package dev.overloadsim;
import net.neoforged.neoforge.common.ModConfigSpec;
public final class SimulationConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue MATRIX_LIMIT,CARD_LIMIT,EJECT_INTERVAL;
    static {var b=new ModConfigSpec.Builder();MATRIX_LIMIT=b.comment("Pack limit; hard maximum is 32 (128 parallel).").defineInRange("matrixLimit",32,1,32);CARD_LIMIT=b.defineInRange("accelerationCardLimit",4,0,4);EJECT_INTERVAL=b.defineInRange("autoEjectIntervalTicks",10,1,200);SPEC=b.build();}
}
