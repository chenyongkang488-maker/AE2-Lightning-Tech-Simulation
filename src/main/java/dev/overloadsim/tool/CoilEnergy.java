package dev.overloadsim.tool;

import appeng.api.config.*;
import com.moakiee.ae2lt.device.energy.DeviceEnergyBuffer;
import com.moakiee.ae2lt.device.network.RailgunNetworkBinding;
import dev.overloadsim.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.energy.IEnergyStorage;

public final class CoilEnergy implements DeviceEnergyBuffer {
    public static final CoilEnergy INSTANCE=new CoilEnergy();private CoilEnergy(){}
    public static long read(ItemStack s){return Math.clamp(s.getOrDefault(ModContent.COIL_FE.get(),0L),0L,CoilModules.capacity(s));}
    public static void clamp(ItemStack s){s.set(ModContent.COIL_FE.get(),read(s));}
    @Override public long stored(ItemStack s){return read(s);}
    @Override public long capacity(ItemStack s){return CoilModules.capacity(s);}
    @Override public boolean tryConsume(ItemStack s,ServerPlayer player,long cost){
        if(cost<0||read(s)<cost)return false;if(cost>0)s.set(ModContent.COIL_FE.get(),read(s)-cost);return true;
    }
    @Override public int receiveFe(ItemStack s,int amount,boolean simulate){int got=(int)Math.max(0,Math.min((long)amount,capacity(s)-read(s)));if(!simulate&&got>0)s.set(ModContent.COIL_FE.get(),read(s)+got);return got;}
    @Override public void refill(ItemStack s,ServerPlayer player){
        if(!CoilModules.hasCore(s)||player==null)return;
        var binding=RailgunNetworkBinding.INSTANCE.resolve(s,player);if(!binding.success()||binding.grid()==null)return;
        charge(s,binding.grid());
    }
    public static void charge(ItemStack s,appeng.api.networking.IGrid grid){
        int request=(int)Math.min(CoilModules.capacity(s)-read(s),SimulationConfig.COIL_CHARGE_FE.get());if(request<=0)return;
        double ae=PowerUnit.FE.convertTo(PowerUnit.AE,request);
        double extracted=grid.getEnergyService().extractAEPower(ae,Actionable.MODULATE,PowerMultiplier.ONE);
        receive(s,(int)Math.min(request,Math.floor(PowerUnit.AE.convertTo(PowerUnit.FE,extracted)+1e-6)));
    }
    private static void receive(ItemStack s,int amount){INSTANCE.receiveFe(s,amount,false);}
    @Override public IEnergyStorage asEnergyStorage(ItemStack s){return new IEnergyStorage(){
        public int receiveEnergy(int amount,boolean simulate){return receiveFe(s,amount,simulate);}
        public int extractEnergy(int amount,boolean simulate){return 0;}
        public int getEnergyStored(){return (int)Math.min(Integer.MAX_VALUE,read(s));}
        public int getMaxEnergyStored(){return (int)Math.min(Integer.MAX_VALUE,capacity(s));}
        public boolean canExtract(){return false;}public boolean canReceive(){return true;}
    };}
}
