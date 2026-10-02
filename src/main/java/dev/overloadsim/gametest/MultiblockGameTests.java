package dev.overloadsim.gametest;

import dev.overloadsim.OverloadSimulation;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(OverloadSimulation.ID)
@PrefixGameTestTemplate(false)
public class MultiblockGameTests {
    @GameTest(template="empty")
    public static void hiddenCrystalSlotsStillReceiveInventoryPackets(GameTestHelper h){
        var inventory=new net.neoforged.neoforge.items.ItemStackHandler(49);
        var slot=new dev.overloadsim.multiblock.SimulationCrystalSlot(new appeng.api.inventories.PlatformInventoryWrapper(inventory),48);
        slot.setVisible(false);slot.set(ironCrystal());
        h.assertTrue(!slot.isActive()&&!slot.getItem().isEmpty(),"hidden input receives Slot.set packet and remains readable for recovery detection");
        slot.setVisible(true);h.assertTrue(slot.isActive()&&slot.getItem().is(dev.overloadsim.ModContent.PERFECT.get()),"recovery exposes synchronized extra crystal");
        slot.setVisible(false);slot.set(net.minecraft.world.item.ItemStack.EMPTY);slot.setVisible(true);
        h.assertTrue(slot.getItem().isEmpty(),"removal packet synchronizes while hidden");h.succeed();
    }
    @GameTest(template="empty")
    public static void frameRoofRejectsLegacyGlassWithoutLosingPaidState(GameTestHelper h){
        var min=h.absolutePos(new net.minecraft.core.BlockPos(2,1,2));var c=build(h,min,3);c.checkStructure();
        c.crystals().setStackInSlot(0,ironCrystal());c.outputs().insert(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_INGOT),71,false);
        var paid=new dev.overloadsim.multiblock.SimulationBatch(java.util.List.of(new dev.overloadsim.multiblock.SimulationBatch.Output(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_INGOT),1)),java.util.Map.of(0,ironCrystal()),180,new dev.overloadsim.multiblock.MultiblockRules.Costs(1000,1,0),false,false,"roof-migration");paid.paid=true;paid.remaining=93;
        var saved=c.saveMachine(h.getLevel().registryAccess());saved.put("Batch",paid.save(h.getLevel().registryAccess()));c.loadMachine(saved,h.getLevel().registryAccess());c.invalidateStructure();
        var roof=min.offset(1,2,1);var glass=net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.parse("ae2:quartz_vibrant_glass"));h.getLevel().setBlockAndUpdate(roof,glass.defaultBlockState());c.checkStructure();
        h.assertTrue(c.structure()==null&&c.error().equals("roof"),"legacy glass roof pauses and identifies roof position");
        h.assertTrue(c.batch().paid&&c.batch().remaining==93&&c.outputs().count(0)==71&&!c.crystals().getStackInSlot(0).isEmpty(),"paid state and inventory remain intact");
        h.getLevel().setBlockAndUpdate(roof,dev.overloadsim.multiblock.MultiblockContent.FRAME.get().defaultBlockState());c.checkStructure();
        h.assertTrue(c.structure()!=null&&c.structure().glass().size()==4&&c.batch().remaining==93,"frame roof reforms without resetting paid job");h.succeed();
    }
    @GameTest(template="empty")
    public static void oldProcessCacheCannotCollideWithCurrentReloadCounters(GameTestHelper h){
        var c=build(h,h.absolutePos(new net.minecraft.core.BlockPos(2,1,2)),3);c.checkStructure();var input=ironCrystal();
        c.fixedRoll(0,input,()->java.util.List.of(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.RAW_IRON)));
        var registry=h.getLevel().registryAccess();var saved=c.saveMachine(registry);var generation=saved.getString("RollGeneration").split(":");
        saved.putString("RollGeneration",generation[generation.length-2]+":"+generation[generation.length-1]);
        var paid=new dev.overloadsim.multiblock.SimulationBatch(java.util.List.of(new dev.overloadsim.multiblock.SimulationBatch.Output(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND),2)),java.util.Map.of(0,input),180,new dev.overloadsim.multiblock.MultiblockRules.Costs(1000,1,0),false,false,"old-process");paid.paid=true;paid.remaining=93;saved.put("Batch",paid.save(registry));
        c.loadMachine(saved,registry);var current=c.fixedRoll(0,input,()->java.util.List.of(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.RAW_GOLD)));
        h.assertTrue(current.getFirst().is(net.minecraft.world.item.Items.RAW_GOLD),"old process counters cannot retain unpaid outputs");
        h.assertTrue(c.batch().paid&&c.batch().remaining==93&&c.batch().outputs.getFirst().prototype().is(net.minecraft.world.item.Items.DIAMOND),"paid journal preserves its fixed result across session changes");h.succeed();
    }
    @GameTest(template="empty")
    public static void formedGlassDropsItsOriginalMaterial(GameTestHelper h){
        var min=h.absolutePos(new net.minecraft.core.BlockPos(2,1,2));var c=build(h,min,3);c.checkStructure();var p=c.structure().glass().getFirst();
        var member=(dev.overloadsim.multiblock.SimulationMemberBlockEntity)h.getLevel().getBlockEntity(p);member.bind(c,net.minecraft.world.level.block.Blocks.QUARTZ_BLOCK.defaultBlockState());
        var params=new net.minecraft.world.level.storage.loot.LootParams.Builder(h.getLevel()).withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN,p.getCenter()).withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_STATE,h.getLevel().getBlockState(p)).withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY,member).withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.TOOL,net.minecraft.world.item.ItemStack.EMPTY);
        var state=h.getLevel().getBlockState(p);var drops=state.getDrops(params);h.assertTrue(drops.size()==1&&drops.getFirst().is(net.minecraft.world.item.Items.QUARTZ_BLOCK),"formed proxy preserves original material loot");
        h.getLevel().setBlockAndUpdate(p,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());drops=state.getDrops(params);
        h.assertTrue(drops.size()==1&&drops.getFirst().is(net.minecraft.world.item.Items.QUARTZ_BLOCK),"player loot after removal retains original material");h.succeed();
    }
    @GameTest(template="empty")
    public static void dataReloadInvalidatesUnpaidCachedOutcomes(GameTestHelper h){
        var c=build(h,h.absolutePos(new net.minecraft.core.BlockPos(2,1,2)),3);c.checkStructure();var input=ironCrystal();
        c.fixedRoll(0,input,()->java.util.List.of(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.RAW_IRON)));
        String before=dev.overloadsim.multiblock.MultiblockData.policy().signature(c.structure());
        try{var method=dev.overloadsim.multiblock.MultiblockData.class.getDeclaredMethod("apply",java.util.Map.class,net.minecraft.server.packs.resources.ResourceManager.class,net.minecraft.util.profiling.ProfilerFiller.class);method.setAccessible(true);method.invoke(new dev.overloadsim.multiblock.MultiblockData(),java.util.Map.of(),null,null);}catch(ReflectiveOperationException e){throw new RuntimeException(e);}
        var roll=c.fixedRoll(0,input,()->java.util.List.of(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.RAW_GOLD)));
        h.assertTrue(roll.getFirst().is(net.minecraft.world.item.Items.RAW_GOLD),"unpaid cached outcome refreshed after data reload");
        h.assertTrue(!before.equals(dev.overloadsim.multiblock.MultiblockData.policy().signature(c.structure())),"unpaid batch policy detects data reload");h.succeed();
    }
    @GameTest(template="empty")
    public static void removedMembersStayRemovedAndReplacementsSurvive(GameTestHelper h){
        var min=h.absolutePos(new net.minecraft.core.BlockPos(2,1,2));var c=build(h,min,3);c.checkStructure();
        var corner=min;h.getLevel().setBlockAndUpdate(corner,net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        h.assertTrue(h.getLevel().getBlockState(corner).is(net.minecraft.world.level.block.Blocks.STONE),"replacement must not be overwritten by stale member state");
        c=build(h,min,3);c.checkStructure();h.getLevel().destroyBlock(corner,true);
        h.assertTrue(h.getLevel().getBlockState(corner).isAir(),"survival removal must not resurrect a dropped frame");
        c=build(h,min,3);c.checkStructure();var glass=c.structure().glass().getFirst();h.getLevel().destroyBlock(glass,true);
        h.assertTrue(h.getLevel().getBlockState(glass).isAir(),"removed glass stays air");h.succeed();
    }
    @GameTest(template="empty")
    public static void obsoleteMemberRestoresAfterSmallerStructureForms(GameTestHelper h){
        var min=h.absolutePos(new net.minecraft.core.BlockPos(2,1,2));var c=build(h,min,3);c.checkStructure();
        var outside=min.offset(5,2,5);var original=net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.parse("ae2:quartz_vibrant_glass")).defaultBlockState();
        h.getLevel().setBlockAndUpdate(outside,dev.overloadsim.multiblock.MultiblockContent.GLASS.get().defaultBlockState());
        var member=(dev.overloadsim.multiblock.SimulationMemberBlockEntity)h.getLevel().getBlockEntity(outside);member.bind(c,original);member.recover();
        h.assertTrue(h.getLevel().getBlockState(outside).equals(original),"member outside current bounds restores its original glass");h.succeed();
    }
    @GameTest(template="empty")
    public static void missingOwnerChunkSchedulesRecoveryWithoutLoadingIt(GameTestHelper h){
        var p=h.absolutePos(new net.minecraft.core.BlockPos(2,1,2));h.getLevel().setBlockAndUpdate(p,dev.overloadsim.multiblock.MultiblockContent.GLASS.get().defaultBlockState());
        var member=(dev.overloadsim.multiblock.SimulationMemberBlockEntity)h.getLevel().getBlockEntity(p);var owner=p.offset(10000,0,10000);
        var dummy=new dev.overloadsim.multiblock.SimulationControllerBlockEntity(owner,dev.overloadsim.multiblock.MultiblockContent.CONTROLLER.get().defaultBlockState());member.bind(dummy,net.minecraft.world.level.block.Blocks.GLASS.defaultBlockState());
        h.getLevel().getBlockTicks().clearArea(new net.minecraft.world.level.levelgen.structure.BoundingBox(p.getX(),p.getY(),p.getZ(),p.getX(),p.getY(),p.getZ()));member.recover();
        h.assertTrue(!h.getLevel().hasChunkAt(owner)&&h.getLevel().getBlockTicks().hasScheduledTick(p,dev.overloadsim.multiblock.MultiblockContent.GLASS.get()),"retry queued without force loading owner chunk");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=110)
    public static void shrinkingRejectsUnpaidOutOfCapacityInputs(GameTestHelper h){
        var c=powered(h,7);var min=c.structure().min();h.runAtTickTime(80,()->{
            c.crystals().setStackInSlot(48,ironCrystal());java.util.function.Consumer<dev.overloadsim.api.MultiblockSimulationEvents.BeforeBatchStart> cancel=e->{if(e.controller==c)e.setCanceled(true);};
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(cancel);
            try{c.tick();}finally{net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(cancel);}
            h.assertTrue(c.batch()!=null&&!c.batch().paid,"unpaid snapshot created");c.invalidateStructure();
            for(int x=0;x<7;x++)for(int y=0;y<7;y++)for(int z=0;z<7;z++){var p=min.offset(x,y,z);if(!p.equals(c.getBlockPos()))h.getLevel().setBlockAndUpdate(p,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());}
            h.assertTrue(build(h,min,3)==c,"shrinking preserves the same controller entity");c.checkStructure();c.tick();h.assertTrue(c.structure()!=null&&c.structure().capacity()==9,"smaller structure formed: "+c.diagnostic());
            h.assertTrue(c.batch()==null||!c.batch().paid,"out-of-capacity unpaid inputs cannot start");
            long stored=c.getMainNode().getGrid().getStorageService().getInventory().extract(com.moakiee.ae2lt.me.key.LightningKey.HIGH_VOLTAGE,Long.MAX_VALUE,appeng.api.config.Actionable.SIMULATE,appeng.api.networking.security.IActionSource.ofMachine(c));
            h.assertTrue(stored==1000,"disabled slot charged nothing, remaining HV="+stored);h.succeed();
        });
    }
    @GameTest(template="empty",timeoutTicks=110)
    public static void sparseCrystalsAlternateWhenOnlyOneCanBePaid(GameTestHelper h){
        var c=powered(h,7);h.runAtTickTime(80,()->{
            c.bridge().extract(false,999,false);c.crystals().setStackInSlot(0,ironCrystal());c.crystals().setStackInSlot(48,ironCrystal());
            c.tick();h.assertTrue(c.batch().inputs.containsKey(0),"first crystal selected");for(int i=1;i<180;i++)c.tick();
            c.bridge().insert(false,1);c.tick();h.assertTrue(c.batch().inputs.containsKey(48),"second sparse crystal selected");for(int i=1;i<180;i++)c.tick();
            c.bridge().insert(false,1);c.tick();h.assertTrue(c.batch().inputs.containsKey(0),"fairness resumes after last selected slot");h.succeed();
        });
    }
    @GameTest(template="empty")
    public static void unpaidPolicyDistinguishesStructureCapacity(GameTestHelper h){
        var min=h.absolutePos(new net.minecraft.core.BlockPos(2,1,2));var small=build(h,min,3);small.checkStructure();var signature=dev.overloadsim.multiblock.MultiblockData.policy().signature(small.structure());
        small.invalidateStructure();var large=build(h,min,7);large.checkStructure();
        h.assertTrue(!signature.equals(dev.overloadsim.multiblock.MultiblockData.policy().signature(large.structure())),"unpaid job signature includes crystal capacity");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=140)
    public static void offlinePaidJobDoesNotShowWorkingEffects(GameTestHelper h){
        var c=powered(h,3);h.runAtTickTime(80,()->{c.crystals().setStackInSlot(0,ironCrystal());c.tick();h.getLevel().setBlockAndUpdate(c.getBlockPos().north(),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());});
        h.runAtTickTime(120,()->{h.assertTrue(c.busy()&&!c.getMainNode().isActive(),"offline job retained");h.assertTrue((c.visualFlags()&8)==0&&(c.visualFlags()&1)!=0,"offline job retains orb without working lightning");h.succeed();});
    }
    @GameTest(template="empty",timeoutTicks=110)
    public static void controllerRequiresAnAEChannel(GameTestHelper h){
        var c=powered(h,3);h.runAtTickTime(80,()->{h.assertTrue(c.getMainNode().getNode().hasFlag(appeng.api.networking.GridFlags.REQUIRE_CHANNEL),"controller requires one AE channel");h.succeed();});
    }
    @GameTest(template="empty",timeoutTicks=110)
    public static void removedInterfaceImmediatelyStopsExport(GameTestHelper h){
        var c=powered(h,3);var min=c.structure().min();c.invalidateStructure();var p=min.offset(0,0,1);
        h.getLevel().setBlockAndUpdate(p,net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.parse("ae2lt:overloaded_interface")).defaultBlockState());c.checkStructure();
        h.runAtTickTime(81,()->{c.bridge().refresh();h.getLevel().setBlockAndUpdate(p,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());c.outputs().insert(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_INGOT),1024,false);c.bridge().refresh();c.bridge().export();
            h.assertTrue(c.outputs().count(0)==1024,"missing live interface prevents export before periodic scan");h.succeed();});
    }
    @GameTest(template="empty")
    public static void formationEventDoesNotReceiveDismantling(GameTestHelper h){
        h.assertTrue(!dev.overloadsim.api.MultiblockSimulationEvents.Formed.class.isAssignableFrom(dev.overloadsim.api.MultiblockSimulationEvents.Invalidated.class),"formation and dismantling events distinct");h.succeed();
    }
    @GameTest(template="empty")
    public static void legacyChamberRetainsPickaxeTag(GameTestHelper h){h.assertTrue(dev.overloadsim.ModContent.CHAMBER.get().defaultBlockState().is(net.minecraft.tags.BlockTags.MINEABLE_WITH_PICKAXE),"legacy chamber remains pickaxe-mineable");h.succeed();}
    @GameTest(template="empty",timeoutTicks=120)
    public static void originalInterfaceBridgesAndExportsAcceptedQuantities(GameTestHelper h){
        var c=powered(h,3);var min=c.structure().min();c.invalidateStructure();
        var p=min.offset(0,0,1);var registry=net.minecraft.core.registries.BuiltInRegistries.BLOCK;
        h.getLevel().setBlockAndUpdate(p,registry.get(net.minecraft.resources.ResourceLocation.parse("ae2lt:overloaded_interface")).defaultBlockState());c.checkStructure();
        h.runAtTickTime(80,()->{
            c.bridge().refresh();var port=(appeng.blockentity.grid.AENetworkedBlockEntity)h.getLevel().getBlockEntity(p);
            h.assertTrue(c.getMainNode().getGrid()==port.getMainNode().getGrid(),"original interface joins controller network");
            c.outputs().insert(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_INGOT),2048,false);c.bridge().export();
            h.assertTrue(c.outputs().count(0)==0&&c.outputs().count(1)==0,"bulk inserted through AE keys");
            long stored=c.getMainNode().getGrid().getStorageService().getInventory().extract(appeng.api.stacks.AEItemKey.of(net.minecraft.world.item.Items.IRON_INGOT),2048,appeng.api.config.Actionable.SIMULATE,appeng.api.networking.security.IActionSource.ofMachine(c));
            h.assertTrue(stored==2048,"exported counts exact");
            h.getLevel().setBlockAndUpdate(p,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());c.checkStructure();
            h.assertTrue(c.structure()==null,"removed original interface invalidates structure");h.succeed();
        });
    }
    @GameTest(template="empty")
    public static void breakingControllerPreservesBulkAndCrystalsInOneItem(GameTestHelper h){
        var c=build(h,h.absolutePos(new net.minecraft.core.BlockPos(2,1,2)),3);c.checkStructure();c.crystals().setStackInSlot(0,ironCrystal());c.outputs().insert(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_INGOT),2048,false);
        var params=new net.minecraft.world.level.storage.loot.LootParams.Builder(h.getLevel()).withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN,c.getBlockPos().getCenter()).withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_STATE,c.getBlockState()).withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY,c).withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.TOOL,net.minecraft.world.item.ItemStack.EMPTY);
        var drops=c.getBlockState().getDrops(params);
        h.assertTrue(drops.size()==1,"one packed controller item");var item=drops.getFirst();var tag=item.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA).copyTag();
        var oldPos=c.getBlockPos();h.getLevel().setBlockAndUpdate(oldPos,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        h.getLevel().setBlockAndUpdate(oldPos,c.getBlockState());var next=(dev.overloadsim.multiblock.SimulationControllerBlockEntity)h.getLevel().getBlockEntity(oldPos);
        next.loadMachine(tag.getCompound("SimulationMachine"),h.getLevel().registryAccess());
        h.assertTrue(next.outputs().count(0)==1024&&next.outputs().count(1)==1024&&next.crystals().getStackInSlot(0).is(dev.overloadsim.ModContent.PERFECT.get()),"packed state restored");h.succeed();
    }
    @GameTest(template="empty")
    public static void multiblockMenuExists(GameTestHelper h){
        var c=build(h,h.absolutePos(new net.minecraft.core.BlockPos(2,1,2)),3);c.checkStructure();
        c.outputs().insert(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_INGOT),2048,false);
        var player=net.neoforged.neoforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"bulk-menu"));
        player.setPos(c.getBlockPos().getCenter());
        var menu=new dev.overloadsim.multiblock.MultiblockSimulationMenu(1,player.getInventory(),c);player.containerMenu=menu;menu.broadcastChanges();
        int revision=menu.revision();menu.take(player,0,revision,0,0);
        h.assertTrue(menu.getCarried().getCount()==64&&c.outputs().count(0)==960,"left take legal stack");
        menu.take(player,0,revision,0,0);h.assertTrue(c.outputs().count(0)==960,"stale revision rejected");
        menu.setCarried(net.minecraft.world.item.ItemStack.EMPTY);menu.take(player,0,menu.revision(),0,1);
        h.assertTrue(menu.getCarried().getCount()==1&&c.outputs().count(0)==959,"right take one");
        menu.setCarried(net.minecraft.world.item.ItemStack.EMPTY);menu.take(player,0,menu.revision(),0,2);
        int total=0;for(int i=0;i<player.getInventory().getContainerSize();i++){var item=player.getInventory().getItem(i);total+=item.getCount();h.assertTrue(item.getCount()<=item.getMaxStackSize(),"shift split legal");}
        h.assertTrue(total==959&&c.outputs().count(0)==0,"shift exact bulk debit");
        menu.take(player,4,menu.revision(),0,2);h.assertTrue(c.outputs().count(1)==1024,"invalid page cannot debit");player.discard();h.succeed();
    }
    @GameTest(template="empty")
    public static void multiblockProductionExists(GameTestHelper h){
        var r=h.getLevel().registryAccess();var input=new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.RAW_IRON);
        var batch=new dev.overloadsim.multiblock.SimulationBatch(java.util.List.of(new dev.overloadsim.multiblock.SimulationBatch.Output(input,2048)),java.util.Map.of(0,input),38,new dev.overloadsim.multiblock.MultiblockRules.Costs(1000,3,1),true,true,"fixed");
        batch.paid=true;batch.remaining=17;var restored=dev.overloadsim.multiblock.SimulationBatch.load(batch.save(r),r);
        h.assertTrue(restored.paid&&restored.remaining==17&&restored.outputs.getFirst().count()==2048,"paid job persistence");
        var full=new dev.overloadsim.multiblock.BulkOutputBuffer();full.insert(input,131072,false);
        h.assertTrue(!restored.flush(full)&&full.count(127)==1024,"blocked job stays intact");h.succeed();
    }
    static dev.overloadsim.multiblock.SimulationControllerBlockEntity powered(GameTestHelper h,int n){
        var min=h.absolutePos(new net.minecraft.core.BlockPos(2,1,2));var c=build(h,min,n);var p=c.getBlockPos();
        var registry=net.minecraft.core.registries.BuiltInRegistries.BLOCK;
        h.getLevel().setBlockAndUpdate(p.north(),registry.get(net.minecraft.resources.ResourceLocation.parse("ae2:creative_energy_cell")).defaultBlockState());
        h.getLevel().setBlockAndUpdate(p.north(2),registry.get(net.minecraft.resources.ResourceLocation.parse("ae2:drive")).defaultBlockState());
        var drive=(appeng.blockentity.storage.DriveBlockEntity)h.getLevel().getBlockEntity(p.north(2));
        var cell=new net.minecraft.world.item.ItemStack(com.moakiee.ae2lt.registry.ModItems.INFINITE_STORAGE_CELL.get());
        var storage=appeng.api.storage.StorageCells.getCellInventory(cell,drive::saveChanges);
        storage.insert(com.moakiee.ae2lt.me.key.LightningKey.HIGH_VOLTAGE,1000,appeng.api.config.Actionable.MODULATE,appeng.api.networking.security.IActionSource.empty());
        storage.insert(com.moakiee.ae2lt.me.key.LightningKey.EXTREME_HIGH_VOLTAGE,1000,appeng.api.config.Actionable.MODULATE,appeng.api.networking.security.IActionSource.empty());storage.persist();drive.getInternalInventory().setItemDirect(0,cell);
        c.checkStructure();return c;
    }
    static net.minecraft.world.item.ItemStack ironCrystal(){return dev.overloadsim.api.CrystalDataAccess.perfect(new dev.overloadsim.api.CrystalData(dev.overloadsim.ModContent.id("iron"),java.util.Optional.empty(),0,1));}
    @GameTest(template="empty",timeoutTicks=110)
    public static void baseCyclePersistsFeesAndPausesWithoutStructure(GameTestHelper h){
        var c=powered(h,3);
        h.runAtTickTime(80,()->{
            h.assertTrue(c.getMainNode().isActive()&&c.energy().getEnergyStored()>0,"idle AE continuously charges FE");
            c.crystals().setStackInSlot(0,ironCrystal());c.crystals().setStackInSlot(1,ironCrystal());c.tick();
            h.assertTrue(c.busy()&&c.batch().cost.fe()==2000&&c.batch().cost.hv()==2&&c.batch().duration==180,"per crystal default cost and duration");
            h.assertTrue(c.bridge().extract(false,Long.MAX_VALUE,true)==1000,"incomplete job has no HV debit");
            for(int i=0;i<88;i++)c.tick();var remaining=c.batch().remaining;
            var saved=c.saveMachine(h.getLevel().registryAccess());c.loadMachine(saved,h.getLevel().registryAccess());
            c.invalidateStructure();c.tick();h.assertTrue(c.batch().remaining==remaining,"structure loss pauses paid work");c.checkStructure();
            for(int i=0;i<remaining;i++)c.tick();
            h.assertTrue(c.outputs().count(0)==2&&!c.busy(),"180 processing ticks yield exactly two raw iron");
            h.assertTrue(c.bridge().extract(false,Long.MAX_VALUE,true)==998,"reload does not recharge paid work");h.succeed();
        });
    }
    @GameTest(template="empty",timeoutTicks=110)
    public static void maximumModulesSmelt49CrystalsIn38Ticks(GameTestHelper h){
        var c=powered(h,7);var min=c.structure().min();c.invalidateStructure();int index=0;
        for(int x=1;x<6;x++)for(int z=1;z<6;z++){
            var block=index<13?dev.overloadsim.multiblock.MultiblockContent.T3.get():index<23?dev.overloadsim.multiblock.MultiblockContent.FORTUNE.get():index==23?dev.overloadsim.multiblock.MultiblockContent.OVERLOAD.get():dev.overloadsim.multiblock.MultiblockContent.SMELTING.get();
            h.getLevel().setBlockAndUpdate(min.offset(x,0,z),block.defaultBlockState());index++;
        }c.checkStructure();
        h.runAtTickTime(80,()->{
            for(int slot=0;slot<49;slot++)c.crystals().setStackInSlot(slot,ironCrystal());c.tick();
            h.assertTrue(c.batch().duration==38&&c.batch().cost.fe()==49000&&c.batch().cost.hv()==147&&c.batch().cost.ehv()==49,"maximum matrix policy");
            for(int i=1;i<38;i++)c.tick();
            long total=0;for(int slot=0;slot<128;slot++){total+=c.outputs().count(slot);if(c.outputs().count(slot)>0)h.assertTrue(c.outputs().prototype(slot).is(net.minecraft.world.item.Items.IRON_INGOT),"smelting output type");}
            h.assertTrue(total==100352&&!c.busy(),"49 x 2 x 1024 outputs after exactly 38 ticks");
            h.assertTrue(c.bridge().extract(false,Long.MAX_VALUE,true)==853&&c.bridge().extract(true,Long.MAX_VALUE,true)==951,"HV and EHV costs remain separate");h.succeed();
        });
    }
    @GameTest(template = "empty")
    public static void multiblockStructureExists(GameTestHelper helper) {
        for(int size=3;size<=7;size++){
            var min=helper.absolutePos(new net.minecraft.core.BlockPos(2,1,2));
            var controller=build(helper,min,size);controller.checkStructure();
            helper.assertTrue(controller.structure()!=null&&controller.structure().capacity()==size*size,"form outer size "+size);
            helper.assertTrue(controller.structure().glass().size()==4*(size-2)*(size-2),"four glass faces and frame roof");
            var pane=controller.structure().glass().getFirst();
            helper.getLevel().setBlockAndUpdate(pane,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
            helper.assertTrue(controller.structure()==null,"breaking any glass invalidates");
            for(int x=0;x<size;x++)for(int y=0;y<size;y++)for(int z=0;z<size;z++)
                helper.getLevel().setBlockAndUpdate(min.offset(x,y,z),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        }
        helper.succeed();
    }
    public static dev.overloadsim.multiblock.SimulationControllerBlockEntity build(GameTestHelper h,net.minecraft.core.BlockPos min,int n){
        var glass=net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.parse("ae2:quartz_vibrant_glass"));
        for(int x=0;x<n;x++)for(int y=0;y<n;y++)for(int z=0;z<n;z++){
            int b=(x==0||x==n-1?1:0)+(y==0||y==n-1?1:0)+(z==0||z==n-1?1:0);
            var block=b>=2||y==0||y==n-1?dev.overloadsim.multiblock.MultiblockContent.FRAME.get():b==1?glass:net.minecraft.world.level.block.Blocks.AIR;
            if(x==1&&y==0&&z==0&&h.getLevel().getBlockState(min.offset(x,y,z)).is(dev.overloadsim.multiblock.MultiblockContent.CONTROLLER.get()))continue;
            h.getLevel().setBlockAndUpdate(min.offset(x,y,z),block.defaultBlockState());
        }
        var pos=min.offset(1,0,0);h.getLevel().setBlockAndUpdate(pos,dev.overloadsim.multiblock.MultiblockContent.CONTROLLER.get().defaultBlockState());
        return (dev.overloadsim.multiblock.SimulationControllerBlockEntity)h.getLevel().getBlockEntity(pos);
    }
    @GameTest(template="empty")
    public static void structureRejectsSolidInteriorAndDuplicateSpecialModules(GameTestHelper h){
        var min=h.absolutePos(new net.minecraft.core.BlockPos(2,1,2));var c=build(h,min,4);
        h.getLevel().setBlockAndUpdate(min.offset(1,1,1),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());c.checkStructure();
        h.assertTrue(c.structure()==null,"solid interior rejected");
        h.getLevel().setBlockAndUpdate(min.offset(1,1,1),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        h.getLevel().setBlockAndUpdate(min.offset(1,0,1),dev.overloadsim.multiblock.MultiblockContent.OVERLOAD.get().defaultBlockState());
        h.getLevel().setBlockAndUpdate(min.offset(2,0,1),dev.overloadsim.multiblock.MultiblockContent.OVERLOAD.get().defaultBlockState());c.checkStructure();
        h.assertTrue(c.structure()==null,"duplicate overload rejected");
        h.getLevel().setBlockAndUpdate(min.offset(2,0,1),dev.overloadsim.multiblock.MultiblockContent.SMELTING.get().defaultBlockState());c.checkStructure();
        h.assertTrue(c.structure()!=null&&c.structure().duration()==90&&c.structure().smelting(),"distinct modules valid");
        var pane=c.structure().glass().getLast();c.invalidateStructure();
        h.assertTrue(h.getLevel().getBlockState(pane).is(net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.parse("ae2:quartz_vibrant_glass"))),"original glass restored");
        c.checkStructure();var tag=c.saveWithoutMetadata(h.getLevel().registryAccess());c.loadTag(tag,h.getLevel().registryAccess());c.checkStructure();c.invalidateStructure();
        h.assertTrue(h.getLevel().getBlockState(pane).is(net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.parse("ae2:quartz_vibrant_glass"))),"reloaded formation retains original state");h.succeed();
    }
    @GameTest(template = "empty")
    public static void bulkBufferExists(GameTestHelper helper) {
        var buffer=new dev.overloadsim.multiblock.BulkOutputBuffer();
        var iron=new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_INGOT);
        helper.assertTrue(buffer.insert(iron,2048,false)==2048&&buffer.count(0)==1024&&buffer.count(1)==1024,"bulk capacity");
        helper.assertTrue(buffer.prototype(0).getCount()==1&&buffer.extract(0,1024,true).getCount()==64,"legal prototypes and extraction");
        var named=iron.copy();named.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("different"));
        helper.assertTrue(buffer.insert(named,1024,false)==1024&&buffer.count(2)==1024,"components remain distinct");
        var restored=new dev.overloadsim.multiblock.BulkOutputBuffer();restored.load(buffer.save(helper.getLevel().registryAccess()),helper.getLevel().registryAccess());
        helper.assertTrue(restored.count(0)==1024&&net.minecraft.world.item.ItemStack.isSameItemSameComponents(named,restored.prototype(2)),"save quantities and components");
        restored.insert(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND_PICKAXE),100,false);
        helper.assertTrue(restored.extract(3,100,false).getCount()==1&&restored.count(3)==99,"unstackable extraction legal");
        var full=new dev.overloadsim.multiblock.BulkOutputBuffer();full.insert(iron,131072,false);
        helper.assertTrue(full.insert(iron,1,true)==0&&full.count(127)==1024,"simulation preserves full buffer");
        helper.succeed();
    }
}
