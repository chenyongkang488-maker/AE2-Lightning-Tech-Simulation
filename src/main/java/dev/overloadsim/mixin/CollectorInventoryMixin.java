package dev.overloadsim.mixin;
import dev.overloadsim.api.CrystalDataAccess;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.moakiee.ae2lt.machine.lightningcollector.LightningCollectorInventory;
@Mixin(value=LightningCollectorInventory.class,remap=false)
public abstract class CollectorInventoryMixin {
    @Inject(method="isItemValid",at=@At("HEAD"),cancellable=true)
    private void allowSimulation(int slot,ItemStack stack,CallbackInfoReturnable<Boolean> cir){if(slot==0&&CrystalDataAccess.isSimulationCrystal(stack))cir.setReturnValue(true);}
}
