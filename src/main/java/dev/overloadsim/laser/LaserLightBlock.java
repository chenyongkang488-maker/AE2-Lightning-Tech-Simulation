package dev.overloadsim.laser;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Invisible, replaceable air-only source for vanilla block-light propagation. */
public final class LaserLightBlock extends BaseEntityBlock {
    public LaserLightBlock() {
        super(Properties.of().noCollission().noOcclusion().replaceable().noLootTable().lightLevel(state -> 15));
    }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return simpleCodec(properties -> new LaserLightBlock()); }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.INVISIBLE; }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return Shapes.empty(); }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new LaserLightBlockEntity(pos, state); }
    @Override protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getBlockEntity(pos) instanceof LaserLightBlockEntity light) light.recover();
    }
}
