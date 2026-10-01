package dev.overloadsim.mixin;
import com.moakiee.ae2lt.menu.hub.DeviceStatusModel;
import dev.overloadsim.compat.CoilHubAccess;
import dev.overloadsim.tool.CoilModules;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=DeviceStatusModel.class,remap=false)
public class CoilHubStatusMixin {
    @Inject(method="fromRailgunStack(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/server/level/ServerPlayer;I)Lcom/moakiee/ae2lt/menu/hub/DeviceStatusModel;",at=@At("HEAD"),cancellable=true)
    private static void coil(ItemStack s,ServerPlayer p,int selected,CallbackInfoReturnable<DeviceStatusModel> cir){if(CoilModules.isCoil(s))cir.setReturnValue(CoilHubAccess.status(s,p,selected));}
}
