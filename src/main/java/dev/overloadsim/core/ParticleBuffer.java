package dev.overloadsim.core;

/** A whole incoming batch is rejected on overflow; the caller triggers containment failure. */
public final class ParticleBuffer {
    private final long capacity;
    private long stored;

    public ParticleBuffer(long capacity) {
        LaserMath.nonnegative(capacity);
        this.capacity = capacity;
    }

    public long stored() { return stored; }
    public long capacity() { return capacity; }

    public boolean receive(long amount) {
        LaserMath.nonnegative(amount);
        if (amount > capacity - stored) return false;
        stored += amount;
        return true;
    }

    public long consume(long amount) {
        LaserMath.nonnegative(amount);
        long consumed = Math.min(amount, stored);
        stored -= consumed;
        return consumed;
    }

    public void load(long value) {
        LaserMath.nonnegative(value);
        stored = Math.min(value, capacity);
    }
}
