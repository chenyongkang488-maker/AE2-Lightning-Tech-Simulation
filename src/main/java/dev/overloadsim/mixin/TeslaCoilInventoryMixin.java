package dev.overloadsim.mixin;

import com.moakiee.ae2lt.machine.teslacoil.TeslaCoilInventory;
import dev.overloadsim.ModContent;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The upstream predicate also routes native menu shift-click and automation insertion. */
@Mixin(value = TeslaCoilInventory.class, remap = false)
public abstract class TeslaCoilInventoryMixin {
    @Inject(method = "isOverloadCrystalDust", at = @At("HEAD"), cancellable = true)
    private void overloadSim$acceptTranscendent(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (!stack.isEmpty() && stack.is(ModContent.TRANSCENDENT.get())) cir.setReturnValue(true);
    }
}
