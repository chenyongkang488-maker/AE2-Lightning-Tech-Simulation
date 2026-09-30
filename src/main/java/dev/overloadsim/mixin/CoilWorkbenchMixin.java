package dev.overloadsim.mixin;
import java.util.Optional;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.moakiee.ae2lt.blockentity.workbench.*;
import net.minecraft.world.item.ItemStack;
import dev.overloadsim.compat.CoilWorkbenchAdapter;
import dev.overloadsim.tool.CoilModules;

@Mixin(value=DeviceWorkbenchAdapters.class,remap=false)
public class CoilWorkbenchMixin {
    @Inject(method="get",at=@At("HEAD"),cancellable=true)
    private static void coilAdapter(ItemStack stack,CallbackInfoReturnable<Optional<DeviceWorkbenchAdapter>> cir){
        if(stack!=null&&CoilModules.isCoil(stack))cir.setReturnValue(Optional.of(CoilWorkbenchAdapter.INSTANCE));
    }
}
