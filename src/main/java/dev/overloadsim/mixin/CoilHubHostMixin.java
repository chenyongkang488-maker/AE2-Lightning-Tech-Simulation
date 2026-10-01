package dev.overloadsim.mixin;
import com.moakiee.ae2lt.menu.hub.DeviceHubHost;
import dev.overloadsim.compat.CoilHubAccess;
import dev.overloadsim.tool.CoilModules;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=DeviceHubHost.class,remap=false)
public class CoilHubHostMixin {
    @Inject(method="hasRailgun",at=@At("HEAD"),cancellable=true)
    private static void coil(Player player,CallbackInfoReturnable<Boolean> cir){if(CoilModules.isCoil(CoilHubAccess.weapon(player)))cir.setReturnValue(true);}
}
