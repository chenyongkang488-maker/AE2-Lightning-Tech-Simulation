package dev.overloadsim.gametest;

import dev.overloadsim.ModContent;
import dev.overloadsim.OverloadSimulation;
import dev.overloadsim.config.MachineSettings;
import dev.overloadsim.laser.*;
import dev.overloadsim.tool.CoilEnergy;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(OverloadSimulation.ID)
@PrefixGameTestTemplate(false)
public final class ExpandedCapacityGameTests {
    @GameTest(template="laser_empty",batch="configuration_laser")
    public static void doubledReceiverCapacitySurvivesSaveAndLoweringWithoutOverflow(GameTestHelper h){
        var host=LaserMachineGameTests.build(h,h.absolutePos(new BlockPos(12,8,12)),LaserKind.RECEIVER,Direction.NORTH);
        with(Map.of(MachineSettings.RECEIVER_ENERGY_CAPACITY,2_048_000_000_000_000_000L,MachineSettings.RECEIVER_PARTICLE_CAPACITY,2_000_000_000L),()->{
            host.checkStructure();host.energy().load(2_048_000_000_000_000_000L);host.particles().load(2_000_000_000L);
            var saved=host.saveMachine(h.getLevel().registryAccess());host.loadMachine(saved,h.getLevel().registryAccess());host.checkStructure();
            h.assertTrue(host.energy().stored()==2_048_000_000_000_000_000L&&host.particles().stored()==2_000_000_000L,"doubled FE and particle stock survives actual machine NBT restoration");
            h.assertTrue(host.energy().insert(1,false)==0,"full upper-bound FE buffer rejects additional input");
            with(Map.of(MachineSettings.RECEIVER_ENERGY_CAPACITY,1000L,MachineSettings.RECEIVER_PARTICLE_CAPACITY,125_000L),()->{
                host.loadMachine(saved,h.getLevel().registryAccess());host.checkStructure();
                h.assertTrue(host.energy().stored()==2_048_000_000_000_000_000L&&host.particles().stored()==2_000_000_000L,"lowering limits retains the full newly supported stock");
                h.assertTrue(!host.acceptParticles(1)&&!host.explosionTriggered(),"over-limit old stock refuses new particles without a containment failure");
            });
        });
        h.succeed();
    }
    @GameTest(template="empty",batch="configuration_coil")
    public static void fortyBillionCoilFeRoundTripsThroughRealItemCodec(GameTestHelper h){
        with(Map.of(MachineSettings.COIL_BASE_CAPACITY,40_000_000_000L,MachineSettings.COIL_CAPACITY_LIMIT,40_000_000_000L),()->{
            var coil=new ItemStack(ModContent.COIL.get());coil.set(ModContent.COIL_FE.get(),40_000_000_000L);
            var restored=ItemStack.parse(h.getLevel().registryAccess(),coil.save(h.getLevel().registryAccess())).orElseThrow();
            h.assertTrue(CoilEnergy.read(restored)==40_000_000_000L,"40G FE survives data-component serialization");
            h.assertTrue(CoilEnergy.INSTANCE.asEnergyStorage(restored).getEnergyStored()==Integer.MAX_VALUE,"native integer capability remains bounded while long stock is retained");
        });h.succeed();
    }
    @GameTest(template="laser_empty",batch="configuration_laser")
    public static void twoBillionEnhancementChoiceSurvivesPersistence(GameTestHelper h){
        with(Map.of(MachineSettings.LASER_BOOST_MAXIMUM,2_000_000_000L),()->{
            var host=LaserMachineGameTests.build(h,h.absolutePos(new BlockPos(8,3,8)),LaserKind.ENHANCEMENT,Direction.WEST);
            host.setBoost(2_000_000_000L);var saved=host.saveMachine(h.getLevel().registryAccess());host.loadMachine(saved,h.getLevel().registryAccess());
            h.assertTrue(host.boost()==2_000_000_000L,"a newly permitted enhancement choice is not truncated by saved-state validation");
        });h.succeed();
    }
    private static void with(Map<ModConfigSpec.ConfigValue<?>,Object> values,Runnable action){
        var old=new java.util.LinkedHashMap<ModConfigSpec.ConfigValue<?>,Object>();values.forEach((k,v)->old.put(k,k.get()));
        try{values.forEach(ExpandedCapacityGameTests::set);action.run();}finally{old.forEach(ExpandedCapacityGameTests::set);}
    }
    @SuppressWarnings({"rawtypes","unchecked"}) private static void set(ModConfigSpec.ConfigValue value,Object v){value.set(v);value.clearCache();}
}
