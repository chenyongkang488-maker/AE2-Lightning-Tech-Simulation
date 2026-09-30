package dev.overloadsim.machine;
import com.mojang.serialization.MapCodec;
import appeng.block.AEBaseEntityBlock;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import dev.overloadsim.ModContent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
public class SimulationChamberBlock extends AEBaseEntityBlock<SimulationChamberBlockEntity> {
    public static final MapCodec<SimulationChamberBlock> CODEC=simpleCodec(p->new SimulationChamberBlock());
    public SimulationChamberBlock(){super(Properties.of().mapColor(MapColor.COLOR_PINK).strength(4).sound(SoundType.METAL));}
    @Override protected MapCodec<? extends SimulationChamberBlock> codec(){return CODEC;}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new SimulationChamberBlockEntity(pos,state);}
    @Override public BlockEntityType<SimulationChamberBlockEntity> getBlockEntityType(){return ModContent.CHAMBER_ENTITY.get();}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return !level.isClientSide()&&type==ModContent.CHAMBER_ENTITY.get()?(l,p,s,be)->((SimulationChamberBlockEntity)be).tick():null;}
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){if(!level.isClientSide())MenuOpener.open(ModContent.MENU.get(),player,MenuLocators.forBlockEntity(level.getBlockEntity(pos)));return InteractionResult.sidedSuccess(level.isClientSide());}
}
