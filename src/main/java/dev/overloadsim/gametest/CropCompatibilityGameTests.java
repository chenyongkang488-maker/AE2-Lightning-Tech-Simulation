package dev.overloadsim.gametest;

import java.util.*;
import dev.overloadsim.*;
import dev.overloadsim.api.*;
import dev.overloadsim.binding.CrystalBinding;
import dev.overloadsim.data.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(OverloadSimulation.ID)
@PrefixGameTestTemplate(false)
public class CropCompatibilityGameTests {
    private static Block crop(){return BuiltInRegistries.BLOCK.get(ModContent.id("test_farmland_crop"));}
    private static BlockPos center(GameTestHelper h){return h.absolutePos(new BlockPos(5,1,5));}
    static ItemStack bind(GameTestHelper h,BlockState plant,BlockState soil){
        var p=center(h);for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)if(x!=0||z!=0){var g=p.offset(x,0,z);h.getLevel().setBlock(g.below(),Blocks.STONE.defaultBlockState(),2);h.getLevel().setBlock(g,soil,2);h.getLevel().setBlock(g.above(),plant,2);}
        return CrystalBinding.bindStructure(h.getLevel(),p,new ItemStack(ModContent.BLANK.get()),false);
    }
    @GameTest(template="empty")
    public static void arbitraryCropConsumesOnlyPlantsAndPreservesSoil(GameTestHelper h){
        var result=bind(h,crop().defaultBlockState(),Blocks.FARMLAND.defaultBlockState());h.assertTrue(result.is(ModContent.BOUND.get()),"unlisted CropBlock must bind");
        for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)if(x!=0||z!=0){var p=center(h).offset(x,0,z);h.assertTrue(h.getLevel().getBlockState(p).is(Blocks.FARMLAND)&&h.getLevel().getBlockState(p.above()).isAir(),"only the 24 plants are consumed");}h.succeed();
    }
    @GameTest(template="empty")
    public static void dynamicCropDropsUseMatureState(GameTestHelper h){
        var stack=bind(h,crop().defaultBlockState(),Blocks.FARMLAND.defaultBlockState());var data=CrystalDataAccess.read(stack).orElseThrow();var outputs=SimulationResolvers.production(h.getLevel(),data).orElseThrow().roll(h.getLevel(),center(h),data,"test");
        h.assertTrue(outputs.stream().anyMatch(s->s.is(Items.APPLE)&&s.getCount()==2),"Java getDrops is called with mature state, not an empty JSON table");h.assertTrue(outputs.stream().anyMatch(s->s.is(Items.WHEAT_SEEDS)),"normal harvest seeds are preserved");h.succeed();
    }
    @GameTest(template="empty")
    public static void farmBlockSubclassesAreAccepted(GameTestHelper h){
        var soil=BuiltInRegistries.BLOCK.get(ModContent.id("test_farmland")).defaultBlockState();var stack=bind(h,crop().defaultBlockState(),soil);h.assertTrue(stack.is(ModContent.BOUND.get()),"modded FarmBlock subclass is valid soil");h.succeed();
    }
    @GameTest(template="empty")
    public static void nonCropBlockGrowthPropertyIsHarvested(GameTestHelper h){
        var plant=BuiltInRegistries.BLOCK.get(ModContent.id("test_growth_crop")).defaultBlockState();var data=CrystalDataAccess.read(bind(h,plant,Blocks.FARMLAND.defaultBlockState())).orElseThrow();
        var outputs=SimulationResolvers.production(h.getLevel(),data).orElseThrow().roll(h.getLevel(),center(h),data,"test");h.assertTrue(outputs.stream().anyMatch(s->s.is(Items.CARROT)&&s.getCount()==3),"non-CropBlock plant with growth property also reaches maturity");h.succeed();
    }
    @GameTest(template="empty")
    public static void cultivatedCropSurvivesColdProfileLookup(GameTestHelper h){
        var stack=bind(h,crop().defaultBlockState(),Blocks.FARMLAND.defaultBlockState());for(int i=0;i<10;i++)stack=CrystalBinding.cultivate(h.getLevel(),center(h),stack,false);
        h.assertTrue(stack.is(ModContent.PERFECT.get()),"generated crop cultivates after ten bolts");var data=CrystalDataAccess.read(stack).orElseThrow();SimulationData.invalidate();h.assertTrue(SimulationData.profile(h.getLevel(),data.profile()).isPresent()&&SimulationResolvers.production(h.getLevel(),data).isPresent(),"saved crop profile recovers after cache invalidation");h.succeed();
    }
    @GameTest(template="empty")
    public static void mixedCropPlotIsNotConsumed(GameTestHelper h){
        bind(h,crop().defaultBlockState(),Blocks.FARMLAND.defaultBlockState());
        var p=center(h);for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)if(x!=0||z!=0)h.getLevel().setBlock(p.offset(x,1,z),crop().defaultBlockState(),2);
        h.getLevel().setBlock(p.offset(1,1,0),Blocks.CARROTS.defaultBlockState(),2);var blank=new ItemStack(ModContent.BLANK.get());var result=CrystalBinding.bindStructure(h.getLevel(),p,blank,false);
        h.assertTrue(result==blank&&h.getLevel().getBlockState(p.offset(-1,1,0)).is(crop()),"mixed crop species refuse without consuming any plant");h.succeed();
    }
    @GameTest(template="empty")
    public static void ordinaryBlocksOnFarmlandAreNotCrops(GameTestHelper h){
        h.assertTrue(bind(h,Blocks.STONE.defaultBlockState(),Blocks.FARMLAND.defaultBlockState()).is(ModContent.BLANK.get()),"solid building blocks must not be classified as crops");h.succeed();
    }
    @GameTest(template="empty")
    public static void existingWheatRecipeHasPriority(GameTestHelper h){
        var data=CrystalDataAccess.read(bind(h,Blocks.WHEAT.defaultBlockState(),Blocks.FARMLAND.defaultBlockState())).orElseThrow();h.assertTrue(data.profile().equals(ModContent.id("wheat")),"existing vanilla crop profile is unchanged");var out=SimulationResolvers.production(h.getLevel(),data).orElseThrow().roll(h.getLevel(),center(h),data,"test");h.assertTrue(out.stream().anyMatch(s->s.is(Items.WHEAT)&&s.getCount()==2),"existing recipe yield has priority over automatic harvest");h.succeed();
    }
    @GameTest(template="empty")
    public static void realMysticalAgricultureHarvestsEssence(GameTestHelper h){
        var plant=BuiltInRegistries.BLOCK.getOptional(ResourceLocation.parse("mysticalagriculture:diamond_crop"));if(plant.isEmpty()){h.succeed();return;}
        var soil=BuiltInRegistries.BLOCK.get(ResourceLocation.parse("mysticalagriculture:inferium_farmland"));var stack=bind(h,plant.get().defaultBlockState(),soil.defaultBlockState());h.assertTrue(stack.is(ModContent.BOUND.get()),"installed MA diamond seed plant binds on MA farmland");var data=CrystalDataAccess.read(stack).orElseThrow();var output=SimulationResolvers.production(h.getLevel(),data).orElseThrow().roll(h.getLevel(),center(h),data,"test");
        h.assertTrue(output.stream().anyMatch(s->BuiltInRegistries.ITEM.getKey(s.getItem()).toString().equals("mysticalagriculture:diamond_essence")),"real MA Java harvest yields diamond essence");h.succeed();
    }
    @GameTest(template="empty")
    public static void mixedGrowthStagesAndAdultFlowerShareOneCrop(GameTestHelper h){
        var p=center(h);for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)if(x!=0||z!=0){var g=p.offset(x,0,z);h.getLevel().setBlock(g.below(),Blocks.STONE.defaultBlockState(),2);h.getLevel().setBlock(g,Blocks.FARMLAND.defaultBlockState(),2);h.getLevel().setBlock(g.above(),(x+z)%2==0?Blocks.TORCHFLOWER_CROP.defaultBlockState():Blocks.TORCHFLOWER.defaultBlockState(),2);}
        var stack=CrystalBinding.bindStructure(h.getLevel(),p,new ItemStack(ModContent.BLANK.get()),false);h.assertTrue(stack.is(ModContent.BOUND.get()),"seedling and mature flower remain the same crop species");var data=CrystalDataAccess.read(stack).orElseThrow();h.assertTrue(SimulationResolvers.production(h.getLevel(),data).orElseThrow().roll(h.getLevel(),p,data,"test").stream().anyMatch(s->s.is(Items.TORCHFLOWER)),"mature flower is produced");h.succeed();
    }
    @GameTest(template="empty")
    public static void growthStagesDoNotNeedToMatch(GameTestHelper h){
        bind(h,crop().defaultBlockState(),Blocks.FARMLAND.defaultBlockState());var p=center(h);for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)if(x!=0||z!=0)h.getLevel().setBlock(p.offset(x,1,z),((CropBlock)crop()).getStateForAge(Math.floorMod(x+z,8)),2);
        h.assertTrue(CrystalBinding.bindStructure(h.getLevel(),p,new ItemStack(ModContent.BLANK.get()),false).is(ModContent.BOUND.get()),"24 plants can have different growth ages");h.succeed();
    }
    @GameTest(template="empty")
    public static void stemsProduceFruitInsteadOfOnlySeeds(GameTestHelper h){
        var data=CrystalDataAccess.read(bind(h,Blocks.PUMPKIN_STEM.defaultBlockState(),Blocks.FARMLAND.defaultBlockState())).orElseThrow();var output=SimulationResolvers.production(h.getLevel(),data).orElseThrow().roll(h.getLevel(),center(h),data,"test");h.assertTrue(output.stream().anyMatch(s->s.is(Items.PUMPKIN)),"stem simulation harvests the fruit");h.succeed();
    }
    @GameTest(template="empty")
    public static void generatedCropRunsInMultiblock(GameTestHelper h){
        var controller=MultiblockGameTests.powered(h,4);var data=new CrystalData(ModContent.id("auto/crop/overload_sim/test_farmland_crop"),Optional.empty(),0,1);
        h.runAtTickTime(80,()->{controller.crystals().setStackInSlot(0,CrystalDataAccess.perfect(data));for(int i=0;i<210;i++)controller.tick();h.assertTrue(controller.outputs().prototype(0).is(Items.APPLE)&&controller.outputs().count(0)==2,"multiblock yields the mature crop once");h.succeed();});
    }
    @GameTest(template="empty")
    public static void generatedCropRunsInSingleChamber(GameTestHelper h){
        var single=SimulationGameTests.capacityMachine(h);var data=new CrystalData(ModContent.id("auto/crop/overload_sim/test_farmland_crop"),Optional.empty(),0,1);single.inventory().setStackInSlot(0,CrystalDataAccess.perfect(data));
        h.runAtTickTime(80,()->{for(int i=0;i<210;i++)single.tick();boolean found=false;for(int i=4;i<13;i++)if(single.inventory().getStackInSlot(i).is(Items.APPLE))found=true;h.assertTrue(found,"single chamber runs the same generated crop");h.succeed();});
    }
    @GameTest(template="empty")
    public static void maturityApiCanOverrideUnusualGrowth(GameTestHelper h){
        var plant=BuiltInRegistries.BLOCK.get(ModContent.id("test_api_crop"));SimulationExtensions.registerCropMaturityResolver(ModContent.id("test_custom_growth"),(level,state)->state.is(plant)?Optional.of(Blocks.DIAMOND_BLOCK.defaultBlockState()):Optional.empty());
        var data=new CrystalData(ModContent.id("auto/crop/overload_sim/test_api_crop"),Optional.empty(),0,1);var drops=SimulationResolvers.production(h.getLevel(),data).orElseThrow().roll(h.getLevel(),center(h),data,"test");h.assertTrue(drops.stream().anyMatch(s->s.is(Items.DIAMOND_BLOCK)),"maturity API supplies an unusual harvest state");h.succeed();
    }
    @GameTest(template="empty")
    public static void explicitProductionOverridesAutomaticHarvest(GameTestHelper h){
        var plant=BuiltInRegistries.BLOCK.get(ModContent.id("test_explicit_crop"));var data=CrystalDataAccess.read(bind(h,plant.defaultBlockState(),Blocks.FARMLAND.defaultBlockState())).orElseThrow();
        var output=SimulationResolvers.production(h.getLevel(),data).orElseThrow().roll(h.getLevel(),center(h),data,"test");h.assertTrue(output.size()==1&&output.getFirst().is(Items.DIAMOND)&&output.getFirst().getCount()==3,"datapack production completely overrides automatic harvest");h.succeed();
    }
    @GameTest(template="empty")
    public static void naturalOnlyCropRecipeCannotFallThrough(GameTestHelper h){
        var plant=BuiltInRegistries.BLOCK.get(ModContent.id("test_natural_crop"));h.assertTrue(bind(h,plant.defaultBlockState(),Blocks.FARMLAND.defaultBlockState()).is(ModContent.BLANK.get())&&h.getLevel().getBlockState(center(h).offset(1,1,0)).is(plant),"explicit natural-only policy refuses artificial auto fallback");h.succeed();
    }
    @GameTest(template="empty")
    public static void cropBlacklistPreservesAllPlants(GameTestHelper h){
        var plant=BuiltInRegistries.BLOCK.get(ModContent.id("test_blacklisted_crop"));h.assertTrue(bind(h,plant.defaultBlockState(),Blocks.FARMLAND.defaultBlockState()).is(ModContent.BLANK.get())&&h.getLevel().getBlockState(center(h).offset(1,1,0)).is(plant),"blacklist refuses automatic simulation without consumption");h.succeed();
    }
    @GameTest(template="empty")
    public static void cropDiagnosisDoesNotHarvest(GameTestHelper h){
        var report=dev.overloadsim.command.SimulationDiagnostics.explainBlock(h.getLevel(),crop().defaultBlockState());h.assertTrue(report.error().isEmpty()&&report.category().equals("crop_binding")&&report.details().stream().anyMatch(s->s.contains("random=not_evaluated")),"diagnosis describes mature state without rolling any drops");h.succeed();
    }
    @GameTest(template="empty")
    public static void immaturePitcherPlantsHaveNoUpperHalf(GameTestHelper h){
        var stack=bind(h,Blocks.PITCHER_CROP.defaultBlockState(),Blocks.FARMLAND.defaultBlockState());h.assertTrue(stack.is(ModContent.BOUND.get()),"pitcher ages 0..2 are valid single-height plants");h.succeed();
    }
    @GameTest(template="empty")
    public static void maturityAdapterRemainsConsistentAfterSaving(GameTestHelper h){
        var plant=BuiltInRegistries.BLOCK.get(ModContent.id("test_stateful_api_crop"));
        SimulationExtensions.registerCropMaturityResolver(ModContent.id("test_canonical_crop"),(level,state)->state.is(plant)?Optional.of(state.getValue(CropBlock.AGE)==7?Blocks.GOLD_BLOCK.defaultBlockState():Blocks.DIAMOND_BLOCK.defaultBlockState()):Optional.empty());
        var atBinding=CropSimulationData.resolve(h.getLevel(),plant.defaultBlockState().setValue(CropBlock.AGE,7)).crop().orElseThrow();var afterSaving=CropSimulationData.resolveProfile(h.getLevel(),atBinding.profile()).crop().orElseThrow();
        h.assertTrue(atBinding.harvest().equals(afterSaving.harvest()),"maturity adapter uses the same canonical source at binding and after saved profile lookup");h.succeed();
    }
    @GameTest(template="empty")
    public static void explicitPlantSupportAcceptsUntaggedBed(GameTestHelper h){
        var soil=BuiltInRegistries.BLOCK.get(ModContent.id("test_supported_soil")).defaultBlockState();var result=bind(h,crop().defaultBlockState(),soil);
        h.assertTrue(result.is(ModContent.BOUND.get()),"explicit NeoForge plant support accepts a custom untagged agricultural bed");h.succeed();
    }
    @GameTest(template="empty")
    public static void tallPitcherBindingDoesNotCreateHarvestDrops(GameTestHelper h){
        var p=center(h);var plant=Blocks.PITCHER_CROP.defaultBlockState().setValue(PitcherCropBlock.AGE,4);var half=net.minecraft.world.level.block.state.properties.BlockStateProperties.DOUBLE_BLOCK_HALF;
        for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)if(x!=0||z!=0){var g=p.offset(x,0,z);h.getLevel().setBlock(g.below(),Blocks.STONE.defaultBlockState(),2);h.getLevel().setBlock(g,Blocks.FARMLAND.defaultBlockState(),2);h.getLevel().setBlock(g.above(),plant,50);h.getLevel().setBlock(g.above(2),plant.setValue(half,net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER),50);}
        var box=new net.minecraft.world.phys.AABB(net.minecraft.world.phys.Vec3.atLowerCornerOf(p.offset(-3,0,-3)),net.minecraft.world.phys.Vec3.atLowerCornerOf(p.offset(4,4,4)));h.assertTrue(h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,box).isEmpty(),"fixture begins without item entities");
        var stack=CrystalBinding.bindStructure(h.getLevel(),p,new ItemStack(ModContent.BLANK.get()),false);h.assertTrue(stack.is(ModContent.BOUND.get()),"tall pitcher binds");h.assertTrue(h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,box).isEmpty(),"removing upper halves must not trigger ordinary loot drops");
        h.assertTrue(h.getLevel().getBlockState(p.offset(1,1,0)).isAir()&&h.getLevel().getBlockState(p.offset(1,2,0)).isAir(),"both halves consumed");h.succeed();
    }
}
