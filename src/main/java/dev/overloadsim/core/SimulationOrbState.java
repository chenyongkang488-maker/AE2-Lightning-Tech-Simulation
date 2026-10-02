package dev.overloadsim.core;

/** Presence and module color are independent of the currently processing batch. */
public record SimulationOrbState(boolean visible, boolean smelting, boolean overload,boolean processing) {
    public static SimulationOrbState of(int flags) {
        return new SimulationOrbState((flags & 1) != 0, (flags & 4) != 0, (flags & 2) != 0,(flags&8)!=0);
    }
}
