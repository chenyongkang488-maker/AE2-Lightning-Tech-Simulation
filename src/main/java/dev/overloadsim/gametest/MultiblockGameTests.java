package dev.overloadsim.gametest;

import dev.overloadsim.OverloadSimulation;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(OverloadSimulation.ID)
@PrefixGameTestTemplate(false)
public class MultiblockGameTests {
    @GameTest(template = "empty")
    public static void bulkBufferExists(GameTestHelper helper) {
        var buffer=new dev.overloadsim.multiblock.BulkOutputBuffer();
        var iron=new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_INGOT);
        helper.assertTrue(buffer.insert(iron,2048,false)==2048&&buffer.count(0)==1024&&buffer.count(1)==1024,"bulk capacity");
        helper.assertTrue(buffer.prototype(0).getCount()==1&&buffer.extract(0,1024,true).getCount()==64,"legal prototypes and extraction");
        var named=iron.copy();named.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("different"));
        helper.assertTrue(buffer.insert(named,1024,false)==1024&&buffer.count(2)==1024,"components remain distinct");
        var restored=new dev.overloadsim.multiblock.BulkOutputBuffer();restored.load(buffer.save(helper.getLevel().registryAccess()),helper.getLevel().registryAccess());
        helper.assertTrue(restored.count(0)==1024&&net.minecraft.world.item.ItemStack.isSameItemSameComponents(named,restored.prototype(2)),"save quantities and components");
        restored.insert(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND_PICKAXE),100,false);
        helper.assertTrue(restored.extract(3,100,false).getCount()==1&&restored.count(3)==99,"unstackable extraction legal");
        var full=new dev.overloadsim.multiblock.BulkOutputBuffer();full.insert(iron,131072,false);
        helper.assertTrue(full.insert(iron,1,true)==0&&full.count(127)==1024,"simulation preserves full buffer");
        helper.succeed();
    }
}
