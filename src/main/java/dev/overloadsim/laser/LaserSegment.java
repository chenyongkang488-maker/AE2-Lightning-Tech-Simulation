package dev.overloadsim.laser;

import dev.overloadsim.core.LaserMath;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.phys.Vec3;

/** Bounded visual segment; resources are settled separately on the server. */
public record LaserSegment(Vec3 start,Vec3 end,long energy,boolean focused,boolean particle) {
    public LaserSegment {
        if(!valid(start)||!valid(end)||energy<0||energy>LaserMath.MAX_BEAM||start.distanceToSqr(end)>4096)
            throw new IllegalArgumentException("invalid laser segment");
    }
    private static boolean valid(Vec3 v){return v!=null&&Double.isFinite(v.x)&&Double.isFinite(v.y)&&Double.isFinite(v.z)&&Math.abs(v.x)<=30_000_001&&Math.abs(v.z)<=30_000_001&&Math.abs(v.y)<=20_000_000;}
    public void write(RegistryFriendlyByteBuf b){b.writeDouble(start.x);b.writeDouble(start.y);b.writeDouble(start.z);b.writeDouble(end.x);b.writeDouble(end.y);b.writeDouble(end.z);b.writeVarLong(energy);b.writeBoolean(focused);b.writeBoolean(particle);}
    public static LaserSegment read(RegistryFriendlyByteBuf b){return new LaserSegment(new Vec3(b.readDouble(),b.readDouble(),b.readDouble()),new Vec3(b.readDouble(),b.readDouble(),b.readDouble()),b.readVarLong(),b.readBoolean(),b.readBoolean());}
}
