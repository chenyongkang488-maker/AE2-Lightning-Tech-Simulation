package dev.overloadsim.gametest;

import dev.overloadsim.*;
import dev.overloadsim.multiblock.*;
import dev.overloadsim.api.*;
import dev.overloadsim.data.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(OverloadSimulation.ID)
@PrefixGameTestTemplate(false)
public class PrimordialModuleGameTests {
    private static Block module(){return BuiltInRegistries.BLOCK.getOptional(ModContent.id("simulation_primordial_module")).orElseThrow();}
    private static ItemStack crystal(String profile){return CrystalDataAccess.perfect(new CrystalData(ModContent.id(profile),Optional.empty(),0,1));}
    private static void install(SimulationControllerBlockEntity c,Block block,int x,int z){
        var min=c.structure().min();c.invalidateStructure();c.getLevel().setBlockAndUpdate(min.offset(x,0,z),block.defaultBlockState());c.checkStructure();
    }

    @GameTest(template="empty")
    public static void primordialFormsButRejectsSmeltingAndDuplicates(GameTestHelper h){
        var c=MultiblockGameTests.build(h,h.absolutePos(new BlockPos(2,1,2)),4);
        var min=c.getBlockPos().offset(-1,0,0);
        h.getLevel().setBlockAndUpdate(min.offset(1,0,1),module().defaultBlockState());c.checkStructure();
        h.assertTrue(c.structure()!=null,"primordial module is a valid floor upgrade");c.invalidateStructure();
        h.getLevel().setBlockAndUpdate(min.offset(2,0,1),MultiblockContent.SMELTING.get().defaultBlockState());
        var conflict=SimulationStructureValidator.validate(c,min,4);
        h.assertTrue(!conflict.valid()&&conflict.error().equals("primordial_smelting_conflict"),"conflict is explicit");
        try{SimulationAutoConstruction.plan(c,4);h.fail("auto construction must reject conflicting modules");}catch(IllegalArgumentException expected){h.assertTrue(expected.getMessage().equals("primordial_smelting_conflict"),"same construction diagnosis");}
        h.getLevel().setBlockAndUpdate(min.offset(2,0,1),module().defaultBlockState());
        h.assertTrue(!SimulationStructureValidator.validate(c,min,4).valid(),"duplicate primordial modules invalidate");
        h.succeed();
    }

    @GameTest(template="empty")
    public static void primordialAliasesArePreservedAndMutuallyExclusive(GameTestHelper h){
        var c=MultiblockGameTests.build(h,h.absolutePos(new BlockPos(2,1,2)),4);var min=c.getBlockPos().offset(-1,0,0);
        var pos=min.offset(1,0,1);var alias=BuiltInRegistries.BLOCK.get(ModContent.id("test_primordial_alias"));
        h.getLevel().setBlockAndUpdate(pos,alias.defaultBlockState());
        var plan=SimulationAutoConstruction.plan(c,4);h.assertTrue(plan.cells().get(pos).is(alias),"auto construction retains alias");
        c.checkStructure();h.assertTrue(c.structure().primordial(),"alias sets primordial state");c.invalidateStructure();
        h.getLevel().setBlockAndUpdate(min.offset(2,0,1),MultiblockContent.SMELTING.get().defaultBlockState());
        h.assertTrue(SimulationStructureValidator.validate(c,min,4).error().equals("primordial_smelting_conflict"),"alias cannot bypass mutual exclusion");h.succeed();
    }

    @GameTest(template="empty")
    public static void primordialResolutionCoversVanillaOverrideUniqueAmbiguousAndNonMinerals(GameTestHelper h){
        var vanilla=Map.of("iron",Items.IRON_ORE,"copper",Items.COPPER_ORE,"gold",Items.GOLD_ORE,"coal",Items.COAL_ORE,"diamond",Items.DIAMOND_ORE,"emerald",Items.EMERALD_ORE,"redstone",Items.REDSTONE_ORE,"lapis",Items.LAPIS_ORE,"quartz",Items.NETHER_QUARTZ_ORE,"netherite",Items.ANCIENT_DEBRIS);
        vanilla.forEach((profile,item)->h.assertTrue(PrimordialOreResolver.resolve(h.getLevel(),CrystalDataAccess.read(crystal(profile)).orElseThrow()).output().is(item),"vanilla mapping "+profile));
        h.assertTrue(resolve(h,"test_natural_profile").output().is(Items.DEEPSLATE_IRON_ORE),"explicit datapack mapping");
        h.assertTrue(resolve(h,"auto/mineral/c/testium").output().is(BuiltInRegistries.ITEM.get(ModContent.id("test_gem_ore"))),"unique valid ore tag");
        h.assertTrue(resolve(h,"test_override").reason().equals("ambiguous_ore"),"multiple ore blocks retain normal production");
        h.assertTrue(resolve(h,"auto/mineral/example/coldium").reason().equals("invalid_mapping"),"missing explicit target falls back");
        for(var profile:List.of("wheat","oak","mob"))h.assertTrue(!resolve(h,profile).converted()&&!resolve(h,profile).fallback(),"non-mineral is excluded even if mapped: "+profile);
        h.assertTrue(!PrimordialOreResolver.resolve(h.getLevel(),new CrystalData(ModContent.id("iron"),Optional.of(net.minecraft.resources.ResourceLocation.withDefaultNamespace("pig")),0,1)).converted(),"entity-bearing identity is never converted");
        SimulationData.invalidate();h.assertTrue(resolve(h,"auto/mineral/c/testium").converted(),"cache rebuild keeps unique tag mapping");h.succeed();
    }
    private static PrimordialOreResolver.Resolution resolve(GameTestHelper h,String profile){return PrimordialOreResolver.resolve(h.getLevel(),CrystalDataAccess.read(crystal(profile)).orElseThrow());}

    @GameTest(template="empty")
    public static void primordialJavaMaterialCacheIsIsolatedByDimension(GameTestHelper h){
        var profile=ModContent.id("test_primordial_dimension");var alias=BuiltInRegistries.BLOCK.get(ModContent.id("test_primordial_alias"));
        SimulationExtensions.registerMineralResolver(ModContent.id("test_primordial_dimension_resolver"),(level,state)->state.is(alias)?Optional.of(new ResolvedMineral(profile,net.minecraft.resources.ResourceLocation.parse(level.dimension().equals(net.minecraft.world.level.Level.NETHER)?"c:explicitium":"c:testium"),"overload_sim:test_primordial_alias",Optional.of(ModContent.id("test_raw_material")),1,1,Optional.empty(),Optional.empty(),true)):Optional.empty());
        SimulationData.invalidate();var data=new CrystalData(profile,Optional.empty(),0,1);
        try{
            h.assertTrue(PrimordialOreResolver.resolve(h.getLevel(),data).converted(),"overworld material has one ore");
            var nether=h.getLevel().getServer().getLevel(net.minecraft.world.level.Level.NETHER);
            h.assertTrue(nether!=null&&PrimordialOreResolver.resolve(nether,data).reason().equals("ambiguous_ore"),"nether Java material has multiple ores and must not reuse overworld cache");
            h.assertTrue(PrimordialOreResolver.resolve(h.getLevel(),data).converted(),"returning to overworld resolves its own material again");
        }finally{SimulationData.invalidate();SimulationData.profile(h.getLevel(),profile);}
        h.succeed();
    }

    @GameTest(template="empty")
    public static void primordialReloadOverridesVanillaAndInvalidatesCachedTargets(GameTestHelper h){
        var original=configResources(h);
        h.assertTrue(resolve(h,"iron").output().is(Items.IRON_ORE),"cache ordinary target first");
        var changed=new HashMap<>(original);changed.put(ModContent.id("zz_primordial_override"),com.google.gson.JsonParser.parseString("{\"policy\":{\"primordial_hv\":7},\"primordial\":[{\"profile\":\"overload_sim:iron\",\"block\":\"minecraft:deepslate_iron_ore\"}]}"));
        try{
            reload(changed);
            h.assertTrue(resolve(h,"iron").output().is(Items.DEEPSLATE_IRON_ORE),"explicit mapping overrides cached vanilla target");
            h.assertTrue(MultiblockData.policy().primordialHv()==7,"reload applies configurable conversion fee");
        }finally{reload(original);}
        h.assertTrue(resolve(h,"iron").output().is(Items.IRON_ORE)&&MultiblockData.policy().primordialHv()==2,"removing override restores defaults");h.succeed();
    }
    static Map<net.minecraft.resources.ResourceLocation,com.google.gson.JsonElement> configResources(GameTestHelper h){
        var files=new HashMap<net.minecraft.resources.ResourceLocation,com.google.gson.JsonElement>();
        h.getLevel().getServer().getResourceManager().listResources("multiblock_simulation",id->id.getPath().endsWith(".json")).forEach((id,resource)->{
            try(var reader=resource.openAsReader()){
                String path=id.getPath();files.put(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(id.getNamespace(),path.substring("multiblock_simulation/".length(),path.length()-5)),com.google.gson.JsonParser.parseReader(reader));
            }catch(java.io.IOException error){throw new IllegalStateException(error);}
        });return files;
    }
    static void reload(Map<net.minecraft.resources.ResourceLocation,com.google.gson.JsonElement> files){
        try{
            var apply=MultiblockData.class.getDeclaredMethod("apply",Map.class,net.minecraft.server.packs.resources.ResourceManager.class,net.minecraft.util.profiling.ProfilerFiller.class);
            apply.setAccessible(true);apply.invoke(new MultiblockData(),files,null,null);
        }catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
    }

    @GameTest(template="empty")
    public static void primordialMixedCostScalingRoundsPerCrystal(GameTestHelper h){
        var s=new SimulationStructure(BlockPos.ZERO,4,List.of(),List.of(),null,0,0,0,10,true,false,true);
        SimulationConfigurationGameTests.withValue(dev.overloadsim.config.MachineSettings.MULTIBLOCK_LIGHTNING_COST_PERCENT,150,()->{
            var policy=MultiblockData.policy();var cost=policy.cost(3,1,s);
            h.assertTrue(cost.hv()==9&&cost.ehv()==6&&cost.fe()==3000,"150 percent: converted ceil(4.5)=5 plus two ordinary ceil(1.5)=2");
        });
        SimulationConfigurationGameTests.withValue(dev.overloadsim.config.MachineSettings.MULTIBLOCK_LIGHTNING_COST_PERCENT,0,()->h.assertTrue(MultiblockData.policy().cost(3,2,s).hv()==0,"zero percentage disables both base and conversion lightning"));h.succeed();
    }

    @GameTest(template="empty",timeoutTicks=110)
    public static void primordialMixedBatchUsesOneOreAndChargesOnlyConversions(GameTestHelper h){
        var c=MultiblockGameTests.powered(h,4);install(c,module(),1,1);install(c,MultiblockContent.FORTUNE.get(),1,2);
        h.runAtTickTime(80,()->{
            c.crystals().setStackInSlot(0,crystal("redstone"));c.crystals().setStackInSlot(1,crystal("test_override"));c.crystals().setStackInSlot(2,crystal("wheat"));
            var batch=SimulationBatchPlanner.plan(c,0);
            h.assertTrue(batch.inputs.size()==3&&batch.convertedCrystals==1&&batch.fallbackCrystals==1,"mixed crystal accounting");
            h.assertTrue(batch.cost.fe()==3000&&batch.cost.hv()==5&&batch.cost.ehv()==0,"base fees plus only one primordial surcharge");
            h.assertTrue(batch.outputs.stream().anyMatch(o->o.prototype().is(Items.REDSTONE_ORE)&&o.count()==2),"redstone base loot replaced with one ore times fortune");
            h.assertTrue(batch.outputs.stream().anyMatch(o->o.prototype().is(BuiltInRegistries.ITEM.get(ModContent.id("test_raw_material")))&&o.count()==6),"ambiguous mineral keeps normal yield times fortune");
            h.assertTrue(batch.outputs.stream().anyMatch(o->o.prototype().is(Items.WHEAT)&&o.count()==4),"crop output is unchanged");
            var restored=SimulationBatch.load(batch.save(h.getLevel().registryAccess()),h.getLevel().registryAccess());
            h.assertTrue(restored.primordial&&restored.convertedCrystals==1&&restored.fallbackCrystals==1&&restored.cost.equals(batch.cost),"full snapshot survives save/load");
            h.succeed();
        });
    }

    @GameTest(template="empty",timeoutTicks=110)
    public static void primordialResourceShortageDoesNotChargeOrBlockOrdinaryCrystals(GameTestHelper h){
        var c=MultiblockGameTests.powered(h,4);install(c,module(),1,1);
        h.runAtTickTime(80,()->{
            c.bridge().extract(false,998,false);c.crystals().setStackInSlot(0,crystal("iron"));c.crystals().setStackInSlot(1,crystal("wheat"));
            var batch=SimulationBatchPlanner.plan(c,0);
            h.assertTrue(batch!=null&&!batch.inputs.containsKey(0)&&batch.inputs.containsKey(1)&&batch.cost.hv()==1,"unaffordable ore skips without blocking ordinary production");
            h.assertTrue(c.bridge().extract(false,Long.MAX_VALUE,true)==2,"planning is read-only");
            c.crystals().setStackInSlot(1,ItemStack.EMPTY);h.assertTrue(SimulationBatchPlanner.plan(c,0)==null,"no partial underfunded ore batch");h.succeed();
        });
    }

    @GameTest(template="empty",timeoutTicks=110)
    public static void primordialUnstartedBatchReplansAfterModuleChange(GameTestHelper h){
        var c=MultiblockGameTests.powered(h,4);
        h.runAtTickTime(80,()->{
            c.crystals().setStackInSlot(0,crystal("iron"));var old=SimulationBatchPlanner.plan(c,0);
            install(c,module(),1,1);var registry=h.getLevel().registryAccess();var tag=c.saveMachine(registry);tag.put("Batch",old.save(registry));c.loadMachine(tag,registry);c.checkStructure();
            c.tick();h.assertTrue(c.batch()==null,"unstarted ordinary batch invalidated after installation");
            c.tick();h.assertTrue(c.batch()!=null&&c.batch().started&&c.batch().primordial&&c.batch().cost.hv()==3&&c.batch().outputs.getFirst().prototype().is(Items.IRON_ORE),"replacement batch uses primordial ore and fee");h.succeed();
        });
    }

    @GameTest(template="empty",timeoutTicks=110)
    public static void primordialStartedBatchSurvivesRemovalAndRestartWithoutRepricing(GameTestHelper h){
        var c=MultiblockGameTests.powered(h,4);install(c,module(),1,1);
        h.runAtTickTime(80,()->{
            c.crystals().setStackInSlot(0,crystal("iron"));c.tick();var old=c.batch();h.assertTrue(old.started&&old.primordial,"ore batch started");
            var min=c.structure().min();c.invalidateStructure();h.getLevel().setBlockAndUpdate(min.offset(1,0,1),Blocks.AIR.defaultBlockState());
            h.getLevel().setBlockAndUpdate(min,Blocks.AIR.defaultBlockState());c.tick();h.assertTrue(old.remaining==179,"broken shell pauses fixed job");
            c.loadMachine(c.saveMachine(h.getLevel().registryAccess()),h.getLevel().registryAccess());
            h.getLevel().setBlockAndUpdate(min,MultiblockContent.FRAME.get().defaultBlockState());c.checkStructure();
            h.assertTrue(!c.structure().primordial()&&c.batch().primordial,"new structure differs from fixed batch");
            for(int i=0;i<179;i++)c.tick();
            h.assertTrue(c.outputs().prototype(0).is(Items.IRON_ORE)&&c.outputs().count(0)==1,"old ore outcome completed exactly once");
            h.assertTrue(c.bridge().extract(false,Long.MAX_VALUE,true)==997,"only original 3 HV charged");
            var next=SimulationBatchPlanner.plan(c,0);h.assertTrue(!next.primordial&&next.cost.hv()==1&&next.outputs.getFirst().prototype().is(Items.RAW_IRON),"next batch uses removed module state");h.succeed();
        });
    }

    @GameTest(template="empty",timeoutTicks=110)
    public static void primordialRespectsBufferCapacityAndMaximumUpgradePolicy(GameTestHelper h){
        var c=MultiblockGameTests.powered(h,7);var min=c.structure().min();c.invalidateStructure();int index=0;
        for(int x=1;x<6;x++)for(int z=1;z<6;z++){
            var block=index<13?MultiblockContent.T3.get():index<23?MultiblockContent.FORTUNE.get():index==23?MultiblockContent.OVERLOAD.get():module();
            h.getLevel().setBlockAndUpdate(min.offset(x,0,z),block.defaultBlockState());index++;
        }c.checkStructure();
        h.runAtTickTime(80,()->{
            for(int i=0;i<49;i++)c.crystals().setStackInSlot(i,crystal("lapis"));
            var batch=SimulationBatchPlanner.plan(c,0);
            h.assertTrue(batch.duration==38&&batch.inputs.size()==49&&batch.outputs.stream().mapToLong(SimulationBatch.Output::count).sum()==50176,"one ore times capped fortune");
            h.assertTrue(batch.cost.fe()==49000&&batch.cost.hv()==147&&batch.cost.ehv()==49,"mixed policy fees are independent of fortune");
            c.outputs().insert(new ItemStack(Items.LAPIS_ORE),131072,false);
            h.assertTrue(SimulationBatchPlanner.plan(c,0)==null,"full output reserves no batch and no fees");h.succeed();
        });
    }

    @GameTest(template="empty")
    public static void primordialOldConstructorsAndBatchTagsRemainCompatible(GameTestHelper h){
        var s=new SimulationStructure(BlockPos.ZERO,3,List.of(),List.of(),null,0,0,0,0,false,false);
        var policy=new MultiblockData.Policy(180,104,10,2,4,8,1000,1,1,2);
        h.assertTrue(!s.primordial()&&policy.cost(1,s).hv()==1,"old public construction retains ordinary behavior");
        var old=new SimulationBatch(List.of(new SimulationBatch.Output(new ItemStack(Items.RAW_IRON),1)),Map.of(0,crystal("iron")),180,new MultiblockRules.Costs(1000,1,0),false,false,"old");
        var tag=old.save(h.getLevel().registryAccess());for(var key:List.of("Primordial","ConvertedCrystals","FallbackCrystals","FallbackReason"))tag.remove(key);
        var restored=SimulationBatch.load(tag,h.getLevel().registryAccess());h.assertTrue(!restored.primordial&&restored.convertedCrystals==0&&restored.fallbackCrystals==0,"old tags default to ordinary");h.succeed();
    }
}
