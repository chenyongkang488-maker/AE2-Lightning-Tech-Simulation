package dev.overloadsim.laser;

import dev.overloadsim.core.LaserMath;
import dev.overloadsim.core.ParticleFlightVisuals;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Bounded visual segment; resources are settled separately on the server. */
public record LaserSegment(Vec3 start,Vec3 end,long energy,boolean focused,boolean particle,long particleCount,long launchTick) {
    public static final double BEAM_RADIUS = 1.5, FOCUS_CORE_RADIUS = .018, FOCUS_DISTANCE = 7;
    public LaserSegment {
        if(!valid(start)||!valid(end)||energy<0||energy>LaserMath.MAX_BEAM||start.distanceToSqr(end)>4096)
            throw new IllegalArgumentException("invalid laser segment");
        if(particleCount<0||particleCount>ParticleFlightVisuals.MAX_BATCH
                ||particleCount>0&&(!particle||launchTick<0||launchTick>Long.MAX_VALUE-ParticleFlightVisuals.DURATION)
                ||particleCount==0&&launchTick!=-1)
            throw new IllegalArgumentException("invalid particle flight");
    }
    /** Existing integrations may still construct topology-only segments, with no invented particles. */
    public LaserSegment(Vec3 start,Vec3 end,long energy,boolean focused,boolean particle){this(start,end,energy,focused,particle,0,-1);}
    private static boolean valid(Vec3 v){return v!=null&&Double.isFinite(v.x)&&Double.isFinite(v.y)&&Double.isFinite(v.z)&&Math.abs(v.x)<=30_000_001&&Math.abs(v.z)<=30_000_001&&Math.abs(v.y)<=20_000_000;}
    public double startRadius() { return particle ? .035 : BEAM_RADIUS; }
    public double endRadius() {
        return particle || !focused ? startRadius()
                : BEAM_RADIUS + (FOCUS_CORE_RADIUS - BEAM_RADIUS) * Math.min(1, start.distanceTo(end) / FOCUS_DISTANCE);
    }
    public AABB bounds() { return new AABB(start, end).inflate(particle ? .18 : BEAM_RADIUS); }

    /** Exact circular cylinder/frustum intersection for the cardinal optical segments produced by traversal. */
    public boolean intersects(AABB box) {
        if (particle) return false;
        Vec3 delta = end.subtract(start);
        if (delta.x != 0 && delta.y == 0 && delta.z == 0)
            return intersectsAxis(start.x, end.x, box.minX, box.maxX,
                    gapSquared(start.y, box.minY, box.maxY) + gapSquared(start.z, box.minZ, box.maxZ));
        if (delta.y != 0 && delta.x == 0 && delta.z == 0)
            return intersectsAxis(start.y, end.y, box.minY, box.maxY,
                    gapSquared(start.x, box.minX, box.maxX) + gapSquared(start.z, box.minZ, box.maxZ));
        if (delta.z != 0 && delta.x == 0 && delta.y == 0)
            return intersectsAxis(start.z, end.z, box.minZ, box.maxZ,
                    gapSquared(start.x, box.minX, box.maxX) + gapSquared(start.y, box.minY, box.maxY));
        return false;
    }
    private boolean intersectsAxis(double from, double to, double minimum, double maximum, double radialSquared) {
        double first = (minimum - from) / (to - from), last = (maximum - from) / (to - from);
        double low = Math.max(0, Math.min(first, last)), high = Math.min(1, Math.max(first, last));
        if (low > high) return false;
        double radius = startRadius() + (endRadius() - startRadius()) * low;
        return radialSquared <= radius * radius;
    }
    private static double gapSquared(double point, double minimum, double maximum) {
        double gap = Math.max(0, Math.max(minimum - point, point - maximum));
        return gap * gap;
    }
    public void write(RegistryFriendlyByteBuf b){b.writeDouble(start.x);b.writeDouble(start.y);b.writeDouble(start.z);b.writeDouble(end.x);b.writeDouble(end.y);b.writeDouble(end.z);b.writeVarLong(energy);b.writeBoolean(focused);b.writeBoolean(particle);b.writeVarLong(particleCount);b.writeLong(launchTick);}
    public static LaserSegment read(RegistryFriendlyByteBuf b){return new LaserSegment(new Vec3(b.readDouble(),b.readDouble(),b.readDouble()),new Vec3(b.readDouble(),b.readDouble(),b.readDouble()),b.readVarLong(),b.readBoolean(),b.readBoolean(),b.readVarLong(),b.readLong());}
}
