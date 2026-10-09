package dev.overloadsim.laser;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Prototype tuning; costs are still settled once per actual 20-tick beam batch. */
public final class LaserSettings {
    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.LongValue EMITTER_FE,CONSTRAINT_FE,BURST_EHV;
    private static final ModConfigSpec.DoubleValue DAMAGE;
    private static final ModConfigSpec.IntValue BLAST_HEIGHT,BLAST_BUDGET;
    private static final ModConfigSpec.BooleanValue EXPLOSION_ENABLED;
    static {
        var b=new ModConfigSpec.Builder();
        EMITTER_FE=b.translation("overload_sim.configuration.laser.emitterFePerBatch").comment("Emitter FE consumed and emitted per 20-tick batch.").defineInRange("emitterFePerBatch",100_000L,1L,dev.overloadsim.core.LaserMath.MAX_BEAM);
        CONSTRAINT_FE=b.translation("overload_sim.configuration.laser.constraintFePerBatch").comment("Constraint matrix FE per 20-tick batch.").defineInRange("constraintFePerBatch",100_000L,0L,2_000_000_000L);
        BURST_EHV=b.translation("overload_sim.configuration.laser.burstExtremeLightningPerBatch").comment("Burst matrix extreme voltage cost per 20-tick batch.").defineInRange("burstExtremeLightningPerBatch",64L,0L,2_000_000_000L);
        DAMAGE=b.translation("overload_sim.configuration.laser.laserDamageMultiplier").defineInRange("laserDamageMultiplier",1D,0D,200D);
        EXPLOSION_ENABLED=b.translation("overload_sim.configuration.explosion.explosionEnabled").comment("Enable charged receiver explosions. False disables destruction, entity damage, sound and animation. Radius is configured in overload_sim-machines.toml.").define("explosionEnabled",true);
        BLAST_HEIGHT=b.translation("overload_sim.configuration.explosion.explosionVerticalRadius").comment("Legacy setting retained for existing configs; ignored. Use explosion.radius in overload_sim-machines.toml.").defineInRange("explosionVerticalRadius",12,1,64);
        BLAST_BUDGET=b.translation("overload_sim.configuration.explosion.explosionBlockBudgetPerTick").comment("Maximum block positions processed per tick for a receiver explosion.").defineInRange("explosionBlockBudgetPerTick",1024,64,8192);
        SPEC=b.build();
    }
    private LaserSettings(){}
    public static long emitterFe(){return EMITTER_FE.get();}
    public static long constraintFe(){return CONSTRAINT_FE.get();}
    public static long burstEhv(){return BURST_EHV.get();}
    public static double damageScale(){return DAMAGE.get();}
    public static boolean explosionEnabled(){return EXPLOSION_ENABLED.get();}
    @Deprecated
    static int blastHeight(){return BLAST_HEIGHT.get();}
    static int blastBudget(){return BLAST_BUDGET.get();}
}
