package dev.overloadsim.tool;
import dev.overloadsim.compat.MekanismCoilCompat;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.ItemAbility;

public final class CoilWrench {
    private CoilWrench(){}
    public static boolean active(ItemStack stack){return CoilModules.isCoil(stack)&&CoilSettings.read(stack).wrenchActive(stack);}
    public static boolean mekanism(){return ModList.get().isLoaded("mekanism");}
    public static boolean ability(ItemStack stack,ItemAbility ability){return active(stack)&&mekanism()&&MekanismCoilCompat.ability(stack,ability);}
    public static boolean cycleMode(ServerPlayer player,int slot,int steps){
        if(!player.isAlive()||player.isSpectator()||!player.isShiftKeyDown()||player.containerMenu!=player.inventoryMenu
            ||slot<0||slot>8||player.getInventory().selected!=slot||steps==0||steps<-8||steps>8)return false;
        var stack=player.getMainHandItem();if(!active(stack))return false;
        int mode=Math.floorMod(CoilSettings.read(stack).wrenchMode()+steps,8);
        if(!CoilConfiguration.apply(stack,6,mode))return false;
        player.getInventory().setChanged();player.inventoryMenu.broadcastChanges();
        player.displayClientMessage(Component.translatable("message.overload_sim.coil.wrench_mode",Component.translatable("gui.overload_sim.coil.wrench_mode."+mode)),true);
        return true;
    }
    public static InteractionResult use(UseOnContext context){
        var p=context.getPlayer();if(p==null||!active(context.getItemInHand()))return InteractionResult.FAIL;
        if(!context.getLevel().mayInteract(p,context.getClickedPos())||!p.mayUseItemAt(context.getClickedPos(),context.getClickedFace(),context.getItemInHand()))return InteractionResult.FAIL;
        return mekanism()?MekanismCoilCompat.use(context):InteractionResult.PASS;
    }
}
