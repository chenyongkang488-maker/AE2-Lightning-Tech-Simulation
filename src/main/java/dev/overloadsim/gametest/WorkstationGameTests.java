package dev.overloadsim.gametest;

import dev.overloadsim.OverloadSimulation;
import dev.overloadsim.workstation.*;
import dev.overloadsim.api.LaserExtensions;
import com.moakiee.ae2lt.machine.overloadfactory.recipe.*;
import com.moakiee.ae2lt.me.key.LightningKey;
import dev.overloadsim.ModContent;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.component.DataComponents;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(OverloadSimulation.ID)
@PrefixGameTestTemplate(false)
public final class WorkstationGameTests {
    public static HighEnergyWorkstationBlockEntity build(GameTestHelper h,Direction facing) {
        var origin=h.absolutePos(new BlockPos(10,2,10));
        for(int x=0;x<7;x++)for(int y=0;y<7;y++)for(int z=0;z<7;z++){
            var pos=origin.relative(facing.getClockWise(),x-3).relative(facing.getOpposite(),z).above(y);
            int walls=(x==0||x==6?1:0)+(z==0||z==6?1:0);
            var block=y==0||y==6||z==6||walls==2?dev.overloadsim.laser.LaserContent.FRAME.get():walls==1?dev.overloadsim.laser.LaserContent.GLASS.get():Blocks.AIR;
            if(x==3&&y==3&&z==6)block=dev.overloadsim.laser.LaserContent.PORT.get();
            if(x==3&&y==0&&z==0)block=WorkstationContent.CONTROLLER.get();
            var state=block.defaultBlockState();
            if(block==WorkstationContent.CONTROLLER.get())state=state.setValue(HighEnergyWorkstationBlock.FACING,facing);
            h.getLevel().setBlockAndUpdate(pos,state);
        }
        var host=(HighEnergyWorkstationBlockEntity)h.getLevel().getBlockEntity(origin);host.checkStructure();return host;
    }
    @GameTest(template="laser_empty") public static void exactSevenCubeAndInteriorMustStayClear(GameTestHelper h) {
        var host=build(h,Direction.NORTH);
        h.assertTrue(host.formed()&&host.template().size()==343,"hand-built exact 7 cube forms and reserves all cells");
        h.getLevel().setBlockAndUpdate(host.center(),Blocks.STONE.defaultBlockState());host.checkStructure();
        h.assertTrue(!host.formed(),"solid interior invalidates workstation");h.succeed();
    }
    @GameTest(template="laser_empty") public static void millionCountsPersistAsIndependentQuantity(GameTestHelper h) {
        var buffer=new WorkstationBuffer();buffer.insert(0,new ItemStack(Items.IRON_INGOT),1_000_001,false);
        h.assertTrue(buffer.count(0)==1_000_000&&buffer.prototype(0).getCount()==1,"million limit uses count-one prototype");
        var restored=new WorkstationBuffer();restored.load(buffer.save(h.getLevel().registryAccess()),h.getLevel().registryAccess());
        h.assertTrue(restored.count(0)==1_000_000,"quantity survives legal codec round trip");
        h.assertTrue(restored.extract(0,1_000_000,false).getCount()==64&&restored.count(0)==999_936,"manual/capability extraction always returns legal stacks");h.succeed();
    }
    @GameTest(template="laser_empty") public static void automationCannotExtractInputOrInsertOutput(GameTestHelper h) {
        var host=build(h,Direction.EAST);host.setSideMode(Direction.UP,3);
        var handler=host.automation(Direction.UP);handler.insertItem(0,new ItemStack(Items.IRON_INGOT,64),false);
        h.assertTrue(handler.extractItem(0,64,false).isEmpty()&&host.inputs().count(0)==64,"input remains input-only");
        h.assertTrue(handler.insertItem(9,new ItemStack(Items.GOLD_INGOT),false).getCount()==1,"output rejects automated inserts");
        host.outputs().insert(0,new ItemStack(Items.GOLD_INGOT),100,false);
        h.assertTrue(handler.extractItem(9,100,false).getCount()==64&&host.outputs().count(0)==36,"automation only extracts output legal stacks");h.succeed();
    }
    @GameTest(template="laser_empty") public static void thresholdIsStrictAndReceiptNeverAddsBeamPowers(GameTestHelper h) {
        var host=build(h,Direction.SOUTH);
        host.receiveLaser(Direction.SOUTH,600_000_000L,h.getLevel().getGameTime());
        host.receiveLaser(Direction.SOUTH,600_000_000L,h.getLevel().getGameTime());
        h.assertTrue(host.currentPower()==600_000_000L,"two weak beams never add to qualifying strength");
        host.receiveLaser(Direction.SOUTH,1_000_000_000L,h.getLevel().getGameTime());
        h.assertTrue(host.currentPower()==1_000_000_000L,"threshold power displayed exactly");
        host.receiveLaser(Direction.NORTH,2_000_000_000L,h.getLevel().getGameTime());
        h.assertTrue(host.currentPower()==1_000_000_000L,"beam from wrong direction cannot activate receiver");h.succeed();
    }
    private static OverloadProcessingRecipe recipe(boolean fluids){return new OverloadProcessingRecipe(10_000,List.of(new OverloadProcessingIngredient(Ingredient.of(Items.IRON_INGOT),1)),fluids?new FluidStack(Fluids.WATER,1000):FluidStack.EMPTY,List.of(new ItemStack(Items.GOLD_INGOT,2)),fluids?new FluidStack(Fluids.LAVA,1000):FluidStack.EMPTY,Long.MAX_VALUE,Integer.MAX_VALUE,LightningKey.Tier.EXTREME_HIGH_VOLTAGE);}
    private static void fixture(GameTestHelper h,boolean fluids,java.util.function.Consumer<net.minecraft.resources.ResourceLocation> test){
        var manager=h.getLevel().getRecipeManager();var original=List.copyOf(manager.getRecipes());var id=ModContent.id("workstation_test/materials_only");var recipes=new ArrayList<RecipeHolder<?>>(original);recipes.add(new RecipeHolder<>(id,recipe(fluids)));manager.replaceRecipes(recipes);
        try{test.accept(id);}finally{manager.replaceRecipes(original);WorkstationRecipes.invalidate();}
    }
    @GameTest(template="laser_empty") public static void twoValidTicksRun1024AndIgnoreAllOriginalFees(GameTestHelper h){
        var host=build(h,Direction.NORTH);host.inputs().insert(0,new ItemStack(Items.IRON_INGOT),1_000_000,false);
        fixture(h,false,id->{
            host.receiveLaser(Direction.NORTH,1_000_000_000L,h.getLevel().getGameTime());host.tick();host.tick();
            h.assertTrue(host.inputs().count(0)==1_000_000&&host.outputs().empty(),"equal threshold never progresses");
            host.receiveLaser(Direction.NORTH,1_000_000_001L,h.getLevel().getGameTime());host.tick();
            h.assertTrue(host.inputs().count(0)==1_000_000&&host.progress()==1,"first valid tick reserves without spending inputs");host.tick();
            h.assertTrue(host.inputs().count(0)==998_976&&host.outputs().count(0)==2048&&host.lastParallel()==1024&&host.lastRecipe().equals(id.toString()),"second valid tick commits max batch with zero network FE/lightning");
        });h.succeed();
    }
    @GameTest(template="laser_empty") public static void fullOutputsAndRemovedRecipeNeverLoseMaterials(GameTestHelper h){
        var host=build(h,Direction.WEST);host.inputs().insert(0,new ItemStack(Items.IRON_INGOT),2048,false);
        fixture(h,false,id->{
            for(int slot=0;slot<9;slot++)host.outputs().insert(slot,new ItemStack(Items.GOLD_INGOT),1_000_000,false);
            host.receiveLaser(Direction.WEST,2_000_000_000L,h.getLevel().getGameTime());host.tick();host.tick();
            h.assertTrue(host.inputs().count(0)==2048,"blocked output cannot debit inputs");
            for(int slot=0;slot<9;slot++)host.outputs().debit(slot,1_000_000);
            host.tick();h.assertTrue(host.progress()==1,"unblocked output can begin job");
            var remaining=h.getLevel().getRecipeManager().getRecipes().stream().filter(holder->!holder.id().equals(id)).toList();h.getLevel().getRecipeManager().replaceRecipes(remaining);host.tick();
            h.assertTrue(host.inputs().count(0)==2048&&host.outputs().empty()&&host.progress()==0,"recipe removal between ticks cancels without static fallback");
        });h.succeed();
    }
    @GameTest(template="laser_empty") public static void fluidAmountsScaleAndOnlyOutputTankCanDrain(GameTestHelper h){
        var host=build(h,Direction.EAST);host.setSideMode(Direction.UP,3);host.inputs().insert(0,new ItemStack(Items.IRON_INGOT),2048,false);
        var fluids=host.fluidAutomation(Direction.UP);fluids.fill(new FluidStack(Fluids.WATER,2_048_000),IFluidHandler.FluidAction.EXECUTE);
        h.assertTrue(fluids.drain(1000,IFluidHandler.FluidAction.EXECUTE).isEmpty()&&host.fluidInput().getAmount()==2_048_000,"input tank cannot be drained by automation");
        fixture(h,true,id->{host.receiveLaser(Direction.EAST,2_000_000_000L,h.getLevel().getGameTime());host.tick();host.tick();
            h.assertTrue(host.fluidInput().getAmount()==1_024_000&&host.fluidOutput().getAmount()==1_024_000&&host.outputs().count(0)==2048,"1024 batch scales both fluid tanks atomically");
            h.assertTrue(fluids.drain(1000,IFluidHandler.FluidAction.EXECUTE).getFluid()==Fluids.LAVA&&host.fluidOutput().getAmount()==1_023_000,"automation extracts output fluid only");
        });h.succeed();
    }
    @GameTest(template="laser_empty") public static void dedicatedRecipeTypeUsesCurrentRecipeObjects(GameTestHelper h){
        var manager=new RecipeManager(h.getLevel().registryAccess());var id=ModContent.id("workstation_test/own_type");
        manager.replaceRecipes(List.of(new RecipeHolder<>(id,new HighEnergyWorkstationRecipe(recipe(false)))));
        var inputs=new WorkstationBuffer();inputs.insert(0,new ItemStack(Items.IRON_INGOT),1024,false);
        var selected=WorkstationRecipes.select(manager,inputs,new WorkstationBuffer(),FluidStack.EMPTY,FluidStack.EMPTY).orElseThrow();
        h.assertTrue(selected.entry().id().equals(id)&&selected.parallel()==1024,"independent recipe type joins inherited execution catalog");
        manager.replaceRecipes(List.of());h.assertTrue(WorkstationRecipes.find(manager,id).isEmpty(),"same manager map replacement invalidates catalog");h.succeed();
    }
    @GameTest(template="laser_empty") public static void sourceReleaseImmediatelyStopsPendingAndPreservesOtherReceipts(GameTestHelper h){
        var host=build(h,Direction.NORTH);host.inputs().insert(0,new ItemStack(Items.IRON_INGOT),1024,false);
        var strong=host.getBlockPos().north(5);var weak=host.getBlockPos().north(6);var strongId=UUID.randomUUID();var weakId=UUID.randomUUID();
        fixture(h,false,id->{
            host.receiveLaserFromSource(Direction.NORTH,2_000_000_000L,h.getLevel().getGameTime(),strong,strongId);host.receiveLaserFromSource(Direction.NORTH,600_000_000L,h.getLevel().getGameTime(),weak,weakId);host.tick();
            h.assertTrue(host.progress()==1,"strong source starts job");host.releaseLaserSource(strong,strongId);
            h.assertTrue(host.progress()==0&&host.currentPower()==600_000_000L,"path deactivation removes only that source and resets progress immediately");host.tick();
            h.assertTrue(host.inputs().count(0)==1024&&host.outputs().empty(),"remaining weak source cannot settle materials");host.releaseLaserSource(weak,weakId);h.assertTrue(host.currentPower()==0,"all sources removed with no anonymous managed copy");
        });h.succeed();
    }
    @GameTest(template="laser_empty") public static void removingControllerInsideAdjacentInsertCannotExportPackedResourcesTwice(GameTestHelper h){
        var host=build(h,Direction.NORTH);host.outputs().insert(0,new ItemStack(Items.IRON_INGOT),64,false);host.outputs().insert(1,new ItemStack(Items.GOLD_INGOT),64,false);
        var saved=host.saveMachine(h.getLevel().registryAccess());var tank=new FluidTank(HighEnergyWorkstationBlockEntity.TANK_CAPACITY);tank.setFluid(new FluidStack(Fluids.LAVA,1000));var fluidTag=new net.minecraft.nbt.CompoundTag();tank.writeToNBT(h.getLevel().registryAccess(),fluidTag);saved.put("OutputFluid",fluidTag);host.loadMachine(saved,h.getLevel().registryAccess());host.checkStructure();
        var chestPos=host.getBlockPos().north();h.getLevel().setBlockAndUpdate(chestPos,Blocks.CHEST.defaultBlockState());
        var chest=new ChestBlockEntity(chestPos,Blocks.CHEST.defaultBlockState()){
            boolean removed;
            @Override public void setItem(int slot,ItemStack stack){super.setItem(slot,stack);if(!removed&&!stack.isEmpty()){removed=true;h.getLevel().setBlockAndUpdate(host.getBlockPos(),Blocks.AIR.defaultBlockState());}}
        };
        h.getLevel().setBlockEntity(chest);host.setSideMode(Direction.NORTH,2);host.setEject(true);host.tick();
        int iron=0,gold=0;for(int slot=0;slot<chest.getContainerSize();slot++){var stack=chest.getItem(slot);if(stack.is(Items.IRON_INGOT))iron+=stack.getCount();if(stack.is(Items.GOLD_INGOT))gold+=stack.getCount();}
        h.assertTrue(iron==64&&gold==0,"only first output may enter external storage before callback removes controller");
        var parcels=h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(host.getBlockPos()).inflate(2)).stream().map(ItemEntity::getItem).filter(item->item.is(WorkstationContent.CONTROLLER_ITEM.get())).toList();
        h.assertTrue(parcels.size()==1,"replacement creates one preserved inventory controller");var machine=parcels.getFirst().get(DataComponents.CUSTOM_DATA).copyTag().getCompound("Workstation");var output=new WorkstationBuffer();output.load(machine.getCompound("Outputs"),h.getLevel().registryAccess());var restoredFluid=new FluidTank(HighEnergyWorkstationBlockEntity.TANK_CAPACITY);restoredFluid.readFromNBT(h.getLevel().registryAccess(),machine.getCompound("OutputFluid"));
        h.assertTrue(output.count(0)==0&&output.count(1)==64&&restoredFluid.getFluidAmount()==1000,"remaining output and fluid only live in packed snapshot");h.succeed();
    }
    @GameTest(template="laser_empty") public static void rejectedFluidFillRemovalDropsOnlyOneOriginalController(GameTestHelper h){fluidRemovalConservesResources(h,0,false);}
    @GameTest(template="laser_empty") public static void partialFluidFillDestructionConservesAllResourcesInOneController(GameTestHelper h){fluidRemovalConservesResources(h,375,true);}
    private static void fluidRemovalConservesResources(GameTestHelper h,int accepted,boolean destroyWithLoot){
        var host=build(h,Direction.NORTH);host.outputs().insert(1,new ItemStack(Items.GOLD_INGOT),64,false);
        var saved=host.saveMachine(h.getLevel().registryAccess());var outputFluid=new FluidTank(HighEnergyWorkstationBlockEntity.TANK_CAPACITY);outputFluid.setFluid(new FluidStack(Fluids.LAVA,1000));var fluidTag=new net.minecraft.nbt.CompoundTag();outputFluid.writeToNBT(h.getLevel().registryAccess(),fluidTag);saved.put("OutputFluid",fluidTag);host.loadMachine(saved,h.getLevel().registryAccess());host.checkStructure();
        var tankPos=host.getBlockPos().north();h.getLevel().setBlockAndUpdate(tankPos,Blocks.STONE.defaultBlockState());
        var target=new FluidTank(10_000){
            boolean removed;
            @Override public int fill(FluidStack resource,IFluidHandler.FluidAction action){
                if(action.simulate())return Math.min(accepted,resource.getAmount());
                int inserted=accepted==0?0:super.fill(resource.copyWithAmount(Math.min(accepted,resource.getAmount())),action);
                if(!removed){removed=true;if(destroyWithLoot)h.getLevel().destroyBlock(host.getBlockPos(),true);else h.getLevel().setBlockAndUpdate(host.getBlockPos(),Blocks.AIR.defaultBlockState());}
                return inserted;
            }
        };
        withFluidTarget(h,tankPos,target,()->{host.setSideMode(Direction.NORTH,2);host.setEject(true);host.tick();});
        h.assertTrue(target.getFluidAmount()==accepted,"external tank receives exactly its declared accepted fluid");
        var parcels=h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(host.getBlockPos()).inflate(2)).stream().map(ItemEntity::getItem).filter(item->item.is(WorkstationContent.CONTROLLER_ITEM.get())).toList();
        h.assertTrue(parcels.size()==1&&parcels.getFirst().getCount()==1,"fluid callback removal must preserve exactly one original controller item");
        var machine=parcels.getFirst().get(DataComponents.CUSTOM_DATA).copyTag().getCompound("Workstation");var output=new WorkstationBuffer();output.load(machine.getCompound("Outputs"),h.getLevel().registryAccess());var restoredFluid=new FluidTank(HighEnergyWorkstationBlockEntity.TANK_CAPACITY);restoredFluid.readFromNBT(h.getLevel().registryAccess(),machine.getCompound("OutputFluid"));
        h.assertTrue(output.count(1)==64&&restoredFluid.getFluidAmount()+target.getFluidAmount()==1000,"one packed controller retains remaining gold and all rejected fluid");h.succeed();
    }
    private static void withFluidTarget(GameTestHelper h,BlockPos position,IFluidHandler target,Runnable action){
        // Install a runtime-scoped provider so ordinary blocks gain no production test capability.
        try{
            var capability=net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK;var field=net.neoforged.neoforge.capabilities.BlockCapability.class.getDeclaredField("providers");field.setAccessible(true);
            @SuppressWarnings("unchecked") var providers=(Map<net.minecraft.world.level.block.Block,List<net.neoforged.neoforge.capabilities.IBlockCapabilityProvider<IFluidHandler,Direction>>>)field.get(capability);
            var original=providers.put(Blocks.STONE,List.of((level,pos,state,entity,side)->level==h.getLevel()&&pos.equals(position)?target:null));
            try{action.run();}finally{if(original==null)providers.remove(Blocks.STONE);else providers.put(Blocks.STONE,original);}
        }catch(ReflectiveOperationException exception){throw new IllegalStateException("cannot install test fluid capability",exception);}
    }
}
