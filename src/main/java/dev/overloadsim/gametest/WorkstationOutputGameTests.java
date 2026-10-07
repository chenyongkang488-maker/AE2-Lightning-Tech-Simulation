package dev.overloadsim.gametest;

import dev.overloadsim.OverloadSimulation;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Automatic outputs are opt-in and independent of the retired face configuration. */
@GameTestHolder(OverloadSimulation.ID)
@PrefixGameTestTemplate(false)
public final class WorkstationOutputGameTests {
    @GameTest(template="laser_empty")
    public static void allControllerFacesAcceptInputsAndExposeOnlyResults(GameTestHelper h) {
        var host=WorkstationGameTests.build(h,Direction.NORTH);
        for(var side:Direction.values()) {
            var handler=host.automation(side);
            h.assertTrue(handler.insertItem(0,new ItemStack(Items.IRON_INGOT),false).isEmpty(),"each controller face accepts a material without face setup");
            h.assertTrue(handler.extractItem(0,64,false).isEmpty(),"no controller face extracts input material");
            h.assertTrue(!handler.insertItem(9,new ItemStack(Items.GOLD_INGOT),false).isEmpty(),"no face accepts items into output");
            host.outputs().insert(0,new ItemStack(Items.GOLD_INGOT),1,false);
            h.assertTrue(handler.extractItem(9,1,false).is(Items.GOLD_INGOT),"each face exposes output only");
            var fluids=host.fluidAutomation(side);
            h.assertTrue(fluids.fill(new FluidStack(Fluids.WATER,100),IFluidHandler.FluidAction.EXECUTE)==100,"each face accepts input fluid");
            h.assertTrue(fluids.drain(100,IFluidHandler.FluidAction.EXECUTE).isEmpty(),"input fluid cannot be automatically drained");
        }
        h.assertTrue(host.inputs().count(0)==6&&host.fluidInput().getAmount()==600,"all six input transactions conserve exact materials");
        h.succeed();
    }

    @GameTest(template="laser_empty")
    public static void adjacentOutputToggleNeedsNoFaceSetupAndNeverExportsMaterials(GameTestHelper h) {
        var host=WorkstationGameTests.build(h,Direction.NORTH);
        var neighbor=host.getBlockPos().north();h.getLevel().setBlockAndUpdate(neighbor,Blocks.CHEST.defaultBlockState());
        var chest=(ChestBlockEntity)h.getLevel().getBlockEntity(neighbor);
        host.inputs().insert(0,new ItemStack(Items.DIAMOND),20,false);
        host.outputs().insert(0,new ItemStack(Items.GOLD_INGOT),100,false);
        host.tick();
        h.assertTrue(chest.isEmpty()&&host.outputs().count(0)==100&&!host.eject()&&!host.networkExport(),"automatic outputs default off");
        host.setNetworkExport(true);host.tick();
        h.assertTrue(chest.isEmpty()&&!host.eject(),"AE export does not implicitly enable adjacent output");
        host.setNetworkExport(false);host.setEject(true);host.tick();
        int gold=0;for(int i=0;i<chest.getContainerSize();i++)if(chest.getItem(i).is(Items.GOLD_INGOT))gold+=chest.getItem(i).getCount();
        h.assertTrue(gold==64&&host.outputs().count(0)==36,"the first automatic transfer sends a legal stack and retains the exact remainder");
        host.tick();
        gold=0;for(int i=0;i<chest.getContainerSize();i++)if(chest.getItem(i).is(Items.GOLD_INGOT))gold+=chest.getItem(i).getCount();
        h.assertTrue(gold==100&&host.outputs().empty()&&host.inputs().count(0)==20&&!host.networkExport(),"adjacent switch alone sends all products and preserves materials");
        host.setEject(false);host.outputs().insert(0,new ItemStack(Items.GOLD_INGOT),12,false);host.tick();
        h.assertTrue(host.outputs().count(0)==12,"turning the switch off immediately stops automatic output");
        h.succeed();
    }

    @GameTest(template="laser_empty")
    public static void legacyClosedFacesCannotDisableNewOutputPolicy(GameTestHelper h) {
        var host=WorkstationGameTests.build(h,Direction.NORTH);
        var saved=host.saveMachine(h.getLevel().registryAccess());saved.putIntArray("SideModes",new int[6]);saved.putBoolean("Eject",true);saved.putBoolean("NetworkExport",false);
        host.loadMachine(saved,h.getLevel().registryAccess());host.checkStructure();
        var neighbor=host.getBlockPos().north();h.getLevel().setBlockAndUpdate(neighbor,Blocks.CHEST.defaultBlockState());
        host.outputs().insert(0,new ItemStack(Items.GOLD_INGOT),7,false);host.tick();
        var chest=(ChestBlockEntity)h.getLevel().getBlockEntity(neighbor);
        h.assertTrue(chest.getItem(0).is(Items.GOLD_INGOT)&&chest.getItem(0).getCount()==7&&host.eject()&&!host.networkExport(),"old all-closed face tags cannot strand outputs after upgrade");
        h.succeed();
    }
}
