package dev.overloadsim.tool;

import java.util.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import dev.overloadsim.ModContent;
import com.moakiee.ae2lt.celestweave.ArmorEnergyModuleItem;
import com.moakiee.ae2lt.registry.ModItems;

/** Public loadout API. Returned stacks are copies; every mutation revalidates prerequisites. */
public final class CoilModules {
    private CoilModules(){}
    public static boolean isCoil(ItemStack stack){return stack.getItem() instanceof ResonanceCoilItem;}
    public static List<ItemStack> entries(ItemStack stack){
        return stack.getOrDefault(ModContent.COIL_MODULES.get(),ItemContainerContents.EMPTY).stream().filter(s->!s.isEmpty()).map(ItemStack::copy).toList();
    }
    public static String typeId(ItemStack module){
        if(module.getItem() instanceof CoilModuleItem m)return m.type.id;
        return isEnergy(module)?"energy":"";
    }
    public static boolean isEnergy(ItemStack module){return module.is(ModItems.ENERGY_MODULE_T1.get())||module.is(ModItems.ENERGY_MODULE_T2.get())||module.is(ModItems.ENERGY_MODULE_T3.get());}
    public static boolean has(ItemStack coil,CoilModuleItem.Type type){return entries(coil).stream().anyMatch(s->s.getItem() instanceof CoilModuleItem m&&m.type==type);}
    public static boolean canInstall(ItemStack coil,ItemStack module){
        if(!isCoil(coil)||module.isEmpty())return false;
        String id=typeId(module);if(id.isEmpty()||entries(coil).stream().anyMatch(s->typeId(s).equals(id)))return false;
        return !(module.getItem() instanceof CoilModuleItem m)||!m.type.needsMimic||has(coil,CoilModuleItem.Type.MIMIC);
    }
    /** Installs one copy. The workbench owns decrementing its input stack. */
    public static boolean install(ItemStack coil,ItemStack module){
        if(!canInstall(coil,module))return false;
        var list=new ArrayList<>(entries(coil));list.add(module.copyWithCount(1));save(coil,list);return true;
    }
    public static ItemStack uninstall(ItemStack coil,String typeId){
        if(!isCoil(coil))return ItemStack.EMPTY;
        var list=new ArrayList<>(entries(coil));
        if(typeId.equals(CoilModuleItem.Type.MIMIC.id)&&list.stream().anyMatch(s->s.getItem() instanceof CoilModuleItem m&&m.type.needsMimic))return ItemStack.EMPTY;
        for(int i=0;i<list.size();i++)if(typeId(list.get(i)).equals(typeId)){var removed=list.remove(i);save(coil,list);return removed;}
        return ItemStack.EMPTY;
    }
    private static void save(ItemStack coil,List<ItemStack> list){coil.set(ModContent.COIL_MODULES.get(),ItemContainerContents.fromItems(list));CoilEnergy.clamp(coil);}
    public static ItemStack core(ItemStack coil){return coil.getOrDefault(ModContent.COIL_CORE.get(),ItemStack.EMPTY).copy();}
    public static boolean hasCore(ItemStack coil){return core(coil).is(ModItems.ULTIMATE_OVERLOAD_CORE.get());}
    public static void setCore(ItemStack coil,ItemStack core){
        if(!isCoil(coil))return;
        if(core.isEmpty())coil.remove(ModContent.COIL_CORE.get());
        else if(core.is(ModItems.ULTIMATE_OVERLOAD_CORE.get()))coil.set(ModContent.COIL_CORE.get(),core.copyWithCount(1));
    }
    public static boolean miningReady(ItemStack coil){return hasCore(coil)&&has(coil,CoilModuleItem.Type.MIMIC);}
    public static long capacity(ItemStack coil){return entries(coil).stream().filter(CoilModules::isEnergy).mapToLong(s->((ArmorEnergyModuleItem)s.getItem()).armorCapacityFe()).max().orElse(10_000_000L);}
}
