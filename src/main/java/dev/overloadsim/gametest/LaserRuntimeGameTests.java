package dev.overloadsim.gametest;

import dev.overloadsim.OverloadSimulation;
import dev.overloadsim.laser.*;
import dev.overloadsim.core.LaserMath;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(OverloadSimulation.ID)
@PrefixGameTestTemplate(false)
public final class LaserRuntimeGameTests {
    @GameTest(template="laser_empty")
    public static void zeroOutputFocusCannotDestroyBlocks(GameTestHelper h){
        var source=LaserMachineGameTests.build(h,h.absolutePos(new BlockPos(3,5,10)),LaserKind.EMITTER,Direction.WEST);
        var focus=LaserMachineGameTests.build(h,h.absolutePos(new BlockPos(9,2,10)),LaserKind.FOCUS,Direction.WEST);
        source.checkStructure();focus.checkStructure();source.energy().load(100_000);
        var stone=focus.center().east(2);h.getLevel().setBlockAndUpdate(stone,Blocks.STONE.defaultBlockState());
        java.util.function.Consumer<dev.overloadsim.api.LaserEvents.BeforeModifier> listener=e->{if(e.position.equals(focus.getBlockPos()))e.setOutputEnergy(0);};
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(listener);
        try{LaserNetwork.processEmitter(source);h.assertTrue(h.getLevel().getBlockState(stone).is(Blocks.STONE),"a zeroed focus output performs no destruction");}finally{net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(listener);}
        source.setEnabled(false);h.succeed();
    }
    @GameTest(template="laser_empty")
    public static void conversionRevalidatesResourcesAfterExtensionEvent(GameTestHelper h){
        var receiver=LaserMachineGameTests.build(h,h.absolutePos(new BlockPos(8,2,8)),LaserKind.RECEIVER,Direction.NORTH);
        receiver.checkStructure();receiver.particles().load(1);receiver.fuelInput().insertItem(0,new ItemStack(Items.NETHER_STAR,2),false);
        var state=receiver.saveMachine(h.getLevel().registryAccess());state.putInt("FuelRemaining",1);state.putInt("ParticleRemaining",1);
        state.putString("ActiveFuel","overload_sim:nether_star");state.putLong("FuelProcessingCredit",10_239_000_000_000L);
        receiver.loadMachine(state,h.getLevel().registryAccess());receiver.checkStructure();
        java.util.function.Consumer<dev.overloadsim.api.LaserEvents.BeforeParticleConversion> listener=e->{if(e.position.equals(receiver.getBlockPos()))receiver.particles().consume(1);};
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(listener);
        try{ParticleConversion.tick(receiver);h.assertTrue(receiver.energy().stored()==0&&receiver.fuelRemaining()==1,"event-drained particle cannot generate free FE or advance the paid clock");}finally{net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(listener);}
        h.succeed();
    }
    @GameTest(template="laser_empty")
    public static void particlesTravelForwardOnly(GameTestHelper h) {
        var core=LaserMachineGameTests.build(h,h.absolutePos(new BlockPos(10,5,10)),LaserKind.CORE,Direction.WEST);
        var receiver=LaserMachineGameTests.build(h,core.getBlockPos().east(6).below(3),LaserKind.RECEIVER,Direction.WEST);
        core.checkStructure();receiver.checkStructure();
        LaserNetwork.sendParticles(core,3,Direction.EAST,h.getLevel().getGameTime());
        h.assertTrue(receiver.particles().stored()==3,"receiver after core accepts forward particles");
        LaserNetwork.sendParticles(core,7,Direction.WEST,h.getLevel().getGameTime());
        h.assertTrue(receiver.particles().stored()==3,"particles never return toward input beam");
        receiver.particles().load(0);
        h.succeed();
    }
    @GameTest(template="laser_empty")
    public static void stoppedEmitterDoesNotPayOrRetainBeam(GameTestHelper h) {
        var emitter=LaserMachineGameTests.build(h,h.absolutePos(new BlockPos(4,5,8)),LaserKind.EMITTER,Direction.WEST);
        emitter.checkStructure();emitter.energy().load(100_000);
        LaserNetwork.processEmitter(emitter);
        h.assertTrue(emitter.energy().stored()==0&&!emitter.segments().isEmpty(),"one actual paid batch emits");
        LaserNetwork.processEmitter(emitter);
        h.assertTrue(emitter.energy().stored()==0&&emitter.segments().isEmpty(),"unpaid second batch emits no beam");
        h.succeed();
    }
    @GameTest(template="laser_empty")
    public static void focusProtectsCoreAndStopsAtBedrock(GameTestHelper h) {
        var source=LaserMachineGameTests.build(h,h.absolutePos(new BlockPos(3,5,10)),LaserKind.EMITTER,Direction.WEST);
        var focus=LaserMachineGameTests.build(h,h.absolutePos(new BlockPos(9,2,10)),LaserKind.FOCUS,Direction.WEST);
        var core=LaserMachineGameTests.build(h,focus.getBlockPos().above(3).east(7),LaserKind.CORE,Direction.WEST);
        source.checkStructure();focus.checkStructure();core.checkStructure();source.energy().load(200_000);
        var path=focus.getBlockPos().above(3).east(2);
        h.getLevel().setBlockAndUpdate(path,Blocks.STONE.defaultBlockState());
        LaserNetwork.processEmitter(source);
        h.assertTrue(h.getLevel().getBlockState(path).isAir()&&!core.isRemoved(),"focus clears breakable path but retains endpoint core");
        h.getLevel().setBlockAndUpdate(path,Blocks.BEDROCK.defaultBlockState());
        LaserNetwork.processEmitter(source);
        h.assertTrue(h.getLevel().getBlockState(path).is(Blocks.BEDROCK),"focus respects unbreakable obstruction");
        h.assertTrue(source.segments().stream().filter(LaserSegment::focused).allMatch(s->s.end().x<=path.getX()+.5),"focus does not render through bedrock");
        h.succeed();
    }
    @GameTest(template="laser_empty")
    public static void conversionPaysItemAndPausesWithoutParticles(GameTestHelper h) {
        var receiver=LaserMachineGameTests.build(h,h.absolutePos(new BlockPos(8,2,8)),LaserKind.RECEIVER,Direction.NORTH);
        receiver.checkStructure();receiver.particles().load(1);
        receiver.fuelInput().insertItem(0,new ItemStack(Items.NETHER_STAR,2),false);
        for(int i=0;i<100;i++)ParticleConversion.tick(receiver);
        h.assertTrue(receiver.fuelInput().getStackInSlot(0).getCount()==1&&receiver.particles().stored()==1&&receiver.energy().stored()==0
            &&receiver.fuelRemaining()==0&&receiver.fuelProcessingCredit()==100_000_000_000L,"one paid window retains fractional throughput without rounding into free FE");
        var state=receiver.saveMachine(h.getLevel().registryAccess());state.putInt("FuelRemaining",1);state.putInt("ParticleRemaining",1);
        state.putLong("FuelProcessingCredit",10_239_000_000_000L);
        receiver.loadMachine(state,h.getLevel().registryAccess());receiver.checkStructure();ParticleConversion.tick(receiver);
        h.assertTrue(receiver.particles().stored()==0&&receiver.energy().stored()==512_000_000_000L&&receiver.fuelProcessingCredit()==0,"exact 1G batch-rate credit converts one whole particle once");
        for(int i=0;i<120;i++)ParticleConversion.tick(receiver);
        h.assertTrue(receiver.fuelInput().getStackInSlot(0).getCount()==1&&receiver.energy().stored()==512_000_000_000L,"no particles pauses and consumes no additional item");
        h.succeed();
    }
    @GameTest(template="laser_demo",timeoutTicks=120)
    public static void completeDemoActuallyGeneratesParticlesAndEnergy(GameTestHelper h) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var player=net.neoforged.neoforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"laser-demo-test"));
        var start=h.absolutePos(new BlockPos(2,2,8));player.setPos(start.getCenter());player.setYRot(-90);player.setXRot(0);player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
        int result=h.getLevel().getServer().getCommands().getDispatcher().execute("overload_sim laser demo",player.createCommandSourceStack().withPermission(4));
        h.assertTrue(result==1,"complete shared-template demo fits an empty loaded region");
        h.runAfterDelay(85,()->{
            var emitter=(LaserMachineBlockEntity)h.getLevel().getBlockEntity(start.above(4).east(8));
            h.assertTrue(emitter!=null&&!emitter.segments().isEmpty(),"complete demo has a paid optical path");
            LaserMachineBlockEntity receiver=null;
            for(var segment:emitter.segments())if(segment.particle()){
                var port=BlockPos.containing(segment.end());var pos=port.below(4);
                if(h.getLevel().getBlockEntity(pos) instanceof LaserMachineBlockEntity r)receiver=r;
            }
            h.assertTrue(receiver!=null&&receiver.energy().stored()>0,"forward receiver actually converts generated particles into FE");
            LaserMachineBlockEntity core=null;
            for(var segment:emitter.segments())if(segment.focused()){
                var candidate=h.getLevel().getBlockEntity(BlockPos.containing(segment.end()));
                if(candidate instanceof LaserMachineBlockEntity c&&c.kind()==LaserKind.CORE)core=c;
            }
            h.assertTrue(core!=null&&core.collapseStartTick()>=0&&core.collapseStartTick()<=h.getLevel().getGameTime()-40,"Repeated network pulses preserve the first animation timestamp");
            emitter.setEnabled(false);
            h.assertTrue(core.collapseStartTick()==-1,"Stopping upstream instantly resets the downstream visual cycle");
            emitter.setEnabled(true);core.acceptCore(LaserMath.CORE_THRESHOLD,Direction.EAST,h.getLevel().getGameTime());
            h.assertTrue(core.collapseStartTick()==h.getLevel().getGameTime(),"A rapid upstream restart can trigger animation again");
            receiver.particles().load(0);receiver.setEnabled(false);emitter.setEnabled(false);h.succeed();
        });
    }
}
