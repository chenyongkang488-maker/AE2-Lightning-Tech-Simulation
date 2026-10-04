package dev.overloadsim.laser;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;

/** Shared static construction materials; removal notifies their owning controller. */
public final class LaserPartBlock extends Block {
    public LaserPartBlock(boolean glass) {
        super(Properties.of().strength(4).sound(glass ? SoundType.GLASS : SoundType.METAL).noOcclusion());
    }
    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!state.is(next.getBlock())) LaserStructureIndex.changed(level, pos, true);
        super.onRemove(state, level, pos, next, moving);
    }
}
