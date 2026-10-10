package dev.overloadsim.output;

import appeng.block.AEBaseEntityBlock;
import appeng.menu.locator.MenuLocators;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class OutputInterfaceBlock extends AEBaseEntityBlock<OutputInterfaceBlockEntity> {
    public OutputInterfaceBlock() { super(metalProps().forceSolidOn()); }

    @Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
            BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        var host = getBlockEntity(level, pos);
        if (host == null || host.getUpgrades().getMaxInstalled(stack.getItem()) <= 0)
            return super.useItemOn(stack, state, level, pos, player, hand, hit);
        if (!level.isClientSide) {
            var remainder = host.getUpgrades().addItems(stack.copy());
            if (remainder.getCount() == stack.getCount()) return ItemInteractionResult.FAIL;
            if (!player.getAbilities().instabuild) player.setItemInHand(hand, remainder);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit) {
        var host=getBlockEntity(level,pos);
        if(host==null)return InteractionResult.PASS;
        if(!level.isClientSide)host.openMenu(player,MenuLocators.forBlockEntity(host));
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public java.util.List<net.minecraft.world.item.ItemStack> getDrops(BlockState state,net.minecraft.world.level.storage.loot.LootParams.Builder builder) {
        var drops=new java.util.ArrayList<>(super.getDrops(state,builder));
        var entity=builder.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY);
        if(entity instanceof OutputInterfaceBlockEntity host){
            if(host.suppressOutputDrop()){drops.removeIf(item->item.is(asItem()));return drops;}
            for(var item:drops)if(item.is(asItem())&&!host.getPendingRefunds().isEmpty()){
                var tag=new net.minecraft.nbt.CompoundTag();tag.put("OutputOwnedRefunds",host.saveOwnedRefunds(builder.getLevel().registryAccess()));
                item.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(tag));
            }
            host.markDropPrepared();
        }
        return drops;
    }
    @Override public void setPlacedBy(Level level,BlockPos pos,BlockState state,net.minecraft.world.entity.LivingEntity placer,net.minecraft.world.item.ItemStack item){
        super.setPlacedBy(level,pos,state,placer,item);
        var data=item.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        if(!level.isClientSide&&data!=null&&data.copyTag().contains("OutputOwnedRefunds")&&level.getBlockEntity(pos) instanceof OutputInterfaceBlockEntity host)
            host.loadOwnedRefunds(data.copyTag().getCompound("OutputOwnedRefunds"),level.registryAccess());
    }
    @Override public BlockState playerWillDestroy(Level level,BlockPos pos,BlockState state,Player player){
        if(!level.isClientSide&&!player.isCreative()&&player.hasCorrectToolForDrops(state)&&level.getBlockEntity(pos) instanceof OutputInterfaceBlockEntity host)host.markDropPrepared();
        return super.playerWillDestroy(level,pos,state,player);
    }
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){
        if(!state.is(next.getBlock())&&level.getBlockEntity(pos) instanceof OutputInterfaceBlockEntity host)host.preserveOnReplacement();
        super.onRemove(state,level,pos,next,moving);
    }
}
