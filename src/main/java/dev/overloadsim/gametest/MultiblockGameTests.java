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
    public static void multiblockStructureExists(GameTestHelper helper) {
        for(int size=3;size<=7;size++){
            var min=helper.absolutePos(new net.minecraft.core.BlockPos(2,1,2));
            var controller=build(helper,min,size);controller.checkStructure();
            helper.assertTrue(controller.structure()!=null&&controller.structure().capacity()==size*size,"form outer size "+size);
            helper.assertTrue(controller.structure().glass().size()==5*(size-2)*(size-2),"five glass faces");
            var pane=controller.structure().glass().getFirst();
            helper.getLevel().setBlockAndUpdate(pane,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
            helper.assertTrue(controller.structure()==null,"breaking any glass invalidates");
            for(int x=0;x<size;x++)for(int y=0;y<size;y++)for(int z=0;z<size;z++)
                helper.getLevel().setBlockAndUpdate(min.offset(x,y,z),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        }
        helper.succeed();
    }
    public static dev.overloadsim.multiblock.SimulationControllerBlockEntity build(GameTestHelper h,net.minecraft.core.BlockPos min,int n){
        var glass=net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.parse("ae2:quartz_vibrant_glass"));
        for(int x=0;x<n;x++)for(int y=0;y<n;y++)for(int z=0;z<n;z++){
            int b=(x==0||x==n-1?1:0)+(y==0||y==n-1?1:0)+(z==0||z==n-1?1:0);
            var block=b>=2||y==0?dev.overloadsim.multiblock.MultiblockContent.FRAME.get():b==1?glass:net.minecraft.world.level.block.Blocks.AIR;
            h.getLevel().setBlockAndUpdate(min.offset(x,y,z),block.defaultBlockState());
        }
        var pos=min.offset(1,0,0);h.getLevel().setBlockAndUpdate(pos,dev.overloadsim.multiblock.MultiblockContent.CONTROLLER.get().defaultBlockState());
        return (dev.overloadsim.multiblock.SimulationControllerBlockEntity)h.getLevel().getBlockEntity(pos);
    }
    @GameTest(template="empty")
    public static void structureRejectsSolidInteriorAndDuplicateSpecialModules(GameTestHelper h){
        var min=h.absolutePos(new net.minecraft.core.BlockPos(2,1,2));var c=build(h,min,4);
        h.getLevel().setBlockAndUpdate(min.offset(1,1,1),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());c.checkStructure();
        h.assertTrue(c.structure()==null,"solid interior rejected");
        h.getLevel().setBlockAndUpdate(min.offset(1,1,1),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        h.getLevel().setBlockAndUpdate(min.offset(1,0,1),dev.overloadsim.multiblock.MultiblockContent.OVERLOAD.get().defaultBlockState());
        h.getLevel().setBlockAndUpdate(min.offset(2,0,1),dev.overloadsim.multiblock.MultiblockContent.OVERLOAD.get().defaultBlockState());c.checkStructure();
        h.assertTrue(c.structure()==null,"duplicate overload rejected");
        h.getLevel().setBlockAndUpdate(min.offset(2,0,1),dev.overloadsim.multiblock.MultiblockContent.SMELTING.get().defaultBlockState());c.checkStructure();
        h.assertTrue(c.structure()!=null&&c.structure().duration()==90&&c.structure().smelting(),"distinct modules valid");
        var pane=c.structure().glass().getLast();c.invalidateStructure();
        h.assertTrue(h.getLevel().getBlockState(pane).is(net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.parse("ae2:quartz_vibrant_glass"))),"original glass restored");
        c.checkStructure();var tag=c.saveWithoutMetadata(h.getLevel().registryAccess());c.loadTag(tag,h.getLevel().registryAccess());c.checkStructure();c.invalidateStructure();
        h.assertTrue(h.getLevel().getBlockState(pane).is(net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.parse("ae2:quartz_vibrant_glass"))),"reloaded formation retains original state");h.succeed();
    }
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
