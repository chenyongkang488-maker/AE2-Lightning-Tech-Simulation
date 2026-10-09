package dev.overloadsim.gametest;

import dev.overloadsim.OverloadSimulation;
import dev.overloadsim.api.LaserEvents;
import dev.overloadsim.config.MachineSettings;
import dev.overloadsim.core.LaserMath;
import dev.overloadsim.laser.*;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(OverloadSimulation.ID)
@PrefixGameTestTemplate(false)
public final class LaserConfigurationGameTests {
    @GameTest(template="laser_empty",batch="configuration_laser")
    public static void worldRestartSettingsKeepOldCacheUntilTheWorldLifecycleResetsIt(GameTestHelper h){
        var setting=MachineSettings.EXPLOSION_RADIUS;int old=setting.get();
        try{setting.set(old==20?21:20);h.assertTrue(setting.get()==old,"WORLD changes leave running-world cached values intact");
            MachineSettings.SPEC.resetCaches(ModConfigSpec.RestartType.WORLD);
            h.assertTrue(setting.get()==(old==20?21:20),"next-world cache reset adopts saved configuration");
        }finally{setting.set(old);setting.clearCache();}h.succeed();
    }
    @GameTest(template="laser_empty",batch="configuration_laser")
    public static void raisingLimitsDoesNotLegitimizeMalformedLegacyStock(GameTestHelper h){
        with(Map.of(MachineSettings.LASER_MACHINE_ENERGY_CAPACITY,2_000_000_000L,MachineSettings.RECEIVER_PARTICLE_CAPACITY,250_000L),()->{
            var host=LaserMachineGameTests.build(h,h.absolutePos(new BlockPos(8,3,8)),LaserKind.EMITTER,Direction.WEST);
            var tag=host.saveMachine(h.getLevel().registryAccess());tag.remove("EnergyCapacity");tag.remove("ParticleCapacity");
            tag.putLong("Energy",Long.MAX_VALUE);tag.putLong("Particles",Long.MAX_VALUE);host.loadMachine(tag,h.getLevel().registryAccess());
            h.assertTrue(host.energy().capacity()==2_000_000_000L&&host.energy().stored()==LaserMachineBlockEntity.MACHINE_FE_CAPACITY,"raised limit leaves malformed old energy clamped to its historical save bound");
            h.assertTrue(host.particles().stored()==LaserMachineBlockEntity.PARTICLE_CAPACITY,"raised limit cannot mint old particles");
        });h.succeed();
    }
    @GameTest(template="laser_empty",batch="configuration_laser")
    public static void configuredBurstMultiplierIsPaidAndUsedInTheRealOpticalPath(GameTestHelper h){
        with(Map.of(MachineSettings.LASER_BURST_MULTIPLIER,3),()->{
            var emitter=LaserMachineGameTests.build(h,h.absolutePos(new BlockPos(3,5,10)),LaserKind.EMITTER,Direction.WEST);
            var burst=LaserMachineGameTests.build(h,h.absolutePos(new BlockPos(9,2,10)),LaserKind.BURST,Direction.WEST);
            emitter.checkStructure();burst.checkStructure();emitter.energy().load(100_000);burst.setExtremeLightning(64);
            LaserNetwork.processEmitter(emitter);
            h.assertTrue(burst.currentPower()==300_000&&emitter.energy().stored()==0&&burst.extremeLightning()==0,"real pulse matrix multiplies by configured three and pays one batch");
        });h.succeed();
    }
    @GameTest(template="laser_empty",batch="configuration_laser")
    public static void lowerLaserCapacityRetainsOldEnergyAndNeverInsertsNegative(GameTestHelper h){
        var pos=h.absolutePos(new BlockPos(8,3,8));
        var old=LaserMachineGameTests.build(h,pos,LaserKind.EMITTER,Direction.WEST);
        old.energy().load(200);var tag=old.saveMachine(h.getLevel().registryAccess());
        with(Map.of(MachineSettings.LASER_MACHINE_ENERGY_CAPACITY,100L),()->{
            var restored=new LaserMachineBlockEntity(pos,old.getBlockState());restored.setLevel(h.getLevel());
            restored.loadMachine(tag,h.getLevel().registryAccess());
            h.assertTrue(restored.energy().capacity()==100&&restored.energy().stored()==200,"new operating limit keeps previously stored FE");
            h.assertTrue(restored.energy().insert(5,true)==0&&restored.energy().insert(5,false)==0,"above-capacity insertion is zero, never negative");
            var again=restored.saveMachine(h.getLevel().registryAccess());restored.loadMachine(again,h.getLevel().registryAccess());
            h.assertTrue(restored.energy().stored()==200&&restored.energy().extract(150,false)==150&&restored.energy().insert(80,false)==50,"extraction and later insert honor reduced capacity");
        });h.succeed();
    }
    @GameTest(template="laser_empty",batch="configuration_laser")
    public static void receiverCapacityReductionPreservesParticlesWithoutNewExplosion(GameTestHelper h){
        var pos=h.absolutePos(new BlockPos(14,12,14));
        var old=LaserMachineGameTests.build(h,pos,LaserKind.RECEIVER,Direction.WEST);old.checkStructure();old.particles().load(200);
        var tag=old.saveMachine(h.getLevel().registryAccess());
        with(Map.of(MachineSettings.RECEIVER_PARTICLE_CAPACITY,100L,MachineSettings.RECEIVER_ENERGY_CAPACITY,1000L),()->{
            // Replace the host while retaining the actual formed shell.
            h.getLevel().removeBlockEntity(pos);var restored=new LaserMachineBlockEntity(pos,old.getBlockState());
            h.getLevel().setBlockEntity(restored);restored.loadMachine(tag,h.getLevel().registryAccess());restored.checkStructure();
            h.assertTrue(restored.formed()&&restored.particles().capacity()==100&&restored.particles().stored()==200,"charged containment keeps old particle stock above a new limit");
            h.assertTrue(!restored.acceptParticles(1)&&!restored.explosionTriggered()&&restored.particles().stored()==200,"lowering the limit cannot cause a new overflow explosion");
            h.assertTrue(restored.energy().capacity()==1000,"receiver output capacity is configurable independently");
        });h.succeed();
    }
    @GameTest(template="laser_empty",batch="configuration_laser")
    public static void configuredCoreGateChecksActualIncomingBatch(GameTestHelper h){
        var core=LaserMachineGameTests.build(h,h.absolutePos(new BlockPos(8,4,8)),LaserKind.CORE,Direction.WEST);core.checkStructure();
        with(Map.of(MachineSettings.CORE_ACTIVATION_THRESHOLD,2_000_000_000L),()->{
            core.acceptCore(1_000_000_000L,Direction.EAST,h.getLevel().getGameTime());
            h.assertTrue(core.coreRemainder()==0,"below configured activation threshold creates no paid remainder");
            core.acceptCore(2_000_000_000L,Direction.EAST,h.getLevel().getGameTime());
            h.assertTrue(core.coreRemainder()==2_000_000_000L,"threshold equality activates the core");
        });h.succeed();
    }
    @GameTest(template="laser_empty",batch="configuration_laser")
    public static void configuredEmitterRangeAndSoftLimitAreEnforced(GameTestHelper h){
        var emitter=LaserMachineGameTests.build(h,h.absolutePos(new BlockPos(8,3,8)),LaserKind.EMITTER,Direction.WEST);emitter.checkStructure();emitter.energy().load(1_000_000);
        with(Map.of(MachineSettings.LASER_EMITTER_RANGE,2),()->{
            LaserNetwork.processEmitter(emitter);
            h.assertTrue(!emitter.segments().isEmpty()&&emitter.segments().getFirst().end().distanceTo(emitter.segments().getFirst().start())==2,"emitter ray uses configured range");
        });
        var before=emitter.energy().stored();
        with(Map.of(MachineSettings.LASER_MAX_BEAM_FE,1L),()->{
            LaserNetwork.processEmitter(emitter);
            h.assertTrue(emitter.currentPower()==0&&emitter.energy().stored()==before,"over-limit source is stopped before paying FE");
        });h.succeed();
    }
    @GameTest(template="laser_empty",batch="configuration_laser")
    public static void enhancementChoiceAcceptsConfiguredRangeAndRejectsInvalidSteps(GameTestHelper h){
        var host=LaserMachineGameTests.build(h,h.absolutePos(new BlockPos(8,3,8)),LaserKind.ENHANCEMENT,Direction.WEST);
        with(Map.of(MachineSettings.LASER_BOOST_MINIMUM,5L,MachineSettings.LASER_BOOST_MAXIMUM,35L,MachineSettings.LASER_BOOST_STEP,10L),()->{
            host.setBoost(25);h.assertTrue(host.boost()==25,"configured low enhancement value is accepted");
            host.setBoost(26);h.assertTrue(host.boost()==25,"non-step values cannot bypass slider validation");
            host.setBoost(45);h.assertTrue(host.boost()==25,"above configured maximum cannot be selected");
        });h.succeed();
    }
    @GameTest(template="laser_empty",batch="configuration_laser")
    public static void explosionEventStartsWithConfiguredSphericalRadius(GameTestHelper h){
        var host=LaserMachineGameTests.build(h,h.absolutePos(new BlockPos(14,12,14)),LaserKind.RECEIVER,Direction.WEST);host.checkStructure();host.particles().load(1);
        int[] radius={-1};Consumer<LaserEvents.BeforeExplosion> listener=e->{if(e.position.equals(host.getBlockPos())){radius[0]=e.radiusBlocks();e.setCanceled(true);}};
        NeoForge.EVENT_BUS.addListener(listener);
        try{with(Map.of(MachineSettings.EXPLOSION_RADIUS,20),()->LaserExplosion.trigger(host));}
        finally{NeoForge.EVENT_BUS.unregister(listener);}
        h.assertTrue(radius[0]==20&&!host.explosionTriggered(),"configuration is supplied to the cancellable spherical explosion event");h.succeed();
    }
    private static void with(Map<ModConfigSpec.ConfigValue<?>,Object> values,Runnable action){
        var old=new java.util.LinkedHashMap<ModConfigSpec.ConfigValue<?>,Object>();values.forEach((k,v)->old.put(k,k.get()));
        try{values.forEach(LaserConfigurationGameTests::set);action.run();}finally{old.forEach(LaserConfigurationGameTests::set);}
    }
    @SuppressWarnings({"rawtypes","unchecked"}) private static void set(ModConfigSpec.ConfigValue value,Object v){value.set(v);value.clearCache();}
}
