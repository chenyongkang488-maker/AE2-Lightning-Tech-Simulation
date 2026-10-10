// SPDX-License-Identifier: LGPL-3.0-only
package dev.overloadsim.output;

import appeng.api.config.*;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.*;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.*;
import appeng.api.storage.MEStorage;
import appeng.api.upgrades.*;
import appeng.api.util.*;
import appeng.core.definitions.AEItems;
import appeng.helpers.MultiCraftingTracker;
import appeng.helpers.externalstorage.GenericStackInv;
import appeng.util.*;
import com.google.common.collect.ImmutableSet;
import com.moakiee.ae2lt.logic.energy.PowerCostUtil;
import java.util.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Independent output-only inventories. The proxy/config layout follows AE2LT's
 * overloaded interface design (Moakiee, LGPL-3.0-only), without InterfaceLogic
 * inheritance, stocking ticks, passive inputs, or reflective field replacement.
 */
public final class OutputInterfaceLogic implements ICraftingRequester {
    private final OutputInterfaceBlockEntity owner;
    private final ConfigInventory config;
    private final GenericStackInv storage;
    private final IUpgradeInventory upgrades;
    private final ConfigManager configManager;
    private final MultiCraftingTracker crafting;
    private boolean networkOperation;
    private boolean suppressUnlimitedCancel;

    public OutputInterfaceLogic(OutputInterfaceBlockEntity owner) {
        this.owner=owner;
        config=new OutputConfig();
        storage=new OutputProxy();
        config.useRegisteredCapacities(); storage.useRegisteredCapacities();
        for(var type:AEKeyTypes.getAll()) { config.setCapacity(type,Long.MAX_VALUE); storage.setCapacity(type,Long.MAX_VALUE); }
        upgrades=UpgradeInventories.forMachine(OutputContent.ITEM.get(),4,owner::configurationChanged);
        configManager=new ConfigManager(owner::configurationChanged);
        configManager.registerSetting(Settings.FUZZY_MODE,FuzzyMode.IGNORE_ALL);
        crafting=new MultiCraftingTracker(this,OutputInterfaceBlockEntity.SLOT_COUNT);
        owner.getMainNode().addService(ICraftingRequester.class,this);
    }
    public ConfigInventory getConfig(){return config;}
    public GenericStackInv getStorage(){return storage;}
    public IUpgradeInventory getUpgrades(){return upgrades;}
    public IConfigManager getConfigManager(){return configManager;}
    public void refreshStorage(){for(int slot=0;slot<storage.size();slot++)storage.getStack(slot);}
    public void setConfigStackSuppressed(int slot,@Nullable GenericStack stack) {
        suppressUnlimitedCancel=true;
        try {config.setStack(slot,stack);}finally{suppressUnlimitedCancel=false;}
    }
    public long capForSlot(int slot) {
        if(slot<0||slot>=config.size()||config.getKey(slot)==null)return 0;
        return owner.isUnlimited(slot)?Long.MAX_VALUE:Math.max(0,config.getAmount(slot));
    }
    private boolean matches(int slot,AEKey key) {
        var configured=config.getKey(slot);
        return configured!=null && key!=null && (configured.equals(key) || upgrades.isInstalled(AEItems.FUZZY_CARD)
            && configured.supportsFuzzyRangeSearch() && configured.fuzzyEquals(key,configManager.getSetting(Settings.FUZZY_MODE)));
    }
    @Nullable private MEStorage network() {
        var grid=owner.getMainNode().getGrid();
        return grid!=null&&owner.getMainNode().isActive()?grid.getStorageService().getInventory():null;
    }
    private KeyCounter networkCandidates(){
        var grid=owner.getMainNode().getGrid();
        if(grid==null||networkOperation)return new KeyCounter();
        networkOperation=true;
        try{return grid.getStorageService().getCachedInventory();}finally{networkOperation=false;}
    }
    public long extractConfigured(int slot,AEKey key,long amount,Actionable mode) {
        return extractConfigured(slot,key,amount,mode,IActionSource.ofMachine(owner));
    }
    public long extractConfigured(int slot,AEKey key,long amount,Actionable mode,IActionSource source) {
        if(slot<0||slot>=config.size()||!matches(slot,key))return 0;
        return extractNetwork(key,Math.min(amount,capForSlot(slot)),mode,source,true);
    }
    long extractNetwork(AEKey key,long amount,Actionable mode,IActionSource source,boolean pay) {
        var network=network();if(network==null)return 0;
        return extractFromNetwork(owner.getMainNode().getGrid(),network,key,amount,mode,source,pay);
    }
    long extractFromNetwork(IGrid grid,MEStorage network,AEKey key,long amount,Actionable mode,IActionSource source,boolean pay) {
        if(networkOperation||key==null||amount<=0||network==null)return 0;
        networkOperation=true;
        try {
            long affordable=pay?PowerCostUtil.maxAffordable(grid,key,amount):amount;
            long extracted=OutputTransferLedger.bounded(network.extract(key,affordable,mode,source),affordable);
            if(pay&&mode==Actionable.MODULATE&&extracted>0)PowerCostUtil.consume(grid,key,extracted);
            return extracted;
        }finally{networkOperation=false;}
    }
    long refundNetwork(AEKey key,long amount) {
        var network=network();if(network==null)return 0;
        return refundIntoNetwork(network,key,amount);
    }
    long refundIntoNetwork(MEStorage network,AEKey key,long amount) {
        if(networkOperation||key==null||amount<=0||network==null)return 0;
        networkOperation=true;
        try{return OutputTransferLedger.bounded(network.insert(key,amount,Actionable.MODULATE,IActionSource.ofMachine(owner)),amount);}
        finally{networkOperation=false;}
    }
    public void tickCrafting() {
        if(!upgrades.isInstalled(AEItems.CRAFTING_CARD)||owner.getMainNode().getGrid()==null||!owner.getMainNode().isActive())return;
        var grid=owner.getMainNode().getGrid();
        for(int slot=0;slot<config.size();slot++) {
            var key=config.getKey(slot);if(key==null||owner.isEnergyKey(key))continue;
            long desired=owner.isUnlimited(slot)?1024L*key.getAmountPerByte():capForSlot(slot);
            long available=grid.getStorageService().getCachedInventory().get(key);
            long deficit=Math.max(0,desired-available);
            if(deficit>0)crafting.handleCrafting(slot,key,deficit,owner.getLevel(),grid.getCraftingService(),IActionSource.ofMachine(owner));
        }
    }
    @Override public IGridNode getActionableNode(){return owner.getActionableNode();}
    @Override public ImmutableSet<ICraftingLink> getRequestedJobs(){return crafting.getRequestedJobs();}
    @Override public void jobStateChange(ICraftingLink link){crafting.jobStateChange(link);owner.changed();}
    /** Only AE's tracked crafting deliveries may insert, never a passive external capability. */
    @Override public long insertCraftedItems(ICraftingLink link,AEKey key,long amount,Actionable mode) {
        if(!getRequestedJobs().contains(link)||networkOperation)return 0;
        var network=network();if(network==null)return 0;
        networkOperation=true;
        try{return network.insert(key,amount,mode,IActionSource.ofMachine(owner));}finally{networkOperation=false;}
    }
    public void save(CompoundTag tag,HolderLookup.Provider registries) {
        saveConfiguration(tag,registries);upgrades.writeToNBT(tag,"OutputUpgrades",registries);crafting.writeToNBT(tag);
    }
    public void load(CompoundTag tag,HolderLookup.Provider registries) {
        loadConfiguration(tag,registries);
        upgrades.readFromNBT(tag,"OutputUpgrades",registries);crafting.readFromNBT(tag);
    }
    public void saveConfiguration(CompoundTag tag,HolderLookup.Provider registries) {
        config.writeToChildTag(tag,"OutputConfig",registries);configManager.writeToNBT(tag,registries);
    }
    public void loadConfiguration(CompoundTag tag,HolderLookup.Provider registries) {
        suppressUnlimitedCancel=true;
        try {
            config.readFromChildTag(tag,"OutputConfig",registries);
            for(int slot=0;slot<config.size();slot++)if(owner.isEnergyKey(config.getKey(slot)))config.setStack(slot,null);
        }finally{suppressUnlimitedCancel=false;}
        configManager.readFromNBT(tag,registries);
    }
    private final class OutputConfig extends ConfigInventory {
        OutputConfig(){super(AEKeyTypes.getAll(),null,Mode.CONFIG_STACKS,OutputInterfaceBlockEntity.SLOT_COUNT,owner::configurationChanged,false);}
        @Override public long getMaxAmount(AEKey key){return Long.MAX_VALUE;}
        @Override public void setStack(int slot,@Nullable GenericStack stack) {
            if(slot<0||slot>=size())return;
            if(stack!=null&&(stack.amount()<0||!isSupportedType(stack.what())||owner.isEnergyKey(stack.what())))return;
            super.setStack(slot,stack);
            if(!suppressUnlimitedCancel)owner.setUnlimited(slot,false);
        }
    }
    private final class OutputProxy extends GenericStackInv {
        OutputProxy(){super(AEKeyTypes.getAll(),null,Mode.STORAGE,OutputInterfaceBlockEntity.SLOT_COUNT);}
        @Override public boolean canInsert(){return false;}
        @Override public boolean canExtract(){return true;}
        @Override public boolean isAllowedIn(int slot,AEKey key){return false;}
        @Override public long getMaxAmount(AEKey key){return Long.MAX_VALUE;}
        @Override public long insert(int slot,AEKey key,long amount,Actionable mode){return 0;}
        @Override public long insert(AEKey key,long amount,Actionable mode,IActionSource source){return 0;}
        @Override public @Nullable GenericStack getStack(int slot) {
            if(owner.getLevel()!=null&&owner.getLevel().isClientSide)return super.getStack(slot);
            var key=config.getKey(slot);
            long amount=key==null?0:extractNetwork(key,capForSlot(slot),Actionable.SIMULATE,IActionSource.ofMachine(owner),false);
            if(amount<=0&&key!=null&&upgrades.isInstalled(AEItems.FUZZY_CARD)&&key.supportsFuzzyRangeSearch()){
                if(!networkOperation)for(var entry:networkCandidates().findFuzzy(key,configManager.getSetting(Settings.FUZZY_MODE))){
                    var variant=entry.getKey();long candidate=extractNetwork(variant,capForSlot(slot),Actionable.SIMULATE,IActionSource.ofMachine(owner),false);
                    if(candidate>0){key=variant;amount=candidate;break;}
                }
            }
            return stacks[slot]=amount>0?new GenericStack(key,amount):null;
        }
        @Override public @Nullable AEKey getKey(int slot){var stack=getStack(slot);return stack==null?null:stack.what();}
        @Override public long getAmount(int slot){var stack=getStack(slot);return stack==null?0:stack.amount();}
        @Override public long extract(int slot,AEKey key,long amount,Actionable mode){return extractConfigured(slot,key,amount,mode);}
        @Override public long extract(AEKey key,long amount,Actionable mode,IActionSource source) {
            if(networkOperation)return 0;
            long cap=0;
            for(int slot=0;slot<size();slot++)if(matches(slot,key)){long add=capForSlot(slot);cap=cap>Long.MAX_VALUE-add?Long.MAX_VALUE:cap+add;}
            return extractNetwork(key,Math.min(amount,cap),mode,source,true);
        }
        @Override public void getAvailableStacks(KeyCounter out) {
            if(networkOperation)return;
            var caps=new LinkedHashMap<AEKey,Long>();
            for(int slot=0;slot<size();slot++){var key=config.getKey(slot);if(key!=null)caps.merge(key,capForSlot(slot),(a,b)->a>Long.MAX_VALUE-b?Long.MAX_VALUE:a+b);}
            var variants=new LinkedHashMap<AEKey,Long>();var candidates=networkCandidates();
            for(var entry:caps.entrySet()){
                var key=entry.getKey();long cap=entry.getValue();variants.merge(key,cap,(a,b)->a>Long.MAX_VALUE-b?Long.MAX_VALUE:a+b);
                if(upgrades.isInstalled(AEItems.FUZZY_CARD)&&key.supportsFuzzyRangeSearch())for(var fuzzy:candidates.findFuzzy(key,configManager.getSetting(Settings.FUZZY_MODE))){
                    if(!fuzzy.getKey().equals(key))variants.merge(fuzzy.getKey(),cap,(a,b)->a>Long.MAX_VALUE-b?Long.MAX_VALUE:a+b);
                }
            }
            variants.forEach((key,cap)->{long amount=extractNetwork(key,cap,Actionable.SIMULATE,IActionSource.ofMachine(owner),false);if(amount>0)out.add(key,amount);});
        }
        @Override public ConfigMenuInventory createMenuWrapper() {
            return new ConfigMenuInventory(this) {
                @Override public boolean isItemValid(int slot,ItemStack stack){return false;}
                @Override public ItemStack insertItem(int slot,ItemStack stack,boolean simulate){return stack;}
                @Override public void setItemDirect(int slot,ItemStack stack) {
                    if(owner.getLevel()!=null&&owner.getLevel().isClientSide)super.setItemDirect(slot,stack);
                }
                @Override public ItemStack extractItem(int slot,int amount,boolean simulate) {
                    var visible=OutputProxy.this.getStack(slot);var key=visible==null?null:visible.what();if(!(key instanceof AEItemKey item))return ItemStack.EMPTY;
                    return item.toStack((int)extractConfigured(slot,key,amount,simulate?Actionable.SIMULATE:Actionable.MODULATE));
                }
            };
        }
    }
}
