package dev.overloadsim;
import net.neoforged.neoforge.common.ModConfigSpec;
public final class SimulationConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue MATRIX_LIMIT,CARD_LIMIT,EJECT_INTERVAL,NETWORK_FE_PER_TICK;
    public static final ModConfigSpec.IntValue COIL_RANGE,COIL_SELF_CHARGE,COIL_COOLDOWN,COIL_MINING_FE,COIL_CHARGE_FE;
    static {var b=new ModConfigSpec.Builder();MATRIX_LIMIT=b.comment("Pack limit; hard maximum is 32 (128 parallel).").defineInRange("matrixLimit",32,1,32);CARD_LIMIT=b.defineInRange("accelerationCardLimit",4,0,4);EJECT_INTERVAL=b.defineInRange("autoEjectIntervalTicks",10,1,200);NETWORK_FE_PER_TICK=b.comment("Maximum FE drawn from an online ME grid per server tick, including while idle. 0 disables ME charging.").defineInRange("networkFeChargePerTick",10000,0,2_000_000);
        b.push("resonanceCoil");COIL_RANGE=b.defineInRange("lightningRange",32,1,128);COIL_SELF_CHARGE=b.defineInRange("selfStrikeChargeTicks",30,1,200);COIL_COOLDOWN=b.defineInRange("lightningCooldownTicks",10,1,200);COIL_MINING_FE=b.comment("FE per mined block or melee hit; lightning separately costs exactly 10 of the selected voltage.").defineInRange("miningEnergyFe",200,0,1_000_000);COIL_CHARGE_FE=b.defineInRange("chargeFePerTick",10000,0,1_000_000);b.pop();SPEC=b.build();}
}
