package dev.overloadsim.gametest;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.*;
import appeng.api.storage.MEStorage;
import com.moakiee.ae2lt.machine.overloadfactory.recipe.*;
import com.moakiee.ae2lt.me.key.LightningKey;
import com.mojang.authlib.GameProfile;
import dev.overloadsim.ModContent;
import dev.overloadsim.OverloadSimulation;
import dev.overloadsim.config.MachineSettings;
import dev.overloadsim.workstation.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.*;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

/** Overrides never survive a callback: unrelated world ticks retain their normal settings. */
@GameTestHolder(OverloadSimulation.ID)
@PrefixGameTestTemplate(false)
public final class WorkstationConfigurationGameTests {
    @GameTest(template="laser_empty",batch="configuration_workstation")
    public static void doubledParallelCeilingCommitsMoreThanEightBillionProducts(GameTestHelper h){
        withSettings(Map.of(MachineSettings.WORKSTATION_MAX_PARALLEL,2_097_152,MachineSettings.WORKSTATION_SLOT_CAPACITY,Integer.MAX_VALUE),()->{
            var host=WorkstationGameTests.build(h,Direction.NORTH);host.inputs().insert(0,new ItemStack(Items.IRON_INGOT),2_097_152,false);
            var recipe=new OverloadProcessingRecipe(10_000,List.of(new OverloadProcessingIngredient(Ingredient.of(Items.IRON_INGOT),1)),FluidStack.EMPTY,List.of(new ItemStack(Items.GOLD_INGOT,4096)),FluidStack.EMPTY,5,1,LightningKey.Tier.HIGH_VOLTAGE);
            withRecipe(h,recipe,()->{host.receiveLaser(Direction.NORTH,2_000_000_000L,h.getLevel().getGameTime());host.tick();host.tick();
                long actual=0;for(int slot=0;slot<9;slot++)actual+=host.outputs().count(slot);
                h.assertTrue(host.inputs().empty()&&host.lastParallel()==2_097_152&&actual==8_589_934_592L,"doubled maximum settles the full batch across bounded output slots without overflow");
            });
        });h.succeed();
    }
    @GameTest(template="laser_empty",batch="configuration_workstation")
    public static void configuredStrictGateParallelAndCycleCommitOnlyAtThirdTick(GameTestHelper h) {
        withSettings(Map.of(MachineSettings.WORKSTATION_LASER_THRESHOLD,10L,
                MachineSettings.WORKSTATION_MAX_PARALLEL,7,MachineSettings.WORKSTATION_CYCLE_TICKS,3),()->{
            var host=WorkstationGameTests.build(h,Direction.NORTH);
            host.inputs().insert(0,new ItemStack(Items.IRON_INGOT),100,false);
            withRecipe(h,()->{
                host.receiveLaser(Direction.NORTH,10,h.getLevel().getGameTime());host.tick();host.tick();host.tick();
                h.assertTrue(host.progress()==0&&host.inputs().count(0)==100,"equal configured threshold never starts a batch");
                host.receiveLaser(Direction.NORTH,11,h.getLevel().getGameTime());host.tick();host.tick();
                h.assertTrue(host.progress()==2&&host.inputs().count(0)==100&&host.outputs().empty(),"two qualifying ticks reserve without debiting a three-tick batch");
                host.tick();
                h.assertTrue(host.lastParallel()==7&&host.inputs().count(0)==93&&host.outputs().count(0)==14,"third tick atomically commits exactly seven configured operations");
            });
        });h.succeed();
    }

    @GameTest(template="laser_empty",batch="configuration_workstation")
    public static void loweringCapacitiesKeepsSavedGoodsAndRejectsOnlyNewInserts(GameTestHelper h) {
        var host=WorkstationGameTests.build(h,Direction.NORTH);
        host.inputs().insert(0,new ItemStack(Items.IRON_INGOT),200,false);
        host.fluidAutomation(Direction.UP).fill(new FluidStack(Fluids.WATER,2000),IFluidHandler.FluidAction.EXECUTE);
        var saved=host.saveMachine(h.getLevel().registryAccess());
        withSettings(Map.of(MachineSettings.WORKSTATION_SLOT_CAPACITY,100,MachineSettings.WORKSTATION_TANK_CAPACITY,1000),()->{
            host.loadMachine(saved,h.getLevel().registryAccess());host.checkStructure();
            var items=host.automation(Direction.UP);var fluids=host.fluidAutomation(Direction.UP);
            h.assertTrue(host.inputs().count(0)==200&&host.fluidInput().getAmount()==2000,"lower operating limits retain saved quantities");
            h.assertTrue(items.getSlotLimit(0)==100&&fluids.getTankCapacity(0)==1000,"capabilities report configured operating limits");
            for(boolean simulate:new boolean[]{true,false}) {
                h.assertTrue(host.inputs().insert(0,new ItemStack(Items.IRON_INGOT),1,simulate)==0,"over-cap item insertion returns zero, never negative");
                h.assertTrue(fluids.fill(new FluidStack(Fluids.WATER,1),simulate?IFluidHandler.FluidAction.SIMULATE:IFluidHandler.FluidAction.EXECUTE)==0,"over-cap fluid insertion returns zero");
            }
            var roundTrip=host.saveMachine(h.getLevel().registryAccess());host.loadMachine(roundTrip,h.getLevel().registryAccess());host.checkStructure();
            h.assertTrue(host.inputs().count(0)==200&&host.fluidInput().getAmount()==2000,"saving below operating capacity does not discard excess");
            h.assertTrue(host.inputs().extract(0,64,false).getCount()==64&&host.manualFluidHandler(false).drain(500,IFluidHandler.FluidAction.EXECUTE).getAmount()==500,"existing over-cap goods remain available for normal extraction");
        });h.succeed();
    }

    @GameTest(template="laser_empty",batch="configuration_workstation")
    public static void raisedItemCapacityRoundTripsTenDigitCountsAndMenuQuantities(GameTestHelper h) {
        withSettings(Map.of(MachineSettings.WORKSTATION_SLOT_CAPACITY,Integer.MAX_VALUE),()->{
            var host=WorkstationGameTests.build(h,Direction.NORTH);
            for(int slot=0;slot<9;slot++){
                host.inputs().insert(slot,new ItemStack(Items.IRON_INGOT),2_000_000_000L,false);
                host.outputs().insert(slot,new ItemStack(Items.GOLD_INGOT),2_000_000_000L,false);
            }
            var saved=host.saveMachine(h.getLevel().registryAccess());host.loadMachine(saved,h.getLevel().registryAccess());host.checkStructure();
            var player=FakePlayerFactory.get(h.getLevel(),new GameProfile(UUID.randomUUID(),"configured-workstation"));
            player.setPos(host.getBlockPos().getCenter());var menu=new HighEnergyWorkstationMenu(11,player.getInventory(),host);
            player.containerMenu=menu;menu.broadcastChanges();
            for(int slot=0;slot<18;slot++)h.assertTrue(menu.quantity(slot)==2_000_000_000L&&menu.slots.get(slot).getItem().getCount()==1,"all eighteen ten-digit quantities sync independently of legal count-one prototypes");
            h.assertTrue(host.inputs().insert(0,new ItemStack(Items.IRON_INGOT),Long.MAX_VALUE,false)==147_483_647L&&host.inputs().count(0)==Integer.MAX_VALUE,"insertion saturates exactly at configured integer storage bound");
            player.containerMenu=player.inventoryMenu;
        });h.succeed();
    }

    @GameTest(template="laser_empty",batch="configuration_workstation")
    public static void overCapOutputsSurvivePartialAndRejectedAutomaticTransfers(GameTestHelper h) {
        var host=WorkstationGameTests.build(h,Direction.NORTH);host.outputs().insert(0,new ItemStack(Items.GOLD_INGOT),200,false);setOutputFluid(h,host,2000);
        var items=new BulkItems(25);var fluid=new FluidTank(250);
        withSettings(Map.of(MachineSettings.WORKSTATION_SLOT_CAPACITY,100,MachineSettings.WORKSTATION_TANK_CAPACITY,1000),()->{
            withTargets(h,host.getBlockPos().north(),items,fluid,()->{
                host.setEject(true);host.tick();
                h.assertTrue(items.count==25&&host.outputs().count(0)==175&&fluid.getFluidAmount()==250&&host.fluidOutput().getAmount()==1750,"partial recipients preserve exact reserved remainders even above new capacities");
                host.tick();
                h.assertTrue(items.count==25&&host.outputs().count(0)==175&&fluid.getFluidAmount()==250&&host.fluidOutput().getAmount()==1750,"fully rejected follow-up transfers cannot discard existing excess");
            });host.setEject(false);
        });h.succeed();
    }

    @GameTest(template="laser_empty",batch="configuration_workstation")
    public static void itemOnlyRecipesCanRetainOverCapStoredOutputFluid(GameTestHelper h) {
        var host=WorkstationGameTests.build(h,Direction.NORTH);host.inputs().insert(0,new ItemStack(Items.IRON_INGOT),20,false);setOutputFluid(h,host,2000);
        withSettings(Map.of(MachineSettings.WORKSTATION_TANK_CAPACITY,1000,MachineSettings.WORKSTATION_MAX_PARALLEL,7),()->withRecipe(h,()->{
            host.receiveLaser(Direction.NORTH,2_000_000_000L,h.getLevel().getGameTime());host.tick();host.tick();
            h.assertTrue(host.outputs().count(0)==14&&host.fluidOutput().getAmount()==2000,"item-only batches preserve over-cap output fluid without manufacturing additional fluid");
        }));h.succeed();
    }

    @GameTest(template="laser_empty",batch="configuration_workstation",timeoutTicks=120)
    public static void configuredBudgetsAreSharedBetweenMeAndAdjacentOutput(GameTestHelper h) {
        var host=WorkstationGameTests.build(h,Direction.NORTH);
        h.getLevel().setBlockAndUpdate(host.getBlockPos().north(),appeng.core.definitions.AEBlocks.CREATIVE_ENERGY_CELL.block().defaultBlockState());
        h.runAtTickTime(80,()->{
            withSettings(Map.of(MachineSettings.WORKSTATION_ITEM_EXPORT_LIMIT,30,MachineSettings.WORKSTATION_FLUID_EXPORT_LIMIT,300),()->{
                host.outputs().insert(0,new ItemStack(Items.GOLD_INGOT),200,false);setOutputFluid(h,host,2000);
                h.assertTrue(host.getMainNode().isActive(),"native AE grid is ready before overriding settings");
                var ae=new BudgetStorage();host.getMainNode().getGrid().getStorageService().addGlobalStorageProvider(mounts->mounts.mount(ae,0));
                var items=new BulkItems(1000);var fluid=new FluidTank(10_000);
                withTargets(h,host.getBlockPos().below(),items,fluid,()->{host.setNetworkExport(true);host.setEject(true);host.tick();});
                h.assertTrue(ae.items==10&&items.count==20&&host.outputs().count(0)==170,"ME and adjacent item transfers share exactly one configured budget");
                h.assertTrue(ae.fluids==100&&fluid.getFluidAmount()==200&&host.fluidOutput().getAmount()==1700,"fluid transfers share their separate configured budget");
                host.setNetworkExport(false);host.setEject(false);
            });h.succeed();
        });
    }

    @GameTest(template="laser_empty",batch="configuration_workstation")
    public static void zeroBudgetsLeaveBothOutputKindsUntouched(GameTestHelper h) {
        var host=WorkstationGameTests.build(h,Direction.NORTH);host.outputs().insert(0,new ItemStack(Items.GOLD_INGOT),200,false);setOutputFluid(h,host,2000);
        withSettings(Map.of(MachineSettings.WORKSTATION_ITEM_EXPORT_LIMIT,0,MachineSettings.WORKSTATION_FLUID_EXPORT_LIMIT,0),()->{
            var items=new BulkItems(1000);var fluids=new FluidTank(10_000);
            withTargets(h,host.getBlockPos().north(),items,fluids,()->{host.setEject(true);host.tick();});
            h.assertTrue(items.count==0&&fluids.isEmpty()&&host.outputs().count(0)==200&&host.fluidOutput().getAmount()==2000,"zero export settings disable each automatic transfer budget");host.setEject(false);
        });h.succeed();
    }

    @GameTest(template="laser_empty",batch="configuration_workstation")
    public static void largeBatchSpansMultipleIntegerBoundedSlotsAndCommitsExactLongTotal(GameTestHelper h) {
        withSettings(Map.of(MachineSettings.WORKSTATION_MAX_PARALLEL,1_048_576,
                MachineSettings.WORKSTATION_SLOT_CAPACITY,Integer.MAX_VALUE),()->{
            var host=WorkstationGameTests.build(h,Direction.NORTH);
            host.inputs().insert(0,new ItemStack(Items.IRON_INGOT),1_048_576,false);
            var recipe=new OverloadProcessingRecipe(10_000,List.of(new OverloadProcessingIngredient(Ingredient.of(Items.IRON_INGOT),1)),
                    FluidStack.EMPTY,List.of(new ItemStack(Items.GOLD_INGOT,4096)),FluidStack.EMPTY,
                    Long.MAX_VALUE,Integer.MAX_VALUE,LightningKey.Tier.EXTREME_HIGH_VOLTAGE);
            var entry=new WorkstationRecipes.Entry(ModContent.id("workstation_test/configured"),recipe);
            var evaluated=WorkstationRecipes.evaluate(entry,host.inputs(),host.outputs(),FluidStack.EMPTY,FluidStack.EMPTY).orElseThrow();
            h.assertTrue(evaluated.parallel()==1_048_576,"parallel evaluation uses aggregate capacity across all nine slots");
            long projected=0;for(int slot=0;slot<9;slot++)projected+=evaluated.outputs().count(slot);
            h.assertTrue(projected==4_294_967_296L,"projected whole-batch total is a long greater than an individual slot limit");
            withRecipe(h,recipe,()->{
                host.receiveLaser(Direction.NORTH,2_000_000_000L,h.getLevel().getGameTime());host.tick();host.tick();
                long actual=0;for(int slot=0;slot<9;slot++)actual+=host.outputs().count(slot);
                h.assertTrue(host.inputs().empty()&&host.lastParallel()==1_048_576&&actual==4_294_967_296L,
                        "commit consumes each input once and preserves all 2^32 products across integer-bounded slots");
                h.assertTrue(host.outputs().count(0)==Integer.MAX_VALUE&&host.outputs().count(1)==Integer.MAX_VALUE&&host.outputs().count(2)==2,
                        "output allocation saturates each slot without overflow or whole-batch truncation");
                var saved=host.saveMachine(h.getLevel().registryAccess());host.loadMachine(saved,h.getLevel().registryAccess());host.checkStructure();
                long restored=0;for(int slot=0;slot<9;slot++)restored+=host.outputs().count(slot);
                h.assertTrue(restored==4_294_967_296L&&host.lastParallel()==1_048_576,"committed multi-slot total and historical parallel survive machine persistence");
                var player=FakePlayerFactory.get(h.getLevel(),new GameProfile(UUID.randomUUID(),"large-batch-workstation"));
                player.setPos(host.getBlockPos().getCenter());var menu=new HighEnergyWorkstationMenu(12,player.getInventory(),host);
                player.containerMenu=menu;
                try{
                    menu.broadcastChanges();long displayed=0;for(int slot=9;slot<18;slot++)displayed+=menu.quantity(slot);
                    h.assertTrue(displayed==4_294_967_296L&&menu.quantity(9)==Integer.MAX_VALUE&&menu.quantity(10)==Integer.MAX_VALUE&&menu.quantity(11)==2,
                            "menu sync preserves each integer-bounded slot and the aggregate long total");
                }finally{player.containerMenu=player.inventoryMenu;}
            });
        });h.succeed();
    }

    private static void withSettings(Map<ModConfigSpec.ConfigValue<?>,Object> overrides,Runnable action) {
        var previous=new LinkedHashMap<ModConfigSpec.ConfigValue<?>,Object>();
        overrides.forEach((key,value)->{previous.put(key,key.get());set(key,value);});
        try{action.run();}finally{previous.forEach(WorkstationConfigurationGameTests::set);}
    }
    @SuppressWarnings("unchecked") private static void set(ModConfigSpec.ConfigValue<?> key,Object value){((ModConfigSpec.ConfigValue<Object>)key).set(value);key.clearCache();}
    private static void withRecipe(GameTestHelper h,Runnable action) {
        var recipe=new OverloadProcessingRecipe(10_000,List.of(new OverloadProcessingIngredient(Ingredient.of(Items.IRON_INGOT),1)),FluidStack.EMPTY,List.of(new ItemStack(Items.GOLD_INGOT,2)),FluidStack.EMPTY,Long.MAX_VALUE,Integer.MAX_VALUE,LightningKey.Tier.EXTREME_HIGH_VOLTAGE);
        withRecipe(h,recipe,action);
    }
    private static void withRecipe(GameTestHelper h,OverloadProcessingRecipe recipe,Runnable action) {
        var manager=h.getLevel().getRecipeManager();var original=List.copyOf(manager.getRecipes());var recipes=new ArrayList<RecipeHolder<?>>(original);
        recipes.add(new RecipeHolder<>(ModContent.id("workstation_test/configured"),new HighEnergyWorkstationRecipe(recipe)));manager.replaceRecipes(recipes);
        try{action.run();}finally{manager.replaceRecipes(original);WorkstationRecipes.invalidate();}
    }
    private static void setOutputFluid(GameTestHelper h,HighEnergyWorkstationBlockEntity host,int amount) {
        var saved=host.saveMachine(h.getLevel().registryAccess());var tank=new FluidTank(Integer.MAX_VALUE);tank.setFluid(new FluidStack(Fluids.LAVA,amount));var tag=new CompoundTag();tank.writeToNBT(h.getLevel().registryAccess(),tag);saved.put("OutputFluid",tag);host.loadMachine(saved,h.getLevel().registryAccess());host.checkStructure();
    }
    private static final class BudgetStorage implements MEStorage {
        long items,fluids;
        @Override public Component getDescription(){return Component.literal("configured workstation test storage");}
        @Override public void getAvailableStacks(KeyCounter out){}
        @Override public long insert(AEKey key,long amount,Actionable mode,IActionSource source){boolean item=key instanceof AEItemKey;long accepted=Math.min(amount,Math.max(0,(item?10:100)-(item?items:fluids)));if(mode==Actionable.MODULATE){if(item)items+=accepted;else fluids+=accepted;}return accepted;}
    }
    private static final class BulkItems implements IItemHandler {
        final int capacity;int count;BulkItems(int capacity){this.capacity=capacity;}
        @Override public int getSlots(){return 1;}
        @Override public ItemStack getStackInSlot(int slot){return count==0?ItemStack.EMPTY:new ItemStack(Items.GOLD_INGOT);}
        @Override public ItemStack insertItem(int slot,ItemStack stack,boolean simulate){if(!stack.is(Items.GOLD_INGOT))return stack;int accepted=Math.min(stack.getCount(),capacity-count);if(!simulate)count+=accepted;return accepted==stack.getCount()?ItemStack.EMPTY:stack.copyWithCount(stack.getCount()-accepted);}
        @Override public ItemStack extractItem(int slot,int amount,boolean simulate){return ItemStack.EMPTY;}
        @Override public int getSlotLimit(int slot){return capacity;}
        @Override public boolean isItemValid(int slot,ItemStack stack){return stack.is(Items.GOLD_INGOT);}
    }
    private static void withTargets(GameTestHelper h,BlockPos position,IItemHandler items,IFluidHandler fluids,Runnable action) {
        h.getLevel().setBlockAndUpdate(position,Blocks.STONE.defaultBlockState());
        withProvider(h,position,Capabilities.ItemHandler.BLOCK,items,()->withProvider(h,position,Capabilities.FluidHandler.BLOCK,fluids,action));
    }
    private static <T> void withProvider(GameTestHelper h,BlockPos position,BlockCapability<T,Direction> capability,T target,Runnable action) {
        try {
            var field=BlockCapability.class.getDeclaredField("providers");field.setAccessible(true);
            @SuppressWarnings("unchecked") var providers=(Map<net.minecraft.world.level.block.Block,List<IBlockCapabilityProvider<T,Direction>>>)field.get(capability);
            var original=providers.put(Blocks.STONE,List.of((level,pos,state,entity,side)->level==h.getLevel()&&pos.equals(position)?target:null));
            try{action.run();}finally{if(original==null)providers.remove(Blocks.STONE);else providers.put(Blocks.STONE,original);}
        }catch(ReflectiveOperationException failure){throw new IllegalStateException("cannot scope workstation test capability",failure);}
    }
}
