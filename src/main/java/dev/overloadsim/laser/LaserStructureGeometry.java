package dev.overloadsim.laser;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/** One construction template shared by the validator, diagnostics and demonstration builder. */
public final class LaserStructureGeometry {
    public enum Part { CONTROLLER, FRAME, GLASS, SOURCE, PORT, AIR }
    private LaserStructureGeometry() {}

    public static Direction basisUp(Direction forward) {
        return forward.getAxis() == Direction.Axis.Y ? Direction.NORTH : Direction.UP;
    }

    public static BlockPos centerOffset(LaserKind kind, Direction facing) {
        return centerOffset(kind, facing, false);
    }

    public static BlockPos centerOffset(LaserKind kind, Direction facing, boolean receiverPedestalLayout) {
        return centerOffset(kind, facing, receiverPedestalLayout, false);
    }

    public static BlockPos centerOffset(LaserKind kind, Direction facing, boolean receiverPedestalLayout, boolean deflectorLegacyLayout) {
        Direction forward = facing.getOpposite();
        if (kind == LaserKind.CORE) return BlockPos.ZERO;
        if (kind == LaserKind.DEFLECTOR && !deflectorLegacyLayout) return BlockPos.ZERO.above(3);
        if (kind == LaserKind.RECEIVER && receiverPedestalLayout)
            return BlockPos.ZERO.relative(forward, 3).relative(basisUp(forward), 4);
        return kind == LaserKind.EMITTER ? BlockPos.ZERO.relative(forward, 2) : BlockPos.ZERO.relative(basisUp(forward), 3);
    }

    /** Historical templates; existing receiver spheres retain their original controller anchor. */
    public static Map<BlockPos, Part> template(LaserKind kind, Direction facing) {
        return template(kind, facing, false);
    }

    /** Argument is the controller's FACING, toward the player/upstream. */
    public static Map<BlockPos, Part> template(LaserKind kind, Direction facing, boolean receiverPedestalLayout) {
        return template(kind, facing, receiverPedestalLayout, false);
    }

    /** Saved deflectors retain their original anchor and inert vertical shell ports. */
    public static Map<BlockPos, Part> template(LaserKind kind, Direction facing, boolean receiverPedestalLayout, boolean deflectorLegacyLayout) {
        var result = new LinkedHashMap<BlockPos, Part>();
        if (kind == LaserKind.CORE) { result.put(BlockPos.ZERO, Part.CONTROLLER); return Collections.unmodifiableMap(result); }
        Direction forward = kind == LaserKind.DEFLECTOR && !deflectorLegacyLayout ? Direction.NORTH : facing.getOpposite();
        Direction up = basisUp(forward);
        var u = up.getNormal(); var f = forward.getNormal();
        var right = new BlockPos(u.getY() * f.getZ() - u.getZ() * f.getY(), u.getZ() * f.getX() - u.getX() * f.getZ(), u.getX() * f.getY() - u.getY() * f.getX());
        BlockPos center = centerOffset(kind, facing, receiverPedestalLayout, deflectorLegacyLayout);
        int radius = kind == LaserKind.EMITTER ? 2 : 3;
        boolean ring = kind == LaserKind.CONSTRAINT || kind == LaserKind.ENHANCEMENT || kind == LaserKind.BURST || kind == LaserKind.FOCUS;
        for (int x = -radius; x <= radius; x++) for (int y = -radius; y <= radius; y++) for (int z = ring ? 0 : -radius; z <= (ring ? 0 : radius); z++) {
            var offset = center.offset(right.getX() * x + u.getX() * y + f.getX() * z,
                    right.getY() * x + u.getY() * y + f.getY() * z,
                    right.getZ() * x + u.getZ() * y + f.getZ() * z);
            Part part;
            if (ring) {
                int radial = x * x + y * y;
                part = radial >= 7 && radial <= 12 ? Part.FRAME : Part.AIR;
            } else if (kind == LaserKind.EMITTER) {
                double ellipsoid = (x * x + y * y) / 5.0625 + z * z / 6.25;
                part = ellipsoid <= 1 && x * x + y * y + z * z >= 3 ? Part.FRAME : Part.AIR;
                if (x == 0 && y == 0 && z == 0) part = Part.SOURCE;
                if (x == 0 && y == 0 && z == 2) part = Part.PORT;
            } else if (kind == LaserKind.DEFLECTOR) {
                int distance = x * x + y * y + z * z;
                part = distance >= 7 && distance <= 12 ? Part.FRAME : Part.AIR;
                if (Math.abs(x) + Math.abs(y) + Math.abs(z) == 3 && (Math.abs(x) == 3 || Math.abs(y) == 3 || Math.abs(z) == 3))
                    part = deflectorLegacyLayout || y == 0 ? Part.PORT : Part.FRAME;
            } else {
                int distance = x * x + y * y + z * z;
                boolean shell = distance >= 7 && distance <= 12;
                // Three great-circle ribs frame transparent faces on the same voxel sphere as the deflector.
                part = shell ? (x == 0 || y == 0 || z == 0 ? Part.FRAME : Part.GLASS) : Part.AIR;
                if (x == 0 && y == 0 && z == -3) part = Part.PORT;
            }
            result.put(offset.immutable(), part);
        }
        if (kind == LaserKind.RECEIVER && receiverPedestalLayout) {
            for (int x = -3; x <= 3; x++) for (int y = -1; y <= 0; y++) for (int z = 0; z <= 6; z++) {
                var offset = new BlockPos(right.getX() * x + u.getX() * y + f.getX() * z,
                        right.getY() * x + u.getY() * y + f.getY() * z,
                        right.getZ() * x + u.getZ() * y + f.getZ() * z);
                result.put(offset, Part.FRAME);
            }
        }
        // The deflector's controller is a control block, never an optical endpoint.
        result.put(BlockPos.ZERO, Part.CONTROLLER);
        return Collections.unmodifiableMap(result);
    }
}
