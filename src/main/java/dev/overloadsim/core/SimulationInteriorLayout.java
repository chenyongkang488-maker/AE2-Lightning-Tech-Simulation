package dev.overloadsim.core;

/** Four separate emitter footprints stay inside the cavity and immediately below its roof. */
public record SimulationInteriorLayout(int size) {
    public SimulationInteriorLayout {
        if(size<3||size>7)throw new IllegalArgumentException("simulation size");
    }
    public float coilScale(){return Math.min(.55f,Math.max(.2f,(size-2)*.18f));}
    public float coilLow(){return 1.05f;}
    public float coilHigh(){return size-1.05f-coilScale();}
    public float coilY(){return size-1.15f;}
}
