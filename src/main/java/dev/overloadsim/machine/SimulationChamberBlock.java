package dev.overloadsim.machine;
import com.mojang.serialization.MapCodec;
import appeng.block.AEBaseEntityBlock;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import dev.overloadsim.ModContent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.item.ItemStack;
import appeng.core.definitions.AEItems;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
public class SimulationChamberBlock extends AEBaseEntityBlock<SimulationChamberBlockEntity> {
    public static final MapCodec<SimulationChamberBlock> CODEC=simpleCodec(p->new SimulationChamberBlock());
    private static final VoxelShape FRAME=createFrame();
    private static VoxelShape createFrame(){
        VoxelShape shape=Shapes.or(box(0,0,0,16,2,16),box(1,2,1,15,3,15),box(0,14,0,2,16,16),box(14,14,0,16,16,16),box(2,14,0,14,16,2),box(2,14,14,14,16,16),box(5.5,3,5.5,10.5,3.5,10.5),box(6,12.5,0,10,16,.1));
        for(double x:new double[]{0,14.5})for(double z:new double[]{0,14.5})shape=Shapes.or(shape,box(x,2,z,x+1.5,14,z+1.5));
        for(double x:new double[]{2,11.5})for(double z:new double[]{2,11.5})shape=Shapes.or(shape,box(x,4,z,x+2.5,6,z+2.5),box(x+.75,3,z+.75,x+1.75,4,z+1.75));
        shape=Shapes.or(shape,box(1.5,3,.25,14.5,14,.5),box(1.5,3,15.5,14.5,14,15.75),box(.25,3,1.5,.5,14,14.5),box(15.5,3,1.5,15.75,14,14.5),box(2,15.5,2,14,15.75,14),box(2,3.05,2,14,3.3,14));
        return shape.optimize();
    }
    public SimulationChamberBlock(){super(Properties.of().mapColor(MapColor.COLOR_PINK).strength(4).sound(SoundType.METAL).noOcclusion());}
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return FRAME;}
    @Override protected MapCodec<? extends SimulationChamberBlock> codec(){return CODEC;}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new SimulationChamberBlockEntity(pos,state);}
    @Override public BlockEntityType<SimulationChamberBlockEntity> getBlockEntityType(){return ModContent.CHAMBER_ENTITY.get();}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return !level.isClientSide()&&type==ModContent.CHAMBER_ENTITY.get()?(l,p,s,be)->((SimulationChamberBlockEntity)be).tick():null;}
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        if(!stack.is(AEItems.SPEED_CARD.asItem()))return super.useItemOn(stack,state,level,pos,player,hand,hit);
        var machine=getBlockEntity(level,pos);if(machine==null)return ItemInteractionResult.FAIL;
        if(!level.isClientSide()){
            var remainder=machine.getUpgrades().addItems(stack.copy());
            if(remainder.getCount()==stack.getCount())return ItemInteractionResult.FAIL;
            if(!player.getAbilities().instabuild)player.setItemInHand(hand,remainder);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){if(!level.isClientSide())MenuOpener.open(ModContent.MENU.get(),player,MenuLocators.forBlockEntity(level.getBlockEntity(pos)));return InteractionResult.sidedSuccess(level.isClientSide());}
}
