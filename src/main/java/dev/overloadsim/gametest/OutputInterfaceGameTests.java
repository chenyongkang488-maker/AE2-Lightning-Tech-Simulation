package dev.overloadsim.gametest;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.*;
import appeng.api.storage.MEStorage;
import dev.overloadsim.OverloadSimulation;
import dev.overloadsim.output.*;
import com.moakiee.ae2lt.logic.energy.AppFluxBridge;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.*;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.gametest.*;
import net.neoforged.neoforge.items.IItemHandler;

@GameTestHolder(OverloadSimulation.ID)
@PrefixGameTestTemplate(false)
public final class OutputInterfaceGameTests {
    @GameTest(template="empty") public static void outputOnlyCapabilitiesRejectEveryExternalInsertion(GameTestHelper h) {
        var host=place(h);
        host.getOutputLogic().getConfig().setStack(0,new GenericStack(AEItemKey.of(Items.IRON_INGOT),16));
        host.getOutputLogic().getConfig().setStack(1,new GenericStack(AEFluidKey.of(Fluids.WATER),1000));
        for(var side:Direction.values()) {
            var items=h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK,host.getBlockPos(),side);
            var offered=new ItemStack(Items.IRON_INGOT,7);
            h.assertTrue(items!=null&&items.insertItem(0,offered,false).getCount()==7,"item insertion always refused");
            h.assertTrue(!items.isItemValid(0,offered),"item slot reports output only");
            var fluids=h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK,host.getBlockPos(),side);
            h.assertTrue(fluids!=null&&fluids.fill(new FluidStack(Fluids.WATER,500),IFluidHandler.FluidAction.EXECUTE)==0,"fluid fill always refused");
        }
        h.assertTrue(host.getOutputLogic().getStorage().insert(0,AEItemKey.of(Items.IRON_INGOT),10,Actionable.MODULATE)==0,"generic insertion refused");
        h.assertTrue(!host.getOutputLogic().getStorage().canInsert(),"generic inventory advertises no insertion");h.succeed();
    }
    @GameTest(template="empty") public static void slotBindingsDeduplicatePersistAndFollowTemplate(GameTestHelper h) {
        var host=place(h);var target=host.getBlockPos().east();h.getLevel().setBlockAndUpdate(target,Blocks.CHEST.defaultBlockState());
        var a=new OutputConnection(0,h.getLevel().dimension(),target,Direction.WEST);var b=new OutputConnection(1,h.getLevel().dimension(),target,Direction.WEST);
        h.assertTrue(host.addConnection(a)&&host.addConnection(b)&&!host.addConnection(a),"several source slots share face, exact duplicate ignored");
        host.getOutputLogic().getConfig().setStack(0,new GenericStack(AEItemKey.of(Items.IRON_INGOT),7));host.setUnlimited(0,true);
        host.getOutputLogic().getConfig().setStack(0,new GenericStack(AEItemKey.of(Items.GOLD_INGOT),4));
        h.assertTrue(!host.isUnlimited(0)&&host.getConnections().contains(a),"template replacement follows slot and cancels unlimited");
        host.getOutputLogic().getConfig().setStack(0,null);h.assertTrue(host.getConnections().contains(a),"clear template retains binding");
        var tag=host.saveWithoutMetadata(h.getLevel().registryAccess());host.clearConnections(0);host.loadTag(tag,h.getLevel().registryAccess());
        h.assertTrue(host.getConnections().equals(List.of(a,b)),"source slot and exact endpoint persist");
        h.assertTrue(host.clearConnections(0)==1&&host.getConnections().equals(List.of(b)),"slot clear affects only its bindings");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=110) public static void wirelessTargetsShareQuantityBudgetRotateAndNeverExtractDestination(GameTestHelper h) {
        var host=place(h);connect(h,host);var east=chest(h,host.getBlockPos().east());var west=chest(h,host.getBlockPos().west());
        east.setItem(0,new ItemStack(Items.GOLD_INGOT,3));west.setItem(0,new ItemStack(Items.DIAMOND,4));
        host.setInterfaceMode(OutputInterfaceBlockEntity.InterfaceMode.WIRELESS);
        host.addConnection(new OutputConnection(0,h.getLevel().dimension(),east.getBlockPos(),Direction.WEST));host.addConnection(new OutputConnection(0,h.getLevel().dimension(),west.getBlockPos(),Direction.EAST));
        host.getOutputLogic().getConfig().setStack(0,new GenericStack(AEItemKey.of(Items.IRON_INGOT),7));
        h.runAtTickTime(80,()->{var source=mount(h,host);source.stock.put(AEItemKey.of(Items.IRON_INGOT),100L);host.setExportMode(OutputInterfaceBlockEntity.ExportMode.AUTO);host.tick();
            h.assertTrue(count(east,Items.IRON_INGOT)+count(west,Items.IRON_INGOT)==7&&source.stock.get(AEItemKey.of(Items.IRON_INGOT))==93,"one slot spends seven total across targets");});
        h.runAtTickTime(81,()->{host.tick();host.setExportMode(OutputInterfaceBlockEntity.ExportMode.OFF);
            h.assertTrue(count(east,Items.IRON_INGOT)==7&&count(west,Items.IRON_INGOT)==7,"successful NORMAL output runs next tick and rotates first target");
            h.assertTrue(count(east,Items.GOLD_INGOT)==3&&count(west,Items.DIAMOND)==4,"destination content never extracted");h.succeed();});
    }
    @GameTest(template="empty",timeoutTicks=110) public static void normalSelectedDirectionLimitsResourceOutputs(GameTestHelper h) {
        var host=place(h);connect(h,host);var east=chest(h,host.getBlockPos().east());var west=chest(h,host.getBlockPos().west());
        host.setEnergyOutputDirection(Direction.EAST);host.getOutputLogic().getConfig().setStack(0,new GenericStack(AEItemKey.of(Items.IRON_INGOT),9));
        h.runAtTickTime(80,()->{var source=mount(h,host);source.stock.put(AEItemKey.of(Items.IRON_INGOT),100L);host.setExportMode(OutputInterfaceBlockEntity.ExportMode.AUTO);host.tick();host.setExportMode(OutputInterfaceBlockEntity.ExportMode.OFF);
            h.assertTrue(count(east,Items.IRON_INGOT)==9&&count(west,Items.IRON_INGOT)==0,"selected original NORMAL direction restricts resources as well as FE");h.succeed();});
    }
    @GameTest(template="empty",timeoutTicks=110) public static void mirroredFluidSlotsSimulateAndDrainNetworkOnlyOnce(GameTestHelper h) {
        var host=place(h);connect(h,host);var water=AEFluidKey.of(Fluids.WATER);
        host.getOutputLogic().getConfig().setStack(0,new GenericStack(water,100));host.getOutputLogic().getConfig().setStack(1,new GenericStack(water,100));
        h.runAtTickTime(80,()->{var source=mount(h,host);source.stock.put(water,10L);
            h.assertTrue(host.fluidAutomation().drain(500,IFluidHandler.FluidAction.SIMULATE).getAmount()==10,"mirrored slots never double-count simulated stock");
            h.assertTrue(host.fluidAutomation().drain(500,IFluidHandler.FluidAction.EXECUTE).getAmount()==10&&source.stock.get(water)==0,"real drain conserves exact fluid");h.succeed();});
    }
    @GameTest(template="empty",timeoutTicks=110) public static void callbackRefusalPersistsRefundAndTemplateChangeCannotDeleteIt(GameTestHelper h) {
        var host=place(h);connect(h,host);var water=AEFluidKey.of(Fluids.WATER);var target=host.getBlockPos().east();
        host.setInterfaceMode(OutputInterfaceBlockEntity.InterfaceMode.WIRELESS);host.getOutputLogic().getConfig().setStack(0,new GenericStack(water,7));
        h.runAtTickTime(80,()->{var source=mount(h,host);source.stock.put(water,20L);source.refundLimit=0;
            var refused=new FluidTank(1000){@Override public int fill(FluidStack stack,FluidAction action){return action.simulate()?stack.getAmount():0;}};
            withCapability(h,target,Capabilities.FluidHandler.BLOCK,refused,()->{
                h.assertTrue(host.addConnection(new OutputConnection(0,h.getLevel().dimension(),target,Direction.WEST)),"registered fluid capability is valid");
                host.setExportMode(OutputInterfaceBlockEntity.ExportMode.AUTO);host.tick();host.setExportMode(OutputInterfaceBlockEntity.ExportMode.OFF);
                h.assertTrue(host.getPendingRefunds().get(water)==7&&source.stock.get(water)==13,"declined post-simulation insertion persists private ownership");
                var saved=host.saveWithoutMetadata(h.getLevel().registryAccess());host.loadTag(saved,h.getLevel().registryAccess());host.getOutputLogic().getConfig().setStack(0,null);
                h.assertTrue(host.getPendingRefunds().get(water)==7,"NBT roundtrip and clearing template preserve refund");source.refundLimit=Long.MAX_VALUE;host.tick();
                h.assertTrue(host.getPendingRefunds().isEmpty()&&source.stock.get(water)==20,"refund returns exact resources after storage accepts again");
            });h.succeed();});
    }
    @GameTest(template="empty",timeoutTicks=110) public static void inductionGatesEnergyAndDeduplicatesEndpointsAcrossEmptySlots(GameTestHelper h) {
        var host=place(h);connect(h,host);host.setInterfaceMode(OutputInterfaceBlockEntity.InterfaceMode.WIRELESS);
        if(AppFluxBridge.FE_KEY==null){h.assertTrue(!host.hasInductionCard(),"optional Flux absence disables energy");h.succeed();return;}
        var target=host.getBlockPos().east();
        h.runAtTickTime(80,()->{var source=mount(h,host);source.stock.put(AppFluxBridge.FE_KEY,1000L);
            var energy=new EnergyTarget();withCapability(h,target,Capabilities.EnergyStorage.BLOCK,energy,()->{
                host.addConnection(new OutputConnection(0,h.getLevel().dimension(),target,Direction.WEST));host.addConnection(new OutputConnection(1,h.getLevel().dimension(),target,Direction.WEST));host.addConnection(new OutputConnection(2,h.getLevel().dimension(),target,Direction.UP));
                host.tick();h.assertTrue(energy.received==0,"FE requires induction even with registered target");
                host.getUpgrades().setItemDirect(0,new ItemStack(AppFluxBridge.getInductionCard()));host.setIOSpeedMode(OutputInterfaceBlockEntity.IOSpeedMode.FAST);host.tick();
                h.assertTrue(energy.calls==2&&energy.received==2&&source.stock.get(AppFluxBridge.FE_KEY)==998,"empty source slots eligible, same endpoint deduplicated and distinct face retained");
            });h.succeed();});
    }
    @GameTest(template="empty",timeoutTicks=110) public static void sourceRemovalInsideTargetCallbackPacksOnlyExactRefundOnce(GameTestHelper h) {
        var host=place(h);connect(h,host);var water=AEFluidKey.of(Fluids.WATER);var target=host.getBlockPos().east();
        host.setInterfaceMode(OutputInterfaceBlockEntity.InterfaceMode.WIRELESS);host.getOutputLogic().getConfig().setStack(0,new GenericStack(water,7));
        h.runAtTickTime(80,()->{var source=mount(h,host);source.stock.put(water,20L);
            var received=new int[1];var sink=new FluidTank(1000){@Override public int fill(FluidStack stack,FluidAction action){if(action.simulate())return stack.getAmount();received[0]+=3;h.getLevel().destroyBlock(host.getBlockPos(),true);return 3;}};
            withCapability(h,target,Capabilities.FluidHandler.BLOCK,sink,()->{host.addConnection(new OutputConnection(0,h.getLevel().dimension(),target,Direction.WEST));host.setExportMode(OutputInterfaceBlockEntity.ExportMode.AUTO);host.tick();});
            var parcels=h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(host.getBlockPos()).inflate(2)).stream().map(net.minecraft.world.entity.item.ItemEntity::getItem).filter(item->item.is(OutputContent.ITEM.get())).toList();
            h.assertTrue(parcels.size()==1,"callback destruction yields exactly one interface item");
            var data=parcels.getFirst().get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
            h.assertTrue(data!=null,"packed item contains refund ownership");
            var refunds=data.copyTag().getCompound("OutputOwnedRefunds").getList("OutputRefunds",net.minecraft.nbt.Tag.TAG_COMPOUND);
            long packed=0;for(var raw:refunds){var stack=GenericStack.readTag(h.getLevel().registryAccess(),(net.minecraft.nbt.CompoundTag)raw);if(stack!=null&&water.equals(stack.what()))packed+=stack.amount();}
            h.assertTrue(received[0]==3&&packed<=4&&source.stock.get(water)+packed+received[0]==20,"accepted, original-network refund, and packed remainder conserve twenty exact units");h.succeed();
        });
    }
    @GameTest(template="empty",timeoutTicks=110) public static void outputEventClearingTemplateRefundsRatherThanExportingOldResource(GameTestHelper h) {
        var host=place(h);connect(h,host);var water=AEFluidKey.of(Fluids.WATER);var target=host.getBlockPos().east();
        host.setInterfaceMode(OutputInterfaceBlockEntity.InterfaceMode.WIRELESS);host.getOutputLogic().getConfig().setStack(0,new GenericStack(water,7));
        h.runAtTickTime(80,()->{var source=mount(h,host);source.stock.put(water,20L);var sink=new FluidTank(1000);
            java.util.function.Consumer<OutputInterfaceExportEvent> listener=event->{if(event.getSource()==host&&!event.isSimulation())host.getOutputLogic().getConfig().setStack(0,null);};
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(listener);
            try{withCapability(h,target,Capabilities.FluidHandler.BLOCK,sink,()->{host.addConnection(new OutputConnection(0,h.getLevel().dimension(),target,Direction.WEST));host.setExportMode(OutputInterfaceBlockEntity.ExportMode.AUTO);host.tick();});}
            finally{net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(listener);}
            h.assertTrue(sink.isEmpty()&&source.stock.get(water)==20&&host.getPendingRefunds().isEmpty(),"MODULATE event template change rejects stale transfer and refunds extraction");h.succeed();
        });
    }
    @GameTest(template="empty",timeoutTicks=110) public static void energySimulationClearingBindingCannotFeedOrphanedTarget(GameTestHelper h) {
        if(AppFluxBridge.FE_KEY==null){h.succeed();return;}
        var host=place(h);connect(h,host);host.setInterfaceMode(OutputInterfaceBlockEntity.InterfaceMode.WIRELESS);var target=host.getBlockPos().east();
        h.runAtTickTime(80,()->{var source=mount(h,host);source.stock.put(AppFluxBridge.FE_KEY,1000L);var armed=new boolean[1];var received=new int[1];
            var energy=new IEnergyStorage(){
                public int receiveEnergy(int amount,boolean simulate){if(simulate){if(armed[0])host.clearConnections(0);}else received[0]++;return Math.min(1,amount);}
                public int extractEnergy(int amount,boolean simulate){throw new AssertionError("no target extraction");}
                public int getEnergyStored(){return received[0];}public int getMaxEnergyStored(){return Integer.MAX_VALUE;}public boolean canExtract(){return false;}public boolean canReceive(){return true;}
            };
            withCapability(h,target,Capabilities.EnergyStorage.BLOCK,energy,()->{host.addConnection(new OutputConnection(0,h.getLevel().dimension(),target,Direction.WEST));host.getUpgrades().setItemDirect(0,new ItemStack(AppFluxBridge.getInductionCard()));armed[0]=true;host.tick();});
            h.assertTrue(received[0]==0&&source.stock.get(AppFluxBridge.FE_KEY)==1000,"removed binding after simulation prevents stale FE insertion and refunds exact energy");h.succeed();
        });
    }
    @GameTest(template="empty",timeoutTicks=110) public static void feCannotBecomeAResourceTemplateOrPassiveProxyWithoutInduction(GameTestHelper h) {
        if(AppFluxBridge.FE_KEY==null){h.succeed();return;}
        var host=place(h);connect(h,host);
        h.runAtTickTime(80,()->{var source=mount(h,host);source.stock.put(AppFluxBridge.FE_KEY,1000L);
            host.getOutputLogic().getConfig().setStack(0,new GenericStack(AppFluxBridge.FE_KEY,50));
            h.assertTrue(host.getConfiguredKey(0)==null,"FE has no resource template slot");
            h.assertTrue(host.getOutputLogic().getStorage().extract(AppFluxBridge.FE_KEY,50,Actionable.MODULATE,IActionSource.empty())==0&&source.stock.get(AppFluxBridge.FE_KEY)==1000,"passive proxy cannot bypass induction path");
            var saved=host.saveWithoutMetadata(h.getLevel().registryAccess());
            var config=(net.minecraft.nbt.ListTag)saved.get("OutputConfig");
            if(config==null)config=new net.minecraft.nbt.ListTag();
            while(config.size()<36)config.add(new net.minecraft.nbt.CompoundTag());
            config.set(0,GenericStack.writeTag(h.getLevel().registryAccess(),new GenericStack(AppFluxBridge.FE_KEY,50)));saved.put("OutputConfig",config);
            host.loadTag(saved,h.getLevel().registryAccess());h.assertTrue(host.getConfiguredKey(0)==null,"NBT restores sanitize FE resource templates too");h.succeed();
        });
    }
    @GameTest(template="empty",timeoutTicks=110) public static void fuzzyProxyExposesRealVariantButAutomaticExportStaysExact(GameTestHelper h) {
        var host=place(h);connect(h,host);var target=chest(h,host.getBlockPos().east());
        var pristine=AEItemKey.of(Items.DIAMOND_SWORD);var damagedStack=new ItemStack(Items.DIAMOND_SWORD);damagedStack.setDamageValue(100);var damaged=AEItemKey.of(damagedStack);
        host.getOutputLogic().getConfig().setStack(0,new GenericStack(pristine,3));host.getUpgrades().setItemDirect(0,appeng.core.definitions.AEItems.FUZZY_CARD.stack());
        host.setEnergyOutputDirection(Direction.EAST);
        h.runAtTickTime(80,()->{var source=mount(h,host);source.stock.put(damaged,8L);
            var available=new KeyCounter();host.getOutputLogic().getStorage().getAvailableStacks(available);
            h.assertTrue(available.get(damaged)==3&&available.get(pristine)==0,"fuzzy card exposes actual variant exactly once under configured cap");
            var visible=host.getOutputLogic().getStorage().getStack(0);h.assertTrue(visible!=null&&damaged.equals(visible.what())&&visible.amount()==3,"manual display exposes actual fuzzy variant");
            h.assertTrue(host.itemAutomation().extractItem(0,2,false).getDamageValue()==100&&source.stock.get(damaged)==6,"manual withdrawal extracts real variant without changing components");
            host.setExportMode(OutputInterfaceBlockEntity.ExportMode.AUTO);host.tick();host.setExportMode(OutputInterfaceBlockEntity.ExportMode.OFF);
            h.assertTrue(target.isEmpty()&&source.stock.get(damaged)==6,"automatic resource output remains precise configured key even with fuzzy card");h.succeed();
        });
    }
    @GameTest(template="empty",timeoutTicks=180) public static void refusedExportAfterWiredNetworkSwitchNeverRefundsIntoForeignNetwork(GameTestHelper h) {
        var host=place(h);var sourceAnchor=host.getBlockPos().west();var foreignPos=host.getBlockPos().south(4);var water=AEFluidKey.of(Fluids.WATER);
        var original=drive(h,sourceAnchor,water,20);h.getLevel().setBlockAndUpdate(sourceAnchor.below(),appeng.core.definitions.AEBlocks.CREATIVE_ENERGY_CELL.block().defaultBlockState());
        var foreign=drive(h,foreignPos,water,0);h.getLevel().setBlockAndUpdate(foreignPos.below(),appeng.core.definitions.AEBlocks.CREATIVE_ENERGY_CELL.block().defaultBlockState());
        host.setInterfaceMode(OutputInterfaceBlockEntity.InterfaceMode.WIRELESS);host.getOutputLogic().getConfig().setStack(0,new GenericStack(water,7));var target=host.getBlockPos().east();
        h.runAtTickTime(80,()->{
            h.assertTrue(host.getMainNode().isActive()&&host.getMainNode().getGrid()==original.getMainNode().getGrid(),"original physical cell fixture online");
            var refusing=new FluidTank(1000){@Override public int fill(FluidStack stack,FluidAction action){if(action.simulate())return stack.getAmount();for(var connection:List.copyOf(host.getMainNode().getNode().getConnections()))connection.destroy();host.getMainNode().setExposedOnSides(Set.of());appeng.api.networking.GridHelper.createConnection(host.getMainNode().getNode(),foreign.getMainNode().getNode());return 0;}};
            withCapability(h,target,Capabilities.FluidHandler.BLOCK,refusing,()->{host.addConnection(new OutputConnection(0,h.getLevel().dimension(),target,Direction.WEST));host.setExportMode(OutputInterfaceBlockEntity.ExportMode.AUTO);host.tick();host.setExportMode(OutputInterfaceBlockEntity.ExportMode.OFF);});
        });
        h.runAtTickTime(140,()->{host.tick();
            var foreignStorage=appeng.api.storage.StorageCells.getCellInventory(foreign.getInternalInventory().getStackInSlot(0),foreign::saveChanges);
            var originalStorage=appeng.api.storage.StorageCells.getCellInventory(original.getInternalInventory().getStackInSlot(0),original::saveChanges);
            long oldAmount=originalStorage.extract(water,100,Actionable.SIMULATE,IActionSource.empty()),newAmount=foreignStorage.extract(water,100,Actionable.SIMULATE,IActionSource.empty());
            h.assertTrue(newAmount==0&&oldAmount+host.getPendingRefunds().getOrDefault(water,0L)==20,"refund stays in original physical cell or original-owned private buffer, never foreign cell");h.succeed();
        });
    }
    private static appeng.blockentity.storage.DriveBlockEntity drive(GameTestHelper h,BlockPos pos,AEKey key,long amount){
        h.getLevel().setBlockAndUpdate(pos,appeng.core.definitions.AEBlocks.DRIVE.block().defaultBlockState());
        var drive=(appeng.blockentity.storage.DriveBlockEntity)h.getLevel().getBlockEntity(pos);var cell=appeng.core.definitions.AEItems.FLUID_CELL_1K.stack();
        var storage=appeng.api.storage.StorageCells.getCellInventory(cell,drive::saveChanges);storage.insert(key,amount,Actionable.MODULATE,IActionSource.empty());storage.persist();drive.getInternalInventory().setItemDirect(0,cell);return drive;
    }
    @GameTest(template="empty") public static void memoryCardCopiesOutputConfigurationWithoutCloningOwnedResources(GameTestHelper h){
        var source=place(h);var key=AEItemKey.of(Items.IRON_INGOT);source.getOutputLogic().getConfig().setStack(3,new GenericStack(key,7));source.setUnlimited(3,true);
        source.setInterfaceMode(OutputInterfaceBlockEntity.InterfaceMode.WIRELESS);source.setIOSpeedMode(OutputInterfaceBlockEntity.IOSpeedMode.FAST);source.setExportMode(OutputInterfaceBlockEntity.ExportMode.AUTO);source.setEnergyOutputDirection(Direction.EAST);
        var chest=chest(h,source.getBlockPos().east());var binding=new OutputConnection(3,h.getLevel().dimension(),chest.getBlockPos(),Direction.WEST);source.addConnection(binding);
        source.getUpgrades().setItemDirect(0,appeng.core.definitions.AEItems.CRAFTING_CARD.stack());
        var refunds=new net.minecraft.nbt.CompoundTag();var refundList=new net.minecraft.nbt.ListTag();refundList.add(GenericStack.writeTag(h.getLevel().registryAccess(),new GenericStack(key,2)));refunds.put("OutputRefunds",refundList);source.loadOwnedRefunds(refunds,h.getLevel().registryAccess());
        var builder=net.minecraft.core.component.DataComponentMap.builder();source.exportSettings(appeng.util.SettingsFrom.MEMORY_CARD,builder,null);
        var targetPos=source.getBlockPos().above(2);h.getLevel().setBlockAndUpdate(targetPos,OutputContent.BLOCK.get().defaultBlockState());var target=(OutputInterfaceBlockEntity)h.getLevel().getBlockEntity(targetPos);
        target.importSettings(appeng.util.SettingsFrom.MEMORY_CARD,builder.build(),null);
        h.assertTrue(key.equals(target.getConfiguredKey(3))&&target.isUnlimited(3)&&target.getConnections().contains(binding),"memory card copies slot configuration, unlimited and exact connections");
        h.assertTrue(target.getInterfaceMode()==source.getInterfaceMode()&&target.getIOSpeedMode()==source.getIOSpeedMode()&&target.getExportMode()==source.getExportMode()&&target.getEnergyOutputDirection()==Direction.EAST,"memory card copies modes and direction");
        h.assertTrue(target.getUpgrades().isEmpty()&&target.getPendingRefunds().isEmpty()&&source.getUpgrades().isInstalled(appeng.core.definitions.AEItems.CRAFTING_CARD)&&source.getPendingRefunds().get(key)==2,"memory settings never clone upgrades or private resource ownership");h.succeed();
    }
    @GameTest(template="empty") public static void unloadedConnectionsRemainAndLoadedInvalidConnectionsAreRemoved(GameTestHelper h) {
        var host=place(h);var far=new BlockPos(25_000_000,80,25_000_000);var old=OutputInterfaceConfig.RANGE.get();
        try{OutputInterfaceConfig.RANGE.set(0);var connection=new OutputConnection(35,h.getLevel().dimension(),far,Direction.NORTH);
            h.assertTrue(!h.getLevel().hasChunkAt(far)&&host.addConnection(connection),"unloaded endpoint can be retained without load");host.tick();h.assertTrue(!h.getLevel().hasChunkAt(far)&&host.getConnections().contains(connection),"tick preserves unloaded endpoint and does not force chunk");
            h.assertTrue(!host.addConnection(new OutputConnection(0,h.getLevel().dimension(),host.getBlockPos(),Direction.UP)),"source cannot target itself");
            h.assertTrue(!host.addConnection(new OutputConnection(0,h.getLevel().dimension(),host.getBlockPos().above(),Direction.DOWN)),"loaded non-capability target rejected");h.succeed();
        }finally{OutputInterfaceConfig.RANGE.set(old);}
    }
    private static OutputInterfaceBlockEntity place(GameTestHelper h){var pos=h.absolutePos(new BlockPos(1,1,1));h.getLevel().setBlockAndUpdate(pos,OutputContent.BLOCK.get().defaultBlockState());return (OutputInterfaceBlockEntity)h.getLevel().getBlockEntity(pos);}
    private static void connect(GameTestHelper h,OutputInterfaceBlockEntity host){h.getLevel().setBlockAndUpdate(host.getBlockPos().below(),appeng.core.definitions.AEBlocks.CREATIVE_ENERGY_CELL.block().defaultBlockState());}
    private static ChestBlockEntity chest(GameTestHelper h,BlockPos pos){h.getLevel().setBlockAndUpdate(pos,Blocks.CHEST.defaultBlockState());return (ChestBlockEntity)h.getLevel().getBlockEntity(pos);}
    private static long count(ChestBlockEntity chest,Item item){long count=0;for(int slot=0;slot<chest.getContainerSize();slot++)if(chest.getItem(slot).is(item))count+=chest.getItem(slot).getCount();return count;}
    private static NetworkStorage mount(GameTestHelper h,OutputInterfaceBlockEntity host){h.assertTrue(host.getMainNode().isActive(),"real AE network fixture ready");var storage=new NetworkStorage();host.getMainNode().getGrid().getStorageService().addGlobalStorageProvider(mounts->mounts.mount(storage,0));return storage;}
    private static final class NetworkStorage implements MEStorage {
        final Map<AEKey,Long> stock=new HashMap<>();long refundLimit=Long.MAX_VALUE;
        @Override public Component getDescription(){return Component.literal("output network fixture");}
        @Override public void getAvailableStacks(KeyCounter out){stock.forEach((key,amount)->out.add(key,amount));}
        @Override public long extract(AEKey key,long amount,Actionable mode,IActionSource source){long taken=Math.min(amount,stock.getOrDefault(key,0L));if(mode==Actionable.MODULATE)stock.merge(key,-taken,Long::sum);return taken;}
        @Override public long insert(AEKey key,long amount,Actionable mode,IActionSource source){long inserted=Math.min(amount,refundLimit);if(mode==Actionable.MODULATE)stock.merge(key,inserted,Long::sum);return inserted;}
    }
    private static final class EnergyTarget implements IEnergyStorage {
        int calls,received;
        @Override public int receiveEnergy(int amount,boolean simulate){if(amount<=0)return 0;if(!simulate){calls++;received++;}return 1;}
        @Override public int extractEnergy(int amount,boolean simulate){throw new AssertionError("output must never extract destination FE");}
        @Override public int getEnergyStored(){return received;}
        @Override public int getMaxEnergyStored(){return Integer.MAX_VALUE;}
        @Override public boolean canExtract(){return false;}
        @Override public boolean canReceive(){return true;}
    }
    private static <T> void withCapability(GameTestHelper h,BlockPos pos,BlockCapability<T,Direction> capability,T target,Runnable action){
        h.getLevel().setBlockAndUpdate(pos,Blocks.STONE.defaultBlockState());
        try{var field=BlockCapability.class.getDeclaredField("providers");field.setAccessible(true);
            @SuppressWarnings("unchecked") var providers=(Map<net.minecraft.world.level.block.Block,List<IBlockCapabilityProvider<T,Direction>>>)field.get(capability);
            var original=providers.put(Blocks.STONE,List.of((level,position,state,entity,side)->level==h.getLevel()&&position.equals(pos)?target:null));
            try{action.run();}finally{if(original==null)providers.remove(Blocks.STONE);else providers.put(Blocks.STONE,original);}
        }catch(ReflectiveOperationException failed){throw new IllegalStateException(failed);}
    }
}
