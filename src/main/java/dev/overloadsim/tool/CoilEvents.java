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
    public static void recoveredBlocks(net.neoforged.neoforge.event.level.BlockDropsEvent event){
        if(event.isCanceled()||!(event.getBreaker() instanceof ServerPlayer player)||player.isCreative()
                ||!event.getLevel().getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DOBLOCKDROPS)
                ||!CoilModules.miningReady(event.getTool())||!CoilModules.has(event.getTool(),CoilModuleItem.Type.ULTIMATE))return;
        var state=event.getState();
        if(!state.is(net.minecraft.world.level.block.Blocks.BEDROCK)
                &&!net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).equals(net.minecraft.resources.ResourceLocation.parse("ae2lt:firmament_conversion_core")))return;
        var item=state.getBlock().asItem();
        if(item==net.minecraft.world.item.Items.AIR||event.getDrops().stream().anyMatch(drop->drop.getItem().is(item)))return;
        var pos=event.getPos().getCenter();
        var drop=new net.minecraft.world.entity.item.ItemEntity(event.getLevel(),pos.x,pos.y,pos.z,new net.minecraft.world.item.ItemStack(item));
        drop.setDefaultPickUpDelay();event.getDrops().add(drop);
    }
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
