package dev.overloadsim.compat;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import com.moakiee.ae2lt.blockentity.workbench.*;
import com.moakiee.ae2lt.device.*;
import com.moakiee.ae2lt.device.energy.DeviceEnergyBuffer;
import com.moakiee.ae2lt.device.module.DeviceModuleStorage;
import com.moakiee.ae2lt.device.network.*;
import com.moakiee.ae2lt.menu.Ae2ltSlotSemantics;
import com.moakiee.ae2lt.registry.ModItems;
import dev.overloadsim.tool.*;

/** Selected by exact item dispatch; never replaces the upstream RAILGUN registry entry. */
public final class CoilWorkbenchAdapter implements DeviceWorkbenchAdapter,DeviceModuleStorage {
    public static final CoilWorkbenchAdapter INSTANCE=new CoilWorkbenchAdapter();private CoilWorkbenchAdapter(){}
    public DeviceKind deviceKind(){return DeviceKind.RAILGUN;}
    public DeviceModuleStorage moduleStorage(){return this;}
    public DeviceEnergyBuffer energyBuffer(){return CoilEnergy.INSTANCE;}
    public DeviceNetworkBinding networkBinding(){return RailgunNetworkBinding.INSTANCE;}
    public List<StructuralSlotSpec> structuralSlots(){return List.of(new StructuralSlotSpec(0,DeviceSlotType.CORE,Ae2ltSlotSemantics.OVERLOAD_DEVICE_WORKBENCH_CORE));}
    public Predicate<ItemStack> moduleInputValidator(ItemStack d,HolderLookup.Provider r){return s->CoilModules.canInstall(d,s);}
    public List<ItemStack> listModuleEntries(ItemStack d,HolderLookup.Provider r){return CoilModules.entries(d);}
    public boolean canInstallOne(ItemStack d,HolderLookup.Provider r,ItemStack s){return CoilModules.canInstall(d,s);}
    public boolean installOne(ItemStack d,HolderLookup.Provider r,ItemStack s){return CoilModules.install(d,s);}
    public ItemStack uninstallOne(ItemStack d,HolderLookup.Provider r,String id){return CoilModules.uninstall(d,id);}
    public ItemStack uninstallAll(ItemStack d,HolderLookup.Provider r,String id){return CoilModules.uninstall(d,id);}
    public String moduleTypeId(ItemStack s){return CoilModules.typeId(s);}
    public int maxInstallAmount(ItemStack s){return moduleTypeId(s).isEmpty()?0:1;}
    public ItemStack getStructuralSlot(ItemStack d,HolderLookup.Provider r,StructuralSlotSpec s){return CoilModules.core(d);}
    public void setStructuralSlot(ItemStack d,HolderLookup.Provider r,StructuralSlotSpec s,ItemStack v){CoilModules.setCore(d,v);}
    public ItemStack removeStructuralSlot(ItemStack d,HolderLookup.Provider r,StructuralSlotSpec s,int amount){if(amount<=0)return ItemStack.EMPTY;var core=CoilModules.core(d);CoilModules.setCore(d,ItemStack.EMPTY);return core;}
    public boolean canPlaceStructural(ItemStack d,HolderLookup.Provider r,StructuralSlotSpec s,ItemStack v){return CoilModules.isCoil(d)&&v.is(ModItems.ULTIMATE_OVERLOAD_CORE.get());}
    public List<ItemStack> listEntries(ItemStack d){return CoilModules.entries(d);}
    public int getCount(ItemStack d,String id){return (int)listEntries(d).stream().filter(s->moduleTypeId(s).equals(id)).count();}
    public boolean canInstallOne(ItemStack d,ItemStack s){return CoilModules.canInstall(d,s);}
    public boolean installOne(ItemStack d,ItemStack s){return CoilModules.install(d,s);}
    public ItemStack uninstallOne(ItemStack d,String id){return CoilModules.uninstall(d,id);}
    public ItemStack uninstallAll(ItemStack d,String id){return CoilModules.uninstall(d,id);}
    public boolean hasAnyInstalled(ItemStack d){return !listEntries(d).isEmpty();}
    public Stream<ItemStack> installedModuleStacks(ItemStack d){return listEntries(d).stream();}
}
