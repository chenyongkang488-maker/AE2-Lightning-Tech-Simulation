package dev.overloadsim.construction;

import dev.overloadsim.ModContent;
import dev.overloadsim.laser.LaserContent;
import dev.overloadsim.laser.LaserKind;
import dev.overloadsim.laser.LaserMachineBlock;
import dev.overloadsim.laser.LaserStructureGeometry;
import dev.overloadsim.workstation.WorkstationContent;
import dev.overloadsim.workstation.WorkstationStructure;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** World-free construction definitions shared with the optional recipe viewer. */
public final class StructurePreviewCatalog {
    private StructurePreviewCatalog(){}
    public record Material(Block block,int count){}
    public record Bounds(int minX,int minY,int minZ,int maxX,int maxY,int maxZ){
        public int width(){return maxX-minX+1;}
        public int height(){return maxY-minY+1;}
        public int depth(){return maxZ-minZ+1;}
        public double centerX(){return (minX+maxX+1)/2d;}
        public double centerY(){return (minY+maxY+1)/2d;}
        public double centerZ(){return (minZ+maxZ+1)/2d;}
    }
    public record Layout(ResourceLocation id,Block controller,LaserKind kind,Map<BlockPos,BlockState> cells){
        public Layout {
            var copy=new LinkedHashMap<BlockPos,BlockState>();
            cells.forEach((pos,state)->copy.put(pos.immutable(),state));
            cells=Collections.unmodifiableMap(copy);
            if(cells.values().stream().filter(s->s.is(controller)).count()!=1)
                throw new IllegalArgumentException("exactly one controller required");
        }
        public List<Material> materials(){
            var counts=new LinkedHashMap<Block,Integer>();
            counts.put(controller,0);
            cells.values().stream().filter(s->!s.isAir()).forEach(s->counts.merge(s.getBlock(),1,Integer::sum));
            return counts.entrySet().stream().map(e->new Material(e.getKey(),e.getValue())).toList();
        }
        public Bounds bounds(){
            var occupied=cells.entrySet().stream().filter(e->!e.getValue().isAir()).map(Map.Entry::getKey).toList();
            return new Bounds(occupied.stream().mapToInt(BlockPos::getX).min().orElseThrow(),occupied.stream().mapToInt(BlockPos::getY).min().orElseThrow(),occupied.stream().mapToInt(BlockPos::getZ).min().orElseThrow(),
                    occupied.stream().mapToInt(BlockPos::getX).max().orElseThrow(),occupied.stream().mapToInt(BlockPos::getY).max().orElseThrow(),occupied.stream().mapToInt(BlockPos::getZ).max().orElseThrow());
        }
        public boolean ring(){return kind==LaserKind.CONSTRAINT||kind==LaserKind.ENHANCEMENT||kind==LaserKind.BURST||kind==LaserKind.FOCUS;}
        public Map<BlockPos,BlockState> cells(boolean flat){return flat&&ring()?laserCells(kind,Direction.DOWN):cells;}
        /** Legal optional substitutions; quantities in materials remain the canonical required set. */
        public List<BlockState> alternatives(BlockPos pos,boolean flat){
            var template=cells(flat);var current=template.get(pos);
            if(current==null)return List.of();
            var replacements=new ArrayList<BlockState>();replacements.add(current);
            if(!current.is(LaserContent.FRAME.get())||kind==null)return List.copyOf(replacements);
            var facing=flat&&ring()?Direction.DOWN:Direction.NORTH;
            var center=LaserStructureGeometry.centerOffset(kind,facing,kind==LaserKind.RECEIVER,false,2);
            var delta=pos.subtract(center);var forward=facing.getOpposite().getNormal();
            if(delta.getX()*forward.getX()+delta.getY()*forward.getY()+delta.getZ()*forward.getZ()<=0){
                var network=BuiltInRegistries.BLOCK.get(ResourceLocation.parse("ae2lt:overloaded_interface"));
                if(!network.defaultBlockState().isAir())replacements.add(network.defaultBlockState());
            }
            if(kind==LaserKind.RECEIVER&&LaserStructureGeometry.receiverOpticalOffsets(Direction.NORTH,true).contains(pos))
                replacements.add(LaserContent.PORT.get().defaultBlockState());
            return List.copyOf(replacements);
        }
    }

    public static List<Layout> layouts(){
        var layouts=new ArrayList<Layout>();
        for(var kind:LaserKind.values())if(kind!=LaserKind.CORE)
            layouts.add(new Layout(ModContent.id(kind.id()),LaserContent.machine(kind),kind,laserCells(kind,Direction.NORTH)));
        layouts.add(new Layout(ModContent.id("high_energy_workstation"),WorkstationContent.CONTROLLER.get(),null,
                WorkstationStructure.template(BlockPos.ZERO,Direction.NORTH)));
        return List.copyOf(layouts);
    }
    private static Map<BlockPos,BlockState> laserCells(LaserKind kind,Direction facing){
        var cells=new LinkedHashMap<BlockPos,BlockState>();
        LaserStructureGeometry.template(kind,facing,kind==LaserKind.RECEIVER,false,2).forEach((pos,part)->{
            Block block=switch(part){
                case CONTROLLER->LaserContent.machine(kind);case FRAME->LaserContent.FRAME.get();
                case GLASS->LaserContent.GLASS.get();case SOURCE->LaserContent.SOURCE.get();
                case PORT->LaserContent.PORT.get();case AIR->Blocks.AIR;
            };
            cells.put(pos,part==LaserStructureGeometry.Part.CONTROLLER?block.defaultBlockState().setValue(LaserMachineBlock.FACING,facing):block.defaultBlockState());
        });
        return Collections.unmodifiableMap(cells);
    }
}
