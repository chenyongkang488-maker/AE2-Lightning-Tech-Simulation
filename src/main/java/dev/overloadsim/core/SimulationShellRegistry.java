package dev.overloadsim.core;
import java.util.*;
/** Immutable position snapshots are safe to read from chunk-render workers. */
public final class SimulationShellRegistry {
    public record Point(int x,int y,int z){}
    public record Cell(int size,int x,int y,int z){public SimulationShellTopology.Role role(){return SimulationShellTopology.role(size,x,y,z);}}
    private record Entry(Object token,Point min,int size){}
    private final Map<Point,Entry> owners=new LinkedHashMap<>();
    private volatile Map<Point,Cell> cells=Map.of();
    public Cell lookup(int x,int y,int z){return cells.get(new Point(x,y,z));}
    public synchronized Set<Point> replace(Object token,int ownerX,int ownerY,int ownerZ,int minX,int minY,int minZ,int size){
        if(size<3||size>7)return remove(token,ownerX,ownerY,ownerZ);
        var owner=new Point(ownerX,ownerY,ownerZ);var entry=new Entry(token,new Point(minX,minY,minZ),size);var old=owners.get(owner);
        if(old!=null&&old.token==token&&old.min.equals(entry.min)&&old.size==size)return Set.of();
        owners.put(owner,entry);return rebuild();
    }
    public synchronized Set<Point> remove(Object token,int ownerX,int ownerY,int ownerZ){
        var owner=new Point(ownerX,ownerY,ownerZ);var old=owners.get(owner);if(old==null||old.token!=token)return Set.of();owners.remove(owner);return rebuild();
    }
    private Set<Point> rebuild(){
        var next=new HashMap<Point,Cell>();for(var e:owners.values())for(int x=0;x<e.size;x++)for(int y=0;y<e.size;y++)for(int z=0;z<e.size;z++){
            if(SimulationShellTopology.role(e.size,x,y,z)==SimulationShellTopology.Role.NONE)continue;
            next.put(new Point(e.min.x+x,e.min.y+y,e.min.z+z),new Cell(e.size,x,y,z));
        }
        var changed=new HashSet<Point>();var before=cells;for(var p:before.keySet())if(!Objects.equals(before.get(p),next.get(p)))changed.add(p);
        for(var p:next.keySet())if(!Objects.equals(before.get(p),next.get(p)))changed.add(p);cells=Map.copyOf(next);return Set.copyOf(changed);
    }
}
