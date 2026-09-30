package dev.overloadsim.tool;

import dev.overloadsim.OverloadSimulation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.entity.player.*;

@EventBusSubscriber(modid=OverloadSimulation.ID)
public final class CoilEvents {
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void breaking(BlockEvent.BreakEvent event){
        var player=event.getPlayer();var stack=player.getMainHandItem();
        if(!event.isCanceled()&&CoilModules.isCoil(stack)&&!CoilMining.ready(stack))event.setCanceled(true);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void leftClick(PlayerInteractEvent.LeftClickBlock event){
        if(!event.isCanceled()&&event.getEntity() instanceof ServerPlayer player&&event.getAction()==PlayerInteractEvent.LeftClickBlock.Action.START&&CoilMining.breakUnbreakable(player,event.getPos()))event.setCanceled(true);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void attacking(AttackEntityEvent event){
        if(!event.isCanceled()&&CoilModules.isCoil(event.getEntity().getMainHandItem())&&!CoilMining.ready(event.getEntity().getMainHandItem()))event.setCanceled(true);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void entityUse(PlayerInteractEvent.EntityInteract event){
        var stack=event.getItemStack();if(event.isCanceled()||!CoilModules.isCoil(stack))return;
        event.setCanceled(true);event.setCancellationResult(stack.getItem().use(event.getLevel(),event.getEntity(),event.getHand()).getResult());
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void entitySpecific(PlayerInteractEvent.EntityInteractSpecific event){
        var stack=event.getItemStack();if(event.isCanceled()||!CoilModules.isCoil(stack))return;
        event.setCanceled(true);event.setCancellationResult(stack.getItem().use(event.getLevel(),event.getEntity(),event.getHand()).getResult());
    }
}
