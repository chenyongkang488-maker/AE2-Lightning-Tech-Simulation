package dev.overloadsim.tool;

import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionSource;
import com.moakiee.ae2lt.device.energy.DeviceEnergyBuffer;
import com.moakiee.ae2lt.device.network.RailgunNetworkBinding;
import dev.overloadsim.*;
import dev.overloadsim.compat.AppliedFluxEnergy;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.energy.IEnergyStorage;

public final class CoilEnergy implements DeviceEnergyBuffer {
    private static final String CREDIT_TAG="overload_sim:network_fe_credit";
    public static final CoilEnergy INSTANCE=new CoilEnergy();private CoilEnergy(){}
    public static long read(ItemStack s){return Math.clamp(s.getOrDefault(ModContent.COIL_FE.get(),0L),0L,40_000_000_000L);}
    public static void clamp(ItemStack s){s.set(ModContent.COIL_FE.get(),read(s));}
    @Override public long stored(ItemStack s){return read(s);}
    @Override public long capacity(ItemStack s){return CoilModules.capacity(s);}
    @Override public boolean tryConsume(ItemStack s,ServerPlayer player,long cost){
        if(cost<0||read(s)<cost)return false;if(cost>0)s.set(ModContent.COIL_FE.get(),read(s)-cost);return true;
    }
    @Override public int receiveFe(ItemStack s,int amount,boolean simulate){int got=(int)Math.max(0,Math.min((long)amount,capacity(s)-read(s)));if(!simulate&&got>0)s.set(ModContent.COIL_FE.get(),read(s)+got);return got;}
    @Override public void refill(ItemStack s,ServerPlayer player){
        settleCredit(s);
        if(!CoilModules.hasCore(s)||player==null)return;
        var binding=RailgunNetworkBinding.INSTANCE.resolve(s,player);if(!binding.success()||binding.grid()==null)return;
        charge(s,binding.grid(),IActionSource.ofPlayer(player));
    }
    public static void charge(ItemStack s,IGrid grid){charge(s,grid,IActionSource.empty());}
    public static void charge(ItemStack s,IGrid grid,IActionSource source){
        settleCredit(s);
        int request=(int)Math.min(Math.max(0,CoilModules.capacity(s)-read(s)),SimulationConfig.COIL_CHARGE_FE.get());if(request<=0)return;
        var transfer=AppliedFluxEnergy.extractInto(grid,source,request,amount->INSTANCE.receiveFe(s,(int)amount,false));
        if(transfer.credit()>0)setCredit(s,Math.addExact(credit(s),transfer.credit()));
    }
    private static long credit(ItemStack s){return Math.max(0,s.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getLong(CREDIT_TAG));}
    private static void setCredit(ItemStack s,long amount){
        var tag=s.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();if(amount>0)tag.putLong(CREDIT_TAG,amount);else tag.remove(CREDIT_TAG);
        if(tag.isEmpty())s.remove(DataComponents.CUSTOM_DATA);else s.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
    }
    private static void settleCredit(ItemStack s){long held=credit(s);if(held==0)return;int accepted=INSTANCE.receiveFe(s,(int)Math.min(Integer.MAX_VALUE,held),false);if(accepted>0)setCredit(s,held-accepted);}
    @Override public IEnergyStorage asEnergyStorage(ItemStack s){return new IEnergyStorage(){
        public int receiveEnergy(int amount,boolean simulate){return receiveFe(s,amount,simulate);}
        public int extractEnergy(int amount,boolean simulate){return 0;}
        public int getEnergyStored(){return (int)Math.min(Integer.MAX_VALUE,read(s));}
        public int getMaxEnergyStored(){return (int)Math.min(Integer.MAX_VALUE,capacity(s));}
        public boolean canExtract(){return false;}public boolean canReceive(){return read(s)<capacity(s);}
    };}
}
