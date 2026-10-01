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
    private static dev.overloadsim.multiblock.SimulationControllerBlockEntity powered(GameTestHelper h,int n){
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
    private static net.minecraft.world.item.ItemStack ironCrystal(){return dev.overloadsim.api.CrystalDataAccess.perfect(new dev.overloadsim.api.CrystalData(dev.overloadsim.ModContent.id("iron"),java.util.Optional.empty(),0,1));}
    @GameTest(template="empty",timeoutTicks=110)
    public static void baseCyclePersistsFeesAndPausesWithoutStructure(GameTestHelper h){
        var c=powered(h,3);
        h.runAtTickTime(80,()->{
            h.assertTrue(c.getMainNode().isActive()&&c.energy().getEnergyStored()>0,"idle AE continuously charges FE");
            c.crystals().setStackInSlot(0,ironCrystal());c.crystals().setStackInSlot(1,ironCrystal());c.tick();
            h.assertTrue(c.busy()&&c.batch().cost.fe()==2000&&c.batch().cost.hv()==2&&c.batch().duration==180,"per crystal default cost and duration");
            h.assertTrue(c.bridge().extract(false,Long.MAX_VALUE,true)==998,"exact HV debit");
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
            helper.assertTrue(controller.structure().glass().size()==5*(size-2)*(size-2),"five glass faces");
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
            var block=b>=2||y==0?dev.overloadsim.multiblock.MultiblockContent.FRAME.get():b==1?glass:net.minecraft.world.level.block.Blocks.AIR;
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
