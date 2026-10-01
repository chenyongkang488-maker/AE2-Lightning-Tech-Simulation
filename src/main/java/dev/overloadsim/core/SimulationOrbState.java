package dev.overloadsim.core;

/** Visual effects follow the paid batch, including its captured upgrade state. */
public record SimulationOrbState(boolean visible, boolean smelting, boolean overload) {
    public static SimulationOrbState of(int flags) {
        return new SimulationOrbState((flags & 1) != 0, (flags & 4) != 0, (flags & 2) != 0);
    }
}
