package dev.overloadsim.laser;

import appeng.api.orientation.IOrientationStrategy;
import appeng.api.orientation.OrientationStrategies;
import appeng.block.AEBaseEntityBlock;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

public final class LaserMachineBlock extends AEBaseEntityBlock<LaserMachineBlockEntity> {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    private final LaserKind kind;
    public LaserMachineBlock(LaserKind kind) {
        super(Properties.of().strength(4).sound(SoundType.METAL).noOcclusion());
        this.kind = kind;
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
    }
    public LaserKind kind() { return kind; }
    @Override public IOrientationStrategy getOrientationStrategy() { return OrientationStrategies.facing(); }
    @Override protected MapCodec<? extends LaserMachineBlock> codec() { return simpleCodec(properties -> new LaserMachineBlock(kind)); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = kind == LaserKind.DEFLECTOR ? context.getHorizontalDirection().getOpposite() : context.getNearestLookingDirection().getOpposite();
        return super.getStateForPlacement(context).setValue(FACING, facing);
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new LaserMachineBlockEntity(pos, state); }
    @Override public BlockEntityType<LaserMachineBlockEntity> getBlockEntityType() { return LaserContent.MACHINE_ENTITY.get(); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return !level.isClientSide && type == getBlockEntityType() ? (world, pos, blockState, entity) -> ((LaserMachineBlockEntity) entity).tick() : null;
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof LaserMachineBlockEntity host) {
            host.checkStructure();
            appeng.menu.MenuOpener.open(LaserContent.MENU.get(), player, appeng.menu.locator.MenuLocators.forBlockEntity(host));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!state.is(next.getBlock()) && level.getBlockEntity(pos) instanceof LaserMachineBlockEntity host) host.memberRemoved();
        super.onRemove(state, level, pos, next, moving);
    }
}
