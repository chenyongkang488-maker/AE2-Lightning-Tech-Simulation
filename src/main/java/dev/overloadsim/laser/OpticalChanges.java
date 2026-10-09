package dev.overloadsim.laser;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Server-only optical observations are reused until a real block or lifecycle change. */
public final class OpticalChanges {
    private record Cell(BlockState state,boolean clear) {}
    private static final class State {
        long revision;
        final Map<BlockPos,Cell> cells=new HashMap<>();
        final Map<LaserNetwork.Path,Boolean> valid=new HashMap<>();
    }
    private static final Map<ServerLevel,State> STATES=new WeakHashMap<>();
    private OpticalChanges(){}
    private static State state(ServerLevel level){return STATES.computeIfAbsent(level,k->new State());}
    static long revision(ServerLevel level){return state(level).revision;}
    static BlockState block(ServerLevel level,BlockPos pos){return cell(level,pos).state();}
    static boolean clear(ServerLevel level,BlockPos pos){return cell(level,pos).clear();}
    private static Cell cell(ServerLevel level,BlockPos pos){
        var cache=state(level);
        if(cache.cells.size()>=65_536){cache.cells.clear();invalidate(level);}
        return cache.cells.computeIfAbsent(pos.immutable(),p->{var block=level.getBlockState(p);return new Cell(block,block.getCollisionShape(level,p).isEmpty());});
    }
    static Boolean valid(ServerLevel level,LaserNetwork.Path path){return state(level).valid.get(path);}
    static void remember(ServerLevel level,LaserNetwork.Path path,boolean valid){
        var cache=state(level);if(cache.valid.size()>=4096)cache.valid.clear();cache.valid.put(path,valid);
    }
    public static void invalidate(Level level){
        if(!(level instanceof ServerLevel server))return;
        var cache=state(server);cache.revision++;cache.valid.clear();DeflectorBatchRouter.invalidate(server);
    }
    public static void changed(Level level,BlockPos pos,BlockState before,BlockState after){
        if(!(level instanceof ServerLevel server)||before==after)return;
        // Invisible lighting cells are optical air; their leases must not invalidate their own beams.
        if(LaserLightManager.isBeamAir(before)&&LaserLightManager.isBeamAir(after))return;
        var cache=state(server);boolean relevant=cache.cells.remove(pos)!=null;
        for(var d:Direction.values())relevant|=cache.cells.remove(pos.relative(d))!=null;
        for(var host:LaserStructureIndex.controllers(server)){
            var delta=host.getBlockPos().subtract(pos);
            if(Math.abs(delta.getX())<=12&&Math.abs(delta.getY())<=12&&Math.abs(delta.getZ())<=12){host.markStructureDirty();relevant=true;}
        }
        if(!relevant)return;
        invalidate(server);
        // Active paths are checked once after the change, including reentrant modifier callbacks.
        for(var root:java.util.List.copyOf(LaserNetwork.PATHS.keySet())){
            var path=LaserNetwork.PATHS.get(root);
            if(root.getLevel()==server&&path!=null&&!LaserNetwork.valid(server,path))LaserNetwork.clear(root,true);
        }
    }
    public static void chunksChanged(Level level){
        if(!(level instanceof ServerLevel server))return;
        var cache=state(server);cache.cells.clear();invalidate(server);
        for(var host:LaserStructureIndex.controllers(server))host.markStructureDirty();
    }
    public static void clearAll(){STATES.clear();DeflectorBatchRouter.clearTopology();}
}
