package dev.overloadsim.core;

/** Long FE storage; insert/extract return the amount actually accepted. */
public final class LongEnergyBuffer {
    private final long capacity;
    private long stored;

    public LongEnergyBuffer(long capacity) {
        LaserMath.nonnegative(capacity);
        this.capacity = capacity;
    }

    public long stored() { return stored; }
    public long capacity() { return capacity; }

    public long insert(long amount, boolean simulate) {
        LaserMath.nonnegative(amount);
        long accepted = Math.min(amount, capacity - stored);
        if (!simulate) stored += accepted;
        return accepted;
    }

    public long extract(long amount, boolean simulate) {
        LaserMath.nonnegative(amount);
        long extracted = Math.min(amount, stored);
        if (!simulate) stored -= extracted;
        return extracted;
    }

    public void load(long value) {
        LaserMath.nonnegative(value);
        stored = Math.min(value, capacity);
    }
}
