package dev.overloadsim.mixin;
import appeng.util.InteractionUtil;
import dev.overloadsim.tool.*;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=InteractionUtil.class,remap=false)
public class CoilAEWrenchMixin {
    @Inject(method={"canWrenchDisassemble","canWrenchRotate"},at=@At("HEAD"),cancellable=true)
    private static void coil(ItemStack stack,CallbackInfoReturnable<Boolean> cir){if(CoilModules.isCoil(stack))cir.setReturnValue(CoilWrench.active(stack));}
}
