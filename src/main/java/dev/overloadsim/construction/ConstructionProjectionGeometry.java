package dev.overloadsim.construction;

import dev.overloadsim.laser.LaserStructureGeometry;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;

/** Read-only, bounded world cells derived from the actual construction template. */
public final class ConstructionProjectionGeometry {
    public static final int MAX_VISIBLE_CELLS = 512;

    private ConstructionProjectionGeometry() {}

    public static Map<BlockPos, LaserStructureGeometry.Part> laserCells(BlockPos controller,
            Map<BlockPos, LaserStructureGeometry.Part> relativeTemplate) {
        if (relativeTemplate.size() > MAX_VISIBLE_CELLS) throw new IllegalArgumentException("projection size");
        var cells = new LinkedHashMap<BlockPos, LaserStructureGeometry.Part>();
        relativeTemplate.forEach((offset, part) -> {
            if (part != LaserStructureGeometry.Part.AIR && part != LaserStructureGeometry.Part.CONTROLLER)
                cells.put(controller.offset(offset).immutable(), part);
        });
        return Collections.unmodifiableMap(cells);
    }
}
