package dev.overloadsim.core;

/** Fractional wheel input, matching the configurator's 20-tick accumulation window. */
public final class ModeScroll {
    private double remainder;
    private long lastTick=Long.MIN_VALUE;

    public int scroll(double delta,long tick){
        if(!Double.isFinite(delta)||delta==0)return 0;
        if(lastTick==Long.MIN_VALUE||tick<lastTick||tick-lastTick>20)remainder=0;
        lastTick=tick;
        remainder+=Math.clamp(delta,-8,8);
        int steps=(int)remainder;remainder%=1;return steps;
    }
    public void reset(){remainder=0;lastTick=Long.MIN_VALUE;}
}
