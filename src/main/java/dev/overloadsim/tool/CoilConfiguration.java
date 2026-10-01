package dev.overloadsim.tool;
import dev.overloadsim.ModContent;
import net.minecraft.world.item.ItemStack;

/** Validated server configuration shared by the native hub and legacy menu. */
public final class CoilConfiguration {
    private CoilConfiguration(){}
    public static boolean apply(ItemStack stack,int action,int value){
        if(!CoilModules.isCoil(stack)||action<0||action>6)return false;
        var module=switch(action){case 0->CoilModuleItem.Type.EXTREME;case 1,2->CoilModuleItem.Type.EFFICIENCY;case 3->CoilModuleItem.Type.FORTUNE;case 4->CoilModuleItem.Type.SILK;default->CoilModuleItem.Type.WRENCH;};
        int max=switch(action){case 2->10;case 3->5;case 6->7;default->1;};
        if(!CoilModules.has(stack,module)||value<0||value>max)return false;
        var s=CoilSettings.read(stack);
        stack.set(ModContent.COIL_SETTINGS.get(),new CoilSettings(action==0?value==1:s.extreme(),action==1?value==1:s.efficiencyEnabled(),action==2?value:s.efficiency(),action==3?value:s.fortune(),action==4?value==1:s.silk(),action==5?value==1:s.wrench(),action==6?value:s.wrenchMode()));return true;
    }
    public static int value(CoilSettings s,int action){return switch(action){case 0->s.extreme()?1:0;case 1->s.efficiencyEnabled()?1:0;case 2->s.efficiency();case 3->s.fortune();case 4->s.silk()?1:0;case 5->s.wrench()?1:0;case 6->s.wrenchMode();default->0;};}
}
