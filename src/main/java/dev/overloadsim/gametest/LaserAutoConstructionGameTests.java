package dev.overloadsim.gametest;

import dev.overloadsim.OverloadSimulation;
import dev.overloadsim.laser.*;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(OverloadSimulation.ID)
@PrefixGameTestTemplate(false)
public final class LaserAutoConstructionGameTests {
    private static ServerPlayer player(GameTestHelper h,BlockPos pos,GameType mode) {
        var p=FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"laser-builder"));
        p.setGameMode(mode);p.getInventory().clearContent();p.setPos(pos.getCenter());return p;
    }
    private static LaserMachineBlockEntity controller(GameTestHelper h,BlockPos pos,LaserKind kind,Direction face) {
        h.getLevel().setBlockAndUpdate(pos,LaserContent.machine(kind).defaultBlockState().setValue(LaserMachineBlock.FACING,face));
        return (LaserMachineBlockEntity)h.getLevel().getBlockEntity(pos);
    }
    @GameTest(template="laser_pedestal_empty") public static void allEightDevicesBuildInAllSixFacings(GameTestHelper h) {
        var pos=h.absolutePos(new BlockPos(12,10,12));var p=player(h,pos,GameType.CREATIVE);
        for(var kind:LaserKind.values())for(var face:Direction.values()) {
            var host=controller(h,pos,kind,face);
            var result=LaserAutoConstruction.build(p,host);
            h.assertTrue(result.successful()&&host.formed(),kind+" forms in "+face+" (result="+result+", status="+host.status()+")");
            if(kind==LaserKind.RECEIVER)h.assertTrue(result.placed()==195&&host.structure().members().size()==441,"new receiver builds both pedestal layers in "+face);
            h.assertTrue(LaserAutoConstruction.build(p,host).placed()==0,"formed repeat never rebuilds "+kind);
            host.invalidateStructure();
            for(var cell:LaserAutoConstruction.plan(host).cells().keySet())h.getLevel().setBlockAndUpdate(cell,Blocks.AIR.defaultBlockState());
        }h.succeed();
    }
    @GameTest(template="laser_empty") public static void receiverCanBeCompletedWithoutResettingItsResources(GameTestHelper h) {
        var pos=h.absolutePos(new BlockPos(8,2,8));var host=controller(h,pos,LaserKind.RECEIVER,Direction.NORTH);var p=player(h,pos,GameType.SURVIVAL);
        var legacy=host.saveMachine(h.getLevel().registryAccess());legacy.remove("ReceiverPedestalLayout");host.loadMachine(legacy,h.getLevel().registryAccess());
        var existing=LaserStructureGeometry.template(LaserKind.RECEIVER,Direction.NORTH).entrySet().stream().filter(e->e.getValue()==LaserStructureGeometry.Part.FRAME).findFirst().orElseThrow().getKey();
        h.getLevel().setBlockAndUpdate(pos.offset(existing),LaserContent.FRAME.get().defaultBlockState());
        p.getInventory().add(new ItemStack(LaserContent.FRAME.get(),39));p.getInventory().add(new ItemStack(LaserContent.GLASS.get(),56));p.getInventory().add(new ItemStack(LaserContent.PORT.get(),1));
        host.energy().load(1234);host.particles().load(4);
        var result=LaserAutoConstruction.build(p,host);
        h.assertTrue(result.successful()&&result.placed()==96&&host.formed(),"existing frame reduces required placements to 96");
        h.assertTrue(p.getInventory().isEmpty()&&host.energy().stored()==1234&&host.particles().stored()==4,"only building materials spent, stored resources untouched");
        host.particles().load(0);h.succeed();
    }
    @GameTest(template="laser_empty") public static void laserBuildMenuRejectsClosedOrReplacedHost(GameTestHelper h) {
        var pos=h.absolutePos(new BlockPos(8,2,8));var host=controller(h,pos,LaserKind.CONSTRAINT,Direction.NORTH);var p=player(h,pos,GameType.CREATIVE);
        var menu=new LaserMenu(9,p.getInventory(),host);p.containerMenu=menu;
        menu.receiveClientAction("laserBuild","null");h.assertTrue(host.formed(),"current menu invokes actual construction");
        host.invalidateStructure();var frame=pos.offset(LaserStructureGeometry.template(LaserKind.CONSTRAINT,Direction.NORTH).entrySet().stream().filter(e->e.getValue()==LaserStructureGeometry.Part.FRAME).findFirst().orElseThrow().getKey());h.getLevel().setBlockAndUpdate(frame,Blocks.AIR.defaultBlockState());
        p.containerMenu=p.inventoryMenu;menu.receiveClientAction("laserBuild","null");h.assertTrue(h.getLevel().getBlockState(frame).isAir(),"closed menu cannot rebuild");
        p.containerMenu=menu;h.getLevel().setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());controller(h,pos,LaserKind.CONSTRAINT,Direction.NORTH);menu.receiveClientAction("laserBuild","null");h.assertTrue(h.getLevel().getBlockState(frame).isAir(),"stale host rejected even at the same location");h.succeed();
    }
}
