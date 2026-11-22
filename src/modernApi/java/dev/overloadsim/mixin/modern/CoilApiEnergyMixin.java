package dev.overloadsim.mixin.modern;

import com.moakiee.ae2lt.api.device.WorkbenchDevice;
import com.moakiee.ae2lt.blockentity.workbench.AddonWorkbenchAdapter;
import com.moakiee.ae2lt.device.energy.DeviceEnergyBuffer;
import dev.overloadsim.compat.modern.ModernUpstreamApi;
import dev.overloadsim.tool.CoilEnergy;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The public workbench contract exposes energy display/tick, but no FE buffer operations. */
@Mixin(value=AddonWorkbenchAdapter.class,remap=false)
public abstract class CoilApiEnergyMixin {
    @Shadow @Final private WorkbenchDevice device;
    @Inject(method="energyBuffer",at=@At("HEAD"),cancellable=true)
    private void coilEnergy(CallbackInfoReturnable<DeviceEnergyBuffer> result) {
        if(device instanceof ModernUpstreamApi.CoilDevice) result.setReturnValue(CoilEnergy.INSTANCE);
    }
}
