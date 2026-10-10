package dev.overloadsim.workstation;

import dev.overloadsim.core.ReceiverShellRegistry;
import dev.overloadsim.compat.BlockRenderViewLevel;
import dev.overloadsim.laser.LaserContent;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;

/** Cosmetic shell ownership uses the existing immutable receiver registry, without server formation. */
public final class WorkstationShellVisuals {
    private static final Map<Level, World> WORLDS = Collections.synchronizedMap(new WeakHashMap<>());
    private record Controller(WeakReference<HighEnergyWorkstationBlockEntity> host, Object token) {}
    private static final class World {
        final WeakReference<Level> level;
        final ReceiverShellRegistry registry = new ReceiverShellRegistry();
        final Map<BlockPos, Controller> controllers = new HashMap<>();
        boolean pending;
        World(Level level) { this.level = new WeakReference<>(level); }
    }
    private WorkstationShellVisuals() {}

    public static void publish(HighEnergyWorkstationBlockEntity host, Object token) {
        var level = host.getLevel();
        if (level == null || !level.isClientSide) return;
        var owner = host.getBlockPos();
        if (host.isRemoved() || !chunkLoaded(level, owner) || level.getBlockEntity(owner) != host) {
            remove(host, token);
            return;
        }
        var world = world(level);
        synchronized (world) { world.controllers.put(owner, new Controller(new WeakReference<>(host), token)); }
        var cells = new HashSet<ReceiverShellRegistry.Point>();
        var template = host.template();
        if (host.formed() && shellChunksLoaded(level, template.keySet()))
            for (var entry : template.entrySet()) {
                var expected = entry.getValue();
                var position = entry.getKey();
                if ((expected.is(LaserContent.FRAME.get()) || expected.is(LaserContent.GLASS.get())
                        || expected.is(LaserContent.PORT.get())) && level.getBlockState(position).is(expected.getBlock()))
                    cells.add(new ReceiverShellRegistry.Point(position.getX(), position.getY(), position.getZ()));
            }
        // The actual controller stays baked and interactive in the fitted front bay.
        refresh(level, world.registry.replace(token, owner.getX(), owner.getY(), owner.getZ(), cells));
    }

    public static void remove(HighEnergyWorkstationBlockEntity host, Object token) {
        var level = host.getLevel();
        if (level == null || !level.isClientSide) return;
        var world = WORLDS.get(level);
        if (world == null) return;
        var owner = host.getBlockPos();
        synchronized (world) {
            var previous = world.controllers.get(owner);
            if (previous != null && previous.host().get() == host) world.controllers.remove(owner);
        }
        refresh(level, world.registry.remove(token, owner.getX(), owner.getY(), owner.getZ()));
    }

    public static boolean visible(HighEnergyWorkstationBlockEntity host) {
        var level = host.getLevel();
        if (level == null || !level.isClientSide || host.isRemoved() || !host.formed()) return false;
        var world = WORLDS.get(level);
        if (world == null) return false;
        var owner = host.getBlockPos();
        synchronized (world) {
            var controller = world.controllers.get(owner);
            return controller != null && controller.host().get() == host
                    && world.registry.owns(controller.token(), owner.getX(), owner.getY(), owner.getZ());
        }
    }

    /** Ordinary shell cells need no block entity or meshing-view light engine. */
    public static boolean hidden(BlockAndTintGetter region, BlockPos position) {
        var level=BlockRenderViewLevel.level(region);
        var world = level==null?null:WORLDS.get(level);
        return world != null && world.registry.hidden(position.getX(), position.getY(), position.getZ());
    }

    public static void unloadChunk(Level level, int x, int z) {
        var world = WORLDS.get(level);
        if (world != null) refresh(level, world.registry.unloadChunk(x, z));
    }

    public static void loadedChunk(Level level) {
        var world = WORLDS.get(level);
        if (world != null) synchronized (world) { world.pending = true; }
    }

    public static void refreshPending() {
        ArrayList<World> worlds;
        synchronized (WORLDS) { worlds = new ArrayList<>(WORLDS.values()); }
        for (var world : worlds) {
            var level = world.level.get();
            if (level == null) continue;
            refresh(level, world.registry.pruneUnavailable((x, z) -> level.getChunkSource().hasChunk(x, z)));
            var controllers = new ArrayList<Controller>();
            synchronized (world) {
                if (!world.pending) continue;
                world.pending = false;
                world.controllers.values().removeIf(controller -> controller.host().get() == null);
                controllers.addAll(world.controllers.values());
            }
            for (var controller : controllers) {
                var host = controller.host().get();
                if (host != null) publish(host, controller.token());
            }
        }
    }

    private static World world(Level level) {
        synchronized (WORLDS) { return WORLDS.computeIfAbsent(level, ignored -> new World(level)); }
    }

    private static boolean chunkLoaded(Level level, BlockPos position) {
        return level.getChunkSource().hasChunk(position.getX() >> 4, position.getZ() >> 4);
    }

    private static boolean shellChunksLoaded(Level level, Set<BlockPos> positions) {
        var chunks = new HashSet<Long>();
        for (var position : positions) {
            long chunk = ((long) (position.getX() >> 4) << 32) | ((position.getZ() >> 4) & 0xffffffffL);
            if (chunks.add(chunk) && !chunkLoaded(level, position)) return false;
        }
        return true;
    }

    private static void refresh(Level level, Set<ReceiverShellRegistry.Point> changed) {
        for (var point : changed) {
            var position = new BlockPos(point.x(), point.y(), point.z());
            if (!chunkLoaded(level, position)) continue;
            var state = level.getBlockState(position);
            level.sendBlockUpdated(position, state, state, 8);
        }
    }
}
