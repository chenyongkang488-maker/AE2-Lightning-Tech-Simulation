package dev.overloadsim.laser;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;

/** Server-thread ownership of shell and aperture cells, without keeping worlds alive. */
public final class LaserStructureIndex {
    private record Owner(BlockPos controller, UUID identity) {}
    private static final Map<Level, Map<BlockPos, Owner>> OWNERS = new WeakHashMap<>();
    private static final Map<Level, Map<Owner, List<BlockPos>>> MEMBERS = new WeakHashMap<>();
    private static final Map<ServerLevel, Map<BlockPos, WeakReference<LaserMachineBlockEntity>>> CONTROLLERS = new WeakHashMap<>();
    private static boolean registered;
    private LaserStructureIndex() {}

    public static boolean occupied(Level level, BlockPos pos, LaserMachineBlockEntity host) {
        var owners = OWNERS.get(level);
        if (owners == null) return false;
        var owner = owners.get(pos);
        if (owner == null || owner.controller.equals(host.getBlockPos()) && owner.identity.equals(host.identity())) return false;
        if (!level.hasChunkAt(owner.controller)) return true;
        if (level.getBlockEntity(owner.controller) instanceof LaserMachineBlockEntity current && !current.isRemoved()
                && current.identity().equals(owner.identity) && current.structure() != null) return true;
        owners.remove(pos);
        return false;
    }

    public static void bind(LaserMachineBlockEntity host, LaserStructure structure) {
        release(host);
        var owners = OWNERS.computeIfAbsent(host.getLevel(), ignored -> new HashMap<>());
        var owner = new Owner(host.getBlockPos().immutable(), host.identity());
        for (var pos : structure.members()) owners.put(pos, owner);
        MEMBERS.computeIfAbsent(host.getLevel(), ignored -> new HashMap<>()).put(owner, structure.members());
    }

    public static void release(LaserMachineBlockEntity host) {
        var owners = OWNERS.get(host.getLevel());
        var bindings = MEMBERS.get(host.getLevel());
        if (owners == null || bindings == null) return;
        var owner = new Owner(host.getBlockPos(), host.identity());
        var members = bindings.remove(owner);
        if (members != null) for (var pos : members) owners.remove(pos, owner);
    }

    public static boolean touching(Level level, BlockPos pos, LaserMachineBlockEntity host) {
        if (host.kind() == LaserKind.CORE) return false;
        var owners = OWNERS.get(level);
        if (owners == null) return false;
        for (var direction : net.minecraft.core.Direction.values()) {
            var adjacent = pos.relative(direction);
            var owner = owners.get(adjacent);
            if (owner == null || owner.controller.equals(host.getBlockPos()) && owner.identity.equals(host.identity())) continue;
            if (!occupied(level, adjacent, host)) continue;
            if (level.hasChunkAt(owner.controller) && level.getBlockEntity(owner.controller) instanceof LaserMachineBlockEntity other && other.kind() == LaserKind.CORE) continue;
            return true;
        }
        return false;
    }

    public static void register(LaserMachineBlockEntity host) {
        if (host.getLevel() instanceof ServerLevel level) CONTROLLERS.computeIfAbsent(level, ignored -> new HashMap<>()).put(host.getBlockPos().immutable(), new WeakReference<>(host));
    }

    public static void unregister(LaserMachineBlockEntity host) {
        release(host);
        if (!(host.getLevel() instanceof ServerLevel level)) return;
        var controllers = CONTROLLERS.get(level);
        if (controllers != null) {
            var reference = controllers.get(host.getBlockPos());
            if (reference != null && reference.get() == host) controllers.remove(host.getBlockPos());
        }
    }

    public static List<LaserMachineBlockEntity> controllers(ServerLevel level) {
        var controllers = CONTROLLERS.get(level);
        if (controllers == null) return List.of();
        var result = new ArrayList<LaserMachineBlockEntity>();
        controllers.entrySet().removeIf(entry -> {
            var host = entry.getValue().get();
            if (host == null || host.isRemoved()) return true;
            if (level.hasChunkAt(entry.getKey()) && level.getBlockEntity(entry.getKey()) == host) result.add(host);
            return false;
        });
        return List.copyOf(result);
    }

    public static void changed(LevelAccessor accessor, BlockPos pos, boolean removed) {
        if (!(accessor instanceof Level level) || level.isClientSide) return;
        var owners = OWNERS.get(level);
        var owner = owners == null ? null : owners.get(pos);
        if (owner != null && level.hasChunkAt(owner.controller) && level.getBlockEntity(owner.controller) instanceof LaserMachineBlockEntity host
                && host.identity().equals(owner.identity)) {
            if (removed) host.memberRemoved(); else host.markStructureDirty();
        }
    }

    public static void register() {
        if (registered) return;
        registered = true;
        NeoForge.EVENT_BUS.addListener((BlockEvent.EntityPlaceEvent event) -> changed(event.getLevel(), event.getPos(), false));
        // BreakEvent precedes mutation and may still be cancelled by another mod. Actual removal and next-tick validation settle failures.
        NeoForge.EVENT_BUS.addListener((BlockEvent.BreakEvent event) -> changed(event.getLevel(), event.getPos(), false));
    }
}
