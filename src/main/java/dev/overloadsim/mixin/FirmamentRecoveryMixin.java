package dev.overloadsim.mixin;

import com.moakiee.ae2lt.blockentity.FirmamentConversionCoreBlockEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Recovered cores retain native recipes and inventories when placed in the Overworld. */
@Mixin(value=FirmamentConversionCoreBlockEntity.class,remap=false)
public abstract class FirmamentRecoveryMixin {
    @Inject(method="canProcessHere",at=@At("HEAD"),cancellable=true)
    private void overworldPlacement(CallbackInfoReturnable<Boolean> cir){
        var core=(FirmamentConversionCoreBlockEntity)(Object)this;
        if(!core.isRemoved()&&core.getLevel() instanceof net.minecraft.server.level.ServerLevel level&&level.dimension()==Level.OVERWORLD)cir.setReturnValue(true);
    }
}
