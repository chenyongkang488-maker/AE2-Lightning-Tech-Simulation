package dev.overloadsim.output;

import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/** An exact source-slot to destination-face binding. */
public record OutputConnection(int sourceSlot, ResourceKey<Level> dimension, BlockPos pos, Direction face) {
    public OutputConnection {
        Objects.requireNonNull(dimension); pos=Objects.requireNonNull(pos).immutable(); Objects.requireNonNull(face);
    }
    public CompoundTag toTag() {
        var tag=new CompoundTag(); tag.putInt("Slot",sourceSlot); tag.putString("Dim",dimension.location().toString());
        tag.putLong("Pos",pos.asLong()); tag.putInt("Face",face.get3DDataValue()); return tag;
    }
    public static OutputConnection fromTag(CompoundTag tag) {
        int face=tag.getInt("Face");
        if (face<0||face>=Direction.values().length) throw new IllegalArgumentException("Invalid output face");
        return new OutputConnection(tag.getInt("Slot"), ResourceKey.create(Registries.DIMENSION,
                ResourceLocation.parse(tag.getString("Dim"))),BlockPos.of(tag.getLong("Pos")),Direction.from3DDataValue(face));
    }
}
