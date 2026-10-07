package dev.overloadsim.laser;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/** Optional receiver apertures are real port blocks; no mode, selection or resources are persisted. */
public final class ReceiverOpticalPorts {
    private ReceiverOpticalPorts() {}

    /** Empty construction keeps the canonical one-port template; chosen ports replace only legal frames. */
    public static Map<BlockPos, LaserStructureGeometry.Part> resolve(LaserMachineBlockEntity host,
            Map<BlockPos, LaserStructureGeometry.Part> canonical) {
        var level = host.getLevel();
        if (host.kind() != LaserKind.RECEIVER || level == null) return canonical;
        var selected = new LinkedHashSet<BlockPos>();
        for (var offset : LaserStructureGeometry.receiverOpticalOffsets(facing(host), host.receiverPedestalLayout())) {
            var part = canonical.get(offset);
            if (part != LaserStructureGeometry.Part.FRAME && part != LaserStructureGeometry.Part.PORT) continue;
            var pos = host.getBlockPos().offset(offset);
            if (loaded(level, pos) && level.getBlockState(pos).is(LaserContent.PORT.get())) selected.add(offset);
        }
        if (selected.isEmpty()) return canonical;
        var resolved = new LinkedHashMap<BlockPos, LaserStructureGeometry.Part>();
        canonical.forEach((offset, part) -> resolved.put(offset,
                part == LaserStructureGeometry.Part.PORT ? LaserStructureGeometry.Part.FRAME : part));
        selected.forEach(offset -> resolved.put(offset, LaserStructureGeometry.Part.PORT));
        return Collections.unmodifiableMap(resolved);
    }

    /** Geometry-only predicate, also usable by construction projections before formation. */
    public static boolean compatible(LaserMachineBlockEntity host, BlockPos pos) {
        return host.kind() == LaserKind.RECEIVER && pos != null
                && LaserStructureGeometry.receiverOpticalOffsets(facing(host), host.receiverPedestalLayout())
                        .contains(pos.subtract(host.getBlockPos()));
    }

    /** Incoming direction is travel toward the sphere; controller facing does not select horizontal ports. */
    public static boolean accepts(LaserMachineBlockEntity host, BlockPos port, Direction incoming) {
        var level = host.getLevel();
        return level != null && incoming != null && port != null && host.kind() == LaserKind.RECEIVER
                && host.formed() && host.enabled() && compatible(host, port)
                && host.center().relative(incoming.getOpposite(), 3).equals(port)
                && loaded(level, port) && level.getBlockState(port).is(LaserContent.PORT.get());
    }

    private static Direction facing(LaserMachineBlockEntity host) {
        return host.getLevel() != null && host.getLevel().isClientSide ? host.receiverVisualFacing()
                : host.getBlockState().getValue(LaserMachineBlock.FACING);
    }

    private static boolean loaded(Level level, BlockPos pos) {
        // ClientLevel.hasChunkAt returns true even for absent chunks; this cache query never loads one.
        return level.getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4);
    }
}
