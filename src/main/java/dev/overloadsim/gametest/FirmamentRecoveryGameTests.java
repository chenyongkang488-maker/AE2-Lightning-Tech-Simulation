package dev.overloadsim.gametest;

import com.moakiee.ae2lt.blockentity.FirmamentConversionCoreBlockEntity;
import dev.overloadsim.OverloadSimulation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(OverloadSimulation.ID)
@PrefixGameTestTemplate(false)
public final class FirmamentRecoveryGameTests {
    @GameTest(template="empty")
    public static void placedOverworldCoreProcessesOriginalRecipeWithoutStarship(GameTestHelper h){
        var pos=h.absolutePos(new BlockPos(3,1,3));
        var block=BuiltInRegistries.BLOCK.get(ResourceLocation.parse("ae2lt:firmament_conversion_core"));
        h.getLevel().setBlockAndUpdate(pos,block.defaultBlockState());
        var core=(FirmamentConversionCoreBlockEntity)h.getLevel().getBlockEntity(pos);
        core.getInventory().setStackInSlot(0,stack("ae2lt:overload_alloy",4));
        core.getInventory().setStackInSlot(1,stack("ae2lt:firmament_mixture",2));
        core.getInventory().setStackInSlot(2,stack("minecraft:netherite_scrap",4));
        for(int i=0;i<41;i++)FirmamentConversionCoreBlockEntity.serverTick(h.getLevel(),pos,block.defaultBlockState(),core);
        int output=0;
        for(int i=3;i<core.getInventory().getSlots();i++)if(core.getInventory().getStackInSlot(i).is(stack("ae2lt:firmament_alloy_ingot",1).getItem()))output+=core.getInventory().getStackInSlot(i).getCount();
        h.assertTrue(output==1,"a recovered core placed outside a starship in the overworld completes the native 40-tick recipe");
        for(int i=0;i<3;i++)h.assertTrue(core.getInventory().getStackInSlot(i).isEmpty(),"native materials consumed exactly once");
        h.succeed();
    }
    private static ItemStack stack(String id,int count){return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)),count);}
}
