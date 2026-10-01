package dev.overloadsim.multiblock;
import java.util.*;
import appeng.blockentity.grid.AENetworkedBlockEntity;
import appeng.api.networking.*;
import appeng.api.networking.security.*;
import appeng.api.orientation.BlockOrientation;
import appeng.api.util.AECableType;
import dev.overloadsim.*;
import dev.overloadsim.api.CrystalDataAccess;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
public class SimulationControllerBlockEntity extends AENetworkedBlockEntity implements IActionHost {
    private UUID identity=UUID.randomUUID();
    private SimulationStructure structure;private String error="no_structure";
    private final BulkOutputBuffer outputs=new BulkOutputBuffer();
    private final ItemStackHandler crystals=new ItemStackHandler(49){
        @Override public int getSlotLimit(int slot){return 1;}
        @Override public boolean isItemValid(int slot,ItemStack s){return s.is(ModContent.PERFECT.get())&&CrystalDataAccess.read(s).isPresent();}
        @Override protected void onContentsChanged(int slot){saveChanges();}
    };
    public SimulationControllerBlockEntity(BlockPos p,BlockState s){super(MultiblockContent.CONTROLLER_ENTITY.get(),p,s);}
    public UUID identity(){return identity;}
    public SimulationStructure structure(){return structure;}
    public String error(){return error;}
    public ItemStackHandler crystals(){return crystals;}
    public BulkOutputBuffer outputs(){return outputs;}
    public void tick(){if(level==null||level.isClientSide)return;if(level.getGameTime()%20==0)checkStructure();}
    public boolean loaded(){return structure!=null&&structure.members().stream().allMatch(level::hasChunkAt);}
    public void checkStructure(){
        if(level==null||level.isClientSide||SimulationStructureIndex.converting())return;
        if(structure!=null&&!loaded()){error="unloaded";return;}
        var result=structure==null?SimulationStructureValidator.find(this):SimulationStructureValidator.validate(this,structure.min(),structure.size());
        if(!result.valid()){error=result.error();invalidateStructure();return;}
        error="";var next=result.structure();
        if(structure!=null){structure=next;return;}
        structure=next;
        SimulationStructureIndex.converting(()->{
            for(var p:next.members()){
                var old=level.getBlockState(p);
                if(next.glass().contains(p)&&!old.is(MultiblockContent.GLASS.get())){
                    int x=p.getX()-next.min().getX(),z=p.getZ()-next.min().getZ();
                    Direction face=p.getY()==next.min().getY()+next.size()-1?Direction.UP:x==0?Direction.WEST:x==next.size()-1?Direction.EAST:z==0?Direction.NORTH:Direction.SOUTH;
                    level.setBlockAndUpdate(p,MultiblockContent.GLASS.get().defaultBlockState().setValue(SimulationGlassBlock.FACE,face).setValue(SimulationPartBlock.FORMED,true));
                    ((SimulationMemberBlockEntity)level.getBlockEntity(p)).bind(this,old);
                }else if(level.getBlockEntity(p) instanceof SimulationMemberBlockEntity m){
                    if(!old.is(MultiblockContent.GLASS.get()))m.bind(this,null);if(old.hasProperty(SimulationPartBlock.FORMED))level.setBlockAndUpdate(p,old.setValue(SimulationPartBlock.FORMED,true));
                }
            }
        });saveChanges();markForClientUpdate();
    }
    public void invalidateStructure(){
        var old=structure;structure=null;if(old==null||level==null)return;
        SimulationStructureIndex.converting(()->{for(var p:old.members())if(level.hasChunkAt(p)&&level.getBlockEntity(p) instanceof SimulationMemberBlockEntity m&&m.ownedBy(this))m.restore();});
        saveChanges();markForClientUpdate();
    }
    @Override public void saveAdditional(CompoundTag tag,HolderLookup.Provider r){super.saveAdditional(tag,r);tag.putUUID("Identity",identity);tag.put("Crystals",crystals.serializeNBT(r));tag.put("Bulk",outputs.save(r));}
    @Override public void loadTag(CompoundTag tag,HolderLookup.Provider r){super.loadTag(tag,r);if(tag.hasUUID("Identity"))identity=tag.getUUID("Identity");crystals.deserializeNBT(r,tag.getCompound("Crystals"));outputs.load(tag.getCompound("Bulk"),r);}
    @Override public IGridNode getActionableNode(){return getMainNode().getNode();}
    @Override public Set<Direction> getGridConnectableSides(BlockOrientation o){return EnumSet.allOf(Direction.class);}
    @Override public AECableType getCableConnectionType(Direction d){return AECableType.SMART;}
    @Override protected Item getItemFromBlockEntity(){return MultiblockContent.CONTROLLER_ITEM.get();}
}
