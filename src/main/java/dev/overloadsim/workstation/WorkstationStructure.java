package dev.overloadsim.workstation;

import dev.overloadsim.core.WorkstationGeometry;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class WorkstationStructure {
    private WorkstationStructure() {}
    public static BlockPos position(BlockPos controller,Direction facing,int x,int y,int z){if(facing.getAxis()==Direction.Axis.Y)throw new IllegalArgumentException("horizontal workstation");return controller.relative(facing.getClockWise(),x-3).relative(facing.getOpposite(),z).above(y);}
    public static Map<BlockPos,BlockState> template(BlockPos controller,Direction facing){
        var cells=new LinkedHashMap<BlockPos,BlockState>();
        for(int x=0;x<7;x++)for(int y=0;y<7;y++)for(int z=0;z<7;z++){
            var state=switch(WorkstationGeometry.part(x,y,z)){
                case CONTROLLER->WorkstationContent.CONTROLLER.get().defaultBlockState().setValue(HighEnergyWorkstationBlock.FACING,facing);
                case FRAME->WorkstationContent.FRAME.get().defaultBlockState();
                case GLASS->WorkstationContent.GLASS.get().defaultBlockState();
                case RECEIVER->WorkstationContent.RECEIVER.get().defaultBlockState();
                case AIR->Blocks.AIR.defaultBlockState();};
            cells.put(position(controller,facing,x,y,z),state);
        }return Collections.unmodifiableMap(cells);
    }
}
