package dev.overloadsim.laser;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import org.jetbrains.annotations.Nullable;

/** Server-thread ownership of shell and aperture cells, without keeping worlds alive. */
public final class LaserStructureIndex {
    private record Owner(BlockPos controller, UUID identity) {}
    private static final Map<Level, Map<BlockPos, Set<Owner>>> OWNERS = new WeakHashMap<>();
    private static final Map<Level, Map<Owner, List<BlockPos>>> MEMBERS = new WeakHashMap<>();
    private static final Map<ServerLevel, Map<BlockPos, WeakReference<LaserMachineBlockEntity>>> CONTROLLERS = new WeakHashMap<>();
    private static boolean registered;
    private LaserStructureIndex() {}

    public static boolean occupied(Level level, BlockPos pos, LaserMachineBlockEntity host) {
        return occupiedByOther(level,pos,host);
    }

    /** Cross-system construction checks must also respect reserved empty laser apertures. */
    public static boolean occupiedByOther(Level level,BlockPos pos,net.minecraft.world.level.block.entity.BlockEntity host) {
        var claims = claims(level,pos);
        if (claims == null) return false;
        for (var owner : claims) {
            if (host instanceof LaserMachineBlockEntity laser && owner.controller.equals(laser.getBlockPos()) && owner.identity.equals(laser.identity())) continue;
            return true;
        }
        return false;
    }

    /** Existing formed ownership only; a click never discovers or forms an unowned shell. */
    public static @Nullable LaserMachineBlockEntity frameOwner(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel) || !level.hasChunkAt(pos) || !level.getBlockState(pos).is(LaserContent.FRAME.get())) return null;
        var claims = claims(level,pos);
        if (claims == null || claims.size() != 1) return null;
        var owner = claims.iterator().next();
        if (!level.hasChunkAt(owner.controller)
                || !(level.getBlockEntity(owner.controller) instanceof LaserMachineBlockEntity host)
                || host.getLevel() != level || host.isRemoved() || !host.identity().equals(owner.identity)
                || !(level.getBlockState(owner.controller).getBlock() instanceof LaserMachineBlock)
                || !level.getBlockState(owner.controller).equals(host.getBlockState())) return null;
        host.checkStructure();
        var currentClaims = claims(level,pos);
        if (!host.formed() || level.getBlockEntity(owner.controller) != host || !host.identity().equals(owner.identity)
                || host.structure() == null || !host.structure().members().contains(pos)
                || !level.getBlockState(pos).is(LaserContent.FRAME.get())
                || host.structureTemplate().get(pos.subtract(host.getBlockPos())) != LaserStructureGeometry.Part.FRAME
                || currentClaims == null || currentClaims.size() != 1 || !currentClaims.contains(owner)) return null;
        return host;
    }

    private static @Nullable Set<Owner> claims(Level level, BlockPos pos) {
        var owners = OWNERS.get(level);
        var claims = owners == null ? null : owners.get(pos);
        if (claims == null) return null;
        claims.removeIf(owner -> level.hasChunkAt(owner.controller)
                && (!(level.getBlockEntity(owner.controller) instanceof LaserMachineBlockEntity host)
                    || host.isRemoved() || host.getLevel() != level || !host.identity().equals(owner.identity) || host.structure() == null));
        if (claims.isEmpty()) { owners.remove(pos); return null; }
        return claims;
    }

    public static void bind(LaserMachineBlockEntity host, LaserStructure structure) {
        release(host);
        var owners = OWNERS.computeIfAbsent(host.getLevel(), ignored -> new HashMap<>());
        var owner = new Owner(host.getBlockPos().immutable(), host.identity());
        for (var pos : structure.members()) owners.computeIfAbsent(pos, ignored -> new HashSet<>()).add(owner);
        MEMBERS.computeIfAbsent(host.getLevel(), ignored -> new HashMap<>()).put(owner, structure.members());
    }

    public static void release(LaserMachineBlockEntity host) {
        var owners = OWNERS.get(host.getLevel());
        var bindings = MEMBERS.get(host.getLevel());
        if (owners == null || bindings == null) return;
        var owner = new Owner(host.getBlockPos(), host.identity());
        var members = bindings.remove(owner);
        if (members != null) for (var pos : members) {
            var claims = owners.get(pos);
            if (claims != null) { claims.remove(owner); if (claims.isEmpty()) owners.remove(pos); }
        }
    }

    public static boolean touching(Level level, BlockPos pos, LaserMachineBlockEntity host) {
        if (host.kind() == LaserKind.CORE) return false;
        var owners = OWNERS.get(level);
        if (owners == null) return false;
        for (var direction : net.minecraft.core.Direction.values()) {
            var adjacent = pos.relative(direction);
            var claims = claims(level,adjacent);
            if (claims == null) continue;
            for (var owner : claims) {
                if (owner.controller.equals(host.getBlockPos()) && owner.identity.equals(host.identity())) continue;
                if (level.hasChunkAt(owner.controller) && level.getBlockEntity(owner.controller) instanceof LaserMachineBlockEntity other && other.kind() == LaserKind.CORE) continue;
                return true;
            }
        }
        return false;
    }

    public static void register(LaserMachineBlockEntity host) {
        if (host.getLevel() instanceof ServerLevel level) {
            var controllers=CONTROLLERS.computeIfAbsent(level, ignored -> new HashMap<>());
            var previous=controllers.get(host.getBlockPos());
            if(previous==null||previous.get()!=host){controllers.put(host.getBlockPos().immutable(),new WeakReference<>(host));OpticalChanges.invalidate(level);}
        }
    }

    public static void unregister(LaserMachineBlockEntity host) {
        release(host);
        if (!(host.getLevel() instanceof ServerLevel level)) return;
        OpticalChanges.invalidate(level);
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
        var claims = owners == null ? null : owners.get(pos);
        if (claims != null) for (var owner : List.copyOf(claims)) {
            if (level.hasChunkAt(owner.controller) && level.getBlockEntity(owner.controller) instanceof LaserMachineBlockEntity host
                    && !host.isRemoved() && host.identity().equals(owner.identity)) {
                if (removed) host.memberRemoved(); else host.markStructureDirty();
            }
        }
    }

    public static void register() {
        if (registered) return;
        registered = true;
        NeoForge.EVENT_BUS.addListener((BlockEvent.EntityPlaceEvent event) -> changed(event.getLevel(), event.getPos(), false));
        // BreakEvent precedes mutation and may still be cancelled by another mod. Actual removal and next-tick validation settle failures.
        NeoForge.EVENT_BUS.addListener((BlockEvent.BreakEvent event) -> changed(event.getLevel(), event.getPos(), false));
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.level.ChunkEvent.Load event) -> {if(event.getLevel() instanceof Level level)OpticalChanges.chunksChanged(level);});
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.level.ChunkEvent.Unload event) -> {if(event.getLevel() instanceof Level level)OpticalChanges.chunksChanged(level);});
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.server.ServerStoppedEvent event) -> OpticalChanges.clearAll());
    }
}
