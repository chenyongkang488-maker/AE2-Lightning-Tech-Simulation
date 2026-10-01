package dev.overloadsim.multiblock;
import java.util.*;
import dev.overloadsim.ModContent;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/** Bounded, chunk-safe scans. A successful result is immutable and owned by one controller. */
public final class SimulationStructureValidator {
    private static final net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> FRAMES=net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK,ModContent.id("simulation_frames"));
    private static final net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> GLASS=net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK,ModContent.id("simulation_glass"));
    public record Result(SimulationStructure structure,String error,BlockPos problem){
        public boolean valid(){return structure!=null;}
    }
    public static Result find(SimulationControllerBlockEntity controller){
        var face=controller.getBlockState().getValue(SimulationControllerBlock.FACING);
        Result last=new Result(null,"no_structure",controller.getBlockPos());
        for(int n=3;n<=7;n++)for(int offset=1;offset<n-1;offset++){
            var p=controller.getBlockPos();
            var min=switch(face){
                case NORTH->p.offset(-offset,0,0);case SOUTH->p.offset(-offset,0,1-n);
                case WEST->p.offset(0,0,-offset);case EAST->p.offset(1-n,0,-offset);
                default->p;};
            var result=validate(controller,min,n);
            if(result.valid())return result;
            if(!result.error.equals("edge"))last=result;
        }return last;
    }
    public static Result validate(SimulationControllerBlockEntity controller,BlockPos min,int n){
        if(n<3||n>7)return new Result(null,"size",min);
        Level level=controller.getLevel();var members=new ArrayList<BlockPos>();var glass=new ArrayList<BlockPos>();
        int c=0,interfaces=0,t1=0,t2=0,t3=0,f=0,o=0,s=0;BlockPos port=null;
        for(int pass=0;pass<2;pass++)for(int x=0;x<n;x++)for(int y=0;y<n;y++)for(int z=0;z<n;z++){
            int boundaries=(x==0||x==n-1?1:0)+(y==0||y==n-1?1:0)+(z==0||z==n-1?1:0);
            boolean edge=boundaries>=2;if((pass==0)!=edge)continue;
            BlockPos p=min.offset(x,y,z);if(!level.hasChunkAt(p))return new Result(null,"unloaded",p);
            var state=level.getBlockState(p);var block=state.getBlock();
            if(SimulationStructureIndex.occupied(level,p,controller))return new Result(null,"owned",p);
            if(boundaries==0){if(!state.isAir())return new Result(null,"interior",p);continue;}
            if(level.getBlockEntity(p) instanceof SimulationMemberBlockEntity member&&member.owner()!=null&&!member.ownedBy(controller))
                return new Result(null,"owned",p);
            members.add(p.immutable());
            if(edge){
                boolean special=y==0&&boundaries==2;
                if(block==MultiblockContent.CONTROLLER.get()){if(!special)return new Result(null,"controller_position",p);c++;if(!p.equals(controller.getBlockPos()))return new Result(null,"controllers",p);}
                else if(BuiltInRegistries.BLOCK.getKey(block).equals(ResourceLocation.parse("ae2lt:overloaded_interface"))){
                    if(!special)return new Result(null,"interface_position",p);interfaces++;port=p.immutable();
                }else if(block!=MultiblockContent.FRAME.get()&&!state.is(FRAMES))return new Result(null,"edge",p);
            }else if(y==0){
                var kind=MultiblockData.module(block);
                if(kind!=null)switch(kind){
                    case FRAME->{}case T1->t1++;case T2->t2++;case T3->t3++;case FORTUNE->f++;case OVERLOAD->o++;case SMELTING->s++;
                }else if(!state.is(FRAMES))return new Result(null,"floor",p);
            }else{
                if(block==MultiblockContent.GLASS.get()){
                    if(!(level.getBlockEntity(p) instanceof SimulationMemberBlockEntity m)||!m.ownedBy(controller))return new Result(null,"owned",p);
                }else if(!state.is(GLASS)&&!BuiltInRegistries.BLOCK.getKey(block).equals(ResourceLocation.parse("ae2:quartz_vibrant_glass")))return new Result(null,"glass",p);
                glass.add(p.immutable());
            }
        }
        if(c!=1||interfaces>1||o>1||s>1)return new Result(null,"duplicates",min);
        return new Result(new SimulationStructure(min,n,members,glass,port,t1,t2,t3,f,o==1,s==1),"",null);
    }
}
