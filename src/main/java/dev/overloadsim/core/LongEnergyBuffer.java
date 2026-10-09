package dev.overloadsim.core;

/** Long FE storage; insert/extract return the amount actually accepted. */
public final class LongEnergyBuffer {
    private long capacity;
    private final long hardCapacity;
    private long stored;

    public LongEnergyBuffer(long capacity) {
        this(capacity, capacity);
    }

    public LongEnergyBuffer(long capacity,long hardCapacity) {
        LaserMath.nonnegative(capacity); LaserMath.nonnegative(hardCapacity);
        if(capacity>hardCapacity)throw new IllegalArgumentException("operating capacity exceeds hard limit");
        this.capacity=capacity;this.hardCapacity=hardCapacity;
    }
    public void setCapacity(long value){LaserMath.nonnegative(value);capacity=Math.min(value,hardCapacity);}
    /** Restore saved stock under its previous limit without opening new insertion space. */
    public void restore(long value,long previousCapacity){LaserMath.nonnegative(value);stored=Math.min(value,Math.min(hardCapacity,Math.max(0,previousCapacity)));}

    public long stored() { return stored; }
    public long capacity() { return capacity; }

    public long insert(long amount, boolean simulate) {
        LaserMath.nonnegative(amount);
        long accepted = Math.min(amount, Math.max(0, capacity - stored));
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
