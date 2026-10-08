package dev.overloadsim.gametest;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.*;
import appeng.api.storage.MEStorage;
import com.moakiee.ae2lt.machine.overloadfactory.recipe.*;
import com.moakiee.ae2lt.me.key.LightningKey;
import dev.overloadsim.ModContent;
import dev.overloadsim.OverloadSimulation;
import dev.overloadsim.workstation.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

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
        h.assertTrue(gold==100&&host.outputs().empty()&&host.inputs().count(0)==20&&!host.networkExport(),"one tick sends all accepted products as legal chest stacks and preserves materials");
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

    @GameTest(template="laser_empty")
    public static void oneTickFillsVanillaChestAndRetainsExactOverflow(GameTestHelper h) {
        var host=WorkstationGameTests.build(h,Direction.NORTH);
        var neighbor=host.getBlockPos().north();h.getLevel().setBlockAndUpdate(neighbor,Blocks.CHEST.defaultBlockState());
        var chest=(ChestBlockEntity)h.getLevel().getBlockEntity(neighbor);
        host.outputs().insert(0,new ItemStack(Items.GOLD_INGOT),2_000,false);host.setEject(true);host.tick();
        int gold=0;for(int slot=0;slot<chest.getContainerSize();slot++) {
            var stack=chest.getItem(slot);h.assertTrue(stack.getCount()<=stack.getMaxStackSize(),"native chest slot limits remain legal");
            if(stack.is(Items.GOLD_INGOT))gold+=stack.getCount();
        }
        h.assertTrue(gold==1_728&&host.outputs().count(0)==272,"one tick fills all 27 chest slots and returns exactly the rejected items");
        host.tick();h.assertTrue(host.outputs().count(0)==272,"a full receiver cannot lose or duplicate remaining output");h.succeed();
    }

    @GameTest(template="laser_empty")
    public static void oneTickExportsAllNineMillionItemsToNativeBulkStorage(GameTestHelper h) {
        var host=WorkstationGameTests.build(h,Direction.NORTH);
        for(int slot=0;slot<WorkstationBuffer.SLOTS;slot++)host.outputs().insert(slot,new ItemStack(Items.GOLD_INGOT),WorkstationBuffer.LIMIT,false);
        host.inputs().insert(0,new ItemStack(Items.DIAMOND),20,false);
        var target=new BulkItems(9_000_000);
        withItemTarget(h,host.getBlockPos().north(),target,()->{host.setEject(true);host.tick();});
        h.assertTrue(target.count==9_000_000&&target.largestOffer==1_000_000&&host.outputs().empty(),"all nine million outputs move in one tick without 64-item offer limits");
        h.assertTrue(host.inputs().count(0)==20,"automatic bulk output preserves all input materials");h.succeed();
    }

    @GameTest(template="laser_empty")
    public static void partialAndBlockedNativeBulkReceiversConserveOutput(GameTestHelper h) {
        var host=WorkstationGameTests.build(h,Direction.NORTH);host.outputs().insert(0,new ItemStack(Items.GOLD_INGOT),WorkstationBuffer.LIMIT,false);
        var target=new BulkItems(123_456);
        withItemTarget(h,host.getBlockPos().north(),target,()->{
            host.setEject(true);host.tick();
            h.assertTrue(target.count==123_456&&host.outputs().count(0)==876_544,"receiver capacity alone determines the exact partial transfer");
            host.tick();h.assertTrue(target.count==123_456&&host.outputs().count(0)==876_544,"a blocked receiver preserves both quantities");
            target.capacity=WorkstationBuffer.LIMIT;host.tick();
        });
        h.assertTrue(target.count==WorkstationBuffer.LIMIT&&host.outputs().empty(),"unblocked storage receives the complete remainder in one tick");h.succeed();
    }

    @GameTest(template="laser_empty")
    public static void oneTickExportsFullFluidTankToNativeReceiver(GameTestHelper h) {
        var host=WorkstationGameTests.build(h,Direction.NORTH);setOutputFluid(h,host,HighEnergyWorkstationBlockEntity.TANK_CAPACITY);
        var target=new FluidTank(HighEnergyWorkstationBlockEntity.TANK_CAPACITY);
        withFluidTarget(h,host.getBlockPos().north(),target,()->{host.setEject(true);host.tick();});
        h.assertTrue(target.getFluidAmount()==HighEnergyWorkstationBlockEntity.TANK_CAPACITY&&host.fluidOutput().isEmpty(),"native fluid output sends the full accepted tank instead of 16000mB per tick");h.succeed();
    }

    @GameTest(template="laser_empty")
    public static void partialAndBlockedNativeFluidReceiverConservesOutput(GameTestHelper h) {
        var host=WorkstationGameTests.build(h,Direction.NORTH);setOutputFluid(h,host,HighEnergyWorkstationBlockEntity.TANK_CAPACITY);
        var target=new FluidTank(314_159);
        withFluidTarget(h,host.getBlockPos().north(),target,()->{
            host.setEject(true);host.tick();
            h.assertTrue(target.getFluidAmount()==314_159&&host.fluidOutput().getAmount()==HighEnergyWorkstationBlockEntity.TANK_CAPACITY-314_159,"native fluid partial acceptance returns the exact unaccepted amount");
            host.tick();
        });
        h.assertTrue((long)target.getFluidAmount()+host.fluidOutput().getAmount()==HighEnergyWorkstationBlockEntity.TANK_CAPACITY,"blocked native fluid storage preserves the complete initial quantity");h.succeed();
    }

    @GameTest(template="laser_empty")
    public static void bulkAdjacentCallbackRemovalStopsBeforeNextReceiverSlot(GameTestHelper h) {
        var host=WorkstationGameTests.build(h,Direction.NORTH);host.outputs().insert(0,new ItemStack(Items.GOLD_INGOT),WorkstationBuffer.LIMIT,false);
        var neighbor=host.getBlockPos().north();h.getLevel().setBlockAndUpdate(neighbor,Blocks.CHEST.defaultBlockState());
        var chest=new ChestBlockEntity(neighbor,Blocks.CHEST.defaultBlockState()) {
            boolean removed;
            @Override public void setItem(int slot,ItemStack stack){super.setItem(slot,stack);if(!removed&&!stack.isEmpty()){removed=true;h.getLevel().setBlockAndUpdate(host.getBlockPos(),Blocks.AIR.defaultBlockState());}}
        };
        h.getLevel().setBlockEntity(chest);host.setEject(true);host.tick();
        int gold=0;for(int slot=0;slot<chest.getContainerSize();slot++)if(chest.getItem(slot).is(Items.GOLD_INGOT))gold+=chest.getItem(slot).getCount();
        var packed=packedOutputs(h,host);
        h.assertTrue(gold==64&&packed.count(0)==999_936,"callback removal halts after the accepted slot and packs the full reserved remainder exactly once");h.succeed();
    }

    @GameTest(template="laser_empty",timeoutTicks=120)
    public static void oneTickExportsNineMillionItemsAndFullTankThroughAeStorage(GameTestHelper h) {
        var host=WorkstationGameTests.build(h,Direction.NORTH);connect(h,host);
        h.runAtTickTime(80,()->{
            for(int slot=0;slot<WorkstationBuffer.SLOTS;slot++)host.outputs().insert(slot,new ItemStack(Items.GOLD_INGOT),WorkstationBuffer.LIMIT,false);
            setOutputFluid(h,host,HighEnergyWorkstationBlockEntity.TANK_CAPACITY);
            var target=mount(h,host,9_000_000,HighEnergyWorkstationBlockEntity.TANK_CAPACITY);
            host.setNetworkExport(true);host.tick();
            h.assertTrue(target.items==9_000_000&&target.fluids==HighEnergyWorkstationBlockEntity.TANK_CAPACITY&&host.outputs().empty()&&host.fluidOutput().isEmpty(),"native AE quantities export the entire item buffer and fluid tank in one tick");
            h.assertTrue(target.largestItemOffer==1_000_000&&target.largestFluidOffer==HighEnergyWorkstationBlockEntity.TANK_CAPACITY,"AE storage sees complete bounded quantities instead of stack or 16000mB throttles");h.succeed();
        });
    }

    @GameTest(template="laser_empty",timeoutTicks=120)
    public static void partialAndBlockedAeStorageConservesItemsAndFluid(GameTestHelper h) {
        var host=WorkstationGameTests.build(h,Direction.NORTH);connect(h,host);
        h.runAtTickTime(80,()->{
            host.outputs().insert(0,new ItemStack(Items.GOLD_INGOT),WorkstationBuffer.LIMIT,false);setOutputFluid(h,host,1_000_000);
            var target=mount(h,host,123_456,543_210);host.setNetworkExport(true);host.tick();
            h.assertTrue(target.items==123_456&&host.outputs().count(0)==876_544&&target.fluids==543_210&&host.fluidOutput().getAmount()==456_790,"AE partial acceptance restores exact item and fluid remainders");
            host.tick();h.assertTrue(target.items==123_456&&host.outputs().count(0)==876_544&&target.fluids==543_210&&host.fluidOutput().getAmount()==456_790,"blocked AE storage cannot consume retained resources");h.succeed();
        });
    }

    @GameTest(template="laser_empty",timeoutTicks=120)
    public static void aePartialAcceptanceAndAdjacentStorageShareOneBulkExport(GameTestHelper h) {
        var host=WorkstationGameTests.build(h,Direction.NORTH);connect(h,host);
        h.runAtTickTime(80,()->{
            for(int slot=0;slot<WorkstationBuffer.SLOTS;slot++)host.outputs().insert(slot,new ItemStack(Items.GOLD_INGOT),WorkstationBuffer.LIMIT,false);
            setOutputFluid(h,host,HighEnergyWorkstationBlockEntity.TANK_CAPACITY);
            var ae=mount(h,host,123_456,543_210);var items=new BulkItems(9_000_000);var fluids=new FluidTank(HighEnergyWorkstationBlockEntity.TANK_CAPACITY);
            withItemTarget(h,host.getBlockPos().below(),items,()->withFluidTarget(h,host.getBlockPos().below(),fluids,()->{
                host.setNetworkExport(true);host.setEject(true);host.tick();
            }));
            h.assertTrue(ae.items==123_456&&items.count==8_876_544&&host.outputs().empty(),"adjacent storage receives all items rejected by AE in the same tick");
            h.assertTrue(ae.fluids==543_210&&(long)fluids.getFluidAmount()+ae.fluids==HighEnergyWorkstationBlockEntity.TANK_CAPACITY&&host.fluidOutput().isEmpty(),"both output paths share the fluid budget without losing or duplicating the AE remainder");h.succeed();
        });
    }

    @GameTest(template="laser_empty",timeoutTicks=120)
    public static void aeCallbackRemovalPacksReservedRemainderOnce(GameTestHelper h) {
        var host=WorkstationGameTests.build(h,Direction.NORTH);connect(h,host);
        h.runAtTickTime(80,()->{
            host.outputs().insert(0,new ItemStack(Items.GOLD_INGOT),WorkstationBuffer.LIMIT,false);host.outputs().insert(1,new ItemStack(Items.IRON_INGOT),WorkstationBuffer.LIMIT,false);
            var target=mount(h,host,123_456,0);
            target.onInsert=()->{target.onInsert=()->{};host.tick();h.getLevel().setBlockAndUpdate(host.getBlockPos(),Blocks.AIR.defaultBlockState());};
            host.setNetworkExport(true);host.tick();var packed=packedOutputs(h,host);
            h.assertTrue(target.items==123_456&&packed.count(0)==876_544&&packed.count(1)==WorkstationBuffer.LIMIT,"AE callback cannot recursively export, double-pack, or lose reserved items after removal");h.succeed();
        });
    }

    @GameTest(template="laser_empty",timeoutTicks=120)
    public static void reentrantAeCallbackCannotCraftIntoReservedOutputSpace(GameTestHelper h) {
        var host=WorkstationGameTests.build(h,Direction.NORTH);connect(h,host);
        h.runAtTickTime(80,()->{
            for(int slot=0;slot<WorkstationBuffer.SLOTS;slot++)host.outputs().insert(slot,new ItemStack(Items.GOLD_INGOT),WorkstationBuffer.LIMIT,false);
            host.inputs().insert(0,new ItemStack(Items.IRON_INGOT),1024,false);host.receiveLaser(Direction.NORTH,2_000_000_000L,h.getLevel().getGameTime());
            var manager=h.getLevel().getRecipeManager();var original=List.copyOf(manager.getRecipes());var recipes=new ArrayList<RecipeHolder<?>>(original);
            var recipe=new OverloadProcessingRecipe(10_000,List.of(new OverloadProcessingIngredient(Ingredient.of(Items.IRON_INGOT),1)),FluidStack.EMPTY,List.of(new ItemStack(Items.GOLD_INGOT,2)),FluidStack.EMPTY,Long.MAX_VALUE,Integer.MAX_VALUE,LightningKey.Tier.EXTREME_HIGH_VOLTAGE);
            recipes.add(new RecipeHolder<>(ModContent.id("workstation_test/reentrant_output"),new HighEnergyWorkstationRecipe(recipe)));manager.replaceRecipes(recipes);
            try {
                var target=mount(h,host,123_456,0);target.onInsert=()->{target.onInsert=()->{};host.tick();host.tick();};
                host.setNetworkExport(true);host.tick();
                long retained=0;for(int slot=0;slot<WorkstationBuffer.SLOTS;slot++)retained+=host.outputs().count(slot);
                h.assertTrue(host.inputs().count(0)==1024&&host.lastParallel()==0&&host.progress()==0,"AE callbacks cannot spend inputs or advance work while an output reservation is held");
                h.assertTrue(target.items==123_456&&retained==8_876_544,"reserved output space is never reused by a recursive machine tick");
            } finally {manager.replaceRecipes(original);WorkstationRecipes.invalidate();}
            h.succeed();
        });
    }

    private static WorkstationBuffer packedOutputs(GameTestHelper h,HighEnergyWorkstationBlockEntity host) {
        var parcels=h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(host.getBlockPos()).inflate(2)).stream().map(ItemEntity::getItem).filter(item->item.is(WorkstationContent.CONTROLLER_ITEM.get())).toList();
        h.assertTrue(parcels.size()==1&&parcels.getFirst().getCount()==1,"exactly one controller preserves the remaining output");
        var buffer=new WorkstationBuffer();buffer.load(parcels.getFirst().get(DataComponents.CUSTOM_DATA).copyTag().getCompound("Workstation").getCompound("Outputs"),h.getLevel().registryAccess());return buffer;
    }

    private static void setOutputFluid(GameTestHelper h,HighEnergyWorkstationBlockEntity host,int amount) {
        var saved=host.saveMachine(h.getLevel().registryAccess());var tank=new FluidTank(HighEnergyWorkstationBlockEntity.TANK_CAPACITY);tank.setFluid(new FluidStack(Fluids.LAVA,amount));
        var tag=new net.minecraft.nbt.CompoundTag();tank.writeToNBT(h.getLevel().registryAccess(),tag);saved.put("OutputFluid",tag);host.loadMachine(saved,h.getLevel().registryAccess());host.checkStructure();
    }

    private static void connect(GameTestHelper h,HighEnergyWorkstationBlockEntity host) {
        h.getLevel().setBlockAndUpdate(host.getBlockPos().north(),appeng.core.definitions.AEBlocks.CREATIVE_ENERGY_CELL.block().defaultBlockState());
    }

    private static BulkAeStorage mount(GameTestHelper h,HighEnergyWorkstationBlockEntity host,long itemCapacity,long fluidCapacity) {
        h.assertTrue(host.getMainNode().getGrid()!=null&&host.getMainNode().isActive(),"workstation fixture has an active native AE grid");
        var target=new BulkAeStorage(itemCapacity,fluidCapacity);host.getMainNode().getGrid().getStorageService().addGlobalStorageProvider(mounts->mounts.mount(target,0));return target;
    }

    private static final class BulkAeStorage implements MEStorage {
        final long itemCapacity,fluidCapacity;long items,fluids,largestItemOffer,largestFluidOffer;Runnable onInsert=()->{};
        BulkAeStorage(long itemCapacity,long fluidCapacity){this.itemCapacity=itemCapacity;this.fluidCapacity=fluidCapacity;}
        @Override public Component getDescription(){return Component.literal("workstation bulk-output fixture");}
        @Override public void getAvailableStacks(KeyCounter out){}
        @Override public long insert(AEKey key,long amount,Actionable action,IActionSource source) {
            boolean item=key instanceof AEItemKey;long accepted=Math.min(amount,item?itemCapacity-items:fluidCapacity-fluids);
            if(action==Actionable.MODULATE){if(item){largestItemOffer=Math.max(largestItemOffer,amount);items+=accepted;}else{largestFluidOffer=Math.max(largestFluidOffer,amount);fluids+=accepted;}onInsert.run();}return accepted;
        }
    }

    private static final class BulkItems implements IItemHandler {
        int capacity,count,largestOffer;BulkItems(int capacity){this.capacity=capacity;}
        @Override public int getSlots(){return 1;}
        @Override public ItemStack getStackInSlot(int slot){return count==0?ItemStack.EMPTY:new ItemStack(Items.GOLD_INGOT);}
        @Override public ItemStack insertItem(int slot,ItemStack stack,boolean simulate){if(!stack.is(Items.GOLD_INGOT))return stack;int moved=Math.min(stack.getCount(),capacity-count);if(!simulate){largestOffer=Math.max(largestOffer,stack.getCount());count+=moved;}return moved==stack.getCount()?ItemStack.EMPTY:stack.copyWithCount(stack.getCount()-moved);}
        @Override public ItemStack extractItem(int slot,int amount,boolean simulate){return ItemStack.EMPTY;}
        @Override public int getSlotLimit(int slot){return capacity;}
        @Override public boolean isItemValid(int slot,ItemStack stack){return stack.is(Items.GOLD_INGOT);}
    }

    private static void withItemTarget(GameTestHelper h,BlockPos position,IItemHandler target,Runnable action) {
        h.getLevel().setBlockAndUpdate(position,Blocks.STONE.defaultBlockState());
        try{
            var capability=net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK;var field=net.neoforged.neoforge.capabilities.BlockCapability.class.getDeclaredField("providers");field.setAccessible(true);
            @SuppressWarnings("unchecked") var providers=(Map<net.minecraft.world.level.block.Block,List<net.neoforged.neoforge.capabilities.IBlockCapabilityProvider<IItemHandler,Direction>>>)field.get(capability);
            var original=providers.put(Blocks.STONE,List.of((level,pos,state,entity,side)->level==h.getLevel()&&pos.equals(position)?target:null));
            try{action.run();}finally{if(original==null)providers.remove(Blocks.STONE);else providers.put(Blocks.STONE,original);}
        }catch(ReflectiveOperationException exception){throw new IllegalStateException("cannot install test item capability",exception);}
    }

    private static void withFluidTarget(GameTestHelper h,BlockPos position,IFluidHandler target,Runnable action) {
        h.getLevel().setBlockAndUpdate(position,Blocks.STONE.defaultBlockState());
        try{
            var capability=net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK;var field=net.neoforged.neoforge.capabilities.BlockCapability.class.getDeclaredField("providers");field.setAccessible(true);
            @SuppressWarnings("unchecked") var providers=(Map<net.minecraft.world.level.block.Block,List<net.neoforged.neoforge.capabilities.IBlockCapabilityProvider<IFluidHandler,Direction>>>)field.get(capability);
            var original=providers.put(Blocks.STONE,List.of((level,pos,state,entity,side)->level==h.getLevel()&&pos.equals(position)?target:null));
            try{action.run();}finally{if(original==null)providers.remove(Blocks.STONE);else providers.put(Blocks.STONE,original);}
        }catch(ReflectiveOperationException exception){throw new IllegalStateException("cannot install test fluid capability",exception);}
    }
}
