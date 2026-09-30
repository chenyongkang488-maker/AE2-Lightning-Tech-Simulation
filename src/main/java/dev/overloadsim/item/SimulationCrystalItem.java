package dev.overloadsim.item;
import java.util.List;
import dev.overloadsim.ModContent;
import dev.overloadsim.api.CrystalDataAccess;
import dev.overloadsim.data.SimulationData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
public class SimulationCrystalItem extends Item {
    public SimulationCrystalItem(Properties p){super(p);}
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> lines,TooltipFlag flag){
        CrystalDataAccess.read(stack).ifPresent(data->{
            lines.add(Component.translatable("tooltip.overload_sim.profile",Component.translatable("profile."+data.profile().getNamespace()+"."+data.profile().getPath())).withStyle(ChatFormatting.LIGHT_PURPLE));
            data.entityType().ifPresent(id->lines.add(Component.translatable("entity."+id.getNamespace()+"."+id.getPath())));
            if(stack.is(ModContent.BOUND.get())) lines.add(Component.translatable("tooltip.overload_sim.progress",data.strikes()));
            if(SimulationData.profile(data.profile()).isEmpty() && context.level()!=null && !context.level().isClientSide()) lines.add(Component.translatable("tooltip.overload_sim.missing").withStyle(ChatFormatting.RED));
        });
        lines.add(Component.translatable("tooltip.overload_sim.guide").withStyle(ChatFormatting.GRAY));
    }
}
