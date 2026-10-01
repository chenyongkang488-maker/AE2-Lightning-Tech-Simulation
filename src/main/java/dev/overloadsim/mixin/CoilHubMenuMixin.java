package dev.overloadsim.mixin;
import com.moakiee.ae2lt.menu.hub.DeviceHubMenu;
import dev.overloadsim.compat.CoilHubAccess;
import dev.overloadsim.tool.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(value=DeviceHubMenu.class,remap=false)
public class CoilHubMenuMixin implements CoilHubAccess.Configuration {
    @Shadow private int selectedTab;
    @Shadow private Player trackedPlayer;
    @Unique private ItemStack overloadSim$original=ItemStack.EMPTY;
    @Inject(method="<init>(ILnet/minecraft/world/entity/player/Inventory;I)V",at=@At("RETURN"))
    private void bind(int id,Inventory inv,int tab,CallbackInfo ci){overloadSim$original=CoilHubAccess.weapon(inv.player);}
    @Inject(method="findRailgun",at=@At("HEAD"),cancellable=true)
    private static void coil(Player p,CallbackInfoReturnable<ItemStack> cir){var s=CoilHubAccess.weapon(p);if(CoilModules.isCoil(s))cir.setReturnValue(s);}
    @Override public boolean overloadSim$configure(ServerPlayer p,int action,int value){
        if(p!=trackedPlayer||!p.isAlive()||p.containerMenu!=(Object)this||selectedTab!=DeviceHubMenu.TAB_RAILGUN||CoilHubAccess.weapon(p)!=overloadSim$original)return false;
        return CoilConfiguration.apply(overloadSim$original,action,value);
    }
    @Inject(method={"toggleRailgunTerrain","toggleRailgunPvp","toggleRailgunSound","toggleRailgunChainDamage","cycleRailgunExecutionMode","toggleRailgunChargedSplash"},at=@At("HEAD"),cancellable=true)
    private void rejectGunAction(CallbackInfo ci){if(trackedPlayer!=null&&CoilModules.isCoil(CoilHubAccess.weapon(trackedPlayer)))ci.cancel();}
}
