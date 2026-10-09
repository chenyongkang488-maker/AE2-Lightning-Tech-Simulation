package dev.overloadsim.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Operational limits; physical geometry and network protocol safety bounds stay fixed. */
public final class MachineSettings {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue SIMULATION_ENERGY_CAPACITY;
    public static final ModConfigSpec.IntValue SIMULATION_RECEIVE_PER_CALL;
    public static final ModConfigSpec.IntValue SIMULATION_MAX_PARALLEL;
    public static final ModConfigSpec.IntValue SIMULATION_PARALLEL_PER_MATRIX;
    public static final ModConfigSpec.IntValue SIMULATION_START_INTERVAL;
    public static final ModConfigSpec.IntValue SIMULATION_EJECT_PER_CYCLE;
    public static final ModConfigSpec.IntValue SIMULATION_TIME_PERCENT;
    public static final ModConfigSpec.IntValue SIMULATION_ENERGY_COST_PERCENT;
    public static final ModConfigSpec.IntValue SIMULATION_LIGHTNING_COST_PERCENT;
    public static final ModConfigSpec.IntValue MULTIBLOCK_ENERGY_CAPACITY;
    public static final ModConfigSpec.IntValue MULTIBLOCK_RECEIVE_PER_CALL;
    public static final ModConfigSpec.IntValue MULTIBLOCK_NETWORK_FE_PER_TICK;
    public static final ModConfigSpec.IntValue MULTIBLOCK_MAX_CRYSTALS;
    public static final ModConfigSpec.IntValue MULTIBLOCK_OUTPUT_SLOT_LIMIT;
    public static final ModConfigSpec.IntValue MULTIBLOCK_EXPORT_INTERVAL;
    public static final ModConfigSpec.IntValue MULTIBLOCK_TIME_PERCENT;
    public static final ModConfigSpec.IntValue MULTIBLOCK_ENERGY_COST_PERCENT;
    public static final ModConfigSpec.IntValue MULTIBLOCK_LIGHTNING_COST_PERCENT;
    public static final ModConfigSpec.LongValue COIL_BASE_CAPACITY;
    public static final ModConfigSpec.LongValue COIL_CAPACITY_LIMIT;
    public static final ModConfigSpec.IntValue COIL_LIGHTNING_COST;
    public static final ModConfigSpec.IntValue COIL_EFFICIENCY_LIMIT;
    public static final ModConfigSpec.IntValue COIL_FORTUNE_LIMIT;
    public static final ModConfigSpec.DoubleValue COIL_BASE_MINING_SPEED;
    public static final ModConfigSpec.LongValue TESLA_CRYSTAL_FE;
    public static final ModConfigSpec.LongValue TESLA_CRYSTAL_EHV;
    public static final ModConfigSpec.LongValue WORKSTATION_LASER_THRESHOLD;
    public static final ModConfigSpec.IntValue WORKSTATION_MAX_PARALLEL;
    public static final ModConfigSpec.IntValue WORKSTATION_CYCLE_TICKS;
    public static final ModConfigSpec.IntValue WORKSTATION_SLOT_CAPACITY;
    public static final ModConfigSpec.IntValue WORKSTATION_TANK_CAPACITY;
    public static final ModConfigSpec.IntValue WORKSTATION_ITEM_EXPORT_LIMIT;
    public static final ModConfigSpec.IntValue WORKSTATION_FLUID_EXPORT_LIMIT;
    public static final ModConfigSpec.LongValue LASER_MAX_BEAM_FE;
    public static final ModConfigSpec.LongValue LASER_MACHINE_ENERGY_CAPACITY;
    public static final ModConfigSpec.IntValue LASER_EMITTER_RANGE;
    public static final ModConfigSpec.IntValue LASER_CONSTRAINT_RANGE;
    public static final ModConfigSpec.IntValue LASER_DEFLECTOR_RANGE;
    public static final ModConfigSpec.LongValue LASER_BOOST_MINIMUM;
    public static final ModConfigSpec.LongValue LASER_BOOST_MAXIMUM;
    public static final ModConfigSpec.LongValue LASER_BOOST_STEP;
    public static final ModConfigSpec.IntValue LASER_BURST_MULTIPLIER;
    public static final ModConfigSpec.IntValue LASER_DAMAGE_INTERVAL;
    public static final ModConfigSpec.DoubleValue LASER_MAX_DAMAGE;
    public static final ModConfigSpec.LongValue CORE_ACTIVATION_THRESHOLD;
    public static final ModConfigSpec.LongValue RECEIVER_ENERGY_CAPACITY;
    public static final ModConfigSpec.LongValue RECEIVER_PARTICLE_CAPACITY;
    public static final ModConfigSpec.IntValue EXPLOSION_RADIUS;
    static {
        var b=new ModConfigSpec.Builder();
        b.translation("overload_sim.configuration.simulation").comment("普通模拟仓").push("simulation");
        SIMULATION_ENERGY_CAPACITY=b.worldRestart().translation("overload_sim.configuration.simulation.energyCapacity").comment("FE storage capacity. Reenter the world to apply.").defineInRange("energyCapacity",2000000,1,2147483647);
        SIMULATION_RECEIVE_PER_CALL=b.worldRestart().translation("overload_sim.configuration.simulation.receivePerCall").comment("FE received per capability call. Reenter the world to apply.").defineInRange("receivePerCall",2000000,0,2147483647);
        SIMULATION_MAX_PARALLEL=b.worldRestart().translation("overload_sim.configuration.simulation.maxParallel").comment("Maximum operations in a batch. Reenter the world to apply.").defineInRange("maxParallel",128,1,128);
        SIMULATION_PARALLEL_PER_MATRIX=b.worldRestart().translation("overload_sim.configuration.simulation.parallelPerMatrix").comment("Parallel operations supplied by each matrix. Reenter the world to apply.").defineInRange("parallelPerMatrix",4,1,128);
        SIMULATION_START_INTERVAL=b.worldRestart().translation("overload_sim.configuration.simulation.startIntervalTicks").comment("Ticks between new batch checks. Reenter the world to apply.").defineInRange("startIntervalTicks",10,1,200);
        SIMULATION_EJECT_PER_CYCLE=b.worldRestart().translation("overload_sim.configuration.simulation.ejectPerCycle").comment("Items exported per output cycle. Reenter the world to apply.").defineInRange("ejectPerCycle",64,0,2147483647);
        SIMULATION_TIME_PERCENT=b.worldRestart().translation("overload_sim.configuration.simulation.timePercent").comment("Recipe time percentage; new batches only. Reenter the world to apply.").defineInRange("timePercent",100,1,10000);
        SIMULATION_ENERGY_COST_PERCENT=b.worldRestart().translation("overload_sim.configuration.simulation.energyCostPercent").comment("Recipe FE percentage; new batches only. Reenter the world to apply.").defineInRange("energyCostPercent",100,0,10000);
        SIMULATION_LIGHTNING_COST_PERCENT=b.worldRestart().translation("overload_sim.configuration.simulation.lightningCostPercent").comment("Recipe lightning percentage; new batches only. Reenter the world to apply.").defineInRange("lightningCostPercent",100,0,10000);
        b.pop();
        b.translation("overload_sim.configuration.multiblock").comment("模拟室").push("multiblock");
        MULTIBLOCK_ENERGY_CAPACITY=b.worldRestart().translation("overload_sim.configuration.multiblock.energyCapacity").comment("FE storage capacity. Reenter the world to apply.").defineInRange("energyCapacity",2000000,1,2147483647);
        MULTIBLOCK_RECEIVE_PER_CALL=b.worldRestart().translation("overload_sim.configuration.multiblock.receivePerCall").comment("FE received per capability call. Reenter the world to apply.").defineInRange("receivePerCall",2000000,0,2147483647);
        MULTIBLOCK_NETWORK_FE_PER_TICK=b.worldRestart().translation("overload_sim.configuration.multiblock.networkFePerTick").comment("FE drawn from Applied Flux storage per tick. Reenter the world to apply.").defineInRange("networkFePerTick",10000,0,2147483647);
        MULTIBLOCK_MAX_CRYSTALS=b.worldRestart().translation("overload_sim.configuration.multiblock.maxCrystals").comment("Maximum crystals participating in new batches; storage is retained. Reenter the world to apply.").defineInRange("maxCrystals",49,1,49);
        MULTIBLOCK_OUTPUT_SLOT_LIMIT=b.worldRestart().translation("overload_sim.configuration.multiblock.outputSlotLimit").comment("Items per bulk output slot; existing goods are retained. Reenter the world to apply.").defineInRange("outputSlotLimit",1024,1,2147483647);
        MULTIBLOCK_EXPORT_INTERVAL=b.worldRestart().translation("overload_sim.configuration.multiblock.exportIntervalTicks").comment("Ticks between automatic export attempts. Reenter the world to apply.").defineInRange("exportIntervalTicks",5,1,200);
        MULTIBLOCK_TIME_PERCENT=b.worldRestart().translation("overload_sim.configuration.multiblock.timePercent").comment("Recipe time percentage; new batches only. Reenter the world to apply.").defineInRange("timePercent",100,1,10000);
        MULTIBLOCK_ENERGY_COST_PERCENT=b.worldRestart().translation("overload_sim.configuration.multiblock.energyCostPercent").comment("Recipe FE percentage; new batches only. Reenter the world to apply.").defineInRange("energyCostPercent",100,0,10000);
        MULTIBLOCK_LIGHTNING_COST_PERCENT=b.worldRestart().translation("overload_sim.configuration.multiblock.lightningCostPercent").comment("Recipe lightning percentage; new batches only. Reenter the world to apply.").defineInRange("lightningCostPercent",100,0,10000);
        b.pop();
        b.translation("overload_sim.configuration.coil").comment("共振线圈").push("coil");
        COIL_BASE_CAPACITY=b.worldRestart().translation("overload_sim.configuration.coil.baseCapacity").comment("Base FE capacity. Reenter the world to apply.").defineInRange("baseCapacity",10000000L,1L,20000000000L);
        COIL_CAPACITY_LIMIT=b.worldRestart().translation("overload_sim.configuration.coil.capacityLimit").comment("Maximum module FE capacity; existing FE is retained. Reenter the world to apply.").defineInRange("capacityLimit",20000000000L,1L,20000000000L);
        COIL_LIGHTNING_COST=b.worldRestart().translation("overload_sim.configuration.coil.lightningCost").comment("Selected lightning voltage consumed per strike. Reenter the world to apply.").defineInRange("lightningCost",10,1,1000000);
        COIL_EFFICIENCY_LIMIT=b.worldRestart().translation("overload_sim.configuration.coil.efficiencyLimit").comment("Effective efficiency level limit. Reenter the world to apply.").defineInRange("efficiencyLimit",10,0,10);
        COIL_FORTUNE_LIMIT=b.worldRestart().translation("overload_sim.configuration.coil.fortuneLimit").comment("Effective fortune level limit. Reenter the world to apply.").defineInRange("fortuneLimit",5,0,5);
        COIL_BASE_MINING_SPEED=b.worldRestart().translation("overload_sim.configuration.coil.baseMiningSpeed").comment("Base mining speed before efficiency. Reenter the world to apply.").defineInRange("baseMiningSpeed",9D,0.1D,1000D);
        b.pop();
        b.translation("overload_sim.configuration.tesla").comment("特斯拉超限水晶配方").push("tesla");
        TESLA_CRYSTAL_FE=b.worldRestart().translation("overload_sim.configuration.tesla.crystalFe").comment("FE per crystal in the addon Tesla recipe. Native five-tick processing remains fixed. Reenter the world to apply.").defineInRange("crystalFe",500000L,5L,16000000L);
        TESLA_CRYSTAL_EHV=b.worldRestart().translation("overload_sim.configuration.tesla.crystalEhv").comment("Extreme voltage output per crystal. Frozen for an already-started batch. Reenter the world to apply.").defineInRange("crystalEhv",64L,1L,1000000L);
        b.pop();
        b.translation("overload_sim.configuration.workstation").comment("强能工作站").push("workstation");
        WORKSTATION_LASER_THRESHOLD=b.worldRestart().translation("overload_sim.configuration.workstation.laserThreshold").comment("Incoming 20-tick laser energy must be strictly greater than this threshold. Reenter the world to apply.").defineInRange("laserThreshold",1000000000L,0L,1000000000000000L);
        WORKSTATION_MAX_PARALLEL=b.worldRestart().translation("overload_sim.configuration.workstation.maxParallel").comment("Maximum recipe operations per batch. Reenter the world to apply.").defineInRange("maxParallel",1024,1,1048576);
        WORKSTATION_CYCLE_TICKS=b.worldRestart().translation("overload_sim.configuration.workstation.cycleTicks").comment("Ticks needed for one successful batch. Reenter the world to apply.").defineInRange("cycleTicks",2,1,72000);
        WORKSTATION_SLOT_CAPACITY=b.worldRestart().translation("overload_sim.configuration.workstation.slotCapacity").comment("Items per input/output slot; existing goods are retained. Reenter the world to apply.").defineInRange("slotCapacity",1000000,1,2147483647);
        WORKSTATION_TANK_CAPACITY=b.worldRestart().translation("overload_sim.configuration.workstation.tankCapacity").comment("mB per fluid tank; existing fluid is retained. Reenter the world to apply.").defineInRange("tankCapacity",1024000000,1,2147483647);
        WORKSTATION_ITEM_EXPORT_LIMIT=b.worldRestart().translation("overload_sim.configuration.workstation.itemExportLimit").comment("Total automatic item export per tick, shared by AE and adjacent containers. Zero disables export. Reenter the world to apply.").defineInRange("itemExportLimit",2147483647,0,2147483647);
        WORKSTATION_FLUID_EXPORT_LIMIT=b.worldRestart().translation("overload_sim.configuration.workstation.fluidExportLimit").comment("Total automatic fluid export mB per tick, shared by AE and adjacent containers. Zero disables export. Reenter the world to apply.").defineInRange("fluidExportLimit",2147483647,0,2147483647);
        b.pop();
        b.translation("overload_sim.configuration.laser").comment("激光网络").push("laser");
        LASER_MAX_BEAM_FE=b.worldRestart().translation("overload_sim.configuration.laser.maxBeamFe").comment("Maximum beam FE per 20-tick batch. An over-limit source emits nothing and pays nothing. An over-limit modifier pays no modifier cost; upstream emitted costs remain paid. Reenter the world to apply.").defineInRange("maxBeamFe",1000000000000000L,1L,1000000000000000L);
        LASER_MACHINE_ENERGY_CAPACITY=b.worldRestart().translation("overload_sim.configuration.laser.energyCapacity").comment("FE capacity of laser matrices and emitters; existing FE is retained. Reenter the world to apply.").defineInRange("energyCapacity",1000000000L,1L,1024000000000000000L);
        LASER_EMITTER_RANGE=b.worldRestart().translation("overload_sim.configuration.laser.emitterRange").comment("Emitter output travel distance in blocks. Reenter the world to apply.").defineInRange("emitterRange",6,1,64);
        LASER_CONSTRAINT_RANGE=b.worldRestart().translation("overload_sim.configuration.laser.constraintRange").comment("Constraint matrix output travel distance in blocks. Reenter the world to apply.").defineInRange("constraintRange",6,1,64);
        LASER_DEFLECTOR_RANGE=b.worldRestart().translation("overload_sim.configuration.laser.deflectorRange").comment("Deflector output travel distance in blocks. Reenter the world to apply.").defineInRange("deflectorRange",6,1,64);
        LASER_BOOST_MINIMUM=b.worldRestart().translation("overload_sim.configuration.laser.boostMinimum").comment("Minimum selectable enhancement FE. Effective minimum is clamped to maximum. Reenter the world to apply.").defineInRange("boostMinimum",100000L,1L,1000000000L);
        LASER_BOOST_MAXIMUM=b.worldRestart().translation("overload_sim.configuration.laser.boostMaximum").comment("Maximum selectable enhancement FE. Reenter the world to apply.").defineInRange("boostMaximum",9000000L,1L,1000000000L);
        LASER_BOOST_STEP=b.worldRestart().translation("overload_sim.configuration.laser.boostStep").comment("Enhancement slider step in FE. Reenter the world to apply.").defineInRange("boostStep",100000L,1L,1000000000L);
        LASER_BURST_MULTIPLIER=b.worldRestart().translation("overload_sim.configuration.laser.burstMultiplier").comment("Beam multiplier of each burst matrix. Reenter the world to apply.").defineInRange("burstMultiplier",2,1,64);
        LASER_DAMAGE_INTERVAL=b.worldRestart().translation("overload_sim.configuration.laser.damageIntervalTicks").comment("Ticks between beam damage checks. Reenter the world to apply.").defineInRange("damageIntervalTicks",2,1,200);
        LASER_MAX_DAMAGE=b.worldRestart().translation("overload_sim.configuration.laser.maxDamage").comment("Maximum damage per beam hit. Reenter the world to apply.").defineInRange("maxDamage",1000000D,0D,1000000D);
        b.pop();
        b.translation("overload_sim.configuration.core").comment("坍缩核心").push("core");
        CORE_ACTIVATION_THRESHOLD=b.worldRestart().translation("overload_sim.configuration.core.activationThreshold").comment("Minimum incoming 20-tick laser energy needed for particle production. Particle conversion cost remains fixed. Reenter the world to apply.").defineInRange("activationThreshold",1000000000L,1L,1000000000000000L);
        b.pop();
        b.translation("overload_sim.configuration.receiver").comment("粒子约束器").push("receiver");
        RECEIVER_ENERGY_CAPACITY=b.worldRestart().translation("overload_sim.configuration.receiver.energyCapacity").comment("FE output storage capacity; existing FE is retained. Reenter the world to apply.").defineInRange("energyCapacity",1024000000000000000L,1L,1024000000000000000L);
        RECEIVER_PARTICLE_CAPACITY=b.worldRestart().translation("overload_sim.configuration.receiver.particleCapacity").comment("Stored particle capacity; lowering preserves existing stock without an explosion. Reenter the world to apply.").defineInRange("particleCapacity",125000L,1L,1000000000L);
        b.pop();
        b.translation("overload_sim.configuration.explosion").comment("约束器爆炸").push("explosion");
        EXPLOSION_RADIUS=b.worldRestart().translation("overload_sim.configuration.explosion.radius").comment("Spherical explosion radius in blocks; loaded chunks only. Explosion enable switch remains in overload_sim-laser.toml. Reenter the world to apply.").defineInRange("radius",12,1,64);
        b.pop();
        SPEC=b.build();
    }
    private MachineSettings() {}
    public static int intValue(ModConfigSpec.IntValue value){return SPEC.isLoaded()?value.get():value.getDefault();}
    public static long longValue(ModConfigSpec.LongValue value){return SPEC.isLoaded()?value.get():value.getDefault();}
    public static double doubleValue(ModConfigSpec.DoubleValue value){return SPEC.isLoaded()?value.get():value.getDefault();}
    public static long beamLimit(){return longValue(LASER_MAX_BEAM_FE);}
    public static long boostMaximum(){return longValue(LASER_BOOST_MAXIMUM);}
    public static long boostMinimum(){return Math.min(longValue(LASER_BOOST_MINIMUM),boostMaximum());}
    public static long boostStep(){return Math.min(longValue(LASER_BOOST_STEP),Math.max(1,boostMaximum()-boostMinimum()));}
    public static long amplify(long power){int factor=intValue(LASER_BURST_MULTIPLIER);if(power<0||power>beamLimit()/factor)throw new IllegalArgumentException("beam exceeds configured limit");return power*factor;}
}
