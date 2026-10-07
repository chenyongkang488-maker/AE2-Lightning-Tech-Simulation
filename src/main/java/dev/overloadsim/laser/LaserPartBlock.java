package dev.overloadsim.laser;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Shared static construction materials; removal notifies their owning controller. */
public final class LaserPartBlock extends Block {
    private final boolean glass;
    public LaserPartBlock(boolean glass) {
        super(Properties.of().strength(4).sound(glass ? SoundType.GLASS : SoundType.METAL).noOcclusion());
        this.glass = glass;
    }
    @Override protected boolean skipRendering(BlockState state, BlockState adjacent, Direction side) {
        return glass && adjacent.is(this) || super.skipRendering(state, adjacent, side);
    }
    @Override protected int getLightBlock(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        return glass ? 0 : 15;
    }
    @Override protected boolean propagatesSkylightDown(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        return glass;
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return state.is(LaserContent.FRAME.get()) || state.is(LaserContent.PORT.get()) ? dev.overloadsim.multiblock.FrameMenuAccess.use(level,pos,player)
                : super.useWithoutItem(state,level,pos,player,hit);
    }
    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!state.is(next.getBlock())) {
            LaserStructureIndex.changed(level, pos, true);
            dev.overloadsim.workstation.WorkstationStructureIndex.removed(level,pos);
        }
        super.onRemove(state, level, pos, next, moving);
    }
}
