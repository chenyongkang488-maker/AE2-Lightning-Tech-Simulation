package dev.overloadsim.core;

/** A whole incoming batch is rejected on overflow; the caller triggers containment failure. */
public final class ParticleBuffer {
    private long capacity;
    private final long hardCapacity;
    private long stored;

    public ParticleBuffer(long capacity) {
        this(capacity,capacity);
    }
    public ParticleBuffer(long capacity,long hardCapacity){LaserMath.nonnegative(capacity);LaserMath.nonnegative(hardCapacity);if(capacity>hardCapacity)throw new IllegalArgumentException("operating capacity exceeds hard limit");this.capacity=capacity;this.hardCapacity=hardCapacity;}
    public void setCapacity(long value){LaserMath.nonnegative(value);capacity=Math.min(value,hardCapacity);}
    public void restore(long value,long previousCapacity){LaserMath.nonnegative(value);stored=Math.min(value,Math.min(hardCapacity,Math.max(0,previousCapacity)));}

    public long stored() { return stored; }
    public long capacity() { return capacity; }

    public boolean receive(long amount) {
        LaserMath.nonnegative(amount);
        if (amount > Math.max(0, capacity - stored)) return false;
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
