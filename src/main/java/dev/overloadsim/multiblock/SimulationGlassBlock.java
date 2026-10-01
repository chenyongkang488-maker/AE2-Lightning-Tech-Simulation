package dev.overloadsim.multiblock;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.shapes.*;
public class SimulationGlassBlock extends SimulationPartBlock {
    public static final DirectionProperty FACE=DirectionProperty.create("face");
    public SimulationGlassBlock(){super(Kind.FRAME);registerDefaultState(defaultBlockState().setValue(FACE,Direction.NORTH));}
    @Override protected MapCodec<? extends BaseEntityBlock> codec(){return simpleCodec(p->new SimulationGlassBlock());}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){super.createBlockStateDefinition(b);b.add(FACE);}
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){
        return switch(state.getValue(FACE)){case NORTH->box(0,0,3,16,16,4);case SOUTH->box(0,0,12,16,16,13);case WEST->box(3,0,0,4,16,16);case EAST->box(12,0,0,13,16,16);case UP->box(0,12,0,16,13,16);case DOWN->box(0,3,0,16,4,16);};
    }
    @Override protected boolean skipRendering(BlockState a,BlockState b,Direction side){return b.is(this)&&a.getValue(FACE)==b.getValue(FACE)||super.skipRendering(a,b,side);}
}
