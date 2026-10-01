package dev.overloadsim.multiblock;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
public class SimulationPartBlock extends BaseEntityBlock {
    public enum Kind{FRAME,T1,T2,T3,FORTUNE,OVERLOAD,SMELTING}
    public static final BooleanProperty FORMED=BooleanProperty.create("formed");
    private final Kind kind;
    public SimulationPartBlock(Kind kind){super(Properties.of().strength(4).sound(SoundType.METAL));this.kind=kind;registerDefaultState(stateDefinition.any().setValue(FORMED,false));}
    public Kind kind(){return kind;}
    @Override protected MapCodec<? extends BaseEntityBlock> codec(){return simpleCodec(p->new SimulationPartBlock(kind));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(FORMED);}
    @Override protected RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new SimulationMemberBlockEntity(pos,state);}
    @Override protected void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){
        if(!state.is(next.getBlock())&&!SimulationStructureIndex.converting()&&level.getBlockEntity(pos) instanceof SimulationMemberBlockEntity m)m.notifyController();
        super.onRemove(state,level,pos,next,moving);
    }
    @Override protected void tick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){if(level.getBlockEntity(pos) instanceof SimulationMemberBlockEntity m)m.recover();}
}
