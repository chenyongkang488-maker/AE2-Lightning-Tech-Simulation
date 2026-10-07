package dev.overloadsim.workstation;

import appeng.api.config.Actionable;
import appeng.api.networking.*;
import appeng.api.networking.security.*;
import appeng.api.orientation.BlockOrientation;
import appeng.api.stacks.*;
import appeng.api.util.AECableType;
import appeng.blockentity.grid.AENetworkedBlockEntity;
import appeng.util.SettingsFrom;
import com.moakiee.ae2lt.api.frequency.*;
import dev.overloadsim.compat.FrequencyBindingLifecycle;
import dev.overloadsim.construction.AutomaticConstruction;
import dev.overloadsim.core.WorkstationLaserLease;
import dev.overloadsim.laser.LaserLightManager;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.component.*;
import net.minecraft.nbt.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.*;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.*;

/** Server authority: no FE/lightning debit, only two valid laser ticks and an atomic material transaction. */
public final class HighEnergyWorkstationBlockEntity extends AENetworkedBlockEntity implements IActionHost,FrequencyBindingHost {
    public static final long LASER_THRESHOLD=1_000_000_000L;
    public static final int TANK_CAPACITY=1_024_000_000,MAX_PARALLEL=1024,RECEIPT_LEASE=22;
    private UUID identity=UUID.randomUUID();
    private final FrequencyBindingAccess frequency=FrequencyApi.createBinding(this);
    private final WorkstationBuffer inputs=new WorkstationBuffer(this::changed),outputs=new WorkstationBuffer(this::changed);
    private final FluidTank inputTank=new FluidTank(TANK_CAPACITY){@Override protected void onContentsChanged(){changed();}};
    private final FluidTank outputTank=new FluidTank(TANK_CAPACITY){@Override protected void onContentsChanged(){changed();}};
    private final int[] modes=new int[6];
    private final WorkstationLaserLease laserLease=new WorkstationLaserLease();
    private boolean claimed,visualFormed,dirty=true,loading,frequencyUnloading,eject,networkExport;
    private boolean exporting,deferredRemoval,replacementDropEmitted;
    private long power,revision,selectionRevision=-1,dropPreparedTick=Long.MIN_VALUE;
    private List<WorkstationRecipes.Entry> selectionCatalog;
    private WorkstationRecipes.Batch cachedSelection,pending;
    private int progress,lastParallel;
    private String lastRecipe="";
    public HighEnergyWorkstationBlockEntity(BlockPos position,BlockState state){super(WorkstationContent.CONTROLLER_ENTITY.get(),position,state);getMainNode().setIdlePowerUsage(0).setFlags(GridFlags.REQUIRE_CHANNEL);}
    public UUID identity(){return identity;}
    public Direction facing(){return getBlockState().getValue(HighEnergyWorkstationBlock.FACING);}
    public BlockPos center(){return worldPosition.relative(facing().getOpposite(),3).above(3);}
    public BlockPos receiver(){return worldPosition.relative(facing().getOpposite(),6).above(3);}
    public Map<BlockPos,BlockState> template(){return WorkstationStructure.template(worldPosition,facing());}
    public WorkstationBuffer inputs(){return inputs;}
    public WorkstationBuffer outputs(){return outputs;}
    public FluidStack fluidInput(){return inputTank.getFluid().copy();}
    public FluidStack fluidOutput(){return outputTank.getFluid().copy();}
    public boolean structureClaimed(){return claimed;}
    public boolean formed(){return level!=null&&!isRemoved()&&(level.isClientSide?visualFormed:claimed&&loaded()&&level.getBlockEntity(worldPosition)==this);}
    public long currentPower(){return level!=null&&formed()?(level.isClientSide?power:laserLease.power(level.getGameTime())):0;}
    public int progress(){return progress;}
    public boolean working(){return formed()&&currentPower()>LASER_THRESHOLD&&pending!=null;}
    public int lastParallel(){return lastParallel;}
    public String lastRecipe(){return lastRecipe;}
    public int sideMode(Direction side){return side==null?0:modes[side.get3DDataValue()];}
    public void setSideMode(Direction side,int mode){if(side!=null&&mode>=0&&mode<=3){modes[side.get3DDataValue()]=mode;sync();}}
    public boolean eject(){return eject;}
    public void setEject(boolean value){eject=value;sync();}
    public boolean networkExport(){return networkExport;}
    public void setNetworkExport(boolean value){networkExport=value;sync();}
    private void changed(){if(!loading){revision++;selectionRevision=-1;if(!isRemoved())sync();}}
    public void sync(){saveChanges();markForClientUpdate();}
    public void markStructureDirty(){dirty=true;}
    public boolean loaded(){
        if(level==null)return false;var positions=List.of(WorkstationStructure.position(worldPosition,facing(),0,0,0),WorkstationStructure.position(worldPosition,facing(),6,0,0),WorkstationStructure.position(worldPosition,facing(),0,0,6),WorkstationStructure.position(worldPosition,facing(),6,0,6));
        for(var position:positions)if(!level.hasChunkAt(position))return false;return true;
    }
    private boolean occupied(BlockPos position){return WorkstationStructureIndex.occupiedByOther(level,position,this)||dev.overloadsim.laser.LaserStructureIndex.occupiedByOther(level,position,this)||dev.overloadsim.multiblock.SimulationStructureIndex.occupiedByOther(level,position,this);}
    public void checkStructure(){
        if(level==null||level.isClientSide||isRemoved())return;dirty=false;
        for(var entry:template().entrySet()){
            var pos=entry.getKey();var expected=entry.getValue();
            if(!level.hasChunkAt(pos)||occupied(pos)){invalidateStructure();return;}
            var current=level.getBlockState(pos);
            if(expected.isAir()?!LaserLightManager.isBeamAir(current):!current.is(expected.getBlock())||pos.equals(worldPosition)&&current.getValue(HighEnergyWorkstationBlock.FACING)!=facing()){invalidateStructure();return;}
        }
        if(!claimed){claimed=true;if(!WorkstationStructureIndex.bind(this,template().keySet())){claimed=false;return;}setFormedStates(true);sync();}
    }
    public void invalidateStructure(){
        boolean previous=claimed;claimed=false;WorkstationStructureIndex.release(this);pending=null;cachedSelection=null;selectionRevision=-1;progress=0;power=0;laserLease.clear();
        if(previous){setFormedStates(false);sync();}
    }
    private void setFormedStates(boolean value){
        if(level==null||level.isClientSide)return;for(var pos:template().keySet())if(level.hasChunkAt(pos)){var state=level.getBlockState(pos);if((state.is(WorkstationContent.CONTROLLER.get())||state.is(WorkstationContent.FRAME.get())||state.is(WorkstationContent.GLASS.get())||state.is(WorkstationContent.RECEIVER.get()))&&state.hasProperty(WorkstationPartBlock.FORMED)&&state.getValue(WorkstationPartBlock.FORMED)!=value)level.setBlock(pos,state.setValue(WorkstationPartBlock.FORMED,value),3);}
    }
    /** incoming is beam TRAVEL direction. Exterior back-wall entry travels toward controller facing. */
    public boolean receiveLaser(Direction incoming,long strength,long receiptTick){
        checkStructure();if(!formed()||incoming!=facing()||strength<0)return false;
        if(!laserLease.accept(strength,receiptTick,level.getGameTime()))return false;power=laserLease.power(level.getGameTime());markForClientUpdate();return true;
    }
    private record LaserSource(BlockPos position,UUID identity) {}
    public boolean receiveLaserFromSource(Direction incoming,long strength,long receiptTick,BlockPos source,UUID sourceIdentity){
        checkStructure();if(!formed()||incoming!=facing()||source==null||sourceIdentity==null)return false;
        if(!laserLease.acceptSource(new LaserSource(source.immutable(),sourceIdentity),strength,receiptTick,level.getGameTime()))return false;power=laserLease.power(level.getGameTime());markForClientUpdate();return true;
    }
    public void releaseLaserSource(BlockPos source,UUID sourceIdentity){
        if(source==null||sourceIdentity==null)return;laserLease.releaseSource(new LaserSource(source,sourceIdentity));power=level==null?0:laserLease.power(level.getGameTime());
        if(power<=LASER_THRESHOLD){pending=null;progress=0;}markForClientUpdate();
    }
    public AutomaticConstruction.Result build(ServerPlayer player){
        if(level==null||level.isClientSide||isRemoved()||player.level()!=level||player.distanceToSqr(worldPosition.getCenter())>64)return failure("unavailable");
        checkStructure();if(formed())return new AutomaticConstruction.Result(true,0,"already_formed",List.of());
        var captured=identity;var state=getBlockState();var cells=template();
        var plan=new AutomaticConstruction.Plan(cells,()->!isRemoved()&&identity.equals(captured)&&level.hasChunkAt(worldPosition)&&level.getBlockEntity(worldPosition)==this&&level.getBlockState(worldPosition).equals(state)&&cells.keySet().stream().noneMatch(this::occupied));
        var result=AutomaticConstruction.build(player,this,plan,getMainNode().isActive()?getMainNode().getGrid():null);if(result.successful())checkStructure();return result;
    }
    private static AutomaticConstruction.Result failure(String reason){return new AutomaticConstruction.Result(false,0,reason,List.of());}
    public void tick(){
        if(level==null||level.isClientSide||isRemoved())return;frequency.serverTick();
        if(dirty||level.getGameTime()%20==0)checkStructure();
        long currentPower=currentPower();if(power!=currentPower){power=currentPower;markForClientUpdate();}
        if(!formed()||currentPower<=LASER_THRESHOLD){if(progress!=0||pending!=null){progress=0;pending=null;markForClientUpdate();}exportOutputs();return;}
        var catalog=WorkstationRecipes.recipes(level.getRecipeManager());
        if(selectionRevision!=revision||selectionCatalog!=catalog){cachedSelection=WorkstationRecipes.select(level.getRecipeManager(),inputs,outputs,fluidInput(),fluidOutput()).orElse(null);selectionRevision=revision;selectionCatalog=catalog;}
        var candidate=cachedSelection;
        if(candidate==null){pending=null;progress=0;exportOutputs();return;}
        if(pending==null||pending.entry().recipe()!=candidate.entry().recipe()||!pending.entry().id().equals(candidate.entry().id())||pending.parallel()!=candidate.parallel()){pending=candidate;progress=0;}
        progress++;markForClientUpdate();
        if(progress>=2){checkStructure();
            var current=WorkstationRecipes.find(level.getRecipeManager(),pending==null?candidate.entry().id():pending.entry().id()).orElse(null);
            var verified=current==null?null:WorkstationRecipes.evaluate(current,inputs,outputs,fluidInput(),fluidOutput()).orElse(null);
            if(formed()&&currentPower()>LASER_THRESHOLD&&pending!=null&&verified!=null&&verified.entry().recipe()==pending.entry().recipe()&&verified.parallel()==pending.parallel())commit(verified);
            pending=null;progress=0;selectionRevision=-1;markForClientUpdate();
        }
        exportOutputs();
    }
    private void commit(WorkstationRecipes.Batch batch){
        var nextInputs=inputs.copy();for(int slot=0;slot<9;slot++)nextInputs.debit(slot,batch.debit()[slot]);
        var fluid=fluidInput();if(batch.fluidDebit()>0)fluid.shrink(batch.fluidDebit());
        loading=true;try{inputs.replace(nextInputs);outputs.replace(batch.outputs());inputTank.setFluid(fluid);outputTank.setFluid(batch.fluidOutput().copy());}finally{loading=false;}
        lastParallel=batch.parallel();lastRecipe=batch.entry().id().toString();changed();
    }
    public IItemHandler automation(Direction side){return new IItemHandler(){
        private void check(int slot){if(slot<0||slot>=18)throw new IllegalArgumentException("workstation automation slot");}
        @Override public int getSlots(){return 18;}
        @Override public ItemStack getStackInSlot(int slot){check(slot);var buffer=slot<9?inputs:outputs;return buffer.extract(slot%9,64,true);}
        @Override public ItemStack insertItem(int slot,ItemStack stack,boolean simulate){check(slot);if(slot>=9||(sideMode(side)&1)==0||!formed())return stack;int moved=(int)inputs.insert(slot,stack,stack.getCount(),simulate);return moved==stack.getCount()?ItemStack.EMPTY:stack.copyWithCount(stack.getCount()-moved);}
        @Override public ItemStack extractItem(int slot,int amount,boolean simulate){check(slot);return slot<9||(sideMode(side)&2)==0||!formed()?ItemStack.EMPTY:outputs.extract(slot-9,amount,simulate);}
        @Override public int getSlotLimit(int slot){check(slot);return WorkstationBuffer.LIMIT;}
        @Override public boolean isItemValid(int slot,ItemStack stack){check(slot);return slot<9&&(sideMode(side)&1)!=0&&!stack.isEmpty();}
    };}
    public IFluidHandler fluidAutomation(Direction side){return new IFluidHandler(){
        @Override public int getTanks(){return 2;}
        @Override public FluidStack getFluidInTank(int tank){if(tank<0||tank>1)throw new IllegalArgumentException("workstation tank");return tank==0?fluidInput():fluidOutput();}
        @Override public int getTankCapacity(int tank){if(tank<0||tank>1)throw new IllegalArgumentException("workstation tank");return TANK_CAPACITY;}
        @Override public boolean isFluidValid(int tank,FluidStack fluid){return tank==0;}
        @Override public int fill(FluidStack fluid,FluidAction action){return !formed()||(sideMode(side)&1)==0?0:inputTank.fill(fluid,action);}
        @Override public FluidStack drain(FluidStack fluid,FluidAction action){return !formed()||(sideMode(side)&2)==0?FluidStack.EMPTY:outputTank.drain(fluid,action);}
        @Override public FluidStack drain(int amount,FluidAction action){return !formed()||(sideMode(side)&2)==0?FluidStack.EMPTY:outputTank.drain(amount,action);}
    };}
    private void exportOutputs(){
        if(exporting||!formed())return;var exportIdentity=identity;int budget=256,fluidBudget=16_000;
        if(networkExport&&getMainNode().isActive()&&getMainNode().getGrid()!=null){var storage=getMainNode().getGrid().getStorageService().getInventory();var source=IActionSource.ofMachine(this);
            if(!exportAlive(exportIdentity))return;
            for(int slot=0;slot<9&&budget>0;slot++){
                var stack=outputs.extract(slot,budget,false);if(stack.isEmpty())continue;
                long accepted;exporting=true;
                try{accepted=Math.clamp(storage.insert(AEItemKey.of(stack),stack.getCount(),Actionable.MODULATE,source),0L,stack.getCount());restoreOutput(exportIdentity,slot,stack,stack.getCount()-accepted);}
                finally{finishOutputTransfer();}
                if(!exportAlive(exportIdentity))return;budget-=(int)accepted;
            }
            var fluid=outputTank.drain(fluidBudget,IFluidHandler.FluidAction.EXECUTE);
            if(!fluid.isEmpty()){
                int accepted;exporting=true;
                try{accepted=(int)Math.clamp(storage.insert(AEFluidKey.of(fluid),fluid.getAmount(),Actionable.MODULATE,source),0L,fluid.getAmount());if(accepted<fluid.getAmount())restoreFluidOutput(fluid.copyWithAmount(fluid.getAmount()-accepted));}
                finally{finishOutputTransfer();}
                if(!exportAlive(exportIdentity))return;fluidBudget-=accepted;
            }
        }
        if(!eject)return;
        for(var side:Direction.values()){if((sideMode(side)&2)==0||!level.hasChunkAt(worldPosition.relative(side)))continue;
            var target=level.getCapability(Capabilities.ItemHandler.BLOCK,worldPosition.relative(side),side.getOpposite());
            if(!exportAlive(exportIdentity))return;
            if(target!=null)for(int slot=0;slot<9&&budget>0;slot++){
                var stack=outputs.extract(slot,budget,false);if(stack.isEmpty())continue;
                int accepted;exporting=true;
                try{var left=ItemHandlerHelper.insertItemStacked(target,stack,false);accepted=stack.getCount()-left.getCount();restoreOutput(exportIdentity,slot,stack,left.getCount());}
                finally{finishOutputTransfer();}
                if(!exportAlive(exportIdentity))return;budget-=accepted;
            }
            if(fluidBudget>0){
                var tank=level.getCapability(Capabilities.FluidHandler.BLOCK,worldPosition.relative(side),side.getOpposite());if(!exportAlive(exportIdentity))return;
                if(tank!=null){var fluid=outputTank.drain(fluidBudget,IFluidHandler.FluidAction.EXECUTE);if(!fluid.isEmpty()){
                    int accepted;exporting=true;
                    try{accepted=Math.clamp(tank.fill(fluid,IFluidHandler.FluidAction.EXECUTE),0,fluid.getAmount());if(accepted<fluid.getAmount())restoreFluidOutput(fluid.copyWithAmount(fluid.getAmount()-accepted));}
                    finally{finishOutputTransfer();}
                    if(!exportAlive(exportIdentity))return;fluidBudget-=accepted;
                }}
            }
        }
    }
    private boolean exportAlive(UUID expected){return level!=null&&!isRemoved()&&identity.equals(expected)&&level.hasChunkAt(worldPosition)&&level.getBlockEntity(worldPosition)==this&&formed();}
    private void restoreOutput(UUID expected,int slot,ItemStack prototype,long amount){if(amount<=0)return;if(!identity.equals(expected)||!deferredRemoval&&(isRemoved()||level.getBlockEntity(worldPosition)!=this)){Block.popResource(level,worldPosition,prototype.copyWithCount((int)amount));return;}long restored=outputs.insert(slot,prototype,amount,false);if(restored<amount)restored+=outputs.insert(prototype,amount-restored,false);if(restored<amount)Block.popResource(level,worldPosition,prototype.copyWithCount((int)(amount-restored)));}
    private void restoreFluidOutput(FluidStack fluid){
        // The outbound drain reserves this exact room. A removed controller still owns
        // its old tank until finishOutputTransfer creates the sole inventory snapshot.
        if(outputTank.fill(fluid,IFluidHandler.FluidAction.EXECUTE)!=fluid.getAmount())throw new IllegalStateException("workstation output fluid reservation was changed during transfer");
    }
    private void finishOutputTransfer(){
        exporting=false;
        if(deferredRemoval){deferredRemoval=false;if(level!=null&&!frequencyUnloading&&(isRemoved()||level.getBlockEntity(worldPosition)!=this))emitReplacementDrop();}
    }
    public CompoundTag saveMachine(HolderLookup.Provider registries){
        var tag=new CompoundTag();tag.putUUID("Identity",identity);tag.put("Inputs",inputs.save(registries));tag.put("Outputs",outputs.save(registries));
        var input=new CompoundTag();var output=new CompoundTag();inputTank.writeToNBT(registries,input);outputTank.writeToNBT(registries,output);tag.put("InputFluid",input);tag.put("OutputFluid",output);
        tag.putIntArray("SideModes",modes);tag.putBoolean("Eject",eject);tag.putBoolean("NetworkExport",networkExport);tag.putInt("LastParallel",lastParallel);tag.putString("LastRecipe",lastRecipe);return tag;
    }
    public void loadMachine(CompoundTag tag,HolderLookup.Provider registries){loading=true;try{if(tag.hasUUID("Identity"))identity=tag.getUUID("Identity");inputs.load(tag.getCompound("Inputs"),registries);outputs.load(tag.getCompound("Outputs"),registries);inputTank.readFromNBT(registries,tag.getCompound("InputFluid"));outputTank.readFromNBT(registries,tag.getCompound("OutputFluid"));int[] sides=tag.getIntArray("SideModes");for(int i=0;i<6;i++)modes[i]=i<sides.length?Math.clamp(sides[i],0,3):0;eject=tag.getBoolean("Eject");networkExport=tag.getBoolean("NetworkExport");lastParallel=Math.clamp(tag.getInt("LastParallel"),0,1024);lastRecipe=tag.getString("LastRecipe");}finally{loading=false;}dirty=true;pending=null;progress=0;power=0;laserLease.clear();selectionRevision=-1;revision++;}
    @Override public void saveAdditional(CompoundTag tag,HolderLookup.Provider registries){super.saveAdditional(tag,registries);frequency.save(tag);tag.put("Workstation",saveMachine(registries));}
    @Override public void loadTag(CompoundTag tag,HolderLookup.Provider registries){super.loadTag(tag,registries);frequency.load(tag);loadMachine(tag.getCompound("Workstation"),registries);}
    @Override protected void writeToStream(RegistryFriendlyByteBuf data){super.writeToStream(data);data.writeBoolean(formed());data.writeVarLong(currentPower());data.writeVarInt(progress);data.writeVarInt(lastParallel);data.writeUtf(lastRecipe);for(int slot=0;slot<9;slot++){ItemStack.OPTIONAL_STREAM_CODEC.encode(data,inputs.prototype(slot));data.writeVarInt(inputs.count(slot));}}
    @Override protected boolean readFromStream(RegistryFriendlyByteBuf data){super.readFromStream(data);visualFormed=data.readBoolean();power=Math.max(0,data.readVarLong());progress=Math.clamp(data.readVarInt(),0,2);lastParallel=Math.clamp(data.readVarInt(),0,1024);lastRecipe=data.readUtf();var next=new WorkstationBuffer();for(int slot=0;slot<9;slot++){var stack=ItemStack.OPTIONAL_STREAM_CODEC.decode(data);int count=Math.clamp(data.readVarInt(),0,WorkstationBuffer.LIMIT);next.insert(slot,stack,count,false);}loading=true;try{inputs.replace(next);}finally{loading=false;}return true;}
    @Override protected void saveVisualState(CompoundTag tag){super.saveVisualState(tag);tag.putBoolean("Formed",formed());tag.putLong("Power",currentPower());tag.putInt("Progress",progress);if(level!=null)tag.put("Inputs",inputs.save(level.registryAccess()));}
    @Override protected void loadVisualState(CompoundTag tag){super.loadVisualState(tag);visualFormed=tag.getBoolean("Formed");power=Math.max(0,tag.getLong("Power"));progress=Math.clamp(tag.getInt("Progress"),0,2);if(level!=null)inputs.load(tag.getCompound("Inputs"),level.registryAccess());}
    public boolean suppressControllerDrop(){if(replacementDropEmitted)return true;if(exporting){deferredRemoval=true;return true;}return false;}
    public void markDropPrepared(){if(exporting){deferredRemoval=true;return;}dropPreparedTick=level==null?Long.MIN_VALUE:level.getGameTime();}
    public void preserveOnReplacement(){if(level==null||level.isClientSide)return;if(exporting){deferredRemoval=true;return;}if(replacementDropEmitted||dropPreparedTick==level.getGameTime()||inputs.empty()&&outputs.empty()&&inputTank.isEmpty()&&outputTank.isEmpty())return;emitReplacementDrop();}
    private void emitReplacementDrop(){if(level==null||level.isClientSide||replacementDropEmitted)return;replacementDropEmitted=true;var stack=new ItemStack(WorkstationContent.CONTROLLER_ITEM.get());var tag=new CompoundTag();tag.put("Workstation",saveMachine(level.registryAccess()));stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));Block.popResource(level,worldPosition,stack);}
    @Override public void onLoad(){frequencyUnloading=false;super.onLoad();if(level!=null&&!level.isClientSide)checkStructure();}
    @Override public AENetworkedBlockEntity getFrequencyBindingBlockEntity(){return this;}
    @Override public FrequencyBindingAccess getFrequencyBindingAccess(){return frequency;}
    @Override public void saveFrequencyBindingChanges(){saveChanges();}
    @Override public void markFrequencyBindingForUpdate(){markForClientUpdate();}
    @Override public void onMainNodeStateChanged(IGridNodeListener.State reason){frequency.onMainNodeStateChanged(reason);}
    @Override public void onReady(){super.onReady();frequency.onReady();}
    @Override public void setRemoved(){if(exporting&&!frequencyUnloading)deferredRemoval=true;if(!frequencyUnloading)frequency.setRemoved();WorkstationStructureIndex.release(this);claimed=false;power=0;laserLease.clear();pending=null;super.setRemoved();}
    @Override public void onChunkUnloaded(){frequencyUnloading=true;FrequencyBindingLifecycle.chunkUnloaded(frequency);WorkstationStructureIndex.release(this);claimed=false;power=0;laserLease.clear();pending=null;super.onChunkUnloaded();}
    @Override public void clearRemoved(){frequencyUnloading=false;replacementDropEmitted=false;deferredRemoval=false;super.clearRemoved();frequency.clearRemoved();dirty=true;}
    @Override public void exportSettings(SettingsFrom mode,DataComponentMap.Builder builder,Player player){super.exportSettings(mode,builder,player);frequency.exportMemorySettings(mode,builder,tag->{});}
    @Override public void importSettings(SettingsFrom mode,DataComponentMap settings,Player player){super.importSettings(mode,settings,player);frequency.importMemorySettings(mode,settings,tag->{});}
    @Override public IGridNode getActionableNode(){return getMainNode().getNode();}
    @Override public Set<Direction> getGridConnectableSides(BlockOrientation orientation){return EnumSet.allOf(Direction.class);}
    @Override public AECableType getCableConnectionType(Direction direction){return AECableType.SMART;}
    @Override protected Item getItemFromBlockEntity(){return WorkstationContent.CONTROLLER_ITEM.get();}
}
