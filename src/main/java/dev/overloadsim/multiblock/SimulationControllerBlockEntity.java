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
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.common.NeoForge;
import net.minecraft.network.RegistryFriendlyByteBuf;
import dev.overloadsim.api.MultiblockSimulationEvents;
import dev.overloadsim.compat.*;
public class SimulationControllerBlockEntity extends AENetworkedBlockEntity implements IActionHost {
    private UUID identity=UUID.randomUUID();
    private SimulationStructure structure;private String error="no_structure";private BlockPos problem;private boolean structureDirty;
    private BlockPos savedMin;private int savedSize;
    private final BulkOutputBuffer outputs=new BulkOutputBuffer();
    private final SimulationGridBridge bridge=new SimulationGridBridge(this);
    private SimulationBatch batch;private int nextCrystal,status,lastVisualFlags;
    private long refundHv,refundEhv,feCredit,inputRevision;private double feFraction;
    private boolean loadingInventory;
    private final Map<Integer,ItemStack> crystalSnapshots=new HashMap<>();
    private record Roll(ItemStack input,List<ItemStack> output){}
    private final Map<Integer,Roll> rolls=new HashMap<>();
    private String rollGeneration=MultiblockData.generation();
    private final EnergyStorage energy=new EnergyStorage(2_000_000,2_000_000,0){
        @Override public int receiveEnergy(int amount,boolean simulate){int received=super.receiveEnergy(amount,simulate);if(received>0&&!simulate)saveChanges();return received;}
    };
    private int visualSize,visualFlags;private BlockPos visualMin;private List<ItemStack> visualCrystals=List.of();
    private final Object visualToken=new Object();
    private final ItemStackHandler crystals=new ItemStackHandler(49){
        @Override public int getSlotLimit(int slot){return 1;}
        @Override public boolean isItemValid(int slot,ItemStack s){return s.is(ModContent.PERFECT.get())&&CrystalDataAccess.read(s).isPresent();}
        @Override public ItemStack insertItem(int slot,ItemStack s,boolean sim){return structure==null||slot>=structure.capacity()?s:super.insertItem(slot,s,sim);}
        @Override protected void onContentsChanged(int slot){
            var current=getStackInSlot(slot);var previous=crystalSnapshots.put(slot,current.copy());
            if(loadingInventory||ItemStack.matches(previous==null?ItemStack.EMPTY:previous,current))return;
            if(level==null||!level.isClientSide){inputRevision++;rolls.remove(slot);abortBatch("crystal_changed");}
            saveChanges();markForClientUpdate();
        }
    };
    private final IItemHandler automation=new IItemHandler(){
        public int getSlots(){return 49+BulkOutputBuffer.SLOTS;}
        public ItemStack getStackInSlot(int slot){if(slot<49)return crystals.getStackInSlot(slot);return outputs.extract(slot-49,Integer.MAX_VALUE,true);}
        public ItemStack insertItem(int slot,ItemStack item,boolean sim){return slot<49?crystals.insertItem(slot,item,sim):item;}
        public ItemStack extractItem(int slot,int amount,boolean sim){var result=slot<49?ItemStack.EMPTY:outputs.extract(slot-49,amount,sim);if(!sim&&!result.isEmpty())saveChanges();return result;}
        public int getSlotLimit(int slot){return slot<49?1:1024;}
        public boolean isItemValid(int slot,ItemStack item){return slot<49&&crystals.isItemValid(slot,item);}
    };
    public SimulationControllerBlockEntity(BlockPos p,BlockState s){super(MultiblockContent.CONTROLLER_ENTITY.get(),p,s);getMainNode().setFlags(GridFlags.REQUIRE_CHANNEL).setIdlePowerUsage(2).setVisualRepresentation(MultiblockContent.CONTROLLER_ITEM.get());}
    public UUID identity(){return identity;}
    public SimulationStructure structure(){return structure;}
    public String error(){return error;}
    public String diagnostic(){return problem==null?error:error+" @ "+problem.getX()+", "+problem.getY()+", "+problem.getZ();}
    public void markStructureDirty(){structureDirty=true;}
    public ItemStackHandler crystals(){return crystals;}
    public BulkOutputBuffer outputs(){return outputs;}
    public EnergyStorage energy(){return energy;}
    public IItemHandler automation(){return automation;}
    public SimulationGridBridge bridge(){return bridge;}
    public SimulationBatch batch(){return batch;}
    public boolean busy(){return batch!=null&&batch.started;}
    public long inputRevision(){return inputRevision;}
    public long availableFe(){return Math.addExact(feCredit,energy.getEnergyStored());}
    public void abortBatch(String reason){
        var cancelled=batch;if(cancelled==null)return;batch=null;status=0;
        if(cancelled.paid){refundHv=Math.addExact(refundHv,cancelled.cost.hv());refundEhv=Math.addExact(refundEhv,cancelled.cost.ehv());feCredit=Math.addExact(feCredit,cancelled.cost.fe());}
        saveChanges();markForClientUpdate();if(level!=null&&!level.isClientSide)NeoForge.EVENT_BUS.post(new MultiblockSimulationEvents.BatchAborted(this,cancelled,reason));
    }
    public int status(){return status;}
    public int visualSize(){return level!=null&&level.isClientSide?visualSize:structure==null?0:structure.size();}
    public BlockPos visualMin(){return level!=null&&level.isClientSide?visualMin:structure==null?worldPosition:structure.min();}
    public int visualFlags(){return level!=null&&level.isClientSide?visualFlags:busy()&&batch.remaining>0&&structure!=null&&loaded()&&getMainNode().isActive()?1|(batch.overload?2:0)|(batch.smelting?4:0):0;}
    public List<ItemStack> visualCrystals(){return level!=null&&level.isClientSide?visualCrystals:java.util.stream.IntStream.range(0,structure==null?0:structure.capacity()).mapToObj(crystals::getStackInSlot).filter(s->!s.isEmpty()).map(ItemStack::copy).toList();}
    public List<ItemStack> fixedRoll(int slot,ItemStack item,java.util.function.Supplier<List<ItemStack>> supplier){
        if(!rollGeneration.equals(MultiblockData.generation())){rolls.clear();rollGeneration=MultiblockData.generation();saveChanges();}
        var cached=rolls.get(slot);if(cached==null||!ItemStack.matches(cached.input,item)){
            cached=new Roll(item.copy(),supplier.get().stream().map(ItemStack::copy).toList());rolls.put(slot,cached);saveChanges();
        }return cached.output;
    }
    public void tick(){
        if(level==null||level.isClientSide)return;
        try{tickServer();}finally{int flags=visualFlags();if(flags!=lastVisualFlags){lastVisualFlags=flags;markForClientUpdate();}}
    }
    private void tickServer(){
        if(structureDirty||level.getGameTime()%(structure==null?20:100)==0){structureDirty=false;checkStructure();}
        bridge.refresh();charge();
        if(refundHv>0||refundEhv>0){refundHv-=bridge.insert(false,refundHv);refundEhv-=bridge.insert(true,refundEhv);saveChanges();if(refundHv>0||refundEhv>0){status=7;return;}}
        if(structure==null){status=6;return;}if(!loaded()){status=8;return;}
        if(level.getGameTime()%5==0)bridge.export();
        if(!getMainNode().isActive()){status=2;return;}
        if(batch==null){try{batch=SimulationBatchPlanner.plan(this,nextCrystal);}catch(RuntimeException e){status=5;org.slf4j.LoggerFactory.getLogger(getClass()).error("Invalid multiblock simulation output",e);return;}saveChanges();}
        if(batch==null){status=3;return;}
        var candidate=batch;
        if(!inputsMatch(candidate)){abortBatch("input_invalid");return;}
        if(!candidate.started){
            if(!candidate.paid&&!candidate.policy.equals(MultiblockData.policy().signature(structure))){batch=null;saveChanges();return;}
            if(!batch.fits(outputs)){status=4;return;}
            if(NeoForge.EVENT_BUS.post(new MultiblockSimulationEvents.BeforeBatchStart(this,candidate)).isCanceled()||batch!=candidate)return;
            checkStructure();
            if(batch!=candidate||structure==null||!loaded()||!inputsMatch(candidate)||!getMainNode().isActive()||!candidate.fits(outputs))return;
            candidate.started=true;saveChanges();markForClientUpdate();
        }
        status=1;
        if(candidate.remaining>0)candidate.remaining--;
        if(candidate.remaining==0){
            if(!candidate.fits(outputs)){status=4;saveChanges();return;}
            long revision=inputRevision;
            if(NeoForge.EVENT_BUS.post(new MultiblockSimulationEvents.BeforeBatchCommit(this,candidate)).isCanceled()||!canCommit(candidate,revision))return;
            if(!candidate.paid){
                if(availableFe()<candidate.cost.fe()){status=3;return;}
                var payment=SimulationLightningPayment.pay(bridge,candidate.cost.hv(),candidate.cost.ehv());
                refundHv=Math.addExact(refundHv,payment.refundHv());refundEhv=Math.addExact(refundEhv,payment.refundEhv());saveChanges();
                if(!payment.paid()){status=3;return;}
                if(!canCommit(candidate,revision)||availableFe()<candidate.cost.fe()){
                    refundHv=Math.addExact(refundHv,candidate.cost.hv());refundEhv=Math.addExact(refundEhv,candidate.cost.ehv());saveChanges();return;
                }
                long credit=Math.min(feCredit,candidate.cost.fe());feCredit-=credit;
                energy.deserializeNBT(level.registryAccess(),IntTag.valueOf(energy.getEnergyStored()-(int)(candidate.cost.fe()-credit)));
                candidate.paid=true;saveChanges();
            }
            if(!candidate.flush(outputs)){status=4;saveChanges();return;}
            var completed=candidate;batch=null;completed.inputs.keySet().forEach(rolls::remove);nextCrystal=completed.nextSlot%structure.capacity();
            saveChanges();markForClientUpdate();NeoForge.EVENT_BUS.post(new MultiblockSimulationEvents.Completed(this,completed));bridge.export();
        }else saveChanges();
    }
    private boolean canCommit(SimulationBatch job,long revision){
        checkStructure();return batch==job&&inputRevision==revision&&structure!=null&&loaded()&&getMainNode().isActive()&&inputsMatch(job)&&job.fits(outputs);
    }
    private boolean inputsMatch(SimulationBatch job){return structure!=null&&job.inputs.entrySet().stream().allMatch(e->e.getKey()>=0&&e.getKey()<structure.capacity()&&ItemStack.matches(e.getValue(),crystals.getStackInSlot(e.getKey())));}
    private void charge(){
        if(feCredit>0){int credited=energy.receiveEnergy((int)Math.min(Integer.MAX_VALUE,feCredit),false);feCredit-=credited;if(credited>0)saveChanges();}
        var grid=getMainNode().getGrid();int needed=Math.min(10000,energy.getMaxEnergyStored()-energy.getEnergyStored());
        if(needed<=0||grid==null||!getMainNode().isActive())return;
        double ae=appeng.api.config.PowerUnit.FE.convertTo(appeng.api.config.PowerUnit.AE,Math.max(0,needed-feFraction));
        double drawn=grid.getEnergyService().extractAEPower(ae,appeng.api.config.Actionable.MODULATE,appeng.api.config.PowerMultiplier.ONE);
        double available=feFraction+appeng.api.config.PowerUnit.AE.convertTo(appeng.api.config.PowerUnit.FE,drawn);
        int received=energy.receiveEnergy(Math.min(needed,(int)Math.floor(available)),false);feFraction=Math.max(0,available-received);if(drawn>0)saveChanges();
    }
    public boolean loaded(){
        if(structure==null)return false;var min=structure.min();int maxX=(min.getX()+structure.size()-1)>>4,maxZ=(min.getZ()+structure.size()-1)>>4;
        for(int x=min.getX()>>4;x<=maxX;x++)for(int z=min.getZ()>>4;z<=maxZ;z++)if(!level.getChunkSource().hasChunk(x,z))return false;
        return true;
    }
    public void checkStructure(){
        if(level==null||level.isClientSide||SimulationStructureIndex.converting())return;
        if(structure!=null&&!loaded()){error="unloaded";return;}
        var result=structure==null?(savedMin==null?SimulationStructureValidator.find(this):SimulationStructureValidator.validate(this,savedMin,savedSize)):SimulationStructureValidator.validate(this,structure.min(),structure.size());
        if(!result.valid()){error=result.error();problem=result.problem();if(error.equals("unloaded"))return;savedMin=null;savedSize=0;invalidateStructure();return;}
        error="";problem=null;var next=result.structure();
        if(structure!=null){structure=next;return;}
        if(NeoForge.EVENT_BUS.post(new MultiblockSimulationEvents.BeforeStructureForm(this,next)).isCanceled())return;
        var verify=SimulationStructureValidator.validate(this,next.min(),next.size());if(!verify.valid())return;next=verify.structure();
        structure=next;savedMin=next.min();savedSize=next.size();SimulationStructureIndex.bind(this,next);
        final var formed=next;
        SimulationStructureIndex.converting(()->{
            for(var p:formed.members()){
                var old=level.getBlockState(p);
                if(formed.glass().contains(p)&&!old.is(MultiblockContent.GLASS.get())){
                    int x=p.getX()-formed.min().getX(),z=p.getZ()-formed.min().getZ();
                    Direction face=p.getY()==formed.min().getY()+formed.size()-1?Direction.UP:x==0?Direction.WEST:x==formed.size()-1?Direction.EAST:z==0?Direction.NORTH:Direction.SOUTH;
                    level.setBlockAndUpdate(p,MultiblockContent.GLASS.get().defaultBlockState().setValue(SimulationGlassBlock.FACE,face).setValue(SimulationPartBlock.FORMED,true));
                    ((SimulationMemberBlockEntity)level.getBlockEntity(p)).bind(this,old);
                }else if(level.getBlockEntity(p) instanceof SimulationMemberBlockEntity m){
                    if(!old.is(MultiblockContent.GLASS.get()))m.bind(this,null);if(old.hasProperty(SimulationPartBlock.FORMED))level.setBlockAndUpdate(p,old.setValue(SimulationPartBlock.FORMED,true));
                }
            }
        });saveChanges();markForClientUpdate();NeoForge.EVENT_BUS.post(new MultiblockSimulationEvents.Formed(this,formed));
    }
    public void invalidateStructure(){
        var old=structure;structure=null;savedMin=null;savedSize=0;SimulationStructureIndex.release(this);bridge.disconnect();if(old==null||level==null)return;
        SimulationStructureIndex.converting(()->{for(var p:old.members())if(level.hasChunkAt(p)&&level.getBlockEntity(p) instanceof SimulationMemberBlockEntity m&&m.ownedBy(this))m.restore();});
        saveChanges();markForClientUpdate();NeoForge.EVENT_BUS.post(new MultiblockSimulationEvents.Invalidated(this,old));
    }
    public CompoundTag saveMachine(HolderLookup.Provider r){
        var tag=new CompoundTag();tag.putUUID("Identity",identity);tag.put("Crystals",crystals.serializeNBT(r));tag.put("Bulk",outputs.save(r));tag.put("Energy",energy.serializeNBT(r));tag.putDouble("FeFraction",feFraction);tag.putLong("RefundHv",refundHv);tag.putLong("RefundEhv",refundEhv);tag.putLong("FeCredit",feCredit);tag.putLong("InputRevision",inputRevision);tag.putInt("NextCrystal",nextCrystal);
        if(batch!=null)tag.put("Batch",batch.save(r));
        var fixed=new ListTag();rolls.forEach((slot,roll)->{var t=new CompoundTag();t.putInt("Slot",slot);t.put("Input",roll.input.save(r));var list=new ListTag();for(var item:roll.output)list.add(item.save(r));t.put("Outputs",list);fixed.add(t);});tag.put("Rolls",fixed);tag.putString("RollGeneration",rollGeneration);return tag;
    }
    public void loadMachine(CompoundTag tag,HolderLookup.Provider r){
        if(tag.hasUUID("Identity"))identity=tag.getUUID("Identity");loadingInventory=true;try{crystals.deserializeNBT(r,tag.getCompound("Crystals"));}finally{loadingInventory=false;}
        crystalSnapshots.clear();for(int slot=0;slot<49;slot++)crystalSnapshots.put(slot,crystals.getStackInSlot(slot).copy());
        outputs.load(tag.getCompound("Bulk"),r);if(tag.contains("Energy"))energy.deserializeNBT(r,tag.get("Energy"));
        feFraction=tag.getDouble("FeFraction");if(!Double.isFinite(feFraction)||feFraction<0||feFraction>=1)feFraction=0;
        refundHv=Math.max(0,tag.getLong("RefundHv"));refundEhv=Math.max(0,tag.getLong("RefundEhv"));feCredit=Math.max(0,tag.getLong("FeCredit"));inputRevision=Math.max(0,tag.getLong("InputRevision"));nextCrystal=Math.clamp(tag.getInt("NextCrystal"),0,48);
        batch=tag.contains("Batch")?SimulationBatch.load(tag.getCompound("Batch"),r):null;rolls.clear();rollGeneration=tag.getString("RollGeneration");
        for(var v:tag.getList("Rolls",Tag.TAG_COMPOUND)){var t=(CompoundTag)v;int slot=t.getInt("Slot");if(slot<0||slot>=49)continue;var list=new ArrayList<ItemStack>();for(var item:t.getList("Outputs",Tag.TAG_COMPOUND))ItemStack.parse(r,item).ifPresent(list::add);rolls.put(slot,new Roll(ItemStack.parseOptional(r,t.getCompound("Input")),List.copyOf(list)));}
    }
    @Override public void saveAdditional(CompoundTag tag,HolderLookup.Provider r){super.saveAdditional(tag,r);tag.put("SimulationMachine",saveMachine(r));if(savedMin!=null){tag.putLong("StructureMin",savedMin.asLong());tag.putInt("StructureSize",savedSize);}}
    @Override public void loadTag(CompoundTag tag,HolderLookup.Provider r){super.loadTag(tag,r);loadMachine(tag.getCompound("SimulationMachine"),r);if(tag.contains("StructureMin")){savedMin=BlockPos.of(tag.getLong("StructureMin"));savedSize=Math.clamp(tag.getInt("StructureSize"),3,7);}}
    private void writeVisual(RegistryFriendlyByteBuf data){
        data.writeVarInt(visualSize());data.writeBlockPos(visualMin());data.writeByte(visualFlags());var items=visualCrystals();data.writeVarInt(items.size());for(var item:items)ItemStack.STREAM_CODEC.encode(data,item);
    }
    @Override protected void writeToStream(RegistryFriendlyByteBuf data){super.writeToStream(data);writeVisual(data);}
    @Override protected boolean readFromStream(RegistryFriendlyByteBuf data){
        boolean changed=super.readFromStream(data);visualSize=Math.clamp(data.readVarInt(),0,7);visualMin=data.readBlockPos();visualFlags=data.readUnsignedByte()&7;int count=data.readVarInt();if(count<0||count>49)throw new IllegalArgumentException("visual crystals");
        var items=new ArrayList<ItemStack>();for(int i=0;i<count;i++)items.add(ItemStack.STREAM_CODEC.decode(data));visualCrystals=List.copyOf(items);SimulationShellVisuals.publish(this,visualToken);return true;
    }
    @Override protected void saveVisualState(CompoundTag tag){super.saveVisualState(tag);tag.putInt("Size",visualSize());tag.putLong("Min",visualMin().asLong());tag.putInt("Flags",visualFlags());if(level!=null){var list=new ListTag();for(var item:visualCrystals())list.add(item.save(level.registryAccess()));tag.put("VisualCrystals",list);}}
    @Override protected void loadVisualState(CompoundTag tag){super.loadVisualState(tag);visualSize=Math.clamp(tag.getInt("Size"),0,7);visualMin=BlockPos.of(tag.getLong("Min"));visualFlags=tag.getInt("Flags")&7;if(level!=null){var list=new ArrayList<ItemStack>();for(var v:tag.getList("VisualCrystals",Tag.TAG_COMPOUND))if(list.size()<49)ItemStack.parse(level.registryAccess(),v).ifPresent(list::add);visualCrystals=List.copyOf(list);}}
    @Override public void onLoad(){super.onLoad();SimulationShellVisuals.publish(this,visualToken);}
    @Override public void setRemoved(){SimulationShellVisuals.remove(this,visualToken);bridge.disconnect();super.setRemoved();}
    @Override public void onChunkUnloaded(){SimulationShellVisuals.remove(this,visualToken);bridge.disconnect();super.onChunkUnloaded();}
    @Override public IGridNode getActionableNode(){return getMainNode().getNode();}
    @Override public Set<Direction> getGridConnectableSides(BlockOrientation o){return EnumSet.allOf(Direction.class);}
    @Override public AECableType getCableConnectionType(Direction d){return AECableType.SMART;}
    @Override protected Item getItemFromBlockEntity(){return MultiblockContent.CONTROLLER_ITEM.get();}
}
