package dev.overloadsim.multiblock;
/** Suppresses recursive member removals while converting/restoring glass. Server thread only. */
public final class SimulationStructureIndex {
    private static final ThreadLocal<Integer> DEPTH=ThreadLocal.withInitial(()->0);
    public static boolean converting(){return DEPTH.get()>0;}
    public static void converting(Runnable work){DEPTH.set(DEPTH.get()+1);try{work.run();}finally{DEPTH.set(DEPTH.get()-1);}}
}
