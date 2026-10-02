package dev.overloadsim.gametest;

import java.util.*;
import dev.overloadsim.*;
import dev.overloadsim.api.*;
import dev.overloadsim.binding.CrystalBinding;
import dev.overloadsim.data.*;
import dev.overloadsim.machine.MobLoot;
import dev.overloadsim.multiblock.MultiblockData;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(OverloadSimulation.ID)
@PrefixGameTestTemplate(false)
public class MineralCompatibilityGameTests {
    @GameTest(template="empty")
    public static void gemTagsUseOreLootAndRejectHeterogeneousTables(GameTestHelper h){
        var bound=bind(h,BuiltInRegistries.BLOCK.get(ModContent.id("test_gem_storage")));h.assertTrue(bound.is(ModContent.BOUND.get()),"tagged gem storage binds");var data=CrystalDataAccess.read(bound).orElseThrow();
        var p=SimulationResolvers.production(h.getLevel(),data).orElseThrow();for(int i=0;i<64;i++){var drops=p.roll(h.getLevel(),h.absolutePos(new BlockPos(5,1,5)),data,"test");h.assertTrue(drops.size()==1&&drops.getFirst().is(BuiltInRegistries.ITEM.get(ModContent.id("test_gem")))&&drops.getFirst().getCount()>=2&&drops.getFirst().getCount()<=4,"third-party ore table controls quantity");}
        var block=BuiltInRegistries.BLOCK.get(ModContent.id("test_heterogem_storage"));var rejected=bind(h,block);h.assertTrue(rejected.is(ModContent.BLANK.get())&&h.getLevel().getBlockState(h.absolutePos(new BlockPos(3,1,3))).is(block),"heterogeneous ore tables do not consume blocks");h.succeed();
    }
    @GameTest(template="empty")
    public static void explicitMineralAndJavaResolverRemainStable(GameTestHelper h){
        var explicit=bind(h,BuiltInRegistries.BLOCK.get(ModContent.id("test_explicit_block")));var data=CrystalDataAccess.read(explicit).orElseThrow();var p=SimulationResolvers.production(h.getLevel(),data).orElseThrow();
        h.assertTrue(p.roll(h.getLevel(),h.absolutePos(new BlockPos(5,1,5)),data,"test").getFirst().getCount()==3&&p.mineral().orElseThrow().smelting().orElseThrow().count()==4,"explicit quantity and smelt mapping");
        var profile=ModContent.id("test_api_profile");var mineral=new ResolvedMineral(profile,net.minecraft.resources.ResourceLocation.parse("c:apiium"),"overload_sim:test_api_block",Optional.of(ModContent.id("test_raw_material")),2,2,Optional.empty(),Optional.empty(),true);
        SimulationExtensions.registerMineralResolver(ModContent.id("test_api_resolver"),(level,state)->state.is(BuiltInRegistries.BLOCK.get(ModContent.id("test_api_block")))?Optional.of(mineral):Optional.empty());
        SimulationData.invalidate();h.assertTrue(SimulationResolvers.production(h.getLevel(),new CrystalData(profile,Optional.empty(),0,1)).orElseThrow().mineral().orElseThrow().equals(mineral),"Java resolver recovers saved ID before a new bind");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=110)
    public static void highYieldFortuneRespectsBulkCapacity(GameTestHelper h){
        var c=MultiblockGameTests.powered(h,7);var min=c.structure().min();c.invalidateStructure();int index=0;for(int x=1;x<6;x++)for(int z=1;z<6;z++){if(index++<10)h.getLevel().setBlockAndUpdate(min.offset(x,0,z),dev.overloadsim.multiblock.MultiblockContent.FORTUNE.get().defaultBlockState());}c.checkStructure();
        h.runAtTickTime(80,()->{
            for(int slot=0;slot<49;slot++)c.crystals().setStackInSlot(slot,CrystalDataAccess.perfect(new CrystalData(ModContent.id("lapis"),Optional.empty(),0,1)));c.tick();var job=c.batch();
            long count=job.outputs.stream().mapToLong(dev.overloadsim.multiblock.SimulationBatch.Output::count).sum();h.assertTrue(count<=128*1024&&job.inputs.size()<49,"base loot times1024 limits participants to true buffer capacity");for(var output:job.outputs)h.assertTrue(output.prototype().getCount()==1,"bulk prototype remains legal");h.succeed();
        });
    }
    @GameTest(template="empty")
    public static void oreLootQuantitiesAndGeneratedReloadAreStable(GameTestHelper h){
        for(var entry:Map.of("redstone",Items.REDSTONE,"lapis",Items.LAPIS_LAZULI).entrySet()){
            var data=new CrystalData(ModContent.id(entry.getKey()),Optional.empty(),0,1);var production=SimulationResolvers.production(h.getLevel(),data).orElseThrow();var counts=new HashSet<Integer>();
            for(int i=0;i<128;i++){var out=production.roll(h.getLevel(),h.absolutePos(new BlockPos(5,1,5)),data,"test");int count=out.stream().filter(s->s.is(entry.getValue())).mapToInt(ItemStack::getCount).sum();h.assertTrue(count>=4&&count<=(entry.getKey().equals("redstone")?5:9),"unenchanted base ore yield");counts.add(count);}
            h.assertTrue(counts.size()>1,"ore roll retains base quantity variation");
        }
        var auto=new CrystalData(ModContent.id("auto/mineral/c/testium"),Optional.empty(),0,1);var old=SimulationResolvers.production(h.getLevel(),auto).orElseThrow();SimulationData.invalidate();var reloaded=SimulationResolvers.production(h.getLevel(),auto).orElseThrow();
        h.assertTrue(old.equals(reloaded)&&reloaded.roll(h.getLevel(),h.absolutePos(new BlockPos(5,1,5)),auto,"test").getFirst().getCount()==1,"generated profile survives cache rebuild with raw yield1");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=110)
    public static void generatedMineralRunsInMultiblockAndFortuneAppliesOnce(GameTestHelper h){
        var c=MultiblockGameTests.powered(h,4);var min=c.structure().min();c.invalidateStructure();h.getLevel().setBlockAndUpdate(min.offset(1,0,1),dev.overloadsim.multiblock.MultiblockContent.FORTUNE.get().defaultBlockState());h.getLevel().setBlockAndUpdate(min.offset(1,0,2),dev.overloadsim.multiblock.MultiblockContent.SMELTING.get().defaultBlockState());c.checkStructure();
        h.runAtTickTime(80,()->{
            var data=new CrystalData(ModContent.id("auto/mineral/c/testium"),Optional.empty(),0,1);c.crystals().setStackInSlot(0,CrystalDataAccess.perfect(data));
            for(int i=0;i<180;i++)c.tick();var expected=BuiltInRegistries.ITEM.get(ModContent.id("test_ingot"));
            h.assertTrue(c.outputs().prototype(0).is(expected)&&c.outputs().count(0)==4,"raw1 smelts2 and multiplies once by2");h.assertTrue(c.bridge().extract(false,Long.MAX_VALUE,true)==997,"per-crystal base and smelting HV costs");h.succeed();
        });
    }
    @GameTest(template="empty",timeoutTicks=110)
    public static void generatedMineralRunsInSingleChamber(GameTestHelper h){
        var c=SimulationGameTests.capacityMachine(h);c.inventory().setStackInSlot(0,CrystalDataAccess.perfect(new CrystalData(ModContent.id("auto/mineral/c/testium"),Optional.empty(),0,1)));
        h.runAtTickTime(80,()->{for(int i=0;i<250;i++)c.tick();int count=0;for(int slot=4;slot<13;slot++){var out=c.inventory().getStackInSlot(slot);if(out.is(BuiltInRegistries.ITEM.get(ModContent.id("test_raw_material"))))count+=out.getCount();}h.assertTrue(count>0,"single chamber consumes the same generated resolver");h.succeed();});
    }
    static ItemStack bind(GameTestHelper h,Block block){
        fill(h,block);
        return CrystalBinding.bindStructure(h.getLevel(),h.absolutePos(new BlockPos(5,1,5)),new ItemStack(ModContent.BLANK.get()),false);
    }
    static void fill(GameTestHelper h,Block block){
        var center=h.absolutePos(new BlockPos(5,1,5));for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)if(x!=0||z!=0)h.getLevel().setBlockAndUpdate(center.offset(x,0,z),block.defaultBlockState());
    }
    @GameTest(template="empty")
    public static void redstoneAndLapisCanBind(GameTestHelper h){
        for(var block:List.of(Blocks.REDSTONE_BLOCK,Blocks.LAPIS_BLOCK,Blocks.COAL_BLOCK,Blocks.EMERALD_BLOCK,Blocks.QUARTZ_BLOCK)){
            var result=bind(h,block);h.assertTrue(result.is(ModContent.BOUND.get()),"missing mineral binding "+BuiltInRegistries.BLOCK.getKey(block));
        }h.succeed();
    }
    @GameTest(template="empty")
    public static void netheriteProducesDebrisAndTwoScraps(GameTestHelper h){
        var data=new CrystalData(ModContent.id("netherite"),Optional.empty(),0,1);var recipe=SimulationData.recipes(h.getLevel(),SimulationRecipe.Kind.PRODUCTION).stream().filter(r->r.value().data().profile().equals(data.profile())).findFirst().orElseThrow().value();
        var out=MobLoot.roll(h.getLevel(),h.absolutePos(new BlockPos(5,1,5)),data,recipe.data().production());
        h.assertTrue(out.size()==1&&out.getFirst().is(Items.ANCIENT_DEBRIS)&&out.getFirst().getCount()==1,"netherite mineral produces debris");
        var smelt=MultiblockData.smelt(h.getLevel(),out.getFirst());h.assertTrue(smelt!=null&&smelt.result().equals(BuiltInRegistries.ITEM.getKey(Items.NETHERITE_SCRAP))&&smelt.count()==2,"debris becomes two scraps");h.succeed();
    }
    @GameTest(template="empty")
    public static void thirdPartyRawTagsBindAndCultivate(GameTestHelper h){
        var block=BuiltInRegistries.BLOCK.get(ModContent.id("test_raw_block"));var other=BuiltInRegistries.BLOCK.get(ModContent.id("test_raw_block_other"));var result=bind(h,block);
        h.assertTrue(result.is(ModContent.BOUND.get()),"tagged third-party raw storage recognized");var data=CrystalDataAccess.read(result).orElseThrow();
        h.assertTrue(data.profile().equals(ModContent.id("auto/mineral/c/testium")),"stable material profile ID");
        for(int i=0;i<10;i++)result=CrystalBinding.cultivate(h.getLevel(),h.absolutePos(new BlockPos(5,1,5)),result,false);
        h.assertTrue(result.is(ModContent.PERFECT.get()),"generated profile cultivates");
        var center=h.absolutePos(new BlockPos(5,1,5));fill(h,block);for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)if((x+z)%2==0&&(x!=0||z!=0))h.getLevel().setBlockAndUpdate(center.offset(x,0,z),other.defaultBlockState());
        h.assertTrue(CrystalBinding.bindStructure(h.getLevel(),center,new ItemStack(ModContent.BLANK.get()),false).is(ModContent.BOUND.get()),"different blocks of same unambiguous raw material bind");h.succeed();
    }
    @GameTest(template="empty")
    public static void ambiguousRawTagsPreserveMaterials(GameTestHelper h){
        var block=BuiltInRegistries.BLOCK.get(ModContent.id("test_ambiguous_block"));var result=bind(h,block);h.assertTrue(result.is(ModContent.BLANK.get()),"ambiguous binding fails closed");
        var center=h.absolutePos(new BlockPos(5,1,5));for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)if(x!=0||z!=0)h.assertBlockPresent(block,new BlockPos(5+x,1,5+z));h.succeed();
    }
}
