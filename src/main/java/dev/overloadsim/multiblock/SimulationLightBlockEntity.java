package dev.overloadsim.multiblock;

import java.util.UUID;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class SimulationLightBlockEntity extends BlockEntity {
    private BlockPos owner;private UUID ownerId;
    public SimulationLightBlockEntity(BlockPos pos,BlockState state){super(MultiblockContent.LIGHT_ENTITY.get(),pos,state);}
    public static BlockPos center(BlockPos min,int size){int coordinate=(size-1)/2;return min.offset(coordinate,coordinate,coordinate);}
    public boolean ownedBy(SimulationControllerBlockEntity controller){return owner!=null&&owner.equals(controller.getBlockPos())&&controller.identity().equals(ownerId);}
    public void bind(SimulationControllerBlockEntity controller){owner=controller.getBlockPos();ownerId=controller.identity();setChanged();}
    public void notifyOwner(){if(level!=null&&!level.isClientSide&&owner!=null&&level.hasChunkAt(owner)&&level.getBlockEntity(owner) instanceof SimulationControllerBlockEntity c&&ownedBy(c))c.markStructureDirty();}
    @Override public void onLoad(){super.onLoad();if(level!=null&&!level.isClientSide)level.scheduleTick(worldPosition,getBlockState().getBlock(),40);}
    public void recover(){
        if(level==null||level.isClientSide)return;
        if(owner!=null&&!level.hasChunkAt(owner)){level.scheduleTick(worldPosition,getBlockState().getBlock(),40);return;}
        if(owner!=null&&level.getBlockEntity(owner) instanceof SimulationControllerBlockEntity c&&ownedBy(c)){
            c.checkStructure();if(c.error().equals("unloaded")){level.scheduleTick(worldPosition,getBlockState().getBlock(),40);return;}
            if(c.structure()!=null&&center(c.structure().min(),c.structure().size()).equals(worldPosition)&&c.hasActiveCrystal())return;
        }
        if(level.getBlockEntity(worldPosition)==this&&level.getBlockState(worldPosition).is(MultiblockContent.LIGHT.get()))SimulationStructureIndex.converting(()->level.setBlockAndUpdate(worldPosition,Blocks.AIR.defaultBlockState()));
    }
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registry){super.saveAdditional(tag,registry);if(owner!=null){tag.putLong("Owner",owner.asLong());tag.putUUID("OwnerId",ownerId);}}
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registry){super.loadAdditional(tag,registry);owner=tag.hasUUID("OwnerId")?BlockPos.of(tag.getLong("Owner")):null;ownerId=owner==null?null:tag.getUUID("OwnerId");}
}
