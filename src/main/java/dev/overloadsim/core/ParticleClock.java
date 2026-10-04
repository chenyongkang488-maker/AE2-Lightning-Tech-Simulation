package dev.overloadsim.core;

import java.util.Objects;

/** Pure effective-tick clock. The caller settles its returned item/particle/FE transaction. */
public final class ParticleClock {
    public static final int MAX_DURATION = 1_000_000;
    public record Fuel(int consumableDurationTicks, int particleIntervalTicks, long energyPerParticle) {
        public Fuel {
            if (consumableDurationTicks < 1 || consumableDurationTicks > MAX_DURATION
                    || particleIntervalTicks < 1 || particleIntervalTicks > consumableDurationTicks
                    || energyPerParticle < 1) throw new IllegalArgumentException("invalid conversion fuel");
        }
    }
    public record State(int fuelRemaining, int particleRemaining) {}
    public record Result(boolean consumedItem, long particlesUsed, long producedFe) {}
    private static final Result PAUSED = new Result(false, 0, 0);
    private int fuelRemaining;
    private int particleRemaining;

    public State state() { return new State(fuelRemaining, particleRemaining); }

    public void restore(int fuelRemaining, int particleRemaining) {
        if (fuelRemaining < 0 || fuelRemaining > MAX_DURATION || particleRemaining < 0 || particleRemaining > MAX_DURATION)
            throw new IllegalArgumentException("invalid conversion clock state");
        this.fuelRemaining = fuelRemaining;
        this.particleRemaining = particleRemaining;
    }

    public Result advance(Fuel fuel, boolean availableItem, long particles, long space) {
        Objects.requireNonNull(fuel, "fuel");
        LaserMath.nonnegative(particles);
        LaserMath.nonnegative(space);
        if (particles == 0 || space < fuel.energyPerParticle() || (fuelRemaining == 0 && !availableItem)) return PAUSED;
        boolean paid = fuelRemaining == 0;
        if (paid) {
            fuelRemaining = fuel.consumableDurationTicks();
            particleRemaining = fuel.particleIntervalTicks();
        } else if (particleRemaining == 0) {
            particleRemaining = fuel.particleIntervalTicks();
        }
        fuelRemaining--;
        particleRemaining--;
        if (particleRemaining == 0) {
            particleRemaining = fuel.particleIntervalTicks();
            return new Result(paid, 1, fuel.energyPerParticle());
        }
        return new Result(paid, 0, 0);
    }
}
