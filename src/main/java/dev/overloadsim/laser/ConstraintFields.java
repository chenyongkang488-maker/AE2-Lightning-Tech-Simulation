package dev.overloadsim.laser;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;

/** Loaded formed interiors prevent natural spawns; only paid working fields deal damage. */
public final class ConstraintFields {
    public static final double RADIUS = 2.5;
    private static boolean registered;
    private ConstraintFields() {}

    public static AABB bounds(BlockPos center) {
        var point = center.getCenter();
        return new AABB(point, point).inflate(RADIUS);
    }

    public static boolean intersects(BlockPos center, AABB box) {
        var point = center.getCenter();
        double x = gap(point.x, box.minX, box.maxX), y = gap(point.y, box.minY, box.maxY), z = gap(point.z, box.minZ, box.maxZ);
        return x * x + y * y + z * z <= RADIUS * RADIUS;
    }

    private static double gap(double point, double min, double max) {
        return Math.max(0, Math.max(min - point, point - max));
    }

    private static boolean preventsSpawn(ServerLevel level, AABB box) {
        for (var host : LaserStructureIndex.controllers(level)) {
            if (host.kind() != LaserKind.CONSTRAINT || !intersects(host.center(), box)) continue;
            host.checkStructure();
            if (host.formed() && intersects(host.center(), box)) return true;
        }
        return false;
    }

    public static void register() {
        if (registered) return;
        registered = true;
        NeoForge.EVENT_BUS.addListener((MobSpawnEvent.SpawnPlacementCheck event) -> {
            if (event.getSpawnType() != MobSpawnType.NATURAL || !(event.getLevel() instanceof ServerLevel level)) return;
            var pos = event.getPos();
            var box = event.getEntityType().getDimensions().makeBoundingBox(pos.getX() + .5, pos.getY(), pos.getZ() + .5);
            if (preventsSpawn(level, box)) event.setResult(MobSpawnEvent.SpawnPlacementCheck.Result.FAIL);
        });
        NeoForge.EVENT_BUS.addListener((MobSpawnEvent.PositionCheck event) -> {
            if (event.getSpawnType() != MobSpawnType.NATURAL || !(event.getLevel() instanceof ServerLevel level)) return;
            if (preventsSpawn(level, event.getEntity().getBoundingBox())) event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
        });
    }
}
