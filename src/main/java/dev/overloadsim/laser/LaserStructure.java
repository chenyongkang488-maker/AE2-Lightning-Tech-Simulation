package dev.overloadsim.laser;

import java.util.List;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

/** Immutable loaded structure. Members include reserved clear cells so optical apertures cannot overlap. */
public record LaserStructure(BlockPos center, List<BlockPos> members, @Nullable BlockPos networkInterface) {
    public LaserStructure {
        center = center.immutable();
        members = members.stream().map(BlockPos::immutable).toList();
        if (networkInterface != null) networkInterface = networkInterface.immutable();
    }
}
