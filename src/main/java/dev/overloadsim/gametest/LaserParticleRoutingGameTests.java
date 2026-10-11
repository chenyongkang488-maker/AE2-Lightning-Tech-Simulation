package dev.overloadsim.gametest;

import dev.overloadsim.OverloadSimulation;
import dev.overloadsim.api.LaserEvents;
import dev.overloadsim.core.LaserMath;
import dev.overloadsim.laser.*;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(OverloadSimulation.ID)
@PrefixGameTestTemplate(false)
public final class LaserParticleRoutingGameTests {
    private static LaserMachineBlockEntity receiver(GameTestHelper h, BlockPos pos, Set<Direction> ports) {
        var host=LaserMachineGameTests.build(h,pos,LaserKind.RECEIVER,Direction.NORTH);
        h.getLevel().setBlockAndUpdate(host.center().north(3),LaserContent.FRAME.get().defaultBlockState());
        for(var side:ports)h.getLevel().setBlockAndUpdate(host.center().relative(side,3),LaserContent.PORT.get().defaultBlockState());
        host.checkStructure();
        h.assertTrue(host.formed(),"selected horizontal ports form independently of the north-facing controller");
        return host;
    }

    private static void sideDelivery(GameTestHelper h,Direction side) {
        var receiver=receiver(h,h.absolutePos(new BlockPos(16,2,16)),EnumSet.of(side));
        var core=LaserMachineGameTests.build(h,receiver.center().relative(side,6),LaserKind.CORE,side);
        core.checkStructure();
        try {
            h.assertTrue(LaserNetwork.sendParticles(core,3,side.getOpposite(),h.getLevel().getGameTime())==receiver,
                    "particles enter the actual "+side+" port independently of receiver controller facing");
            h.assertTrue(receiver.particles().stored()==3,"the side-port delivery settles the whole batch immediately");
        } finally {receiver.particles().load(0);}
        h.succeed();
    }

    @GameTest(template="laser_empty") public static void particlesEnterSoleEastPort(GameTestHelper h){sideDelivery(h,Direction.EAST);}
    @GameTest(template="laser_empty") public static void particlesEnterSoleWestPort(GameTestHelper h){sideDelivery(h,Direction.WEST);}
    @GameTest(template="laser_empty") public static void particlesEnterSoleSouthPort(GameTestHelper h){sideDelivery(h,Direction.SOUTH);}

    @GameTest(template="laser_empty")
    public static void focusedDeliveryUsesContactedPortAndReplacingItStopsThePath(GameTestHelper h) {
        var source=LaserMachineGameTests.build(h,h.absolutePos(new BlockPos(3,5,10)),LaserKind.EMITTER,Direction.WEST);
        var focus=LaserMachineGameTests.build(h,h.absolutePos(new BlockPos(9,2,10)),LaserKind.FOCUS,Direction.WEST);
        var core=LaserMachineGameTests.build(h,h.absolutePos(new BlockPos(16,5,10)),LaserKind.CORE,Direction.WEST);
        var receiver=receiver(h,h.absolutePos(new BlockPos(22,2,10)),EnumSet.of(Direction.WEST,Direction.SOUTH));
        for(var host:new LaserMachineBlockEntity[]{source,focus,core})host.checkStructure();
        source.energy().load(1_000_000);
        Consumer<LaserEvents.BeforeModifier> boost=event->{if(event.position.equals(focus.getBlockPos()))event.setOutputEnergy(LaserMath.PARTICLE_COST);};
        NeoForge.EVENT_BUS.addListener(boost);
        try {
            LaserNetwork.processEmitter(source);
            var port=receiver.center().west(3);
            var segment=source.segments().stream().filter(LaserSegment::particle).findFirst().orElse(null);
            h.assertTrue(segment!=null&&segment.end().equals(port.getCenter()),"the delivery ends at the contacted west port, not the canonical north face");
            h.assertTrue(receiver.particles().stored()==1,"the contacted side port receives its one real particle");
            receiver.particles().load(0);
            h.getLevel().setBlockAndUpdate(port,LaserContent.FRAME.get().defaultBlockState());
            h.assertTrue(source.segments().isEmpty(),"replacing the contacted port immediately clears the previous active optical path");
            receiver.checkStructure();LaserNetwork.tick(source);
            h.assertTrue(receiver.formed(),"the other selected south port keeps the receiver formed");
            // A batch-boundary tick may legitimately draw a new upstream beam after clearing the old path.
            h.assertTrue(source.segments().stream().noneMatch(LaserSegment::particle),"a renewed upstream beam cannot retain the removed port's particle delivery");
            h.assertTrue(receiver.particles().stored()==0,"a removed optical port receives no further particles");
        } finally {NeoForge.EVENT_BUS.unregister(boost);receiver.particles().load(0);source.setEnabled(false);}
        h.succeed();
    }
}
