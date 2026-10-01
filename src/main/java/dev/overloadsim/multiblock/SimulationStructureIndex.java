package dev.overloadsim.multiblock;
/** Suppresses recursive member removals while converting/restoring glass. Server thread only. */
public final class SimulationStructureIndex {
    private static final ThreadLocal<Integer> DEPTH=ThreadLocal.withInitial(()->0);
    private record Owner(net.minecraft.core.BlockPos controller,java.util.UUID identity){}
    private static final java.util.Map<net.minecraft.world.level.Level,java.util.Map<net.minecraft.core.BlockPos,Owner>> OWNERS=new java.util.WeakHashMap<>();
    public static boolean occupied(net.minecraft.world.level.Level level,net.minecraft.core.BlockPos pos,SimulationControllerBlockEntity host){
        var map=OWNERS.get(level);if(map==null)return false;var owner=map.get(pos);if(owner==null||owner.identity.equals(host.identity())&&owner.controller.equals(host.getBlockPos()))return false;
        if(!level.hasChunkAt(owner.controller))return true;
        if(level.getBlockEntity(owner.controller) instanceof SimulationControllerBlockEntity c&&c.identity().equals(owner.identity)&&c.structure()!=null)return true;
        map.remove(pos);return false;
    }
    public static void bind(SimulationControllerBlockEntity host,SimulationStructure structure){var map=OWNERS.computeIfAbsent(host.getLevel(),l->new java.util.HashMap<>());for(int x=0;x<structure.size();x++)for(int y=0;y<structure.size();y++)for(int z=0;z<structure.size();z++)map.put(structure.min().offset(x,y,z),new Owner(host.getBlockPos(),host.identity()));}
    public static void release(SimulationControllerBlockEntity host){var map=OWNERS.get(host.getLevel());if(map!=null)map.values().removeIf(o->o.controller.equals(host.getBlockPos())&&o.identity.equals(host.identity()));}
    private static void changed(net.minecraft.world.level.LevelAccessor accessor,net.minecraft.core.BlockPos pos){
        if(converting()||!(accessor instanceof net.minecraft.world.level.Level level))return;
        var map=OWNERS.get(level);if(map==null)return;var owner=map.get(pos);
        if(owner!=null&&level.hasChunkAt(owner.controller)&&level.getBlockEntity(owner.controller) instanceof SimulationControllerBlockEntity c)c.markStructureDirty();
    }
    public static void register(){
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.level.BlockEvent.EntityPlaceEvent event)->changed(event.getLevel(),event.getPos()));
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.level.BlockEvent.BreakEvent event)->changed(event.getLevel(),event.getPos()));
    }
    public static boolean converting(){return DEPTH.get()>0;}
    public static void converting(Runnable work){DEPTH.set(DEPTH.get()+1);try{work.run();}finally{DEPTH.set(DEPTH.get()-1);}}
}
