package dev.overloadsim.tool;
import dev.overloadsim.compat.MekanismCoilCompat;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.ItemAbility;

public final class CoilWrench {
    private CoilWrench(){}
    public static boolean active(ItemStack stack){return CoilModules.isCoil(stack)&&CoilSettings.read(stack).wrenchActive(stack);}
    public static boolean mekanism(){return ModList.get().isLoaded("mekanism");}
    public static boolean ability(ItemStack stack,ItemAbility ability){return active(stack)&&mekanism()&&MekanismCoilCompat.ability(stack,ability);}
    public static InteractionResult use(UseOnContext context){
        var p=context.getPlayer();if(p==null||!active(context.getItemInHand()))return InteractionResult.FAIL;
        if(!context.getLevel().mayInteract(p,context.getClickedPos())||!p.mayUseItemAt(context.getClickedPos(),context.getClickedFace(),context.getItemInHand()))return InteractionResult.FAIL;
        return mekanism()?MekanismCoilCompat.use(context):InteractionResult.PASS;
    }
}
