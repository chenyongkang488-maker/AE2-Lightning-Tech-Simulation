package dev.overloadsim.multiblock;
import java.util.*;
import net.minecraft.core.BlockPos;
public record SimulationStructure(BlockPos min,int size,List<BlockPos> members,List<BlockPos> glass,
        BlockPos networkInterface,int t1,int t2,int t3,int fortune,boolean overload,boolean smelting) {
    public SimulationStructure{min=min.immutable();members=List.copyOf(members);glass=List.copyOf(glass);}
    public int capacity(){return size*size;}
    public int duration(){return MultiblockRules.duration(t1,t2,t3,overload);}
    public long multiplier(){return MultiblockRules.multiplier(fortune);}
}
