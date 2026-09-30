package dev.overloadsim.machine;

import appeng.api.inventories.BaseInternalInventory;
import appeng.api.upgrades.*;
import dev.overloadsim.ModContent;
import dev.overloadsim.SimulationConfig;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

/** Native AE2 upgrade storage, with the pack's live card limit applied to every insertion path. */
final class SimulationUpgrades extends BaseInternalInventory implements IUpgradeInventory {
    private final IUpgradeInventory delegate;
    SimulationUpgrades(Runnable changed){delegate=UpgradeInventories.forMachine(ModContent.CHAMBER_ITEM.get(),4,changed::run);}
    @Override public int size(){return delegate.size();}
    @Override public int getSlotLimit(int slot){return 1;}
    @Override public ItemStack getStackInSlot(int slot){return delegate.getStackInSlot(slot);}
    @Override public void setItemDirect(int slot,ItemStack stack){delegate.setItemDirect(slot,stack);}
    @Override public ItemLike getUpgradableItem(){return delegate.getUpgradableItem();}
    @Override public int getMaxInstalled(ItemLike card){return Math.min(delegate.getMaxInstalled(card),SimulationConfig.CARD_LIMIT.get());}
    @Override public int getInstalledUpgrades(ItemLike card){return Math.min(delegate.getInstalledUpgrades(card),getMaxInstalled(card));}
    @Override public boolean isItemValid(int slot,ItemStack stack){return delegate.getInstalledUpgrades(stack.getItem())<getMaxInstalled(stack.getItem())&&delegate.isItemValid(slot,stack);}
    @Override public ItemStack insertItem(int slot,ItemStack stack,boolean simulate){return isItemValid(slot,stack)?delegate.insertItem(slot,stack,simulate):stack;}
    @Override public ItemStack extractItem(int slot,int amount,boolean simulate){return delegate.extractItem(slot,amount,simulate);}
    @Override public void readFromNBT(CompoundTag tag,String name,HolderLookup.Provider registries){delegate.readFromNBT(tag,name,registries);}
    @Override public void writeToNBT(CompoundTag tag,String name,HolderLookup.Provider registries){delegate.writeToNBT(tag,name,registries);}
    @Override public void sendChangeNotification(int slot){delegate.sendChangeNotification(slot);}
}
