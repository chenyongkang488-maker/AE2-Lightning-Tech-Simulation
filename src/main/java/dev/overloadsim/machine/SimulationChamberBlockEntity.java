package dev.overloadsim.machine;
import java.util.*;
import appeng.blockentity.grid.AENetworkedBlockEntity;
import appeng.api.networking.*;
import appeng.api.networking.security.*;
import appeng.api.orientation.BlockOrientation;
import appeng.api.util.AECableType;
import appeng.api.stacks.AEItemKey;
import appeng.api.upgrades.IUpgradeableObject;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.core.definitions.AEItems;
import appeng.util.SettingsFrom;
import com.moakiee.ae2lt.api.frequency.*;
import dev.overloadsim.*;
import dev.overloadsim.api.*;
import dev.overloadsim.compat.LightningNetwork;
import dev.overloadsim.core.SimulationRules;
import dev.overloadsim.data.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.items.*;
import net.neoforged.neoforge.common.NeoForge;

public class SimulationChamberBlockEntity extends AENetworkedBlockEntity implements IActionHost,FrequencyBindingHost,IUpgradeableObject {
    public static final int OUTPUT_START=4,SLOTS=13;
    private final FrequencyBindingAccess frequency=FrequencyApi.createBinding(this);
    private final IUpgradeInventory upgrades=new SimulationUpgrades(this::saveChanges);
    private final ItemStackHandler inventory=new ItemStackHandler(SLOTS){
        @Override public boolean isItemValid(int slot,ItemStack stack){return switch(slot){case 0->stack.is(ModContent.PERFECT.get())&&CrystalDataAccess.read(stack).isPresent();case 1->BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(ResourceLocation.parse("ae2lt:lightning_collapse_matrix"));case 3->true;default->false;};}
        @Override public int getSlotLimit(int slot){return switch(slot){case 0->1;case 1->SimulationConfig.MATRIX_LIMIT.get();case 2->0;default->64;};}
        @Override public ItemStack insertItem(int slot,ItemStack stack,boolean simulate){return slot<OUTPUT_START&&busy()?stack:super.insertItem(slot,stack,simulate);}
        @Override public ItemStack extractItem(int slot,int count,boolean simulate){return slot<OUTPUT_START&&busy()?ItemStack.EMPTY:super.extractItem(slot,count,simulate);}
        @Override protected void onContentsChanged(int slot){saveChanges();if(slot==0)updateVisualState();}
    };
    private final IItemHandler automation=new IItemHandler(){
        public int getSlots(){return SLOTS+upgrades.size();}
        public ItemStack getStackInSlot(int slot){return slot<SLOTS?inventory.getStackInSlot(slot):upgrades.getStackInSlot(slot-SLOTS);}
        public ItemStack insertItem(int slot,ItemStack s,boolean sim){return slot<SLOTS?inventory.insertItem(slot,s,sim):upgrades.insertItem(slot-SLOTS,s,sim);}
        public ItemStack extractItem(int slot,int count,boolean sim){return slot>=OUTPUT_START&&slot<SLOTS?inventory.extractItem(slot,count,sim):ItemStack.EMPTY;}
        public int getSlotLimit(int slot){return slot<SLOTS?inventory.getSlotLimit(slot):upgrades.getSlotLimit(slot-SLOTS);}
        public boolean isItemValid(int slot,ItemStack s){return slot<SLOTS?inventory.isItemValid(slot,s):upgrades.isItemValid(slot-SLOTS,s);}
    };
    private final EnergyStorage energy=new EnergyStorage(2_000_000,2_000_000,0){@Override public int receiveEnergy(int max,boolean simulate){int moved=super.receiveEnergy(max,simulate);if(moved>0&&!simulate)saveChanges();return moved;}};
    private final List<ItemStack> pending=new ArrayList<>();
    private CrystalData taskCrystal;
    private ResourceLocation taskRecipe;
    private int remaining,totalTicks,actualParallel,cardsSnapshot;
    private long paidFe,paidLightning,lightningRefund;
    private double networkFeFraction;
    private boolean legacyEhvRefund;
    private int outputMask=63,status=0;
    private boolean eject=true,taskActive=false;
    private int visualFlags;
    private static final org.slf4j.Logger LOG=org.slf4j.LoggerFactory.getLogger(SimulationChamberBlockEntity.class);
    public SimulationChamberBlockEntity(BlockPos pos,BlockState state){super(ModContent.CHAMBER_ENTITY.get(),pos,state);getMainNode().setFlags(GridFlags.REQUIRE_CHANNEL).setIdlePowerUsage(2).setVisualRepresentation(ModContent.CHAMBER_ITEM.get());}
    public ItemStackHandler inventory(){return inventory;}
    @Override public IUpgradeInventory getUpgrades(){return upgrades;}
    public IItemHandler automation(){return automation;}
    public EnergyStorage energy(){return energy;}
    public boolean busy(){return taskActive;}
    public boolean displayHasCrystal(){return (visualFlags&1)!=0;}
    public boolean displayWorking(){return (visualFlags&2)!=0;}
    private int currentVisualFlags(){return inventory.getStackInSlot(0).is(ModContent.PERFECT.get())?1|((taskActive&&status==1&&getMainNode().isActive())?2:0):0;}
    private void updateVisualState(){
        if(level==null||level.isClientSide())return;
        int next=currentVisualFlags();if(next!=visualFlags){visualFlags=next;markForClientUpdate();}
    }
    @Override protected void writeToStream(RegistryFriendlyByteBuf data){super.writeToStream(data);data.writeByte(currentVisualFlags());}
    @Override protected boolean readFromStream(RegistryFriendlyByteBuf data){boolean changed=super.readFromStream(data);int next=data.readUnsignedByte()&3;changed|=next!=visualFlags;visualFlags=next;return changed;}
    @Override protected void saveVisualState(CompoundTag data){super.saveVisualState(data);data.putByte("SimulationVisual",(byte)visualFlags);}
    @Override protected void loadVisualState(CompoundTag data){super.loadVisualState(data);visualFlags=data.getByte("SimulationVisual")&3;}
    public int remaining(){return remaining;}public int totalTicks(){return totalTicks;}public int actualParallel(){return actualParallel;}public int status(){return status;}public boolean eject(){return eject;}public int outputMask(){return outputMask;}
    public int maximumParallel(){return SimulationRules.parallel(Math.min(32,inventory.getStackInSlot(1).getCount()));}
    public void toggleEject(){eject=!eject;saveChanges();}
    public void toggleSide(int side){if(side>=0&&side<6){outputMask^=1<<side;saveChanges();}}
    public void tick(){
        try{tickSimulation();}finally{updateVisualState();}
    }
    private void tickSimulation(){
        if(!(level instanceof ServerLevel server))return;frequency.serverTick();
        chargeFromNetwork((int)Math.min(energy.getMaxEnergyStored(),(long)energy.getEnergyStored()+SimulationConfig.NETWORK_FE_PER_TICK.get()));
        if(eject&&server.getGameTime()%SimulationConfig.EJECT_INTERVAL.get()==0)ejectOutputs();
        if(lightningRefund>0){lightningRefund-=LightningNetwork.refund(this,lightningRefund,legacyEhvRefund);saveChanges();if(lightningRefund>0){status=7;return;}legacyEhvRefund=false;}
        if(taskActive){
            if(taskCrystal==null||SimulationData.profile(server,taskCrystal.profile()).isEmpty()){status=6;return;}
            if(!getMainNode().isActive()){status=2;return;}
            if(remaining>0){remaining--;status=1;saveChanges();return;}
            flushPending();if(!pending.isEmpty()){status=4;return;}
            var completed=taskCrystal;taskActive=false;taskCrystal=null;status=0;saveChanges();NeoForge.EVENT_BUS.post(new SimulationEvents.Completed("production",server,worldPosition,completed));return;
        }
        if(server.getGameTime()%10==0)start(server);
    }
    private void start(ServerLevel server){
        var crystal=inventory.getStackInSlot(0).copy();var data=CrystalDataAccess.read(crystal);
        if(!crystal.is(ModContent.PERFECT.get())||data.isEmpty()){status=0;return;}if(SimulationData.profile(server,data.get().profile()).isEmpty()){status=6;return;}
        if(!getMainNode().isActive()){status=2;return;}
        var chosen=SimulationResolvers.production(server,data.get());if(chosen.isEmpty()){status=5;return;}var production=chosen.get().config();
        boolean dynamic=chosen.get().mineral().isPresent();int p=outputParallel(production,maximumParallel(),dynamic);if(p<1){status=4;return;}
        if(production.lightning()>0)p=(int)Math.min(p,LightningNetwork.extract(this,Long.MAX_VALUE,true)/production.lightning());
        if(production.input().isPresent()){var input=production.input().get();var aux=inventory.getStackInSlot(3);if(!BuiltInRegistries.ITEM.getKey(aux.getItem()).equals(input.item())){status=3;return;}p=Math.min(p,aux.getCount()/input.count());}
        if(production.fe()>0){p=(int)Math.min(p,energy.getMaxEnergyStored()/production.fe());p=(int)Math.min(p,energy.getEnergyStored()/production.fe());}
        if(p<1){status=3;return;}
        if(NeoForge.EVENT_BUS.post(new SimulationEvents.BeforeSimulation(server,worldPosition,data.get(),p)).isCanceled())return;
        // A listener may mutate inventory/energy/network. Verify the full transaction again.
        if(!ItemStack.matches(crystal,inventory.getStackInSlot(0))||p>maximumParallel()||p>outputParallel(production,p,dynamic)||energy.getEnergyStored()<SimulationRules.batchEnergy(production.fe(),p))return;
        if(production.input().isPresent()){var in=production.input().get();var aux=inventory.getStackInSlot(3);if(!BuiltInRegistries.ITEM.getKey(aux.getItem()).equals(in.item())||aux.getCount()<in.count()*p)return;}
        long lightningCost=SimulationRules.batchEnergy(production.lightning(),p);if(LightningNetwork.extract(this,lightningCost,true)<lightningCost)return;
        long extracted=LightningNetwork.extract(this,lightningCost,false);if(extracted!=lightningCost){lightningRefund=Math.addExact(lightningRefund,extracted);saveChanges();return;}
        // Roll only after a successful HV reservation; failed reservations cannot reroll loot.
        var fixed=new ArrayList<ItemStack>();try{for(int op=0;op<p;op++)fixed.addAll(chosen.get().roll(server,worldPosition,data.get(),"single"));}catch(RuntimeException error){lightningRefund=Math.addExact(lightningRefund,extracted);status=5;saveChanges();LOG.error("Invalid simulation output for {}",chosen.get().id(),error);return;}
        long feCost=SimulationRules.batchEnergy(production.fe(),p);energy.deserializeNBT(server.registryAccess(),IntTag.valueOf(energy.getEnergyStored()-(int)feCost));
        if(production.input().isPresent()){var aux=inventory.getStackInSlot(3).copy();aux.shrink(production.input().get().count()*p);inventory.setStackInSlot(3,aux);}
        pending.clear();pending.addAll(fixed);taskCrystal=data.get();taskRecipe=chosen.get().id();actualParallel=p;cardsSnapshot=getInstalledUpgrades(AEItems.SPEED_CARD);totalTicks=SimulationRules.duration(production.ticks(),cardsSnapshot);remaining=totalTicks;paidFe=feCost;paidLightning=lightningCost;taskActive=true;status=1;saveChanges();
    }
    private void chargeFromNetwork(int requestedFe){
        int needed=Math.min(energy.getMaxEnergyStored(),requestedFe)-energy.getEnergyStored();
        var grid=getMainNode().getGrid();if(needed<=0||grid==null||!getMainNode().isActive())return;
        double wanted=appeng.api.config.PowerUnit.FE.convertTo(appeng.api.config.PowerUnit.AE,Math.max(0,needed-networkFeFraction));
        double drawn=grid.getEnergyService().extractAEPower(wanted,appeng.api.config.Actionable.MODULATE,appeng.api.config.PowerMultiplier.ONE);
        if(drawn<=0&&networkFeFraction==0)return;
        double available=networkFeFraction+appeng.api.config.PowerUnit.AE.convertTo(appeng.api.config.PowerUnit.FE,drawn);
        int received=energy.receiveEnergy(Math.min(needed,(int)Math.floor(available)),false);
        networkFeFraction=Math.max(0,available-received);saveChanges();
    }
    private int outputParallel(SimulationRecipe.Production production,int maximum){return outputParallel(production,maximum,false);}
    private int outputParallel(SimulationRecipe.Production production,int maximum,boolean dynamic){
        // Unknown loot/provider output is rolled once after EHV reservation and journaled.
        // Require an empty buffer slot; overflow waits without discarding or rerolling.
        if(dynamic||production.entityLoot()||production.provider().isPresent()){
            for(int slot=OUTPUT_START;slot<SLOTS;slot++)if(inventory.getStackInSlot(slot).isEmpty())return maximum;
            return 0;
        }
        var simulated=new ArrayList<ItemStack>();for(int slot=OUTPUT_START;slot<SLOTS;slot++)simulated.add(inventory.getStackInSlot(slot).copy());
        for(int operation=0;operation<maximum;operation++){
            for(var output:production.outputs()){
                if(output.chance()==0)continue;
                var item=BuiltInRegistries.ITEM.getOptional(output.item());if(item.isEmpty()||item.get()==Items.AIR)return 0;
                var offered=new ItemStack(item.get());int left=output.count();
                for(int pass=0;pass<2&&left>0;pass++)for(int slot=0;slot<simulated.size()&&left>0;slot++){
                    var dest=simulated.get(slot);if((pass==0)==dest.isEmpty())continue;if(!dest.isEmpty()&&!ItemStack.isSameItemSameComponents(dest,offered))continue;
                    int moved=Math.min(left,Math.min(64,offered.getMaxStackSize())-dest.getCount());if(moved<=0)continue;
                    simulated.set(slot,offered.copyWithCount(dest.getCount()+moved));left-=moved;
                }
                if(left>0)return operation;
            }
        }
        return maximum;
    }
    private void flushPending(){
        for(var iter=pending.listIterator();iter.hasNext();){var stack=iter.next();for(int pass=0;pass<2&&!stack.isEmpty();pass++)for(int i=OUTPUT_START;i<SLOTS&&!stack.isEmpty();i++){
            var dest=inventory.getStackInSlot(i);if((pass==0)==dest.isEmpty())continue;int limit=Math.min(stack.getMaxStackSize(),64);if(!dest.isEmpty()&&!ItemStack.isSameItemSameComponents(dest,stack))continue;int moved=Math.min(stack.getCount(),limit-dest.getCount());if(moved<=0)continue;var next=stack.copyWithCount(dest.getCount()+moved);inventory.setStackInSlot(i,next);stack.shrink(moved);
        }if(stack.isEmpty())iter.remove();else break;}saveChanges();
    }
    private void ejectOutputs(){
        int budget=64;
        for(var side:Direction.values()){
            if((outputMask&(1<<side.ordinal()))==0||!level.hasChunkAt(worldPosition.relative(side)))continue;
            var target=level.getCapability(Capabilities.ItemHandler.BLOCK,worldPosition.relative(side),side.getOpposite());if(target==null)continue;
            for(int slot=OUTPUT_START;slot<SLOTS&&budget>0;slot++){
                var stack=inventory.getStackInSlot(slot);if(stack.isEmpty())continue;var offered=stack.copyWithCount(Math.min(budget,stack.getCount()));var remainder=ItemHandlerHelper.insertItemStacked(target,offered,false);int moved=offered.getCount()-remainder.getCount();if(moved>0){inventory.extractItem(slot,moved,false);budget-=moved;}
            }
            if(budget==0)break;
        }
    }
    @Override public void saveAdditional(CompoundTag tag,HolderLookup.Provider registries){super.saveAdditional(tag,registries);tag.put("Inventory",inventory.serializeNBT(registries));upgrades.writeToNBT(tag,"Upgrades",registries);tag.put("Energy",energy.serializeNBT(registries));tag.putDouble("NetworkFeFraction",networkFeFraction);frequency.save(tag);tag.putBoolean("Eject",eject);tag.putInt("OutputMask",outputMask);tag.putLong("LightningRefund",lightningRefund);tag.putString("LightningRefundTier",legacyEhvRefund?"ehv":"hv");
        if(taskActive){var job=new CompoundTag();job.putInt("Remaining",remaining);job.putInt("Duration",totalTicks);job.putInt("Parallel",actualParallel);job.putInt("Cards",cardsSnapshot);job.putString("Recipe",taskRecipe.toString());job.putLong("PaidFe",paidFe);job.putLong("PaidLightning",paidLightning);job.put("Crystal",CrystalData.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE,taskCrystal).getOrThrow());var outputs=new ListTag();for(var stack:pending)outputs.add(stack.save(registries));job.put("Outputs",outputs);tag.put("Job",job);}
    }
    @Override public void loadTag(CompoundTag tag,HolderLookup.Provider registries){super.loadTag(tag,registries);inventory.deserializeNBT(registries,tag.getCompound("Inventory"));if(tag.contains("Energy"))energy.deserializeNBT(registries,tag.get("Energy"));frequency.load(tag);eject=!tag.contains("Eject")||tag.getBoolean("Eject");outputMask=tag.contains("OutputMask")?tag.getInt("OutputMask")&63:63;lightningRefund=Math.max(0,tag.getLong("LightningRefund"));pending.clear();taskActive=false;
        upgrades.clear();upgrades.readFromNBT(tag,"Upgrades",registries);
        // Alpha.1/.2 stored all cards in main slot 2; preserve the other slot indices.
        if(!tag.contains("Upgrades")){var legacy=inventory.getStackInSlot(2).copy();if(legacy.is(AEItems.SPEED_CARD.asItem())){for(int slot=0;slot<upgrades.size()&&!legacy.isEmpty();slot++){upgrades.setItemDirect(slot,legacy.copyWithCount(1));legacy.shrink(1);}inventory.setStackInSlot(2,legacy);}}
        networkFeFraction=tag.getDouble("NetworkFeFraction");if(!Double.isFinite(networkFeFraction)||networkFeFraction<0||networkFeFraction>=1)networkFeFraction=0;
        legacyEhvRefund=lightningRefund>0&&(!tag.contains("LightningRefundTier")||tag.getString("LightningRefundTier").equals("ehv"));
        if(tag.contains("Job")){var job=tag.getCompound("Job");taskCrystal=CrystalData.CODEC.parse(NbtOps.INSTANCE,job.get("Crystal")).getOrThrow();taskRecipe=ResourceLocation.parse(job.getString("Recipe"));remaining=Math.max(0,job.getInt("Remaining"));totalTicks=Math.max(1,job.getInt("Duration"));actualParallel=Math.clamp(job.getInt("Parallel"),1,128);cardsSnapshot=Math.clamp(job.getInt("Cards"),0,4);paidFe=job.getLong("PaidFe");paidLightning=job.getLong("PaidLightning");for(var entry:job.getList("Outputs",Tag.TAG_COMPOUND))ItemStack.parse(registries,entry).ifPresent(pending::add);taskActive=true;}
    }
    @Override public AENetworkedBlockEntity getFrequencyBindingBlockEntity(){return this;}
    @Override public FrequencyBindingAccess getFrequencyBindingAccess(){return frequency;}
    @Override public void saveFrequencyBindingChanges(){saveChanges();}
    @Override public void markFrequencyBindingForUpdate(){markForClientUpdate();}
    @Override public void onMainNodeStateChanged(IGridNodeListener.State reason){frequency.onMainNodeStateChanged(reason);}
    @Override public void onReady(){super.onReady();frequency.onReady();}
    @Override public void setRemoved(){frequency.setRemoved();super.setRemoved();}
    @Override public void onChunkUnloaded(){frequency.setRemoved();super.onChunkUnloaded();}
    @Override public void clearRemoved(){super.clearRemoved();frequency.clearRemoved();}
    @Override public IGridNode getActionableNode(){return getMainNode().getNode();}
    @Override public Set<Direction> getGridConnectableSides(BlockOrientation o){return EnumSet.allOf(Direction.class);}
    @Override public AECableType getCableConnectionType(Direction d){return AECableType.SMART;}
    @Override protected Item getItemFromBlockEntity(){return ModContent.CHAMBER_ITEM.get();}
    @Override public void addAdditionalDrops(Level level,BlockPos pos,List<ItemStack> drops){for(int i=0;i<SLOTS;i++)if(!inventory.getStackInSlot(i).isEmpty())drops.add(inventory.getStackInSlot(i).copy());for(var card:upgrades)if(!card.isEmpty())drops.add(card.copy());if(taskActive&&remaining==0)for(var s:pending)drops.add(s.copy());}
    @Override public void clearContent(){for(int i=0;i<SLOTS;i++)inventory.setStackInSlot(i,ItemStack.EMPTY);upgrades.clear();pending.clear();taskActive=false;}
    @Override public void exportSettings(SettingsFrom mode,DataComponentMap.Builder builder,Player p){super.exportSettings(mode,builder,p);frequency.exportMemorySettings(mode,builder,t->{t.putBoolean("Eject",eject);t.putInt("OutputMask",outputMask);});}
    @Override public void importSettings(SettingsFrom mode,DataComponentMap map,Player p){super.importSettings(mode,map,p);frequency.importMemorySettings(mode,map,t->{eject=t.getBoolean("Eject");outputMask=t.getInt("OutputMask")&63;saveChanges();});}
}
