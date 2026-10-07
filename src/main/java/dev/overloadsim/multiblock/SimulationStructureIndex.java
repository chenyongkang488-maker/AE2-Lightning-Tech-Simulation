package dev.overloadsim.multiblock;

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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import org.jetbrains.annotations.Nullable;

/** Server-thread chamber ownership, including its reserved interior cells. */
public final class SimulationStructureIndex {
    private static final ThreadLocal<Integer> DEPTH = ThreadLocal.withInitial(() -> 0);
    private record Owner(BlockPos controller, UUID identity) {}
    private static final Map<Level, Map<BlockPos, Set<Owner>>> OWNERS = new WeakHashMap<>();
    private static boolean registered;
    private SimulationStructureIndex() {}

    public static boolean occupied(Level level, BlockPos pos, SimulationControllerBlockEntity host) {
        return occupiedByOther(level,pos,host);
    }

    public static boolean occupiedByOther(Level level, BlockPos pos, BlockEntity host) {
        var claims = claims(level,pos);
        if (claims == null) return false;
        for (var owner : claims) {
            if (host instanceof SimulationControllerBlockEntity chamber
                    && owner.identity.equals(chamber.identity()) && owner.controller.equals(chamber.getBlockPos())) continue;
            return true;
        }
        return false;
    }

    /** Never scans for a controller or forces a chunk load when a frame is used. */
    public static @Nullable SimulationControllerBlockEntity frameOwner(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel) || !level.hasChunkAt(pos) || !level.getBlockState(pos).is(MultiblockContent.FRAME.get())) return null;
        var claims = claims(level,pos);
        if (claims == null || claims.size() != 1) return null;
        var owner = claims.iterator().next();
        if (!level.hasChunkAt(owner.controller)
                || !(level.getBlockEntity(owner.controller) instanceof SimulationControllerBlockEntity host)
                || host.getLevel() != level || host.isRemoved() || !host.identity().equals(owner.identity)
                || !level.getBlockState(owner.controller).is(MultiblockContent.CONTROLLER.get())
                || !level.getBlockState(owner.controller).equals(host.getBlockState())) return null;
        host.checkStructure();
        var currentClaims = claims(level,pos);
        var state = level.getBlockState(pos);
        if (host.isRemoved() || level.getBlockEntity(owner.controller) != host || !host.identity().equals(owner.identity)
                || host.structure() == null || !host.loaded() || !host.structure().members().contains(pos)
                || !state.is(MultiblockContent.FRAME.get()) || !state.getValue(SimulationPartBlock.FORMED)
                || !(level.getBlockEntity(pos) instanceof SimulationMemberBlockEntity member) || !member.ownedBy(host)
                || currentClaims == null || currentClaims.size() != 1 || !currentClaims.contains(owner)) return null;
        return host;
    }

    private static @Nullable Set<Owner> claims(Level level, BlockPos pos) {
        var owners = OWNERS.get(level);
        var claims = owners == null ? null : owners.get(pos);
        if (claims == null) return null;
        claims.removeIf(owner -> level.hasChunkAt(owner.controller)
                && (!(level.getBlockEntity(owner.controller) instanceof SimulationControllerBlockEntity host)
                    || host.isRemoved() || host.getLevel() != level || !host.identity().equals(owner.identity) || host.structure() == null));
        if (claims.isEmpty()) { owners.remove(pos); return null; }
        return claims;
    }

    public static void bind(SimulationControllerBlockEntity host, SimulationStructure structure) {
        release(host);
        var owners = OWNERS.computeIfAbsent(host.getLevel(), ignored -> new HashMap<>());
        var owner = new Owner(host.getBlockPos().immutable(),host.identity());
        for (int x=0;x<structure.size();x++) for (int y=0;y<structure.size();y++) for (int z=0;z<structure.size();z++)
            owners.computeIfAbsent(structure.min().offset(x,y,z), ignored -> new HashSet<>()).add(owner);
    }

    public static void release(SimulationControllerBlockEntity host) {
        var owners = OWNERS.get(host.getLevel());
        if (owners == null) return;
        var owner = new Owner(host.getBlockPos(),host.identity());
        owners.entrySet().removeIf(entry -> { entry.getValue().remove(owner); return entry.getValue().isEmpty(); });
    }

    private static void changed(LevelAccessor accessor, BlockPos pos) {
        if (converting() || !(accessor instanceof Level level) || level.isClientSide) return;
        var owners = OWNERS.get(level);
        var claims = owners == null ? null : owners.get(pos);
        if (claims != null) for (var owner : List.copyOf(claims)) {
            if (level.hasChunkAt(owner.controller) && level.getBlockEntity(owner.controller) instanceof SimulationControllerBlockEntity host
                    && !host.isRemoved() && host.identity().equals(owner.identity)) host.markStructureDirty();
        }
    }

    public static void register() {
        if (registered) return;
        registered = true;
        NeoForge.EVENT_BUS.addListener((BlockEvent.EntityPlaceEvent event) -> changed(event.getLevel(),event.getPos()));
        NeoForge.EVENT_BUS.addListener((BlockEvent.BreakEvent event) -> changed(event.getLevel(),event.getPos()));
    }

    public static boolean converting() { return DEPTH.get() > 0; }
    public static void converting(Runnable work) {
        DEPTH.set(DEPTH.get()+1);
        try { work.run(); } finally { DEPTH.set(DEPTH.get()-1); }
    }
}
