package dev.overloadsim.core;

/** Cosmetic clock independent of transient beam paths, which are cleared every pulse. */
public final class CoreCollapseCycle {
    public static final int DURATION=40, INPUT_GRACE=20;
    private long startTick=-1, lastInputTick=-1;
    public CoreCollapseCycle() {}
    public long startTick(){return startTick;}
    public long lastInputTick(){return lastInputTick;}
    public void stop(){startTick=lastInputTick=-1;}
    public boolean accept(long tick){
        if(tick<0)return false;
        boolean restart=lastInputTick<0||tick<lastInputTick||tick-lastInputTick>INPUT_GRACE;
        if(restart)startTick=tick;
        lastInputTick=tick;
        return restart;
    }
    public void restore(long start,long last){
        if(start<0||last<start){stop();return;}
        startTick=start;lastInputTick=last;
    }
    public static double radius(double age){
        if(!Double.isFinite(age)||age<=0)return 1.5;
        if(age<18)return 1.5+(0.18-1.5)*smooth(age/18);
        if(age<22)return .18;
        return .18+(.85-.18)*smooth(Math.min(1,(age-22)/18));
    }
    public static double halo(double age){
        return Double.isFinite(age)?smooth(Math.clamp((age-22)/18,0,1)):0;
    }
    private static double smooth(double x){return x*x*(3-2*x);}
}
