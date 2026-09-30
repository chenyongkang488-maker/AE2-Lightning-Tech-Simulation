package dev.overloadsim.tool;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;

/** Addon-specific modules are accepted only by the coil's workbench adapter. */
public final class CoilModuleItem extends Item {
    public enum Type {
        EXTREME("extreme_voltage_module",false), MIMIC("mimic_tool_module",false),
        ULTIMATE("ultimate_destruction_module",true), EFFICIENCY("efficiency_module",true),
        FORTUNE("fortune_module",true), SILK("silk_touch_module",true);
        public final String id;public final boolean needsMimic;
        Type(String id,boolean needsMimic){this.id=id;this.needsMimic=needsMimic;}
    }
    public final Type type;
    public CoilModuleItem(Type type){super(new Properties());this.type=type;}
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> lines,TooltipFlag flag){
        lines.add(Component.translatable("tooltip.overload_sim.coil.module").withStyle(ChatFormatting.GRAY));
        if(type.needsMimic)lines.add(Component.translatable("tooltip.overload_sim.coil.requires_mimic").withStyle(ChatFormatting.LIGHT_PURPLE));
        lines.add(Component.translatable("tooltip.overload_sim.coil."+type.id).withStyle(ChatFormatting.GRAY));
    }
}
