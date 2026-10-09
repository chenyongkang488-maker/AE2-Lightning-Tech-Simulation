package dev.overloadsim.gametest;

import dev.overloadsim.OverloadSimulation;
import dev.overloadsim.api.LaserEvents;
import dev.overloadsim.laser.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(OverloadSimulation.ID)
@PrefixGameTestTemplate(false)
public final class LaserOpticalCacheGameTests {
    @GameTest(template="laser_empty")
    public static void unchangedDeflectorReusesValidatedStructure(GameTestHelper h){
        var host=LaserMachineGameTests.build(h,h.absolutePos(new BlockPos(15,5,15)),LaserKind.DEFLECTOR,Direction.NORTH);
        host.checkStructure();var validated=host.structure();
        for(int i=0;i<10;i++)host.checkStructure();
        h.assertTrue(host.structure()==validated,"unchanged optical geometry is not rediscovered by repeated checks");
        var frame=host.structure().members().stream().filter(p->h.getLevel().getBlockState(p).is(LaserContent.FRAME.get())).findFirst().orElseThrow();
        h.getLevel().setBlockAndUpdate(frame,net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());host.checkStructure();
        h.assertTrue(!host.formed(),"automation block replacement invalidates cached geometry");h.succeed();
    }
    @GameTest(template="laser_empty",timeoutTicks=80)
    public static void stableDeflectorRenewalNeverPublishesEmptySourceBeam(GameTestHelper h){
        var matrix=LaserMachineGameTests.build(h,h.absolutePos(new BlockPos(15,5,15)),LaserKind.DEFLECTOR,Direction.NORTH);
        var source=LaserMachineGameTests.build(h,h.absolutePos(new BlockPos(5,8,15)),LaserKind.EMITTER,Direction.WEST);
        matrix.setSideMode(Direction.WEST,LaserMachineBlockEntity.SIDE_INPUT);matrix.setSideMode(Direction.EAST,LaserMachineBlockEntity.SIDE_OUTPUT);
        // Deferred native onLoad legitimately clears a newly created controller's initial visual state.
        h.runAfterDelay(3,()->{
        source.energy().load(1_000_000);LaserNetwork.processEmitter(source);
        h.assertTrue(!source.segments().isEmpty(),"first paid batch produces visible beams");
        matrix.setSideMode(Direction.WEST,LaserMachineBlockEntity.SIDE_INPUT);source.setEnabled(true);
        h.assertTrue(!source.segments().isEmpty(),"reapplying unchanged controls does not stop a working optical path");
        boolean[] vanished={false};int[] calls={0};
        java.util.function.Consumer<LaserEvents.BeforeModifier> inspect=e->{if(e.level==h.getLevel()&&e.position.equals(matrix.getBlockPos())){calls[0]++;if(source.segments().isEmpty())vanished[0]=true;}};
        NeoForge.EVENT_BUS.addListener(inspect);
        h.runAfterDelay(45,()->{try{
            h.assertTrue(calls[0]>=2&&!vanished[0],"later paid batches retain the previous beam until the replacement is ready; callbacks="+calls[0]+" vanished="+vanished[0]);
            h.assertTrue(!source.segments().isEmpty(),"beam remains visible across multiple native settlement ticks");
        }finally{NeoForge.EVENT_BUS.unregister(inspect);source.setEnabled(false);}h.succeed();});
        });
    }
}
