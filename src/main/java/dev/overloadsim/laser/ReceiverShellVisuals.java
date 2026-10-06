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
        if (level == null || !level.isClientSide || controller.kind() == LaserKind.CORE) return;
        var owner = controller.getBlockPos();
        if (controller.isRemoved() || !chunkLoaded(level, owner) || level.getBlockEntity(owner) != controller) {
            remove(controller, token);
            return;
        }
        var world = world(level);
        synchronized (world) { world.controllers.put(owner, new WeakReference<>(controller)); }
        var cells = new HashSet<ReceiverShellRegistry.Point>();
        var template = controller.structureTemplate();
        if (controller.formed() && shellChunksLoaded(level, owner, template)) {
            // A pedestal controller keeps its server block and interaction,
            // while the shared base renders a flush ceramic body and screen.
            if(controller.kind()==LaserKind.RECEIVER&&controller.receiverPedestalLayout())
                cells.add(new ReceiverShellRegistry.Point(owner.getX(),owner.getY(),owner.getZ()));
            for (var entry : template.entrySet()) {
                var pos = owner.offset(entry.getKey());
                var block = level.getBlockState(pos).getBlock();
                // Optional network interfaces retain their own baked model and interaction faces.
                if ((entry.getValue() == LaserStructureGeometry.Part.FRAME && block == LaserContent.FRAME.get())
                        || (entry.getValue() == LaserStructureGeometry.Part.GLASS && block == LaserContent.GLASS.get()))
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

    /** Chunk load events can precede FULL promotion; defer skin republication to the next client tick. */
    public static void refreshPending() {
        ArrayList<World> worlds;
        synchronized (WORLDS) { worlds = new ArrayList<>(WORLDS.values()); }
        for (var world : worlds) {
            var level = world.level.get();
            if (level == null) continue;
            // Cached chunk identities avoid cell scans during ordinary per-tick availability checks.
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
    private static boolean shellChunksLoaded(Level level, BlockPos owner, Map<BlockPos,LaserStructureGeometry.Part> template) {
        var chunks = new HashSet<Long>();
        for (var entry : template.entrySet()) {
            if (entry.getValue() == LaserStructureGeometry.Part.AIR) continue;
            var pos = owner.offset(entry.getKey());
            long chunk = ((long) (pos.getX() >> 4) << 32) ^ ((pos.getZ() >> 4) & 0xffffffffL);
            if (chunks.add(chunk) && !chunkLoaded(level,pos)) return false;
        }
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
