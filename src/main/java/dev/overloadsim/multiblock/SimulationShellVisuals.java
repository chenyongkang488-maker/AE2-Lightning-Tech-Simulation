package dev.overloadsim.multiblock;
import dev.overloadsim.core.SimulationShellRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.*;
import java.util.*;
/** Client geometry ownership only; this never mutates server blocks or native interface data. */
public final class SimulationShellVisuals {
    private static final Map<Level,SimulationShellRegistry> WORLDS=Collections.synchronizedMap(new WeakHashMap<>());
    public static void publish(SimulationControllerBlockEntity c,Object token){
        var level=c.getLevel();if(level==null||!level.isClientSide)return;
        var registry=registry(level);var owner=c.getBlockPos();var min=c.visualMin();
        if(min==null)min=owner;refresh(level,registry.replace(token,owner.getX(),owner.getY(),owner.getZ(),min.getX(),min.getY(),min.getZ(),c.visualSize()));
    }
    public static void remove(SimulationControllerBlockEntity c,Object token){
        var level=c.getLevel();if(level==null||!level.isClientSide)return;var owner=c.getBlockPos();
        refresh(level,registry(level).remove(token,owner.getX(),owner.getY(),owner.getZ()));
    }
    private static SimulationShellRegistry registry(Level level){synchronized(WORLDS){return WORLDS.computeIfAbsent(level,k->new SimulationShellRegistry());}}
    public static SimulationShellRegistry.Cell lookup(BlockAndTintGetter region,BlockPos pos){
        var be=region.getBlockEntity(pos);if(be==null||be.getLevel()==null)return null;
        var registry=WORLDS.get(be.getLevel());return registry==null?null:registry.lookup(pos.getX(),pos.getY(),pos.getZ());
    }
    private static void refresh(Level level,Set<SimulationShellRegistry.Point> changed){
        for(var p:changed){var pos=new BlockPos(p.x(),p.y(),p.z());if(level.hasChunkAt(pos)){var state=level.getBlockState(pos);level.sendBlockUpdated(pos,state,state,8);}}
    }
}
