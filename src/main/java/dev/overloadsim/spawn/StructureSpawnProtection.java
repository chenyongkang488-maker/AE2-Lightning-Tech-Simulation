package dev.overloadsim.spawn;

import dev.overloadsim.laser.ConstraintFields;
import dev.overloadsim.laser.LaserMachineBlockEntity;
import dev.overloadsim.laser.LaserStructureIndex;
import dev.overloadsim.multiblock.SimulationStructureIndex;
import dev.overloadsim.workstation.WorkstationStructureIndex;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Natural-spawn protection belongs to formed cavities, independent of power or mob category. */
public final class StructureSpawnProtection {
    private StructureSpawnProtection() {}

    public static boolean preventsSpawn(ServerLevel level, BlockPos position, AABB box) {
        if (!level.hasChunkAt(position)) return false;
        var workstation = WorkstationStructureIndex.owner(level, position);
        if (workstation != null && new AABB(workstation.center()).inflate(2).intersects(box)) return true;
        var chamber = SimulationStructureIndex.owner(level, position);
        if (chamber != null) {
            var structure = chamber.structure();
            int edge = structure.size() - 1;
            if (new AABB(Vec3.atLowerCornerOf(structure.min().offset(1, 1, 1)),
                    Vec3.atLowerCornerOf(structure.min().offset(edge, edge, edge))).intersects(box)) return true;
        }
        for (var host : LaserStructureIndex.controllers(level)) {
            if (!laserInterior(host, box)) continue;
            host.checkStructure();
            if (host.formed() && laserInterior(host, box)) return true;
        }
        return false;
    }

    private static boolean laserInterior(LaserMachineBlockEntity host, AABB box) {
        return switch (host.kind()) {
            case CORE -> false;
            case CONSTRAINT, DEFLECTOR, RECEIVER -> ConstraintFields.intersects(host.center(), box);
            case EMITTER -> sphere(host.center(), box, 1.5);
            case ENHANCEMENT, BURST, FOCUS -> aperture(host, box);
        };
    }

    private static boolean sphere(BlockPos center, AABB box, double radius) {
        var point = center.getCenter();
        return gapSquared(point.x, box.minX, box.maxX) + gapSquared(point.y, box.minY, box.maxY)
                + gapSquared(point.z, box.minZ, box.maxZ) <= radius * radius;
    }

    /** A flat matrix owns its circular aperture through the one-block ring plane. */
    private static boolean aperture(LaserMachineBlockEntity host, AABB box) {
        var point = host.center().getCenter();
        double radialSquared;
        boolean throughPlane;
        switch (host.forward().getAxis()) {
            case X -> {
                throughPlane = box.maxX >= point.x - .5 && box.minX <= point.x + .5;
                radialSquared = gapSquared(point.y, box.minY, box.maxY) + gapSquared(point.z, box.minZ, box.maxZ);
            }
            case Y -> {
                throughPlane = box.maxY >= point.y - .5 && box.minY <= point.y + .5;
                radialSquared = gapSquared(point.x, box.minX, box.maxX) + gapSquared(point.z, box.minZ, box.maxZ);
            }
            case Z -> {
                throughPlane = box.maxZ >= point.z - .5 && box.minZ <= point.z + .5;
                radialSquared = gapSquared(point.x, box.minX, box.maxX) + gapSquared(point.y, box.minY, box.maxY);
            }
            default -> throw new IllegalStateException("unknown matrix axis");
        }
        return throughPlane && radialSquared <= ConstraintFields.RADIUS * ConstraintFields.RADIUS;
    }

    private static double gapSquared(double point, double minimum, double maximum) {
        double gap = Math.max(0, Math.max(minimum - point, point - maximum));
        return gap * gap;
    }
}
