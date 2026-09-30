package dev.overloadsim.mixin;
import dev.overloadsim.api.CrystalDataAccess;
import dev.overloadsim.binding.CrystalBinding;
import com.moakiee.ae2lt.blockentity.LightningCollectorBlockEntity;
import com.moakiee.ae2lt.config.AE2LTCommonConfig;
import com.moakiee.ae2lt.me.key.LightningKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=LightningCollectorBlockEntity.class,remap=false)
public abstract class CollectorMixin {
    @Inject(method="cultivateCrystal",at=@At("HEAD"),cancellable=true)
    private void skipVanilla(RandomSource random,CallbackInfoReturnable<Boolean> cir){if(CrystalDataAccess.isSimulationCrystal(((LightningCollectorBlockEntity)(Object)this).getInstalledCrystal()))cir.setReturnValue(false);}
    @Inject(method="getPreview",at=@At("HEAD"),cancellable=true)
    private void basePreview(LightningKey.Tier tier,CallbackInfoReturnable<LightningCollectorBlockEntity.OutputPreview> cir){
        var be=(LightningCollectorBlockEntity)(Object)this;if(!CrystalDataAccess.isSimulationCrystal(be.getInstalledCrystal()))return;
        boolean ehv=tier==LightningKey.Tier.EXTREME_HIGH_VOLTAGE;int min=ehv?AE2LTCommonConfig.lightningCollectorEhvBaseMin():AE2LTCommonConfig.lightningCollectorHvBaseMin();int max=ehv?AE2LTCommonConfig.lightningCollectorEhvBaseMax():AE2LTCommonConfig.lightningCollectorHvBaseMax();cir.setReturnValue(new LightningCollectorBlockEntity.OutputPreview(Math.min(min,max),Math.max(min,max)));
    }
    @Inject(method="captureLightning",at=@At("RETURN"))
    private void afterCapture(boolean natural,CallbackInfoReturnable<Boolean> cir){
        var be=(LightningCollectorBlockEntity)(Object)this;if(!cir.getReturnValueZ()||!(be.getLevel() instanceof ServerLevel level))return;var stack=be.getInstalledCrystal();if(!CrystalDataAccess.isSimulationCrystal(stack))return;
        var next=stack.is(dev.overloadsim.ModContent.BLANK.get())?CrystalBinding.bindStructure(level,be.getBlockPos(),stack,natural,be::getInstalledCrystal):CrystalBinding.cultivate(level,be.getBlockPos(),stack,natural);
        if(be.getInstalledCrystal()==stack && next!=stack){be.getInventory().setStackInSlot(0,next);be.saveChanges();be.markForClientUpdate();net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new dev.overloadsim.api.SimulationEvents.Completed(stack.is(dev.overloadsim.ModContent.BLANK.get())?"binding":"cultivation",level,be.getBlockPos(),CrystalDataAccess.read(next).orElseThrow()));}
    }
}
