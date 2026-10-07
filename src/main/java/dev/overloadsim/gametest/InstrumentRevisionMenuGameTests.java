package dev.overloadsim.gametest;

import com.mojang.authlib.GameProfile;
import dev.overloadsim.OverloadSimulation;
import dev.overloadsim.laser.*;
import dev.overloadsim.workstation.*;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(OverloadSimulation.ID)
@PrefixGameTestTemplate(false)
public final class InstrumentRevisionMenuGameTests {
    private static ServerPlayer player(GameTestHelper h,BlockPos position) {
        var player=FakePlayerFactory.get(h.getLevel(),new GameProfile(UUID.randomUUID(),"instrument-revision"));
        player.getInventory().clearContent();player.setPos(position.getCenter());return player;
    }
    // The new action is intentionally absent in the RED baseline; rejection still leaves a clear state assertion.
    private static void action(appeng.menu.AEBaseMenu menu,String name,String value) {
        try { menu.receiveClientAction(name,value); } catch (IllegalArgumentException missingAction) { }
    }
    @GameTest(template="laser_empty") public static void workstationUsesCommonLaserParts(GameTestHelper h) {
        var origin=h.absolutePos(new BlockPos(10,2,10));var facing=Direction.NORTH;
        for(int x=0;x<7;x++)for(int y=0;y<7;y++)for(int z=0;z<7;z++) {
            var position=WorkstationStructure.position(origin,facing,x,y,z);
            int walls=(x==0||x==6?1:0)+(z==0||z==6?1:0);
            var block=y==0||y==6||z==6||walls==2?LaserContent.FRAME.get():walls==1?LaserContent.GLASS.get():Blocks.AIR;
            if(x==3&&y==3&&z==6)block=LaserContent.PORT.get();
            if(x==3&&y==0&&z==0)block=WorkstationContent.CONTROLLER.get();
            var state=block.defaultBlockState();if(block==WorkstationContent.CONTROLLER.get())state=state.setValue(HighEnergyWorkstationBlock.FACING,facing);
            h.getLevel().setBlockAndUpdate(position,state);
        }
        var host=(HighEnergyWorkstationBlockEntity)h.getLevel().getBlockEntity(origin);host.checkStructure();
        h.assertTrue(host.formed(),"workstation forms from common laser frame, glass and optical port");h.succeed();
    }
    @GameTest(template="laser_empty") public static void sharedRoofBlocksLightAndFrameRemovalInvalidatesImmediately(GameTestHelper h) {
        var host=WorkstationGameTests.build(h,Direction.NORTH);
        var roof=WorkstationStructure.position(host.getBlockPos(),host.facing(),3,6,3);
        var state=h.getLevel().getBlockState(roof);
        h.assertTrue(state.is(LaserContent.FRAME.get())&&state.getLightBlock(h.getLevel(),roof)==15
                &&!state.propagatesSkylightDown(h.getLevel(),roof),"shared solid roof is opaque to block and sky light");
        h.getLevel().setBlockAndUpdate(roof,Blocks.AIR.defaultBlockState());
        h.assertTrue(!host.formed(),"shared frame removal revokes the workstation claim immediately");h.succeed();
    }
    @GameTest(template="laser_empty") public static void dedicatedPartsAreRetiredAndOldStructureMigrates(GameTestHelper h) {
        var host=WorkstationGameTests.build(h,Direction.EAST);host.inputs().insert(0,new ItemStack(Items.IRON_INGOT),777,false);
        host.checkStructure();
        h.assertTrue(WorkstationContent.playerBlocks().size()==1,"only the controller remains in the creative catalogue");
        h.assertTrue(h.getLevel().getBlockState(host.receiver()).is(LaserContent.PORT.get()),"old receiving block becomes common port");
        h.assertTrue(h.getLevel().getBlockState(WorkstationStructure.position(host.getBlockPos(),host.facing(),0,0,0)).is(LaserContent.FRAME.get()),"old roof/floor frame becomes common frame");
        h.assertTrue(host.formed()&&host.inputs().count(0)==777,"migration retains formation and resources");h.succeed();
    }
    @GameTest(template="laser_empty") public static void enhancementAcceptsExactSliderValueAndRejectsInvalidPackets(GameTestHelper h) {
        var position=h.absolutePos(new BlockPos(10,2,10));var host=LaserMachineGameTests.build(h,position,LaserKind.ENHANCEMENT,Direction.NORTH);host.checkStructure();
        var player=player(h,position);var menu=new LaserMenu(21,player.getInventory(),host);player.containerMenu=menu;
        action(menu,"laserSetBoost","6500000");h.assertTrue(host.boost()==6_500_000,"slider sends the exact absolute enhancement value");
        for(String invalid:new String[]{"0","100001","9000001","9223372036854775807","-100000","null"})action(menu,"laserSetBoost",invalid);
        h.assertTrue(host.boost()==6_500_000,"invalid range, step and null slider messages are ignored");
        player.containerMenu=player.inventoryMenu;action(menu,"laserSetBoost","200000");
        h.assertTrue(host.boost()==6_500_000,"stale menu cannot change enhancement");h.succeed();
    }
    private record FluidSession(HighEnergyWorkstationBlockEntity host,ServerPlayer player,HighEnergyWorkstationMenu menu) {}
    private static FluidSession fluidSession(GameTestHelper h) {
        var host=WorkstationGameTests.build(h,Direction.NORTH);var player=player(h,host.getBlockPos());
        var menu=new HighEnergyWorkstationMenu(22,player.getInventory(),host);player.containerMenu=menu;
        return new FluidSession(host,player,menu);
    }
    private static void seedInput(FluidSession s) {
        s.host.setSideMode(Direction.UP,3);s.host.fluidAutomation(Direction.UP).fill(new FluidStack(Fluids.WATER,1000),IFluidHandler.FluidAction.EXECUTE);s.host.setSideMode(Direction.UP,0);
    }
    private static void seedOutput(GameTestHelper h,FluidSession s) {
        var tag=s.host.saveMachine(h.getLevel().registryAccess());var tank=new FluidTank(HighEnergyWorkstationBlockEntity.TANK_CAPACITY);tank.fill(new FluidStack(Fluids.WATER,1000),IFluidHandler.FluidAction.EXECUTE);
        tag.put("OutputFluid",tank.writeToNBT(h.getLevel().registryAccess(),new CompoundTag()));s.host.loadMachine(tag,h.getLevel().registryAccess());s.host.checkStructure();
    }
    @GameTest(template="laser_empty") public static void inputFluidSlotEmptiesBucketWithAllFacesClosed(GameTestHelper h) {
        var s=fluidSession(h);s.menu.setCarried(new ItemStack(Items.WATER_BUCKET));action(s.menu,"workstationFluid","0");
        h.assertTrue(s.host.fluidInput().getAmount()==1000&&s.menu.getCarried().is(Items.BUCKET),"manual input slot accepts a real bucket independently of automation faces");h.succeed();
    }
    @GameTest(template="laser_empty") public static void stackedBucketsFillOneAndFullInventoryRejectsWithoutLoss(GameTestHelper h) {
        var s=fluidSession(h);seedInput(s);s.menu.setCarried(new ItemStack(Items.BUCKET,16));
        for(int i=0;i<s.player.getInventory().items.size();i++)s.player.getInventory().setItem(i,new ItemStack(Items.COBBLESTONE,64));
        action(s.menu,"workstationFluid","0");h.assertTrue(s.host.fluidInput().getAmount()==1000&&s.menu.getCarried().getCount()==16,"no destination for filled bucket preserves fluid and all empty buckets");
        s.player.getInventory().clearContent();action(s.menu,"workstationFluid","0");
        int filled=0;for(var stack:s.player.getInventory().items)if(stack.is(Items.WATER_BUCKET))filled+=stack.getCount();
        h.assertTrue(s.host.fluidInput().isEmpty()&&s.menu.getCarried().is(Items.BUCKET)&&s.menu.getCarried().getCount()==15&&filled==1,"one filled bucket and fifteen empty buckets conserve the stack");h.succeed();
    }
    @GameTest(template="laser_empty") public static void outputFluidSlotDrainsButCannotBeFilled(GameTestHelper h) {
        var s=fluidSession(h);seedOutput(h,s);s.menu.setCarried(new ItemStack(Items.LAVA_BUCKET));action(s.menu,"workstationFluid","1");
        h.assertTrue(s.host.fluidOutput().getAmount()==1000&&s.host.fluidOutput().getFluid()==Fluids.WATER&&s.menu.getCarried().is(Items.LAVA_BUCKET),"output rejects reverse insertion");
        s.menu.setCarried(new ItemStack(Items.BUCKET));action(s.menu,"workstationFluid","1");
        h.assertTrue(s.host.fluidOutput().isEmpty()&&s.menu.getCarried().is(Items.WATER_BUCKET),"output drains into a normal fluid container");h.succeed();
    }
    @GameTest(template="laser_empty") public static void staleFluidMenuAndInvalidSlotCannotMoveAnything(GameTestHelper h) {
        var s=fluidSession(h);s.menu.setCarried(new ItemStack(Items.WATER_BUCKET));
        action(s.menu,"workstationFluid","-1");action(s.menu,"workstationFluid","2");
        s.player.containerMenu=s.player.inventoryMenu;action(s.menu,"workstationFluid","0");
        h.assertTrue(s.host.fluidInput().isEmpty()&&s.menu.getCarried().is(Items.WATER_BUCKET),"invalid and stale fluid actions preserve both tank and container");h.succeed();
    }
}
