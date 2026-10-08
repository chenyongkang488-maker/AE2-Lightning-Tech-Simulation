package dev.overloadsim.laser;

import dev.overloadsim.api.LaserEvents;
import dev.overloadsim.core.ExplosionSphere;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/** Incremental spherical destruction; never requests or generates a chunk. */
public final class LaserExplosion {
    private static final Map<ServerLevel, ArrayDeque<Blast>> JOBS = new WeakHashMap<>();

    private static final class Blast {
        final BlockPos center;
        final int radius, minX, minY, minZ, width, height;
        int cursor;

        Blast(ServerLevel level, BlockPos center, int radius) {
            this.center = center.immutable();
            this.radius = radius;
            minX = center.getX() - radius;
            minZ = center.getZ() - radius;
            width = radius * 2 + 1;
            minY = Math.max(level.getMinBuildHeight(), center.getY() - radius);
            height = Math.max(0, Math.min(level.getMaxBuildHeight(), center.getY() + radius + 1) - minY);
        }

        boolean done() { return cursor >= width * width * height; }

        BlockPos next() {
            int c = cursor++;
            return new BlockPos(minX + c % width, minY + c / (width * width), minZ + c / width % width);
        }

        boolean contains(BlockPos pos) {
            // Both centers have the same half-block offset, so their integer difference is exact.
            return ExplosionSphere.contains(radius, pos.getX() - center.getX(), pos.getY() - center.getY(), pos.getZ() - center.getZ());
        }
    }

    private LaserExplosion() {}

    public static void trigger(LaserMachineBlockEntity host) {
        if (!LaserSettings.explosionEnabled() || !(host.getLevel() instanceof ServerLevel level) || host.explosionTriggered()) return;
        var centerBlock = host.center().immutable();
        var event = new LaserEvents.BeforeExplosion(level, host.getBlockPos(), centerBlock,
                host.formed() ? "particle_overflow" : "containment_broken", ExplosionSphere.MAX_RADIUS_BLOCKS);
        NeoForge.EVENT_BUS.post(event);
        if (event.isCanceled() || !LaserSettings.explosionEnabled() || !host.markExplosionTriggered()) return;

        host.particles().load(0);
        host.setEnabled(false);
        host.sync();
        host.saveChanges();
        int radius = event.radiusBlocks();
        JOBS.computeIfAbsent(level, l -> new ArrayDeque<>()).add(new Blast(level, centerBlock, radius));
        var center = centerBlock.getCenter();
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, center.x, center.y, center.z, 1, 0, 0, 0, 0);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 6, 0.65F);
        // The candidate box includes the boundary; only entity positions within the sphere receive damage.
        for (var entity : level.getEntitiesOfClass(LivingEntity.class, new AABB(centerBlock).inflate(radius))) {
            if (ExplosionSphere.contains(radius, entity.getX() - center.x, entity.getY() - center.y, entity.getZ() - center.z)) {
                entity.hurt(level.damageSources().explosion(null, null), 80);
            }
        }
    }

    public static void tick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!LaserSettings.explosionEnabled()) { JOBS.remove(level); return; }
        var jobs = JOBS.get(level);
        if (jobs == null) return;
        int budget = LaserSettings.blastBudget();
        while (budget > 0 && !jobs.isEmpty()) {
            var job = jobs.peek();
            while (budget > 0 && !job.done()) {
                budget--;
                var pos = job.next();
                if (job.contains(pos)) LaserNetwork.breakCell(level, pos);
            }
            if (job.done()) jobs.remove();
        }
        if (jobs.isEmpty()) JOBS.remove(level);
    }
}
