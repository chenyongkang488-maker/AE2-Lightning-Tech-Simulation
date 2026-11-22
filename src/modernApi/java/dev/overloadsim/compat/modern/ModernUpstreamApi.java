package dev.overloadsim.compat.modern;

import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import com.moakiee.ae2lt.api.device.DeviceWorkbenchApi;
import com.moakiee.ae2lt.api.device.WorkbenchDevice;
import com.moakiee.ae2lt.api.event.LightningCaptureCompletedEvent;
import com.moakiee.ae2lt.api.lightning.LightningApi;
import com.moakiee.ae2lt.api.lightning.LightningTier;
import com.moakiee.ae2lt.api.lightning.collector.CollectorCrystalApi;
import com.moakiee.ae2lt.api.lightning.collector.CollectorCrystalBehavior;
import dev.overloadsim.ModContent;
import dev.overloadsim.api.CrystalDataAccess;
import dev.overloadsim.api.SimulationEvents;
import dev.overloadsim.binding.CrystalBinding;
import dev.overloadsim.compat.UpstreamApi;
import dev.overloadsim.tool.CoilEnergy;
import dev.overloadsim.tool.CoilModules;
import java.util.HashMap;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;

/** Linked only when all three upstream public extension APIs exist. */
public final class ModernUpstreamApi implements UpstreamApi {
    private record CaptureKey(ServerLevel level,BlockPos pos) {}
    private record Pending(ItemStack result,String operation) {}
    private final HashMap<CaptureKey,Pending> completions=new HashMap<>();

    @Override public void register() {
        var behavior=new CollectorCrystalBehavior() {
            @Override public OutputRange preview(ItemStack crystal,LightningTier tier,OutputRange base) { return base; }
            @Override public ItemStack onCaptured(Capture context,ItemStack original) {
                if(context.insertedAmount()<=0||context.installedCrystal().get()!=original) return original;
                var next=original.is(ModContent.BLANK.get())
                        ? CrystalBinding.bindStructure(context.level(),context.collectorPos(),original,context.naturalWeather(),context.installedCrystal())
                        : CrystalBinding.cultivate(context.level(),context.collectorPos(),original,context.naturalWeather());
                if(context.installedCrystal().get()!=original) return original;
                if(next!=original) completions.put(new CaptureKey(context.level(),context.collectorPos()),
                        new Pending(next,original.is(ModContent.BLANK.get())?"binding":"cultivation"));
                return next;
            }
        };
        for(var item:List.of(ModContent.BLANK.get(),ModContent.BOUND.get(),ModContent.PERFECT.get()))
            CollectorCrystalApi.register(BuiltInRegistries.ITEM.getKey(item),behavior);
        // This observer only publishes our completion after the API has installed the returned crystal.
        // Cultivation happens exclusively in onCaptured, never again in this success event.
        NeoForge.EVENT_BUS.addListener(this::captureCompleted);
        DeviceWorkbenchApi.register(BuiltInRegistries.ITEM.getKey(ModContent.COIL.get()),new CoilDevice());
    }

    private void captureCompleted(LightningCaptureCompletedEvent event) {
        var pending=completions.remove(new CaptureKey(event.getLevel(),event.getCollectorPos()));
        if(pending==null) return;
        if(event.getLevel().getBlockEntity(event.getCollectorPos()) instanceof com.moakiee.ae2lt.blockentity.LightningCollectorBlockEntity collector
                &&ItemStack.matches(collector.getInstalledCrystal(),pending.result()))
            CrystalDataAccess.read(pending.result()).ifPresent(data->NeoForge.EVENT_BUS.post(new SimulationEvents.Completed(
                    pending.operation(),event.getLevel(),event.getCollectorPos(),data)));
    }

    @Override public AEKey key(boolean extreme) { return LightningApi.keyOf(tier(extreme)); }
    @Override public long extract(IActionHost host,boolean extreme,long amount,boolean simulate,IActionSource source) {
        return amount<=0?0:LightningApi.forHost(host,source).extract(tier(extreme),amount,simulate);
    }
    private static LightningTier tier(boolean extreme) { return extreme?LightningTier.EXTREME_HIGH_VOLTAGE:LightningTier.HIGH_VOLTAGE; }

    public static final class CoilDevice implements WorkbenchDevice {
        @Override public ItemStack core(ItemStack device) { return CoilModules.core(device); }
        @Override public boolean canPlaceCore(ItemStack device,ItemStack core) {
            return CoilModules.isCoil(device)&&core.is(com.moakiee.ae2lt.registry.ModItems.ULTIMATE_OVERLOAD_CORE.get());
        }
        @Override public void setCore(ItemStack device,ItemStack core) { CoilModules.setCore(device,core); }
        @Override public List<ItemStack> modules(ItemStack device) { return CoilModules.entries(device); }
        @Override public String moduleId(ItemStack module) { return CoilModules.typeId(module); }
        @Override public int maxInstallAmount(ItemStack module) { return moduleId(module).isEmpty()?0:1; }
        @Override public boolean canInstallOne(ItemStack device,ItemStack module) { return CoilModules.canInstall(device,module); }
        @Override public boolean installOne(ItemStack device,ItemStack module) { return CoilModules.install(device,module); }
        @Override public ItemStack uninstallOne(ItemStack device,String id) { return CoilModules.uninstall(device,id); }
        @Override public ItemStack uninstallAll(ItemStack device,String id) { return CoilModules.uninstall(device,id); }
        @Override public long storedEnergy(ItemStack device) { return CoilEnergy.read(device); }
        @Override public long energyCapacity(ItemStack device) { return CoilModules.capacity(device); }
        @Override public void onInserted(ItemStack device) { CoilEnergy.clamp(device); }
        @Override public void onModulesChanged(ItemStack device,boolean clientSide) { CoilEnergy.clamp(device); }
        @Override public boolean serverTick(ItemStack device,Context context) {
            if(!CoilModules.hasCore(device)||context.grid()==null) return false;
            var before=device.copy();
            // Resolve the actual workbench host for the same machine security source as the old path.
            if(!(context.level().getBlockEntity(context.pos()) instanceof IActionHost host)) return false;
            CoilEnergy.charge(device,context.grid(),IActionSource.ofMachine(host));
            return !ItemStack.matches(before,device); // Includes retained FE credit, not only visible energy.
        }
    }
}
