package dev.overloadsim.binding;

import java.util.*;
import java.util.function.Supplier;
import dev.overloadsim.ModContent;
import dev.overloadsim.api.*;
import dev.overloadsim.data.CropSimulationData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.*;
import net.neoforged.neoforge.common.NeoForge;

/** A plot is validated before any plant is consumed; soil always remains in place. */
public final class CropBinding {
    public static ItemStack bind(ServerLevel level,BlockPos center,ItemStack stack,Supplier<ItemStack> current){
        if(!stack.is(ModContent.BLANK.get()))return stack;
        var original=stack.copy();var plants=new LinkedHashMap<BlockPos,BlockState>();var soils=new LinkedHashMap<BlockPos,BlockState>();CropSimulationData.Template selected=null;
        for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++){
            if(x==0&&z==0)continue;var ground=center.offset(x,0,z);var pos=ground.above();
            if(!level.hasChunkAt(ground)||!level.hasChunkAt(pos)||level.getBlockEntity(pos)!=null)return stack;
            var state=level.getBlockState(pos);var resolved=CropSimulationData.resolve(level,state);
            if(!resolved.valid()||!CropSimulationData.supports(level,ground,state))return stack;var crop=resolved.crop().orElseThrow();
            if(selected==null)selected=crop;else if(!selected.equals(crop))return stack;
            soils.put(ground,level.getBlockState(ground));plants.put(pos,state);
            if(state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)&&state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF)==DoubleBlockHalf.LOWER){
                var upper=pos.above();if(!level.hasChunkAt(upper)||level.getBlockEntity(upper)!=null)return stack;var top=level.getBlockState(upper);
                // Some crops (e.g. young pitcher crops) only grow an upper half at later ages.
                if(top.is(state.getBlock())&&top.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)&&top.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF)==DoubleBlockHalf.UPPER)plants.put(upper,top);
            }
        }
        if(selected==null)return stack;var data=new CrystalData(selected.profile(),Optional.empty(),0,1);
        if(NeoForge.EVENT_BUS.post(new SimulationEvents.BeforeBinding(level,center,data)).isCanceled()||current.get()!=stack||!ItemStack.matches(original,stack))return stack;
        for(var e:soils.entrySet())if(!level.hasChunkAt(e.getKey())||!level.getBlockState(e.getKey()).equals(e.getValue()))return stack;
        for(var e:plants.entrySet()){
            if(!level.hasChunkAt(e.getKey())||level.getBlockEntity(e.getKey())!=null||!level.getBlockState(e.getKey()).equals(e.getValue()))return stack;
            if(soils.containsKey(e.getKey().below())&&(!CropSimulationData.supports(level,e.getKey().below(),e.getValue())||!CropSimulationData.resolve(level,e.getValue()).crop().filter(selected::equals).isPresent()))return stack;
        }
        // Remove upper halves first, without neighbour drop cascades, then update the plot.
        for(var pos:plants.keySet().stream().sorted(Comparator.comparingInt((BlockPos p)->p.getY()).reversed()).toList())level.setBlock(pos,Blocks.AIR.defaultBlockState(),Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE|Block.UPDATE_SUPPRESS_DROPS);
        for(var pos:plants.keySet())level.updateNeighborsAt(pos,Blocks.AIR);
        return CrystalDataAccess.bound(data);
    }
    private CropBinding(){}
}
