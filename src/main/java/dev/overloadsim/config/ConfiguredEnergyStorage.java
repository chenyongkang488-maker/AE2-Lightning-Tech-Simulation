package dev.overloadsim.config;

import java.util.function.IntSupplier;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.Tag;
import net.neoforged.neoforge.energy.EnergyStorage;

/** A receive-only FE buffer whose operating limit never truncates previously stored energy. */
public final class ConfiguredEnergyStorage extends EnergyStorage {
    private final IntSupplier capacityLimit;
    private final IntSupplier receiveLimit;
    private final Runnable changed;
    public ConfiguredEnergyStorage(IntSupplier capacityLimit,IntSupplier receiveLimit,Runnable changed){
        super(Integer.MAX_VALUE,Integer.MAX_VALUE,0);
        this.capacityLimit=capacityLimit;this.receiveLimit=receiveLimit;this.changed=changed;
    }
    @Override public int getMaxEnergyStored(){return capacityLimit.getAsInt();}
    @Override public boolean canReceive(){return receiveLimit.getAsInt()>0&&energy<getMaxEnergyStored();}
    @Override public int receiveEnergy(int amount,boolean simulate){
        if(amount<=0)return 0;
        int accepted=Math.min(amount,Math.min(receiveLimit.getAsInt(),Math.max(0,getMaxEnergyStored()-energy)));
        if(!simulate&&accepted>0){energy+=accepted;changed.run();}
        return accepted;
    }
    @Override public void deserializeNBT(HolderLookup.Provider provider,Tag tag){
        if(!(tag instanceof IntTag value))throw new IllegalArgumentException("FE must be stored as an integer");
        energy=Math.max(0,value.getAsInt());
    }
}
