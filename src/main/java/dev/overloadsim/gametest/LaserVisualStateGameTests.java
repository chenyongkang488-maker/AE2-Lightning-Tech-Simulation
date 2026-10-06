package dev.overloadsim.gametest;

import dev.overloadsim.OverloadSimulation;
import dev.overloadsim.core.LaserMath;
import dev.overloadsim.laser.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(OverloadSimulation.ID)
@PrefixGameTestTemplate(false)
public final class LaserVisualStateGameTests {
    @GameTest(template="laser_empty")
    public static void coreContinuousInputAndRestartClock(GameTestHelper h){
        var core=LaserMachineGameTests.build(h,h.absolutePos(new BlockPos(8,3,8)),LaserKind.CORE,Direction.NORTH);
        core.checkStructure();long now=h.getLevel().getGameTime();
        h.assertTrue(core.acceptCore(LaserMath.PARTICLE_COST,Direction.SOUTH,now)>0,"Core accepts a valid first pulse");
        h.assertTrue(core.collapseStartTick()==now,"First accepted work starts animation");
        core.acceptCore(LaserMath.CORE_THRESHOLD,Direction.SOUTH,now+20);
        h.assertTrue(core.collapseStartTick()==now,"Continuous pulses do not restart animation or sound");
        var saved=core.saveMachine(h.getLevel().registryAccess());core.loadMachine(saved,h.getLevel().registryAccess());core.checkStructure();
        h.assertTrue(core.collapseStartTick()==now,"Visual timestamp persists exactly");
        core.setEnabled(false);core.setEnabled(true);core.acceptCore(LaserMath.CORE_THRESHOLD,Direction.SOUTH,now+21);
        h.assertTrue(core.collapseStartTick()==now+21,"Explicit restart plays a fresh animation");
        core.acceptCore(LaserMath.CORE_THRESHOLD,Direction.SOUTH,now+44);
        h.assertTrue(core.collapseStartTick()==now+44,"Expired upstream input restarts animation");
        h.succeed();
    }

    @GameTest(template="laser_empty")
    public static void cancelledCoreWorkCannotStartVisuals(GameTestHelper h){
        var core=LaserMachineGameTests.build(h,h.absolutePos(new BlockPos(8,3,8)),LaserKind.CORE,Direction.NORTH);core.checkStructure();
        java.util.function.Consumer<dev.overloadsim.api.LaserEvents.BeforeCoreEmission> cancel=e->{if(e.position.equals(core.getBlockPos()))e.setCanceled(true);};
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(cancel);
        try{core.acceptCore(LaserMath.CORE_THRESHOLD,Direction.SOUTH,h.getLevel().getGameTime());
            h.assertTrue(core.collapseStartTick()==-1&&core.currentPower()==0,"Cancelled work emits neither animation nor current strength");}
        finally{net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(cancel);}
        h.succeed();
    }

    @GameTest(template="laser_empty")
    public static void matrixMenuShowsCurrentPulseAndZeroAfterExpiry(GameTestHelper h){
        var ring=LaserMachineGameTests.build(h,h.absolutePos(new BlockPos(8,3,8)),LaserKind.FOCUS,Direction.NORTH);ring.checkStructure();
        var tag=ring.saveMachine(h.getLevel().registryAccess());
        tag.putLong("LastPower",1_234_567L);tag.putLong("ActiveUntil",h.getLevel().getGameTime()+10);
        ring.loadMachine(tag,h.getLevel().registryAccess());ring.checkStructure();
        var menu=new LaserMenu(1,h.makeMockPlayer(net.minecraft.world.level.GameType.CREATIVE).getInventory(),ring);menu.broadcastChanges();
        h.assertTrue(menu.power==1_234_567L,"Menu reports exact pulse FE rather than the local energy buffer");
        tag.putLong("ActiveUntil",0);ring.loadMachine(tag,h.getLevel().registryAccess());ring.checkStructure();menu.broadcastChanges();
        h.assertTrue(menu.power==0,"Expired pulse strength is zero");h.succeed();
    }
}
