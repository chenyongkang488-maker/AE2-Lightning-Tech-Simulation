package dev.overloadsim.compat;
import java.util.*;
import com.moakiee.ae2lt.item.railgun.*;
import com.moakiee.ae2lt.menu.hub.*;
import dev.overloadsim.tool.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class CoilHubAccess {
    private CoilHubAccess(){}
    public interface Configuration {boolean overloadSim$configure(ServerPlayer player,int action,int value);}
    public static ItemStack weapon(Player player){
        for(var s:List.of(player.getMainHandItem(),player.getOffhandItem()))if(CoilModules.isCoil(s)||s.getItem() instanceof ElectromagneticRailgunItem)return s;
        return ItemStack.EMPTY;
    }
    public static void open(ServerPlayer p){if(CoilModules.isCoil(weapon(p)))DeviceHubHost.open(p,DeviceHubMenu.TAB_RAILGUN);}
    public static DeviceStatusModel status(ItemStack stack,ServerPlayer p,int selected){
        var modules=CoilModules.entries(stack).stream().map(s->new DeviceStatusModel.ModuleInfo(s.getDescriptionId(),1,true)).toList();
        var settings=CoilSettings.read(stack);var configs=new ArrayList<DeviceStatusModel.ModuleConfigInfo>();
        for(int i=0;i<7;i++)configs.add(new DeviceStatusModel.ModuleConfigInfo(Integer.toString(i),"",Integer.toString(CoilConfiguration.value(settings,i)),editable(stack,i)));
        boolean power=CoilModules.hasCore(stack)&&(CoilEnergy.read(stack)>0||com.moakiee.ae2lt.device.network.RailgunNetworkBinding.INSTANCE.resolve(stack,p).success());
        return new DeviceStatusModel(stack.getHoverName().getString(),CoilModules.hasCore(stack),power,modules,selected,configs,false,false,false,false,RailgunExecutionMode.NORMAL,false);
    }
    public static boolean editable(ItemStack stack,int action){return CoilModules.has(stack,switch(action){case 0->CoilModuleItem.Type.EXTREME;case 1,2->CoilModuleItem.Type.EFFICIENCY;case 3->CoilModuleItem.Type.FORTUNE;case 4->CoilModuleItem.Type.SILK;default->CoilModuleItem.Type.WRENCH;});}
}
