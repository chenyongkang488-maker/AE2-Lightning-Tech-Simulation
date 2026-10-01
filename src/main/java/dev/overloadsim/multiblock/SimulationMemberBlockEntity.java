package dev.overloadsim.multiblock;
import java.util.UUID;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class SimulationMemberBlockEntity extends BlockEntity {
    private BlockPos owner;private UUID ownerId;private BlockState original,removalOriginal;
    public SimulationMemberBlockEntity(BlockPos pos,BlockState state){super(MultiblockContent.MEMBER_ENTITY.get(),pos,state);}
    public BlockPos owner(){return owner;}
    public boolean ownedBy(SimulationControllerBlockEntity controller){return owner!=null&&owner.equals(controller.getBlockPos())&&controller.identity().equals(ownerId);}
    public void bind(SimulationControllerBlockEntity controller,BlockState original){this.owner=controller.getBlockPos();this.ownerId=controller.identity();this.original=original;removalOriginal=null;setChanged();}
    public BlockState originalForDrops(){return original!=null?original:removalOriginal;}
    public void release(){
        owner=null;ownerId=null;original=null;setChanged();
    }
    public void notifyController(){if(level!=null&&!level.isClientSide&&owner!=null&&level.hasChunkAt(owner)&&level.getBlockEntity(owner) instanceof SimulationControllerBlockEntity c)c.invalidateStructure();}
    public void restore(){
        if(level==null)return;
        BlockState before=original;removalOriginal=before;release();
        var current=level.getBlockState(worldPosition);
        if(level.getBlockEntity(worldPosition)!=this||!current.is(getBlockState().getBlock()))return;
        if(current.is(MultiblockContent.GLASS.get())&&before!=null)
            SimulationStructureIndex.converting(()->level.setBlockAndUpdate(worldPosition,before));
        else if(current.hasProperty(SimulationPartBlock.FORMED))
            level.setBlockAndUpdate(worldPosition,current.setValue(SimulationPartBlock.FORMED,false));
    }
    @Override public void onLoad(){super.onLoad();if(level!=null&&!level.isClientSide)level.scheduleTick(worldPosition,getBlockState().getBlock(),40);}
    public void recover(){
        if(owner==null||level==null)return;
        if(!level.hasChunkAt(owner)){level.scheduleTick(worldPosition,getBlockState().getBlock(),40);return;}
        if(level.getBlockEntity(owner) instanceof SimulationControllerBlockEntity c&&ownedBy(c)){
            c.checkStructure();if(c.structure()!=null&&c.structure().members().contains(worldPosition))return;
            if(c.error().equals("unloaded")){level.scheduleTick(worldPosition,getBlockState().getBlock(),40);return;}
        }restore();
    }
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries){
        super.saveAdditional(tag,registries);if(owner!=null){tag.putLong("Owner",owner.asLong());tag.putUUID("OwnerId",ownerId);}
        if(original!=null)tag.put("Original",NbtUtils.writeBlockState(original));
    }
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries){
        super.loadAdditional(tag,registries);owner=tag.hasUUID("OwnerId")?BlockPos.of(tag.getLong("Owner")):null;ownerId=owner==null?null:tag.getUUID("OwnerId");
        original=tag.contains("Original")?NbtUtils.readBlockState(registries.lookupOrThrow(net.minecraft.core.registries.Registries.BLOCK),tag.getCompound("Original")):null;
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries){return saveWithoutMetadata(registries);}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
}
