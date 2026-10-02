package dev.overloadsim.item;
import java.util.List;
import dev.overloadsim.ModContent;
import dev.overloadsim.api.CrystalDataAccess;
import dev.overloadsim.data.SimulationData;
import dev.overloadsim.data.SimulationRecipe;
import java.util.Locale;
import java.util.Comparator;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
public class SimulationCrystalItem extends Item {
    public SimulationCrystalItem(Properties p){super(p);}
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> lines,TooltipFlag flag){
        var stored=CrystalDataAccess.read(stack);int required=10;
        if(stored.isPresent()&&context.level()!=null){
            var recipes=context.level().getRecipeManager().getAllRecipesFor(ModContent.RECIPE_TYPES.get(SimulationRecipe.Kind.CULTIVATION).get()).stream().filter(h->h.value().data().profile().equals(stored.get().profile())||h.value().data().profile().equals(ModContent.id("any"))).sorted(Comparator.<RecipeHolder<SimulationRecipe>>comparingInt(h->h.value().data().priority()).reversed().thenComparing(h->h.id().toString())).toList();
            required=SimulationData.select(recipes).map(h->h.value().data().cultivation().required()).orElse(10);
        }
        double progress=stack.is(ModContent.PERFECT.get())?1:Math.clamp(stored.map(d->(double)d.strikes()).orElse(0d)/required,0d,1d);
        int stage=progress>=.85?3:progress>=.55?2:progress>=.20?1:0;
        lines.add(Component.translatable("item.ae2lt.electro_chime_crystal.percent",String.format(Locale.ROOT,"%.1f",progress*100)).withStyle(ChatFormatting.AQUA));
        lines.add(Component.translatable("item.ae2lt.electro_chime_crystal.stage",Component.translatable("item.ae2lt.electro_chime_crystal.stage."+stage)).withStyle(ChatFormatting.LIGHT_PURPLE));
        CrystalDataAccess.read(stack).ifPresent(data->{
            var view=SimulationData.profile(data.profile());var label=data.profile().getPath().startsWith("auto/mineral/")?view.map(v->net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(v.icon()).map(i->new ItemStack(i).getHoverName()).orElse(Component.literal(v.translationKey()))).orElse(Component.literal(data.profile().toString())):Component.translatable("profile."+data.profile().getNamespace()+"."+data.profile().getPath());
            lines.add(Component.translatable("tooltip.overload_sim.profile",label).withStyle(ChatFormatting.GRAY));
            data.entityType().ifPresent(id->lines.add(Component.translatable("entity."+id.getNamespace()+"."+id.getPath())));
            if(SimulationData.profile(data.profile()).isEmpty() && context.level()!=null && !context.level().isClientSide()) lines.add(Component.translatable("tooltip.overload_sim.missing").withStyle(ChatFormatting.RED));
        });
    }
}
