package dev.overloadsim.output;

import appeng.api.AECapabilities;
import appeng.api.config.Actionable;
import appeng.api.networking.*;
import appeng.api.networking.security.*;
import appeng.api.stacks.*;
import appeng.api.storage.MEStorage;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.util.AECableType;
import appeng.blockentity.grid.AENetworkedBlockEntity;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuHostLocator;
import appeng.parts.automation.StackWorldBehaviors;
import appeng.util.SettingsFrom;
import com.moakiee.ae2lt.api.frequency.*;
import com.moakiee.ae2lt.logic.energy.*;
import dev.overloadsim.compat.FrequencyBindingLifecycle;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.*;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

/** An output-only network proxy; deliberately independent of AE's InterfaceLogic. */
public final class OutputInterfaceBlockEntity extends AENetworkedBlockEntity implements IActionHost,FrequencyBindingHost {
    public static final int SLOT_COUNT=36, MAX_CONNECTIONS=1024;
    public enum InterfaceMode { NORMAL, WIRELESS }
    public enum IOSpeedMode { NORMAL, FAST }
    public enum ExportMode { OFF, AUTO }
    private final FrequencyBindingAccess frequency=FrequencyApi.createBinding(this);
    private final OutputInterfaceLogic logic;
    private final List<OutputConnection> connections=new ArrayList<>();
    private final boolean[] unlimited=new boolean[SLOT_COUNT];
    private final OutputTransferLedger<AEKey> ledger=new OutputTransferLedger<>(this::changed);
    private @Nullable OutputRefundOrigin refundOrigin;
    private InterfaceMode interfaceMode=InterfaceMode.NORMAL;
    private IOSpeedMode ioSpeedMode=IOSpeedMode.NORMAL;
    private ExportMode exportMode=ExportMode.OFF;
    private @Nullable Direction energyOutputDirection;
    private int lastViewedPage;
    private boolean loading,frequencyUnloading,exporting,deferredRemoval,replacementDropEmitted;
    private long dropPreparedTick=Long.MIN_VALUE;
    private final Map<Endpoint,EndpointState> endpointCache=new HashMap<>();
    private long lastCycle=Long.MIN_VALUE;
    private final OutputCooldown[] cooldowns=java.util.stream.IntStream.range(0,SLOT_COUNT).mapToObj(slot->new OutputCooldown()).toArray(OutputCooldown[]::new);
    private int lastRange=Integer.MIN_VALUE;
    private boolean lastCrossDimension;
    private final IItemHandler items=new ItemOutput();
    private final IFluidHandler fluids=new FluidOutput();
    private record Endpoint(net.minecraft.resources.ResourceKey<Level> dimension,BlockPos pos,Direction face) {}
    private static final class EndpointState {
        BlockEntity entity; BlockState blockState; long refreshed=Long.MIN_VALUE;
        final Map<AEKeyType,MEStorage> wrappers=new IdentityHashMap<>();
        TargetAccess energy;
    }
    public OutputInterfaceBlockEntity(BlockPos pos,BlockState state) {
        super(OutputContent.ENTITY.get(),pos,state);
        getMainNode().setFlags(GridFlags.REQUIRE_CHANNEL).setIdlePowerUsage(5);
        logic=new OutputInterfaceLogic(this);
    }
    public OutputInterfaceLogic getOutputLogic(){return logic;}
    public IUpgradeInventory getUpgrades(){return logic.getUpgrades();}
    public InterfaceMode getInterfaceMode(){return interfaceMode;}
    public IOSpeedMode getIOSpeedMode(){return ioSpeedMode;}
    public ExportMode getExportMode(){return exportMode;}
    public void setInterfaceMode(InterfaceMode value){if(value!=null&&value!=interfaceMode){interfaceMode=value;resetCycle();}}
    public void setIOSpeedMode(IOSpeedMode value){if(value!=null&&value!=ioSpeedMode){ioSpeedMode=value;resetCycle();}}
    public void setExportMode(ExportMode value){if(value!=null&&value!=exportMode){exportMode=value;resetCycle();}}
    public boolean isUnlimited(int slot){return slot>=0&&slot<SLOT_COUNT&&unlimited[slot];}
    public void setUnlimited(int slot,boolean value){if(slot>=0&&slot<SLOT_COUNT&&unlimited[slot]!=value){unlimited[slot]=value;changed();}}
    public @Nullable Direction getEnergyOutputDirection(){return energyOutputDirection;}
    public void setEnergyOutputDirection(@Nullable Direction direction){if(direction!=energyOutputDirection){energyOutputDirection=direction;resetCycle();}}
    public int getLastViewedPage(){return lastViewedPage;}
    public void setLastViewedPage(int page){int valid=Math.max(0,Math.min(1,page));if(lastViewedPage!=valid){lastViewedPage=valid;changed();}}
    public @Nullable AEKey getConfiguredKey(int slot){return slot>=0&&slot<SLOT_COUNT?logic.getConfig().getKey(slot):null;}
    public List<OutputConnection> getConnections(){return List.copyOf(connections);}
    public boolean addConnection(OutputConnection connection) {
        if(connection==null||connection.sourceSlot()<0||connection.sourceSlot()>=SLOT_COUNT||connections.size()>=MAX_CONNECTIONS||connections.contains(connection))return false;
        if(level==null||level.isClientSide||!isConnectionAllowed(connection))return false;
        var targetLevel=((ServerLevel)level).getServer().getLevel(connection.dimension());
        if(targetLevel!=null&&targetLevel.hasChunkAt(connection.pos())&&!isTargetValid(targetLevel,connection))return false;
        connections.add(connection);resetCycle();return true;
    }
    public boolean removeConnection(OutputConnection connection){boolean removed=connections.remove(connection);if(removed)resetCycle();return removed;}
    public int clearConnections(int sourceSlot){int before=connections.size();connections.removeIf(connection->connection.sourceSlot()==sourceSlot);int removed=before-connections.size();if(removed>0)resetCycle();return removed;}
    public boolean isConnectionAllowed(OutputConnection connection) {
        if(level==null)return false;
        boolean same=level.dimension().equals(connection.dimension());
        if(!same)return OutputInterfaceConfig.CROSS_DIMENSION.get();
        if(worldPosition.equals(connection.pos()))return false;
        int range=OutputInterfaceConfig.RANGE.get();
        return range==0||worldPosition.distSqr(connection.pos())<=(double)range*range;
    }
    public boolean isTargetValid(ServerLevel target,OutputConnection connection) {
        if(!target.hasChunkAt(connection.pos()))return true;
        var endpoint=new Endpoint(connection.dimension(),connection.pos(),connection.face());
        boolean valid=resolve(target,endpoint,true)!=null;
        if(connections.stream().noneMatch(bound->sameEndpoint(bound,endpoint)))endpointCache.remove(endpoint);
        return valid;
    }
    private @Nullable EndpointState resolve(ServerLevel target,Endpoint endpoint,boolean force) {
        if(!target.hasChunkAt(endpoint.pos()))return null;
        var entity=target.getBlockEntity(endpoint.pos());
        var blockState=target.getBlockState(endpoint.pos());
        var state=endpointCache.computeIfAbsent(endpoint,unused->new EndpointState());
        long now=target.getGameTime();
        if(force||state.entity!=entity||state.blockState!=blockState||now-state.refreshed>=20||now<state.refreshed||state.refreshed==Long.MIN_VALUE) {
            state.entity=entity;state.blockState=blockState;state.refreshed=now;state.wrappers.clear();state.energy=null;
            for(var entry:StackWorldBehaviors.createExternalStorageStrategies(target,endpoint.pos(),endpoint.face()).entrySet()) {
                var wrapper=entry.getValue().createWrapper(false,()->{});
                if(wrapper!=null)state.wrappers.put(entry.getKey(),wrapper);
            }
            if(AppFluxBridge.canUseEnergyHandler()) {
                var cap=AppFluxBridge.createCapCache(target,endpoint.pos().relative(endpoint.face()),()->getMainNode().getGrid());
                state.energy=AppFluxBridge.resolveEnergyTarget(cap,endpoint.face().getOpposite());
            }
        }
        if(state.wrappers.isEmpty()&&state.energy==null){endpointCache.remove(endpoint);return null;}
        return state;
    }
    private static boolean sameEndpoint(OutputConnection connection,Endpoint endpoint){return connection.dimension().equals(endpoint.dimension())&&connection.pos().equals(endpoint.pos())&&connection.face()==endpoint.face();}
    private boolean endpointLive(ServerLevel provider,Endpoint endpoint,InterfaceMode mode,BlockEntity entity,BlockState blockState){
        if(isRemoved()||!getMainNode().isActive()||interfaceMode!=mode)return false;
        var target=provider.getServer().getLevel(endpoint.dimension());
        return target!=null&&target.hasChunkAt(endpoint.pos())&&target.getBlockEntity(endpoint.pos())==entity&&target.getBlockState(endpoint.pos())==blockState;
    }
    private boolean resourceDestinationLive(ServerLevel provider,Endpoint endpoint,InterfaceMode mode,BlockEntity entity,BlockState blockState,int slot,AEKey key){
        if(!endpointLive(provider,endpoint,mode,entity,blockState)||exportMode!=ExportMode.AUTO||!key.equals(getConfiguredKey(slot))||logic.capForSlot(slot)<=0)return false;
        if(mode==InterfaceMode.NORMAL)return energyOutputDirection==null||endpoint.face()==energyOutputDirection.getOpposite();
        var connection=new OutputConnection(slot,endpoint.dimension(),endpoint.pos(),endpoint.face());
        return connections.contains(connection)&&isConnectionAllowed(connection);
    }
    private boolean energyDestinationLive(ServerLevel provider,Endpoint endpoint,InterfaceMode mode,BlockEntity entity,BlockState blockState){
        if(!endpointLive(provider,endpoint,mode,entity,blockState)||!hasInductionCard())return false;
        if(mode==InterfaceMode.NORMAL)return energyOutputDirection!=null&&endpoint.face()==energyOutputDirection.getOpposite();
        return connections.stream().anyMatch(connection->sameEndpoint(connection,endpoint)&&isConnectionAllowed(connection));
    }
    public boolean isEnergyKey(@Nullable AEKey key){return key!=null&&AppFluxBridge.FE_KEY!=null&&key.getType()==AppFluxBridge.FE_KEY.getType();}
    public boolean hasInductionCard(){for(var stack:logic.getUpgrades())if(AppFluxBridge.isInductionCard(stack.getItem()))return true;return false;}
    public Map<AEKey,Long> getPendingRefunds(){return ledger.pendingRefunds();}
    private OutputTransferLedger.Network<AEKey> network() {
        var originalGrid=getMainNode().getGrid();
        var originalInventory=originalGrid==null?null:originalGrid.getStorageService().getInventory();
        var transactionOrigin=originalGrid==null?null:OutputRefundOrigin.capture(this,originalGrid);
        return new OutputTransferLedger.Network<>() {
            private boolean extractedHere;
            @Override public long extract(AEKey key,long amount) {
                if(originalGrid==null||!getMainNode().isActive()||getMainNode().getGrid()!=originalGrid)return 0;
                if(!ledger.pendingRefunds().isEmpty()&&(transactionOrigin==null||!transactionOrigin.equals(refundOrigin)))return 0;
                long cap=isEnergyKey(key)?amount:PowerCostUtil.maxAffordable(originalGrid,key,amount);
                if(ledger.pendingRefunds().isEmpty())refundOrigin=transactionOrigin;
                long extracted=logic.extractFromNetwork(originalGrid,originalInventory,key,cap,Actionable.MODULATE,IActionSource.ofMachine(OutputInterfaceBlockEntity.this),false);
                extractedHere|=extracted>0;
                return extracted;
            }
            @Override public long refund(AEKey key,long amount){
                // A public anchor is not required for initial supply. Its current transaction
                // can still return to the captured inventory; an unproven persisted cohort
                // remains privately owned instead of entering a later selected network.
                if(refundOrigin==null)return extractedHere?logic.refundIntoNetwork(originalInventory,key,amount):0;
                if(!(level instanceof ServerLevel server))return 0;
                var originGrid=refundOrigin.resolve(server.getServer());if(originGrid==null)return 0;
                var inventory=originGrid==originalGrid?originalInventory:originGrid.getStorageService().getInventory();
                return logic.refundIntoNetwork(inventory,key,amount);
            }
        };
    }
    public void tick() {
        if(!(level instanceof ServerLevel server)||isRemoved()||exporting)return;
        frequency.serverTick();
        int range=OutputInterfaceConfig.RANGE.get();boolean crossDimension=OutputInterfaceConfig.CROSS_DIMENSION.get();
        if(range!=lastRange||crossDimension!=lastCrossDimension){lastRange=range;lastCrossDimension=crossDimension;lastCycle=Long.MIN_VALUE;for(var cooldown:cooldowns)cooldown.reset();}
        if(!getMainNode().isActive()||getMainNode().getGrid()==null)return;
        exporting=true;
        try {
            ledger.refund(network());
            long now=server.getGameTime();int interval=1;
            if(lastCycle!=Long.MIN_VALUE&&now-lastCycle<interval&&now>=lastCycle)return;
            lastCycle=now;
            var active=new LinkedHashMap<Endpoint,EndpointState>();
            if(interfaceMode==InterfaceMode.WIRELESS) {
                boolean removed=false;
                for(var connection:List.copyOf(connections)) {
                    if(!isConnectionAllowed(connection))continue;
                    var target=server.getServer().getLevel(connection.dimension());
                    if(target==null||!target.hasChunkAt(connection.pos()))continue;
                    var endpoint=new Endpoint(connection.dimension(),connection.pos(),connection.face());
                    var state=resolve(target,endpoint,false);
                    if(state==null){connections.remove(connection);endpointCache.remove(endpoint);removed=true;}else active.put(endpoint,state);
                }
                if(removed)changed();
            } else {
                for(var side:Direction.values()) {
                    if(energyOutputDirection!=null&&side!=energyOutputDirection)continue;
                    var endpoint=new Endpoint(server.dimension(),worldPosition.relative(side),side.getOpposite());
                    var state=resolve(server,endpoint,false);if(state!=null)active.put(endpoint,state);
                }
            }
            var bindingsBySlot=new HashMap<Integer,Set<Endpoint>>();
            for(var connection:connections)bindingsBySlot.computeIfAbsent(connection.sourceSlot(),unused->new HashSet<>()).add(new Endpoint(connection.dimension(),connection.pos(),connection.face()));
            if(exportMode==ExportMode.AUTO)for(int slot=0;slot<SLOT_COUNT&&!isRemoved();slot++) {
                var key=getConfiguredKey(slot);long budget=logic.capForSlot(slot);
                var transferGrid=getMainNode().getGrid();
                if(key==null||budget<=0||isEnergyKey(key))continue;
                if(!cooldowns[slot].isDue(now))continue;
                var destinations=new ArrayList<OutputTransferLedger.Target<AEKey>>();
                for(var entry:active.entrySet()) {
                    var endpoint=entry.getKey();
                    if(interfaceMode==InterfaceMode.WIRELESS&&!bindingsBySlot.getOrDefault(slot,Set.of()).contains(endpoint))continue;
                    var wrapper=entry.getValue().wrappers.get(key.getType());if(wrapper==null)continue;
                    var capturedSlot=slot;var capturedMode=interfaceMode;var capturedEntity=entry.getValue().entity;var capturedState=entry.getValue().blockState;
                    destinations.add((what,amount,simulate)-> {
                        if(getMainNode().getGrid()!=transferGrid)return 0;
                        if(!resourceDestinationLive(server,endpoint,capturedMode,capturedEntity,capturedState,capturedSlot,what))return 0;
                        var event=new OutputInterfaceExportEvent(this,capturedSlot,new OutputConnection(capturedSlot,endpoint.dimension(),endpoint.pos(),endpoint.face()),what,amount,simulate);
                        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(event);
                        if(event.isCanceled()||getMainNode().getGrid()!=transferGrid||!resourceDestinationLive(server,endpoint,capturedMode,capturedEntity,capturedState,capturedSlot,what))return 0;
                        return wrapper.insert(what,Math.min(amount,event.getAmount()),simulate?Actionable.SIMULATE:Actionable.MODULATE,IActionSource.ofMachine(this));
                    });
                }
                long moved=ledger.export(slot,key,budget,destinations,network());
                cooldowns[slot].complete(now,moved>0,ioSpeedMode==IOSpeedMode.FAST);
                if(moved>0)PowerCostUtil.consume(transferGrid,key,moved);
            }
            if(hasInductionCard()&&AppFluxBridge.FE_KEY!=null&&!isRemoved()) {
                int index=SLOT_COUNT;
                for(var entry:active.entrySet()) {
                    if(interfaceMode==InterfaceMode.NORMAL&&(energyOutputDirection==null||entry.getKey().face()!=energyOutputDirection.getOpposite()))continue;
                    var target=entry.getValue().energy;if(target==null)continue;
                    var transferGrid=getMainNode().getGrid();
                    var endpoint=entry.getKey();var capturedMode=interfaceMode;var capturedEntity=entry.getValue().entity;var capturedState=entry.getValue().blockState;
                    ledger.export(index++,AppFluxBridge.FE_KEY,AppFluxBridge.TRANSFER_RATE,List.of((key,amount,simulate)-> {
                        if(getMainNode().getGrid()!=transferGrid)return 0;
                        if(!energyDestinationLive(server,endpoint,capturedMode,capturedEntity,capturedState))return 0;
                        int representativeSlot=capturedMode==InterfaceMode.WIRELESS?connections.stream().filter(connection->sameEndpoint(connection,endpoint)&&isConnectionAllowed(connection)).mapToInt(OutputConnection::sourceSlot).findFirst().orElse(-1):-1;
                        var event=new OutputInterfaceExportEvent(this,representativeSlot,new OutputConnection(representativeSlot,endpoint.dimension(),endpoint.pos(),endpoint.face()),key,amount,simulate);
                        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(event);
                        if(event.isCanceled()||getMainNode().getGrid()!=transferGrid||!energyDestinationLive(server,endpoint,capturedMode,capturedEntity,capturedState))return 0;
                        amount=Math.min(amount,event.getAmount());
                        return simulate?target.simulateReceive(amount):target.receive(amount);
                    }),network());
                }
            }
            if(!isRemoved()&&now%5==0)logic.tickCrafting();
        }finally{
            if(ledger.pendingRefunds().isEmpty())refundOrigin=null;
            exporting=false;
            if(deferredRemoval){deferredRemoval=false;if(!frequencyUnloading&&(isRemoved()||server.getBlockEntity(worldPosition)!=this))emitReplacementDrop();}
        }
    }
    private void resetCycle(){lastCycle=Long.MIN_VALUE;for(var cooldown:cooldowns)cooldown.reset();endpointCache.clear();updateIdlePower();changed();}
    public void configurationChanged(){resetCycle();}
    private void updateIdlePower(){getMainNode().setIdlePowerUsage((5+(interfaceMode==InterfaceMode.WIRELESS?5+connections.size():0))*(ioSpeedMode==IOSpeedMode.FAST?1.5:1));}
    public void changed(){if(!loading){saveChanges();markForClientUpdate();}}
    public IItemHandler itemAutomation(){return items;}
    public IFluidHandler fluidAutomation(){return fluids;}
    public static void registerCapabilities(RegisterCapabilitiesEvent event,BlockEntityType<OutputInterfaceBlockEntity> type) {
        event.registerBlockEntity(AECapabilities.IN_WORLD_GRID_NODE_HOST,type,(host,context)->host);
        event.registerBlockEntity(AECapabilities.GENERIC_INTERNAL_INV,type,(host,side)->host.getOutputLogic().getStorage());
        event.registerBlockEntity(AECapabilities.ME_STORAGE,type,(host,side)->host.getOutputLogic().getStorage());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK,type,(host,side)->host.itemAutomation());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK,type,(host,side)->host.fluidAutomation());
    }
    private final class ItemOutput implements IItemHandler {
        @Override public int getSlots(){return SLOT_COUNT;}
        @Override public ItemStack getStackInSlot(int slot){var stack=logic.getStorage().getStack(slot);return stack!=null&&stack.what() instanceof AEItemKey item?item.toStack((int)Math.min(Integer.MAX_VALUE,stack.amount())):ItemStack.EMPTY;}
        @Override public ItemStack insertItem(int slot,ItemStack stack,boolean simulate){return stack;}
        @Override public ItemStack extractItem(int slot,int amount,boolean simulate){var visible=logic.getStorage().getStack(slot);var key=visible==null?null:visible.what();return key instanceof AEItemKey item?item.toStack((int)logic.extractConfigured(slot,key,Math.max(0,amount),simulate?Actionable.SIMULATE:Actionable.MODULATE)):ItemStack.EMPTY;}
        @Override public int getSlotLimit(int slot){return Integer.MAX_VALUE;}
        @Override public boolean isItemValid(int slot,ItemStack stack){return false;}
    }
    private final class FluidOutput implements IFluidHandler {
        @Override public int getTanks(){return SLOT_COUNT;}
        @Override public FluidStack getFluidInTank(int slot){var stack=logic.getStorage().getStack(slot);return stack!=null&&stack.what() instanceof AEFluidKey fluid?fluid.toStack((int)Math.min(Integer.MAX_VALUE,stack.amount())):FluidStack.EMPTY;}
        @Override public int getTankCapacity(int slot){return Integer.MAX_VALUE;}
        @Override public boolean isFluidValid(int slot,FluidStack stack){return false;}
        @Override public int fill(FluidStack resource,FluidAction action){return 0;}
        @Override public FluidStack drain(FluidStack resource,FluidAction action){if(resource.isEmpty())return FluidStack.EMPTY;return drainKey(AEFluidKey.of(resource),resource.getAmount(),action);}
        @Override public FluidStack drain(int amount,FluidAction action){for(int slot=0;slot<SLOT_COUNT;slot++)if(getConfiguredKey(slot) instanceof AEFluidKey key){var result=drainKey(key,amount,action);if(!result.isEmpty())return result;}return FluidStack.EMPTY;}
        private FluidStack drainKey(AEFluidKey key,int amount,FluidAction action){long cap=0;for(int slot=0;slot<SLOT_COUNT;slot++)if(key.equals(getConfiguredKey(slot))){long add=logic.capForSlot(slot);cap=cap>Long.MAX_VALUE-add?Long.MAX_VALUE:cap+add;}long extracted=logic.extractNetwork(key,Math.min(Math.max(0,amount),cap),action.simulate()?Actionable.SIMULATE:Actionable.MODULATE,IActionSource.ofMachine(OutputInterfaceBlockEntity.this),true);return key.toStack((int)extracted);}
    }
    @Override public IGridNode getActionableNode(){return getMainNode().getNode();}
    @Override public AECableType getCableConnectionType(Direction side){return AECableType.SMART;}
    @Override public AENetworkedBlockEntity getFrequencyBindingBlockEntity(){return this;}
    @Override public FrequencyBindingAccess getFrequencyBindingAccess(){return frequency;}
    @Override public void saveFrequencyBindingChanges(){saveChanges();}
    @Override public void markFrequencyBindingForUpdate(){markForClientUpdate();}
    @Override public void onMainNodeStateChanged(IGridNodeListener.State reason){frequency.onMainNodeStateChanged(reason);}
    @Override public void onReady(){super.onReady();frequency.onReady();}
    @Override public void onLoad(){frequencyUnloading=false;super.onLoad();}
    @Override public void setRemoved(){if(exporting&&!frequencyUnloading)deferredRemoval=true;if(!frequencyUnloading)frequency.setRemoved();super.setRemoved();}
    @Override public void onChunkUnloaded(){frequencyUnloading=true;FrequencyBindingLifecycle.chunkUnloaded(frequency);super.onChunkUnloaded();}
    @Override public void clearRemoved(){frequencyUnloading=false;replacementDropEmitted=false;deferredRemoval=false;super.clearRemoved();frequency.clearRemoved();}
    public void openMenu(Player player,MenuHostLocator locator){MenuOpener.open(OutputMenu.TYPE,player,locator);}
    @Override public void exportSettings(SettingsFrom mode,DataComponentMap.Builder builder,Player player){super.exportSettings(mode,builder,player);frequency.exportMemorySettings(mode,builder,tag->saveOutput(tag,level.registryAccess(),false));}
    @Override public void importSettings(SettingsFrom mode,DataComponentMap settings,Player player){super.importSettings(mode,settings,player);frequency.importMemorySettings(mode,settings,tag->loadOutput(tag,level.registryAccess(),false));}
    @Override public void saveAdditional(CompoundTag tag,HolderLookup.Provider registries){super.saveAdditional(tag,registries);frequency.save(tag);saveOutput(tag,registries,true);}
    @Override public void loadTag(CompoundTag tag,HolderLookup.Provider registries){super.loadTag(tag,registries);frequency.load(tag);loadOutput(tag,registries,true);}
    public CompoundTag saveOwnedRefunds(HolderLookup.Provider registries){var tag=new CompoundTag();writeRefunds(tag,registries);return tag;}
    public void loadOwnedRefunds(CompoundTag tag,HolderLookup.Provider registries){readRefunds(tag,registries);changed();}
    /** Loot inside an external insertion callback must wait until its result is settled. */
    public boolean suppressOutputDrop(){if(replacementDropEmitted)return true;if(exporting){deferredRemoval=true;return true;}return false;}
    public void markDropPrepared(){if(exporting){deferredRemoval=true;return;}dropPreparedTick=level==null?Long.MIN_VALUE:level.getGameTime();}
    public void preserveOnReplacement(){
        if(level==null||level.isClientSide)return;
        if(exporting){deferredRemoval=true;return;}
        if(replacementDropEmitted||dropPreparedTick==level.getGameTime()||ledger.pendingRefunds().isEmpty())return;
        emitReplacementDrop();
    }
    private void emitReplacementDrop(){
        if(level==null||level.isClientSide||replacementDropEmitted)return;
        replacementDropEmitted=true;
        var item=new ItemStack(OutputContent.ITEM.get());var tag=new CompoundTag();tag.put("OutputOwnedRefunds",saveOwnedRefunds(level.registryAccess()));
        item.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(tag));
        net.minecraft.world.level.block.Block.popResource(level,worldPosition,item);
        ledger.clearRefunds();
    }
    private void saveOutput(CompoundTag tag,HolderLookup.Provider registries,boolean full) {
        tag.putString("InterfaceMode",interfaceMode.name());tag.putString("IOSpeedMode",ioSpeedMode.name());tag.putString("ExportMode",exportMode.name());
        tag.putInt("EnergyDirection",energyOutputDirection==null?-1:energyOutputDirection.get3DDataValue());tag.putInt("OutputPage",lastViewedPage);
        var slots=new int[SLOT_COUNT];for(int slot=0;slot<SLOT_COUNT;slot++)slots[slot]=unlimited[slot]?1:0;tag.putIntArray("OutputUnlimited",slots);
        var list=new ListTag();for(var connection:connections)list.add(connection.toTag());tag.put("OutputConnections",list);
        if(full)logic.save(tag,registries);else logic.saveConfiguration(tag,registries);
        if(full){writeRefunds(tag,registries);var cursor=new CompoundTag();ledger.cursors().forEach((slot,index)->cursor.putInt(slot.toString(),index));tag.put("OutputCursors",cursor);}
    }
    private void loadOutput(CompoundTag tag,HolderLookup.Provider registries,boolean full) {
        loading=true;
        try {
            interfaceMode=readEnum(InterfaceMode.class,tag.getString("InterfaceMode"),InterfaceMode.NORMAL);
            ioSpeedMode=readEnum(IOSpeedMode.class,tag.getString("IOSpeedMode"),IOSpeedMode.NORMAL);
            exportMode=readEnum(ExportMode.class,tag.getString("ExportMode"),ExportMode.OFF);
            int direction=tag.contains("EnergyDirection")?tag.getInt("EnergyDirection"):-1;energyOutputDirection=direction>=0&&direction<6?Direction.from3DDataValue(direction):null;
            lastViewedPage=Math.max(0,Math.min(1,tag.getInt("OutputPage")));
            if(full)logic.load(tag,registries);else logic.loadConfiguration(tag,registries);var slots=tag.getIntArray("OutputUnlimited");for(int slot=0;slot<SLOT_COUNT;slot++)unlimited[slot]=slot<slots.length&&slots[slot]!=0;
            connections.clear();for(var raw:tag.getList("OutputConnections",Tag.TAG_COMPOUND)) {
                try{var connection=OutputConnection.fromTag((CompoundTag)raw);if(connection.sourceSlot()>=0&&connection.sourceSlot()<SLOT_COUNT&&connections.size()<MAX_CONNECTIONS&&!connections.contains(connection))connections.add(connection);}catch(IllegalArgumentException malformed){}
            }
            if(full){readRefunds(tag,registries);var cursors=new HashMap<Integer,Integer>();var raw=tag.getCompound("OutputCursors");for(var slot:raw.getAllKeys())try{cursors.put(Integer.parseInt(slot),raw.getInt(slot));}catch(NumberFormatException malformed){}ledger.restoreCursors(cursors);}
            endpointCache.clear();lastCycle=Long.MIN_VALUE;updateIdlePower();
        }finally{loading=false;}
        changed();
    }
    private void writeRefunds(CompoundTag tag,HolderLookup.Provider registries){var list=new ListTag();ledger.pendingRefunds().forEach((key,amount)->list.add(GenericStack.writeTag(registries,new GenericStack(key,amount))));tag.put("OutputRefunds",list);if(refundOrigin!=null&&!list.isEmpty())tag.put("OutputRefundOrigin",refundOrigin.toTag());}
    private void readRefunds(CompoundTag tag,HolderLookup.Provider registries){var pending=new LinkedHashMap<AEKey,Long>();for(var raw:tag.getList("OutputRefunds",Tag.TAG_COMPOUND)){var stack=GenericStack.readTag(registries,(CompoundTag)raw);if(stack!=null&&stack.amount()>0)pending.merge(stack.what(),stack.amount(),(a,b)->a>Long.MAX_VALUE-b?Long.MAX_VALUE:a+b);}ledger.restoreRefunds(pending);refundOrigin=pending.isEmpty()?null:OutputRefundOrigin.fromTag(tag.getCompound("OutputRefundOrigin"));}
    private static <E extends Enum<E>> E readEnum(Class<E> type,String value,E fallback){try{return Enum.valueOf(type,value);}catch(IllegalArgumentException invalid){return fallback;}}
    @Override public void addAdditionalDrops(Level level,BlockPos pos,List<ItemStack> drops){for(var stack:logic.getUpgrades())if(!stack.isEmpty())drops.add(stack.copy());}
    @Override public void clearContent(){logic.getUpgrades().clear();}
}
