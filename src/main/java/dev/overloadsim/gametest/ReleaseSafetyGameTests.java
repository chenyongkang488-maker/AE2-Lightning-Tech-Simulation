package dev.overloadsim.gametest;

import java.util.*;
import dev.overloadsim.*;
import dev.overloadsim.api.*;
import dev.overloadsim.binding.CrystalBinding;
import dev.overloadsim.data.*;
import dev.overloadsim.multiblock.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(OverloadSimulation.ID)
@PrefixGameTestTemplate(false)
public class ReleaseSafetyGameTests {
    @GameTest(template="empty",timeoutTicks=110)
    public static void missingBatchOutputPreservesControllerAndPaidJournal(GameTestHelper h){
        var c=MultiblockGameTests.powered(h,3);
        h.runAtTickTime(80,()->{
            var r=h.getLevel().registryAccess();c.crystals().setStackInSlot(0,MultiblockGameTests.ironCrystal());c.crystals().setStackInSlot(1,MultiblockGameTests.ironCrystal());
            c.outputs().insert(new ItemStack(Items.DIAMOND),12,false);c.energy().deserializeNBT(r,IntTag.valueOf(123456));
            var job=new SimulationBatch(List.of(new SimulationBatch.Output(new ItemStack(Items.RAW_IRON),7),new SimulationBatch.Output(new ItemStack(Items.DIAMOND),2)),Map.of(0,MultiblockGameTests.ironCrystal()),180,new MultiblockRules.Costs(1000,1,0),false,false,"removed-mod");job.paid=true;job.started=true;job.remaining=90;
            var saved=c.saveWithFullMetadata(r);var machine=saved.getCompound("SimulationMachine");var journal=job.save(r);
            journal.getList("Outputs",Tag.TAG_COMPOUND).getCompound(0).getCompound("Item").putString("id","removed_mod:ore_drop");machine.put("Batch",journal);
            var loaded=BlockEntity.loadStatic(c.getBlockPos(),c.getBlockState(),saved,r);
            h.assertTrue(loaded instanceof SimulationControllerBlockEntity,"unknown output must not discard whole controller block entity");
            var restored=(SimulationControllerBlockEntity)loaded;
            h.assertTrue(!restored.crystals().getStackInSlot(1).isEmpty()&&restored.outputs().count(0)==12&&restored.energy().getEnergyStored()==123456,"surviving inventory/buffer/energy retained");
            h.assertTrue(restored.saveMachine(r).getCompound("Batch").getList("Outputs",Tag.TAG_COMPOUND).equals(journal.getList("Outputs",Tag.TAG_COMPOUND)),"unresolved fixed outcomes preserved verbatim for reinstall");
            h.assertTrue(!restored.batch().fits(restored.outputs())&&!restored.batch().flush(restored.outputs()),"unresolved job cannot partially flush surviving outputs");
            c.loadTag(saved,r);c.tick();h.assertTrue(c.batch()!=null&&c.batch().remaining==90&&c.bridge().extract(false,Long.MAX_VALUE,true)==1000&&c.outputs().count(0)==12,"missing output pauses without debit or progression");
            c.crystals().extractItem(0,1,false);var refund=c.saveMachine(r);h.assertTrue(c.batch()==null&&refund.getLong("FeCredit")==1000&&refund.getLong("RefundHv")==1,"explicit cancellation refunds legacy paid journal");h.succeed();
        });
    }
    @GameTest(template="empty",timeoutTicks=110)
    public static void coldJsonMineralStartsBothMachinesAndResumesSingle(GameTestHelper h){
        var c=MultiblockGameTests.powered(h,3);var single=SimulationGameTests.capacityMachine(h);var profile=ModContent.id("auto/mineral/example/coldium");var item=CrystalDataAccess.perfect(new CrystalData(profile,Optional.empty(),0,1));
        h.runAtTickTime(80,()->{
            c.crystals().setStackInSlot(0,item.copy());coldCache();c.tick();h.assertTrue(c.batch()!=null&&c.batch().inputs.size()==1,"cold untagged auto JSON mineral starts multiblock");
            var saved=single.saveWithoutMetadata(h.getLevel().registryAccess());saved.remove("Job");single.loadTag(saved,h.getLevel().registryAccess());single.inventory().setStackInSlot(0,item.copy());coldCache();
            try{var start=single.getClass().getDeclaredMethod("start",net.minecraft.server.level.ServerLevel.class);start.setAccessible(true);start.invoke(single,h.getLevel());}catch(ReflectiveOperationException e){throw new RuntimeException(e);}
            h.assertTrue(single.busy(),"cold untagged auto JSON mineral starts ordinary chamber");saved=single.saveWithoutMetadata(h.getLevel().registryAccess());int remaining=single.remaining();single.loadTag(saved,h.getLevel().registryAccess());coldCache();single.tick();
            h.assertTrue(single.remaining()==remaining-1,"cold saved single-chamber job resumes without tag/profile warming");h.succeed();
        });
    }
    private static void coldCache(){
        try{for(var name:List.of("profiles","materials","bindings")){var field=MineralSimulationData.class.getDeclaredField(name);field.setAccessible(true);field.set(null,Map.of());}var field=MineralSimulationData.class.getDeclaredField("indexedLevel");field.setAccessible(true);field.set(null,null);SimulationData.invalidate();}catch(ReflectiveOperationException e){throw new RuntimeException(e);}
    }
    @GameTest(template="empty")
    public static void conflictingProfileBindingsPreserveAllMaterials(GameTestHelper h){
        for(var id:List.of("test_conflict_block_a","test_conflict_block_b")){
            var block=BuiltInRegistries.BLOCK.get(ModContent.id(id));var result=MineralCompatibilityGameTests.bind(h,block);
            h.assertTrue(result.is(ModContent.BLANK.get()),"same-profile conflict rejects binding for "+id);
            for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)if(x!=0||z!=0)h.assertBlockPresent(block,new net.minecraft.core.BlockPos(5+x,1,5+z));
        }h.succeed();
    }
    @GameTest(template="empty")
    public static void javaMineralOverridesAutomaticTagsWithoutFalseConflict(GameTestHelper h){
        var profile=ModContent.id("auto/mineral/c/overrideium");var material=ResourceLocation.parse("c:overrideium");var block=BuiltInRegistries.BLOCK.get(ModContent.id("test_java_override_block"));
        var definition=new ResolvedMineral(profile,material,"#c:storage_blocks/raw_overrideium",Optional.of(ModContent.id("test_raw_material")),2,2,Optional.empty(),Optional.empty(),false);
        SimulationExtensions.registerMineralResolver(ModContent.id("release_java_override"),(level,state)->state.is(block)?Optional.of(definition):Optional.empty());SimulationData.invalidate();
        var binding=MineralSimulationData.resolveBinding(h.getLevel(),block.defaultBlockState());var production=MineralSimulationData.resolveProfile(h.getLevel(),profile);
        h.assertTrue(binding.valid()&&production.valid()&&binding.mineral().orElseThrow().equals(definition)&&production.mineral().orElseThrow().equals(definition),"one Java definition overrides automatic quantity1: "+binding.error());
        h.assertTrue(MineralCompatibilityGameTests.bind(h,block).is(ModContent.BOUND.get()),"Java override remains bindable");h.succeed();
    }
}
