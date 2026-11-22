package dev.overloadsim.gametest;

import com.moakiee.ae2lt.blockentity.workbench.DeviceWorkbenchAdapters;
import dev.overloadsim.ModContent;
import dev.overloadsim.OverloadSimulation;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(OverloadSimulation.ID)
@PrefixGameTestTemplate(false)
@net.neoforged.fml.common.EventBusSubscriber(modid=OverloadSimulation.ID,bus=net.neoforged.fml.common.EventBusSubscriber.Bus.MOD)
public final class UpstreamApiGameTests {
    private static com.moakiee.ae2lt.blockentity.LightningCollectorBlockEntity collector(GameTestHelper h,long initial) {
        var pos=new net.minecraft.core.BlockPos(5,3,5);
        h.setBlock(pos,com.moakiee.ae2lt.registry.ModBlocks.LIGHTNING_COLLECTOR.get());
        h.setBlock(pos.below(),appeng.core.definitions.AEBlocks.CREATIVE_ENERGY_CELL.block());
        h.setBlock(pos.north(),appeng.core.definitions.AEBlocks.DRIVE.block());
        var cell=new ItemStack(com.moakiee.ae2lt.registry.ModItems.BULK_LIGHTNING_STORAGE_COMPONENT.get());
        var storage=appeng.api.storage.StorageCells.getCellInventory(cell,null);
        if(initial>0) storage.insert(com.moakiee.ae2lt.me.key.LightningKey.HIGH_VOLTAGE,initial,
                appeng.api.config.Actionable.MODULATE,appeng.api.networking.security.IActionSource.empty());
        storage.persist();
        ((appeng.blockentity.storage.DriveBlockEntity)h.getBlockEntity(pos.north())).getInternalInventory().setItemDirect(0,cell);
        var collector=(com.moakiee.ae2lt.blockentity.LightningCollectorBlockEntity)h.getBlockEntity(pos);
        collector.getInventory().setStackInSlot(0,dev.overloadsim.api.CrystalDataAccess.bound(
                new dev.overloadsim.api.CrystalData(ModContent.id("iron"),java.util.Optional.empty(),0,1)));
        return collector;
    }

    @GameTest(template="empty",timeoutTicks=100)
    public static void partialCaptureCultivatesOnceAndCompletionSeesInstalledResult(GameTestHelper h) {
        var collector=collector(h,Long.MAX_VALUE-1);
        h.runAtTickTime(40,()->{
            int[] count={0};
            java.util.function.Consumer<dev.overloadsim.api.SimulationEvents.Completed> listener=e->{
                if(e.level!=h.getLevel()||!e.position.equals(collector.getBlockPos())) return;
                h.assertTrue(dev.overloadsim.api.CrystalDataAccess.read(collector.getInstalledCrystal()).orElseThrow().equals(e.data),
                        "completion fires after actual crystal replacement");
                count[0]++;
            };
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(listener);
            try {
                h.assertTrue(collector.captureLightning(false),"one remaining storage unit permits partial capture");
                h.assertTrue(dev.overloadsim.api.CrystalDataAccess.read(collector.getInstalledCrystal()).orElseThrow().strikes()==1,
                        "partial success cultivates exactly one strike");
                h.assertTrue(count[0]==1&&!collector.captureLightning(false),"one completion and cooldown blocks duplicate capture");
            } finally { net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(listener); }
            h.succeed();
        });
    }

    @GameTest(template="empty",timeoutTicks=100)
    public static void fullCollectorStorageCannotCultivate(GameTestHelper h) {
        var collector=collector(h,Long.MAX_VALUE);
        h.runAtTickTime(40,()->{
            var before=collector.getInstalledCrystal().copy();
            h.assertTrue(!collector.captureLightning(false)&&ItemStack.matches(before,collector.getInstalledCrystal()),
                    "zero actual insertion must not cultivate");
            h.succeed();
        });
    }

    @GameTest(template="empty",timeoutTicks=100)
    public static void canceledCaptureCannotCultivate(GameTestHelper h) {
        var collector=collector(h,0);
        h.runAtTickTime(40,()->{
            var before=collector.getInstalledCrystal().copy();
            java.util.function.Consumer<com.moakiee.ae2lt.api.event.LightningCollectedEvent> cancel=e->{
                if(e.getCollectorPos().equals(collector.getBlockPos())) e.setCanceled(true);
            };
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(cancel);
            try {
                h.assertTrue(!collector.captureLightning(false)&&ItemStack.matches(before,collector.getInstalledCrystal()),
                        "canceled capture neither inserts nor cultivates");
            } finally { net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(cancel); }
            h.succeed();
        });
    }

    @GameTest(template="empty",timeoutTicks=220)
    public static void unavailableStorageReportsActualRefundAndRetriesAfterPowerReturns(GameTestHelper h) {
        var collector=collector(h,0);
        h.runAtTickTime(40,()->{
            var cellPos=collector.getBlockPos().below();
            h.getLevel().setBlockAndUpdate(cellPos,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
            h.runAfterDelay(20,()->{
                h.assertTrue(collector.getMainNode().getGrid()!=null&&!collector.getMainNode().isActive(),"retained storage grid is inactive");
                h.assertTrue(dev.overloadsim.compat.LightningNetwork.refund(collector,7,false)==0,
                        "unpowered drive is unmounted: no accepted HV may be claimed");
                h.assertTrue(dev.overloadsim.compat.LightningNetwork.refund(collector,9,true)==0,
                        "unavailable EHV refund also reports zero actual acceptance");
                h.assertTrue(dev.overloadsim.compat.LightningNetwork.extract(collector,7,false)==0,
                        "ordinary resource payment still requires active node");
                h.getLevel().setBlockAndUpdate(cellPos,appeng.core.definitions.AEBlocks.CREATIVE_ENERGY_CELL.block().defaultBlockState());
                // Replacing an AE node restarts network initialization; use the same 80-tick
                // readiness interval as the existing network fixtures, rather than 20.
                h.runAfterDelay(80,()->{
                    long hv=dev.overloadsim.compat.LightningNetwork.refund(collector,7,false);
                    long ehv=dev.overloadsim.compat.LightningNetwork.refund(collector,9,true);
                    var drive=(appeng.blockentity.storage.DriveBlockEntity)h.getLevel().getBlockEntity(collector.getBlockPos().north());
                    var cell=appeng.api.storage.StorageCells.getCellInventory(drive.getInternalInventory().getStackInSlot(0),null);
                    long direct=cell.insert(com.moakiee.ae2lt.me.key.LightningKey.HIGH_VOLTAGE,7,
                            appeng.api.config.Actionable.SIMULATE,appeng.api.networking.security.IActionSource.empty());
                    h.assertTrue(hv==7&&ehv==9,
                            "refund readiness hv="+hv+" ehv="+ehv+" hostActive="+collector.getMainNode().isActive()
                            +" driveActive="+drive.getMainNode().isActive()+" sameGrid="+(collector.getMainNode().getGrid()==drive.getMainNode().getGrid())+" directCellCapacity="+direct);
                    h.assertTrue(dev.overloadsim.compat.LightningNetwork.extract(collector,7,true)==7,
                            "simulation sees refunded HV after power returns");
                    h.assertTrue(dev.overloadsim.compat.LightningNetwork.extract(collector,7,false)==7
                            &&dev.overloadsim.compat.LightningNetwork.extract(collector,1,false)==0,
                            "simulation did not debit and EHV cannot cover HV deficit");
                    h.succeed();
                });
            });
        });
    }

    @GameTest(template="empty",timeoutTicks=220)
    public static void unpaidRefundSurvivesSaveAndRetriesExactlyOnce(GameTestHelper h) {
        var pos=new net.minecraft.core.BlockPos(5,3,5);
        h.setBlock(pos,ModContent.CHAMBER.get());
        h.setBlock(pos.below(),appeng.core.definitions.AEBlocks.CREATIVE_ENERGY_CELL.block());
        h.setBlock(pos.north(),appeng.core.definitions.AEBlocks.DRIVE.block());
        var drive=(appeng.blockentity.storage.DriveBlockEntity)h.getBlockEntity(pos.north());
        drive.getInternalInventory().setItemDirect(0,new ItemStack(com.moakiee.ae2lt.registry.ModItems.BULK_LIGHTNING_STORAGE_COMPONENT.get()));
        var machine=(dev.overloadsim.machine.SimulationChamberBlockEntity)h.getBlockEntity(pos);
        h.runAtTickTime(40,()->{
            h.getLevel().setBlockAndUpdate(machine.getBlockPos().below(),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
            h.runAfterDelay(20,()->{
                var saved=machine.saveWithoutMetadata(h.getLevel().registryAccess());
                saved.putLong("LightningRefund",7);saved.putString("LightningRefundTier","hv");
                machine.loadTag(saved,h.getLevel().registryAccess());machine.tick();
                var pending=machine.saveWithoutMetadata(h.getLevel().registryAccess());
                h.assertTrue(pending.getLong("LightningRefund")==7,"failed offline return preserves full durable debt");
                machine.loadTag(pending,h.getLevel().registryAccess());
                h.getLevel().setBlockAndUpdate(machine.getBlockPos().below(),appeng.core.definitions.AEBlocks.CREATIVE_ENERGY_CELL.block().defaultBlockState());
                h.runAfterDelay(80,()->{
                    h.assertTrue(machine.saveWithoutMetadata(h.getLevel().registryAccess()).getLong("LightningRefund")==0,
                            "restored debt readiness: debt="+machine.saveWithoutMetadata(h.getLevel().registryAccess()).getLong("LightningRefund")
                            +" hostActive="+machine.getMainNode().isActive()+" driveActive="+drive.getMainNode().isActive());
                    h.assertTrue(dev.overloadsim.compat.LightningNetwork.extract(machine,Long.MAX_VALUE,true)==7,
                            "saved offline refund is returned exactly once");
                    machine.tick();machine.tick();
                    h.assertTrue(dev.overloadsim.compat.LightningNetwork.extract(machine,Long.MAX_VALUE,true)==7,
                            "repeated updates cannot duplicate already returned lightning");
                    h.succeed();
                });
            });
        });
    }

    @GameTest(template="empty",timeoutTicks=100)
    public static void cultivationListenerReplacementIsNeverOverwritten(GameTestHelper h) {
        var collector=collector(h,0);
        h.runAtTickTime(40,()->{
            var replacement=dev.overloadsim.api.CrystalDataAccess.perfect(new dev.overloadsim.api.CrystalData(
                    ModContent.id("gold"),java.util.Optional.empty(),0,1));
            java.util.function.Consumer<dev.overloadsim.api.SimulationEvents.BeforeCultivation> swap=e->{
                if(e.level==h.getLevel()&&e.position.equals(collector.getBlockPos())) collector.getInventory().setStackInSlot(0,replacement);
            };
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(swap);
            try {
                h.assertTrue(collector.captureLightning(false),"capture still commits");
                h.assertTrue(ItemStack.matches(collector.getInstalledCrystal(),replacement),"listener replacement must survive callback return");
            } finally { net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(swap); }
            h.succeed();
        });
    }
    @net.neoforged.bus.api.SubscribeEvent
    public static void setupProbe(net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) {
        if (!"overload_sim".equals(System.getProperty("neoforge.enabledGameTestNamespaces"))) return;
        event.enqueueWork(() -> {
            if (!hasApi()) return;
            try {
                var contract=Class.forName("com.moakiee.ae2lt.api.device.WorkbenchDevice");
                var probe=java.lang.reflect.Proxy.newProxyInstance(contract.getClassLoader(),new Class<?>[]{contract},(p,m,a)->{
                    return switch(m.getName()) {
                        case "hasCoreSlot","canPlaceCore","mayRemoveCore","canInstallOne","installOne" -> false;
                        case "core","uninstallOne","uninstallAll" -> ItemStack.EMPTY;
                        case "modules" -> java.util.List.of();
                        case "moduleId" -> "";
                        case "maxInstallAmount" -> 0;
                        case "storedEnergy","energyCapacity" -> 0L;
                        case "serverTick" -> {
                            var stack=(ItemStack)a[0];
                            stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,
                                    net.minecraft.network.chat.Component.literal("native API tick"));
                            yield true;
                        }
                        default -> null;
                    };
                });
                Class.forName("com.moakiee.ae2lt.api.device.DeviceWorkbenchApi")
                        .getMethod("register",net.minecraft.resources.ResourceLocation.class,contract)
                        .invoke(null,net.minecraft.resources.ResourceLocation.parse("minecraft:stick"),probe);
            } catch(ReflectiveOperationException error) { throw new RuntimeException(error); }
        });
    }

    @GameTest(template="empty",timeoutTicks=100)
    public static void ordinaryRegisteredDeviceKeepsItsNativeTick(GameTestHelper h) {
        if(!hasApi()) { h.succeed(); return; }
        var pos=new net.minecraft.core.BlockPos(5,3,5);
        h.setBlock(pos,com.moakiee.ae2lt.registry.ModBlocks.OVERLOAD_DEVICE_WORKBENCH.get());
        h.setBlock(pos.below(),appeng.core.definitions.AEBlocks.CREATIVE_ENERGY_CELL.block());
        var bench=(com.moakiee.ae2lt.blockentity.OverloadDeviceWorkbenchBlockEntity)h.getBlockEntity(pos);
        var stick=new ItemStack(net.minecraft.world.item.Items.STICK);
        bench.getDeviceInventory().setItemDirect(0,stick);
        h.runAtTickTime(40,()->{
            h.assertTrue(stick.getHoverName().getString().equals("native API tick"),
                    "addon must not replace the upstream workbench ticker; other registered devices still tick");
            h.succeed();
        });
    }
    private static boolean hasApi() {
        try { Class.forName("com.moakiee.ae2lt.api.device.DeviceWorkbenchApi"); return true; }
        catch (ClassNotFoundException missing) { return false; }
    }

    @GameTest(template="empty")
    public static void coilUsesPublicAdapterOnlyWhenAvailable(GameTestHelper h) {
        var adapter=DeviceWorkbenchAdapters.get(new ItemStack(ModContent.COIL.get())).orElseThrow();
        h.assertTrue(adapter.getClass().getName().equals(hasApi()
                ? "com.moakiee.ae2lt.blockentity.workbench.AddonWorkbenchAdapter"
                : "dev.overloadsim.compat.CoilWorkbenchAdapter"),
                "exactly one adapter path: new API when present, legacy otherwise; actual="+adapter.getClass().getName());
        h.succeed();
    }

    @GameTest(template="empty")
    public static void allSimulationCrystalsUseRegisteredBehavior(GameTestHelper h) {
        if (!hasApi()) { h.succeed(); return; }
        try {
            var find=Class.forName("com.moakiee.ae2lt.api.lightning.collector.CollectorCrystalApi").getMethod("find",ItemStack.class);
            for(var item:java.util.List.of(ModContent.BLANK.get(),ModContent.BOUND.get(),ModContent.PERFECT.get()))
                h.assertTrue(find.invoke(null,new ItemStack(item))!=null,"simulation crystal registered via upstream API: "+item);
            h.assertTrue(find.invoke(null,new ItemStack(net.minecraft.world.item.Items.DIAMOND))==null,
                    "ordinary items keep upstream admission behavior");
        } catch (ReflectiveOperationException error) { throw new RuntimeException(error); }
        h.succeed();
    }
}
