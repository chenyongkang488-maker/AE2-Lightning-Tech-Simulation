package dev.overloadsim.laser;

import dev.overloadsim.core.ReceiverShellRegistry;
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
import net.minecraft.world.level.lighting.LevelLightEngine;

/** Cosmetic ownership only: neither block states nor server structure data are changed. */
public final class ReceiverShellVisuals {
    private static final Map<LevelLightEngine, World> WORLDS = Collections.synchronizedMap(new WeakHashMap<>());
    private static final class World {
        final WeakReference<Level> level;
        final ReceiverShellRegistry registry = new ReceiverShellRegistry();
        final Map<BlockPos, WeakReference<LaserMachineBlockEntity>> controllers = new HashMap<>();
        boolean pending;
        World(Level level) { this.level = new WeakReference<>(level); }
    }
    private ReceiverShellVisuals() {}

    public static void publish(LaserMachineBlockEntity controller, Object token) {
        var level = controller.getLevel();
        if (level == null || !level.isClientSide || controller.kind() != LaserKind.RECEIVER) return;
        var owner = controller.getBlockPos();
        if (controller.isRemoved() || !chunkLoaded(level, owner) || level.getBlockEntity(owner) != controller) {
            remove(controller, token);
            return;
        }
        var world = world(level);
        synchronized (world) { world.controllers.put(owner, new WeakReference<>(controller)); }
        var cells = new HashSet<ReceiverShellRegistry.Point>();
        if (controller.formed() && shellChunksLoaded(level, controller.center())) {
            var template = LaserStructureGeometry.template(LaserKind.RECEIVER, controller.receiverVisualFacing());
            for (var entry : template.entrySet()) {
                var pos = owner.offset(entry.getKey());
                if (entry.getValue() == LaserStructureGeometry.Part.FRAME || entry.getValue() == LaserStructureGeometry.Part.GLASS)
                    cells.add(new ReceiverShellRegistry.Point(pos.getX(), pos.getY(), pos.getZ()));
            }
        }
        refresh(level, world.registry.replace(token, owner.getX(), owner.getY(), owner.getZ(), cells));
    }

    public static void remove(LaserMachineBlockEntity controller, Object token) {
        var level = controller.getLevel();
        if (level == null || !level.isClientSide) return;
        var world = WORLDS.get(level.getLightEngine());
        if (world == null) return;
        var owner = controller.getBlockPos();
        synchronized (world) {
            var previous = world.controllers.get(owner);
            if (previous != null && previous.get() == controller) world.controllers.remove(owner);
        }
        refresh(level, world.registry.remove(token, owner.getX(), owner.getY(), owner.getZ()));
    }

    /** RenderChunkRegion shares its world's light engine; shell parts need no block entity lookup. */
    public static boolean hidden(BlockAndTintGetter region, BlockPos pos) {
        var world = WORLDS.get(region.getLightEngine());
        return world != null && world.registry.hidden(pos.getX(), pos.getY(), pos.getZ());
    }

    public static boolean visible(LaserMachineBlockEntity controller, Object token) {
        var level = controller.getLevel();
        if (level == null || !level.isClientSide) return false;
        var world = WORLDS.get(level.getLightEngine()); var owner = controller.getBlockPos();
        return world != null && world.registry.owns(token, owner.getX(), owner.getY(), owner.getZ());
    }

    public static void unloadChunk(Level level, int x, int z) {
        var world = WORLDS.get(level.getLightEngine());
        if (world != null) refresh(level, world.registry.unloadChunk(x, z));
    }

    public static void loadedChunk(Level level) {
        var world = WORLDS.get(level.getLightEngine());
        if (world != null) synchronized (world) { world.pending = true; }
    }

    /** Chunk load events can precede FULL promotion; defer receiver republication to the next client tick. */
    public static void refreshPending() {
        ArrayList<World> worlds;
        synchronized (WORLDS) { worlds = new ArrayList<>(WORLDS.values()); }
        for (var world : worlds) {
            var level = world.level.get();
            if (level == null) continue;
            // Cached chunk identities keep this bounded to at most four checks per formed receiver per tick.
            refresh(level, world.registry.pruneUnavailable((x, z) -> level.getChunkSource().hasChunk(x, z)));
            ArrayList<LaserMachineBlockEntity> controllers = new ArrayList<>();
            synchronized (world) {
                if (!world.pending) continue;
                world.pending = false;
                world.controllers.values().removeIf(reference -> reference.get() == null);
                for (var reference : world.controllers.values()) {
                    var controller = reference.get();
                    if (controller != null) controllers.add(controller);
                }
            }
            for (var controller : controllers) controller.publishReceiverVisuals();
        }
    }

    private static World world(Level level) {
        synchronized (WORLDS) { return WORLDS.computeIfAbsent(level.getLightEngine(), key -> new World(level)); }
    }
    private static boolean chunkLoaded(Level level, BlockPos pos) {
        // ClientLevel.hasChunkAt always returns true; ChunkSource checks FULL with load=false.
        return level.getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4);
    }
    private static boolean shellChunksLoaded(Level level, BlockPos center) {
        for (int x = (center.getX() - 3) >> 4; x <= (center.getX() + 3) >> 4; x++)
            for (int z = (center.getZ() - 3) >> 4; z <= (center.getZ() + 3) >> 4; z++)
                if (!level.getChunkSource().hasChunk(x, z)) return false;
        return true;
    }
    private static void refresh(Level level, Set<ReceiverShellRegistry.Point> changed) {
        for (var point : changed) {
            var pos = new BlockPos(point.x(), point.y(), point.z());
            if (!chunkLoaded(level, pos)) continue;
            var state = level.getBlockState(pos);
            level.sendBlockUpdated(pos, state, state, 8);
        }
    }
}
