package dev.overloadsim.compat;
import dev.overloadsim.tool.*;
import mekanism.common.item.ItemConfigurator.ConfiguratorMode;
import mekanism.common.registries.MekanismItems;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.neoforged.neoforge.common.ItemAbility;

/** Loaded only after ModList confirms Mekanism. Native configurator keeps security and side rules. */
public final class MekanismCoilCompat {
    private MekanismCoilCompat(){}
    private static ConfiguratorMode mode(ItemStack coil){int n=CoilSettings.read(coil).wrenchMode();return ConfiguratorMode.values()[n==0?7:n-1];}
    private static ItemStack proxy(ItemStack coil){var item=MekanismItems.CONFIGURATOR.get();var proxy=new ItemStack(item);proxy.set(item.getModeDataType(),mode(coil));return proxy;}
    public static boolean ability(ItemStack coil,ItemAbility ability){var proxy=proxy(coil);return proxy.canPerformAction(ability);}
    public static InteractionResult use(UseOnContext context){
        var proxy=proxy(context.getItemInHand());
        var hit=new net.minecraft.world.phys.BlockHitResult(context.getClickLocation(),context.getClickedFace(),context.getClickedPos(),context.isInside());
        // Never persist a component owned by the optional mod onto the addon item.
        var nativeContext=new UseOnContext(context.getPlayer(),context.getHand(),hit){@Override public ItemStack getItemInHand(){return proxy;}};
        return MekanismItems.CONFIGURATOR.get().useOn(nativeContext);
    }
}
