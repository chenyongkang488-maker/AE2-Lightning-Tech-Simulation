package dev.overloadsim.core;

import java.util.Objects;

/** Pure effective-tick clock. The caller settles its returned item/particle/FE transaction. */
public final class ParticleClock {
    public static final int MAX_DURATION = 1_000_000;
    public static final long PROCESSING_UNIT = LaserMath.PARTICLE_COST * 20;
    public record Fuel(int consumableDurationTicks, int particleIntervalTicks, long energyPerParticle, long inputEnergyPerBatch) {
        public Fuel(int duration, int interval, long energy) { this(duration, interval, energy, 0); }
        public Fuel {
            if (consumableDurationTicks < 1 || consumableDurationTicks > MAX_DURATION
                    || particleIntervalTicks < 1 || particleIntervalTicks > consumableDurationTicks
                    || energyPerParticle < 1 || inputEnergyPerBatch < 0 || inputEnergyPerBatch > LaserMath.MAX_BEAM)
                throw new IllegalArgumentException("invalid conversion fuel");
        }
    }
    public record State(int fuelRemaining, int particleRemaining, long processingCredit) {
        public State(int fuel, int particle) { this(fuel, particle, 0); }
    }
    public record Result(boolean consumedItem, long particlesUsed, long producedFe) {}
    private static final Result PAUSED = new Result(false, 0, 0);
    private int fuelRemaining;
    private int particleRemaining;
    private long processingCredit;

    public State state() { return new State(fuelRemaining, particleRemaining, processingCredit); }

    public void restore(int fuelRemaining, int particleRemaining) {
        restore(fuelRemaining, particleRemaining, 0);
    }
    public void restore(int fuelRemaining, int particleRemaining, long processingCredit) {
        if (fuelRemaining < 0 || fuelRemaining > MAX_DURATION || particleRemaining < 0 || particleRemaining > MAX_DURATION
                || processingCredit < 0 || processingCredit >= PROCESSING_UNIT)
            throw new IllegalArgumentException("invalid conversion clock state");
        this.fuelRemaining = fuelRemaining;
        this.particleRemaining = particleRemaining;
        this.processingCredit = processingCredit;
    }

    public Result advance(Fuel fuel, boolean availableItem, long particles, long space) {
        Objects.requireNonNull(fuel, "fuel");
        LaserMath.nonnegative(particles);
        LaserMath.nonnegative(space);
        if (particles == 0 || space < fuel.energyPerParticle() || (fuelRemaining == 0 && !availableItem)) return PAUSED;
        boolean paid = fuelRemaining == 0;
        if (fuel.inputEnergyPerBatch() > 0) {
            if (paid) fuelRemaining = fuel.consumableDurationTicks();
            // The laser delivers one batch every 20 ticks. Keep units scaled by 20 so
            // even a one-FE batch and fractional particle rates remain exact.
            long quota = processingCredit + fuel.inputEnergyPerBatch();
            long used = Math.min(quota / PROCESSING_UNIT, Math.min(particles, space / fuel.energyPerParticle()));
            processingCredit = quota % PROCESSING_UNIT;
            fuelRemaining--;
            particleRemaining = (int) Math.min(MAX_DURATION,
                    (PROCESSING_UNIT - processingCredit - 1) / fuel.inputEnergyPerBatch() + 1);
            // Capacity-limited whole quotas expire; idle/full storage cannot bank a burst.
            return new Result(paid, used, used * fuel.energyPerParticle());
        }
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
