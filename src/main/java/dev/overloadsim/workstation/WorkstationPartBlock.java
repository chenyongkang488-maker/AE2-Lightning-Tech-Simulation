package dev.overloadsim.workstation;

import net.minecraft.core.*;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

public final class WorkstationPartBlock extends Block {
    public static final BooleanProperty FORMED=BooleanProperty.create("formed");
    private final boolean glass;
    public WorkstationPartBlock(boolean glass){super(properties(glass));this.glass=glass;registerDefaultState(defaultBlockState().setValue(FORMED,false));}
    private static Properties properties(boolean glass){var properties=Properties.of().strength(4).sound(glass?SoundType.GLASS:SoundType.METAL);return glass?properties.noOcclusion():properties;}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(FORMED);}
    @Override protected boolean skipRendering(BlockState state,BlockState adjacent,Direction side){return glass&&adjacent.is(this)||super.skipRendering(state,adjacent,side);}
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){
        if(!level.isClientSide&&(state.is(WorkstationContent.FRAME.get())||state.is(WorkstationContent.RECEIVER.get()))){var owner=WorkstationStructureIndex.owner(level,pos);if(owner!=null){WorkstationMenuAccess.open(owner,pos,player);return InteractionResult.CONSUME;}}
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){if(!state.is(next.getBlock())){var owner=WorkstationStructureIndex.owner(level,pos);if(owner!=null)owner.invalidateStructure();}super.onRemove(state,level,pos,next,moving);}
}
