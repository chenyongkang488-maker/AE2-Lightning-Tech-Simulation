package dev.overloadsim.core;

/** Shared block-center and entity-position boundary for receiver explosions. */
public final class ExplosionSphere {
    public static final int MAX_RADIUS_BLOCKS = 12;
    public static final int HARD_RADIUS_LIMIT = 64;

    private ExplosionSphere() {}

    public static boolean contains(int radiusBlocks, double dx, double dy, double dz) {
        if (radiusBlocks < 0 || radiusBlocks > HARD_RADIUS_LIMIT) {
            throw new IllegalArgumentException("explosion radius must be between 0 and 64 blocks");
        }
        return dx * dx + dy * dy + dz * dz <= (double) radiusBlocks * radiusBlocks;
    }
}
