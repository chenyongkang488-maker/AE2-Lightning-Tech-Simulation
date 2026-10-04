package dev.overloadsim.core;

/** Deterministic renderer samples; storage controls density, world time controls phase. */
public final class ReceiverParticleVisuals {
    public static final int MAX_DOTS = 96;
    public record Point(double x, double y, double z) {}
    private ReceiverParticleVisuals() {}

    public static int count(long stored, long capacity) {
        if (stored <= 0) return 0;
        if (capacity <= 0) return 1;
        double fill = Math.min(1, stored / (double) capacity);
        return Math.max(1, Math.min(MAX_DOTS, (int) Math.ceil(MAX_DOTS * Math.sqrt(fill))));
    }

    public static Point position(int index, double time) {
        int lane = Math.floorMod(index, 4);
        // Reducing time first avoids floating-point overflow even for malformed client snapshots.
        double speed = .025 + lane * .004;
        double phaseTime = Double.isFinite(time) ? time % (Math.PI * 2 / speed) : 0;
        double phase = phaseTime * speed + index * 2.399963229728653;
        double radius = 1.58 + lane * .19;
        double x = Math.cos(phase) * radius, z = Math.sin(phase) * radius;
        double tilt = -.65 + lane * .43;
        double y = z * Math.sin(tilt);
        z *= Math.cos(tilt);
        double yaw = lane * .7;
        return new Point(x * Math.cos(yaw) - z * Math.sin(yaw), y,
                x * Math.sin(yaw) + z * Math.cos(yaw));
    }
}
