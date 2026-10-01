package dev.overloadsim.multiblock;
import com.mojang.serialization.MapCodec;
import appeng.block.AEBaseEntityBlock;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
public class SimulationControllerBlock extends AEBaseEntityBlock<SimulationControllerBlockEntity> {
    public static final DirectionProperty FACING=BlockStateProperties.HORIZONTAL_FACING;
    public SimulationControllerBlock(){super(Properties.of().strength(4).sound(SoundType.METAL));registerDefaultState(defaultBlockState().setValue(FACING,Direction.NORTH));}
    @Override protected MapCodec<? extends SimulationControllerBlock> codec(){return simpleCodec(p->new SimulationControllerBlock());}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){super.createBlockStateDefinition(b);b.add(FACING);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext context){return super.getStateForPlacement(context).setValue(FACING,context.getHorizontalDirection().getOpposite());}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new SimulationControllerBlockEntity(p,s);}
    @Override public BlockEntityType<SimulationControllerBlockEntity> getBlockEntityType(){return MultiblockContent.CONTROLLER_ENTITY.get();}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return !level.isClientSide&&type==getBlockEntityType()?(l,p,s,be)->((SimulationControllerBlockEntity)be).tick():null;}
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){
        if(!level.isClientSide&&level.getBlockEntity(pos) instanceof SimulationControllerBlockEntity c){c.checkStructure();player.displayClientMessage(net.minecraft.network.chat.Component.literal(c.structure()==null?"结构未成型："+c.error():"模拟室 "+c.structure().size()+" × "+c.structure().size()+" × "+c.structure().size()),true);}
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public void onRemove(BlockState s,Level l,BlockPos p,BlockState next,boolean moving){if(!s.is(next.getBlock())&&l.getBlockEntity(p) instanceof SimulationControllerBlockEntity c)c.invalidateStructure();super.onRemove(s,l,p,next,moving);}
    @Override public java.util.List<net.minecraft.world.item.ItemStack> getDrops(BlockState state,net.minecraft.world.level.storage.loot.LootParams.Builder builder){
        var drops=new java.util.ArrayList<>(super.getDrops(state,builder));
        var be=builder.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY);
        if(be instanceof SimulationControllerBlockEntity c)for(var item:drops)if(item.is(MultiblockContent.CONTROLLER_ITEM.get())){
            var tag=new net.minecraft.nbt.CompoundTag();tag.put("SimulationMachine",c.saveMachine(builder.getLevel().registryAccess()));
            item.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(tag));
        }return drops;
    }
    @Override public void setPlacedBy(Level level,BlockPos pos,BlockState state,net.minecraft.world.entity.LivingEntity entity,net.minecraft.world.item.ItemStack item){
        super.setPlacedBy(level,pos,state,entity,item);
        var data=item.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        if(!level.isClientSide&&data!=null&&data.copyTag().contains("SimulationMachine")&&level.getBlockEntity(pos) instanceof SimulationControllerBlockEntity c)
            c.loadMachine(data.copyTag().getCompound("SimulationMachine"),level.registryAccess());
    }
}
