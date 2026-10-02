package dev.overloadsim.multiblock;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.*;

/** Invisible owner-managed interior source. No item, collision, loot or forced chunk loading. */
public final class SimulationLightBlock extends BaseEntityBlock {
    public SimulationLightBlock(){super(Properties.of().noCollission().noOcclusion().replaceable().noLootTable().lightLevel(s->6));}
    @Override protected MapCodec<? extends BaseEntityBlock> codec(){return simpleCodec(p->new SimulationLightBlock());}
    @Override protected RenderShape getRenderShape(BlockState state){return RenderShape.INVISIBLE;}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new SimulationLightBlockEntity(pos,state);}
    @Override protected void tick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){if(level.getBlockEntity(pos) instanceof SimulationLightBlockEntity light)light.recover();}
    @Override protected void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){
        if(!state.is(next.getBlock())&&!SimulationStructureIndex.converting()&&level.getBlockEntity(pos) instanceof SimulationLightBlockEntity light)light.notifyOwner();super.onRemove(state,level,pos,next,moving);
    }
}
