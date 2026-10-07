package dev.overloadsim.workstation;

import dev.overloadsim.laser.LaserContent;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/** Menu prediction from synchronized formation only; never validates or forms a server structure. */
public final class WorkstationMenuAnchors {
    private static final Map<Level, Map<BlockPos, Anchor>> WORLDS = new WeakHashMap<>();
    private record Anchor(WeakReference<HighEnergyWorkstationBlockEntity> host, UUID identity, Direction facing, Set<Long> chunks) {}
    private WorkstationMenuAnchors() {}

    /** Keep geometric anchors when other shell chunks arrive after the controller's formed snapshot. */
    public static void publish(HighEnergyWorkstationBlockEntity host) {
        var level = host.getLevel();
        if (level == null) return;
        synchronized (WORLDS) {
            remove(level, host);
            if (!validHost(level, host, host.identity())) return;
            var template = host.template();
            var chunks = new HashSet<Long>();
            for (var pos : template.keySet()) chunks.add(chunk(pos));
            var anchor = new Anchor(new WeakReference<>(host), host.identity(), host.facing(), Set.copyOf(chunks));
            var world = WORLDS.computeIfAbsent(level, ignored -> new HashMap<>());
            for (var entry : template.entrySet())
                if (entry.getValue().is(LaserContent.FRAME.get()) || entry.getValue().is(LaserContent.PORT.get()))
                    world.put(entry.getKey().immutable(), anchor);
        }
    }

    public static HighEnergyWorkstationBlockEntity owner(Level level, BlockPos position) {
        if (level == null || position == null) return null;
        synchronized (WORLDS) {
            var world = WORLDS.get(level);
            if (world == null) return null;
            var anchor = world.get(position);
            if (anchor == null) return null;
            var host = anchor.host().get();
            if (host == null || !validHost(level, host, anchor.identity()) || host.facing() != anchor.facing()) {
                world.values().removeIf(value -> value == anchor);
                return null;
            }
            // Missing part chunks are temporary; a later load can use the same valid formed snapshot.
            for (long chunk : anchor.chunks())
                if (!level.getChunkSource().hasChunk((int) (chunk >> 32), (int) chunk)) return null;
            var current = level.getBlockState(position);
            if (current.is(LaserContent.FRAME.get()) || current.is(LaserContent.PORT.get())) return host;
            world.remove(position);
            return null;
        }
    }

    public static void remove(HighEnergyWorkstationBlockEntity host) {
        var level = host.getLevel();
        if (level == null) return;
        synchronized (WORLDS) { remove(level, host); }
    }

    private static void remove(Level level, HighEnergyWorkstationBlockEntity host) {
        var world = WORLDS.get(level);
        if (world != null) world.values().removeIf(anchor -> anchor.host().get() == null || anchor.host().get() == host);
    }

    private static boolean validHost(Level level, HighEnergyWorkstationBlockEntity host, UUID identity) {
        var position = host.getBlockPos();
        // ClientLevel.hasChunkAt does not detect absent chunks; this cache query never loads one.
        return host.getLevel() == level && !host.isRemoved() && host.identity().equals(identity)
                && level.getChunkSource().hasChunk(position.getX() >> 4, position.getZ() >> 4)
                && level.getBlockEntity(position) == host && host.formed();
    }

    private static long chunk(BlockPos position) {
        return ((long) (position.getX() >> 4) << 32) | ((position.getZ() >> 4) & 0xffffffffL);
    }
}
