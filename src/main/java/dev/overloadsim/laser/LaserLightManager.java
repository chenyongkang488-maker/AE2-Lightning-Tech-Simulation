package dev.overloadsim.laser;

import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/** At most128 air-only light cells per controller, with shared leases and loaded-only cleanup. */
public final class LaserLightManager {
    public static final int MAX_CELLS = 128;
    private static final int SAMPLE_DISTANCE = 8;
    private final LaserMachineBlockEntity host;
    private final Set<BlockPos> cells = new LinkedHashSet<>();

    public LaserLightManager(LaserMachineBlockEntity host) { this.host = host; }

    /** Structure apertures and optical traversal treat only this invisible proxy as air. */
    public static boolean isBeamAir(BlockState state) { return state.isAir() || state.is(LaserContent.LIGHT.get()); }

    /** The shared acquisition boundary never replaces water, plants, solid blocks or foreign light sources. */
    public static boolean claim(ServerLevel level, BlockPos pos, LaserMachineBlockEntity owner) {
        if (owner.isRemoved() || owner.getLevel() != level || !level.hasChunkAt(pos)
                || !level.getWorldBorder().isWithinBounds(pos) || level.isOutsideBuildHeight(pos)) return false;
        var state = level.getBlockState(pos);
        if (state.isAir()) level.setBlockAndUpdate(pos, LaserContent.LIGHT.get().defaultBlockState());
        return level.getBlockEntity(pos) instanceof LaserLightBlockEntity light && light.claim(owner);
    }

    public static void release(ServerLevel level, BlockPos pos, LaserMachineBlockEntity owner) {
        if (level.hasChunkAt(pos) && level.getBlockEntity(pos) instanceof LaserLightBlockEntity light) light.release(owner);
    }

    public void refresh() {
        if (!(host.getLevel() instanceof ServerLevel level)) return;
        var desired = new LinkedHashSet<BlockPos>();
        if (!host.isRemoved() && host.formed()) {
            // The formed emitter's cap and rings stay visibly illuminated even between work batches.
            if (host.kind() == LaserKind.EMITTER) desired.add(host.center().relative(host.forward(), 3));
            if (host.enabled() && host.activeUntil() > level.getGameTime()) {
                for (var segment : host.segments()) {
                    if (desired.size() >= MAX_CELLS) break;
                    var delta = segment.end().subtract(segment.start());
                    double distance = delta.length();
                    if (distance < 0.001) continue;
                    var direction = delta.scale(1 / distance);
                    for (int step = 0; step <= distance && desired.size() < MAX_CELLS; step += SAMPLE_DISTANCE)
                        desired.add(BlockPos.containing(segment.start().add(direction.scale(step))));
                    if (desired.size() < MAX_CELLS) desired.add(BlockPos.containing(segment.end()));
                }
            }
        }
        for (var previous : Set.copyOf(cells)) if (!desired.contains(previous)) {
            release(level, previous, host); cells.remove(previous);
        }
        for (var pos : desired) if (claim(level, pos, host)) cells.add(pos.immutable());
    }

    public void clear() {
        if (host.getLevel() instanceof ServerLevel level) for (var pos : cells) release(level, pos, host);
        cells.clear();
    }
}
