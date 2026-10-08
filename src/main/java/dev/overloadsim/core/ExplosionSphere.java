package dev.overloadsim.core;

/** Shared block-center and entity-position boundary for receiver explosions. */
public final class ExplosionSphere {
    public static final int MAX_RADIUS_BLOCKS = 12;

    private ExplosionSphere() {}

    public static boolean contains(int radiusBlocks, double dx, double dy, double dz) {
        if (radiusBlocks < 0 || radiusBlocks > MAX_RADIUS_BLOCKS) {
            throw new IllegalArgumentException("explosion radius must be between 0 and 12 blocks");
        }
        return dx * dx + dy * dy + dz * dz <= (double) radiusBlocks * radiusBlocks;
    }
}
