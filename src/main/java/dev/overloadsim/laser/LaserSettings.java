package dev.overloadsim.laser;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Prototype tuning; costs are still settled once per actual 20-tick beam batch. */
public final class LaserSettings {
    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.LongValue EMITTER_FE,CONSTRAINT_FE,BURST_EHV;
    private static final ModConfigSpec.DoubleValue DAMAGE;
    private static final ModConfigSpec.IntValue BLAST_HEIGHT,BLAST_BUDGET;
    static {var b=new ModConfigSpec.Builder();EMITTER_FE=b.defineInRange("emitterFePerBatch",100_000L,1L,1_000_000_000L);CONSTRAINT_FE=b.defineInRange("constraintFePerBatch",100_000L,0L,1_000_000_000L);BURST_EHV=b.defineInRange("burstExtremeLightningPerBatch",64L,0L,1_000_000_000L);DAMAGE=b.defineInRange("laserDamageMultiplier",1D,0D,100D);BLAST_HEIGHT=b.comment("Charged receiver blast spans its 3x3 chunks and this vertical radius; loaded chunks only.").defineInRange("explosionVerticalRadius",12,1,64);BLAST_BUDGET=b.defineInRange("explosionBlockBudgetPerTick",1024,64,4096);SPEC=b.build();}
    private LaserSettings(){}
    public static long emitterFe(){return EMITTER_FE.get();}
    public static long constraintFe(){return CONSTRAINT_FE.get();}
    public static long burstEhv(){return BURST_EHV.get();}
    public static double damageScale(){return DAMAGE.get();}
    static int blastHeight(){return BLAST_HEIGHT.get();}
    static int blastBudget(){return BLAST_BUDGET.get();}
}
