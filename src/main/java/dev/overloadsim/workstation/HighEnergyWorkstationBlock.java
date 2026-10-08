package dev.overloadsim.workstation;

import appeng.block.AEBaseEntityBlock;
import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;

public final class HighEnergyWorkstationBlock extends AEBaseEntityBlock<HighEnergyWorkstationBlockEntity> {
    public static final DirectionProperty FACING=BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty FORMED=BooleanProperty.create("formed");
    public HighEnergyWorkstationBlock(){super(Properties.of().strength(4).sound(SoundType.METAL));registerDefaultState(defaultBlockState().setValue(FACING,Direction.NORTH).setValue(FORMED,false));}
    @Override protected MapCodec<? extends HighEnergyWorkstationBlock> codec(){return simpleCodec(properties->new HighEnergyWorkstationBlock());}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){super.createBlockStateDefinition(builder);builder.add(FACING,FORMED);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext context){return super.getStateForPlacement(context).setValue(FACING,context.getHorizontalDirection().getOpposite());}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new HighEnergyWorkstationBlockEntity(pos,state);}
    @Override public BlockEntityType<HighEnergyWorkstationBlockEntity> getBlockEntityType(){return WorkstationContent.CONTROLLER_ENTITY.get();}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return !level.isClientSide&&type==getBlockEntityType()?(world,pos,blockState,entity)->((HighEnergyWorkstationBlockEntity)entity).tick():null;}
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){if(!level.isClientSide&&level.getBlockEntity(pos) instanceof HighEnergyWorkstationBlockEntity host)WorkstationMenuAccess.open(host,pos,player);return InteractionResult.sidedSuccess(level.isClientSide);}
    @Override public java.util.List<ItemStack> getDrops(BlockState state,LootParams.Builder builder){
        var drops=new ArrayList<>(super.getDrops(state,builder));var entity=builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if(entity instanceof HighEnergyWorkstationBlockEntity host){if(host.suppressControllerDrop()){drops.removeIf(item->item.is(asItem()));return drops;}for(var item:drops)if(item.is(asItem())){var tag=new net.minecraft.nbt.CompoundTag();tag.put("Workstation",host.saveMachine(builder.getLevel().registryAccess()));item.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));host.markDropPrepared();}}return drops;
    }
    @Override public void setPlacedBy(Level level,BlockPos pos,BlockState state,LivingEntity entity,ItemStack item){super.setPlacedBy(level,pos,state,entity,item);var data=item.get(DataComponents.CUSTOM_DATA);if(!level.isClientSide&&data!=null&&data.copyTag().contains("Workstation")&&level.getBlockEntity(pos) instanceof HighEnergyWorkstationBlockEntity host)host.loadMachine(data.copyTag().getCompound("Workstation"),level.registryAccess());}
    @Override public BlockState playerWillDestroy(Level level,BlockPos pos,BlockState state,Player player){
        // Survival loot runs after onRemove. Its captured BE supplies the packed item;
        // creative removal and non-player replacement need the onRemove fallback.
        if(!level.isClientSide&&!player.isCreative()&&player.hasCorrectToolForDrops(state)&&level.getBlockEntity(pos) instanceof HighEnergyWorkstationBlockEntity host)host.markDropPrepared();
        return super.playerWillDestroy(level,pos,state,player);
    }
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){if(!state.is(next.getBlock())&&level.getBlockEntity(pos) instanceof HighEnergyWorkstationBlockEntity host){host.preserveOnReplacement();host.invalidateStructure();}super.onRemove(state,level,pos,next,moving);}
}
