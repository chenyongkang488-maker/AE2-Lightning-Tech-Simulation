package dev.overloadsim.gametest;

import dev.overloadsim.ModContent;
import dev.overloadsim.OverloadSimulation;
import dev.overloadsim.SimulationConfig;
import dev.overloadsim.config.MachineSettings;
import dev.overloadsim.machine.SimulationChamberBlockEntity;
import dev.overloadsim.multiblock.BulkOutputBuffer;
import dev.overloadsim.multiblock.MultiblockContent;
import dev.overloadsim.multiblock.MultiblockData;
import dev.overloadsim.multiblock.SimulationControllerBlockEntity;
import dev.overloadsim.multiblock.SimulationStructure;
import dev.overloadsim.tool.CoilEnergy;
import dev.overloadsim.tool.CoilModuleItem;
import dev.overloadsim.tool.CoilModules;
import dev.overloadsim.tool.CoilSettings;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.IntTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Configuration contracts against real inventories, saved energy and executable policies. */
@GameTestHolder(OverloadSimulation.ID)
@PrefixGameTestTemplate(false)
public final class SimulationConfigurationGameTests {
    private SimulationConfigurationGameTests() {}

    static <T> void withValue(ModConfigSpec.ConfigValue<T> config,T value,Runnable test){
        synchronized(MachineSettings.class){
            T previous=config.get();
            try{config.set(value);config.clearCache();test.run();}finally{config.set(previous);config.clearCache();}
        }
    }

    @GameTest(template="empty")
    public static void configuredChamberChargeKeepsExcessSavedFe(GameTestHelper h){
        var machine=new SimulationChamberBlockEntity(h.absolutePos(new BlockPos(2,1,2)),ModContent.CHAMBER.get().defaultBlockState());
        machine.energy().deserializeNBT(h.getLevel().registryAccess(),IntTag.valueOf(5000));
        withValue(MachineSettings.SIMULATION_ENERGY_CAPACITY,1000,()->withValue(MachineSettings.SIMULATION_RECEIVE_PER_CALL,37,()->{
            h.assertTrue(machine.energy().getEnergyStored()==5000&&machine.energy().getMaxEnergyStored()==1000,"lower capacity keeps all saved FE visible");
            h.assertTrue(machine.energy().receiveEnergy(100,false)==0&&machine.energy().getEnergyStored()==5000,"overfull buffer rejects charging without deleting FE");
            var saved=machine.energy().serializeNBT(h.getLevel().registryAccess());
            var restored=new SimulationChamberBlockEntity(machine.getBlockPos(),machine.getBlockState());
            restored.energy().deserializeNBT(h.getLevel().registryAccess(),saved);
            h.assertTrue(restored.energy().getEnergyStored()==5000,"overfull energy survives another load");
            restored.energy().deserializeNBT(h.getLevel().registryAccess(),IntTag.valueOf(100));
            h.assertTrue(restored.energy().receiveEnergy(100,false)==37&&restored.energy().getEnergyStored()==137,"configured receive budget governs actual FE acceptance");
        }));h.succeed();
    }

    @GameTest(template="empty")
    public static void loweredMatrixLimitRestrictsEffectAndPreservesItems(GameTestHelper h){
        var machine=new SimulationChamberBlockEntity(h.absolutePos(new BlockPos(2,1,2)),ModContent.CHAMBER.get().defaultBlockState());
        machine.inventory().setStackInSlot(1,new ItemStack(com.moakiee.ae2lt.registry.ModItems.LIGHTNING_COLLAPSE_MATRIX.get(),32));
        withValue(SimulationConfig.MATRIX_LIMIT,2,()->withValue(MachineSettings.SIMULATION_PARALLEL_PER_MATRIX,3,()->withValue(MachineSettings.SIMULATION_MAX_PARALLEL,5,()->{
            h.assertTrue(machine.maximumParallel()==5,"actual parallel combines installed matrix limit, per-matrix effect and machine ceiling");
            h.assertTrue(machine.inventory().getStackInSlot(1).getCount()==32,"lower matrix limit never destroys existing modules");
            h.assertTrue(!machine.inventory().insertItem(1,com.moakiee.ae2lt.registry.ModItems.LIGHTNING_COLLAPSE_MATRIX.toStack(),false).isEmpty(),"overfull matrix slot rejects another module");
            h.assertTrue(machine.inventory().extractItem(1,30,false).getCount()==30,"old excess modules remain removable");
        })));h.succeed();
    }

    @GameTest(template="empty")
    public static void configuredControllerChargeKeepsExcessSavedFe(GameTestHelper h){
        var machine=new SimulationControllerBlockEntity(h.absolutePos(new BlockPos(2,1,2)),MultiblockContent.CONTROLLER.get().defaultBlockState());
        machine.energy().deserializeNBT(h.getLevel().registryAccess(),IntTag.valueOf(5000));
        withValue(MachineSettings.MULTIBLOCK_ENERGY_CAPACITY,1000,()->withValue(MachineSettings.MULTIBLOCK_RECEIVE_PER_CALL,23,()->{
            h.assertTrue(machine.energy().getEnergyStored()==5000&&machine.energy().receiveEnergy(100,false)==0,"controller retains excess FE and refuses additional charging");
            var saved=machine.saveMachine(h.getLevel().registryAccess());var restored=new SimulationControllerBlockEntity(machine.getBlockPos(),machine.getBlockState());
            restored.loadMachine(saved,h.getLevel().registryAccess());
            h.assertTrue(restored.availableFe()==5000,"controller serialization does not clamp old energy to new capacity");
            restored.energy().deserializeNBT(h.getLevel().registryAccess(),IntTag.valueOf(100));
            h.assertTrue(restored.energy().receiveEnergy(100,false)==23,"controller receive budget is independent of capacity");
        }));h.succeed();
    }

    @GameTest(template="empty")
    public static void lowerBulkLimitPreservesOldStacksAndAvoidsNegativeInserts(GameTestHelper h){
        var buffer=new BulkOutputBuffer();var iron=new ItemStack(Items.IRON_INGOT);buffer.insert(iron,1024,false);
        var saved=buffer.save(h.getLevel().registryAccess());
        withValue(MachineSettings.MULTIBLOCK_OUTPUT_SLOT_LIMIT,100,()->{
            var restored=new BulkOutputBuffer();restored.load(saved,h.getLevel().registryAccess());
            h.assertTrue(restored.count(0)==1024,"all old bulk quantity survives a lower slot limit");
            h.assertTrue(restored.insert(iron,180,true)==180&&restored.count(0)==1024&&restored.count(1)==0,"capacity probe skips overfull entries without negative insertion or mutation");
            h.assertTrue(restored.insert(iron,180,false)==180&&restored.count(0)==1024&&restored.count(1)==100&&restored.count(2)==80,"new entries obey the limit while old quantity remains intact");
            h.assertTrue(restored.extract(0,64,false).getCount()==64&&restored.count(0)==960,"old overfull output is still extractable");
        });h.succeed();
    }

    @GameTest(template="empty")
    public static void multiblockFeeAndTimePercentagesApplyWithoutChangingLoot(GameTestHelper h){
        var structure=new SimulationStructure(BlockPos.ZERO,3,List.of(),List.of(),null,0,0,0,2,true,true);
        var policy=MultiblockData.policy();var signature=policy.signature(structure);
        withValue(MachineSettings.MULTIBLOCK_TIME_PERCENT,150,()->withValue(MachineSettings.MULTIBLOCK_ENERGY_COST_PERCENT,125,()->withValue(MachineSettings.MULTIBLOCK_LIGHTNING_COST_PERCENT,150,()->{
            var costs=policy.cost(2,structure);
            h.assertTrue(policy.duration(structure)==135,"final overload duration is scaled from 90 to 135 ticks");
            h.assertTrue(costs.fe()==2500&&costs.hv()==10&&costs.ehv()==4,"ceil per-crystal fees before multiplying participant count");
            h.assertTrue(policy.multiplier(structure)==4,"machine fee and time controls preserve recipe outcomes and fortune");
            h.assertTrue(!signature.equals(policy.signature(structure)),"unstarted batch signature detects changed configuration");
        })));h.succeed();
    }

    @GameTest(template="empty")
    public static void coilCapacityReductionAndModuleRemovalKeepPaidEnergy(GameTestHelper h){
        var coil=new ItemStack(ModContent.COIL.get());CoilModules.install(coil,com.moakiee.ae2lt.registry.ModItems.ENERGY_MODULE_T3.toStack());
        coil.set(ModContent.COIL_FE.get(),123456789L);
        withValue(MachineSettings.COIL_BASE_CAPACITY,1000L,()->withValue(MachineSettings.COIL_CAPACITY_LIMIT,2000L,()->{
            h.assertTrue(CoilModules.capacity(coil)==2000&&CoilEnergy.read(coil)==123456789L,"coil exposes configured capacity and preserves stored excess");
            h.assertTrue(CoilEnergy.INSTANCE.receiveFe(coil,100,false)==0,"overfull coil rejects new FE");
            h.assertTrue(!CoilModules.uninstall(coil,"energy").isEmpty()&&CoilModules.capacity(coil)==1000&&CoilEnergy.read(coil)==123456789L,"module removal returns module without deleting prepaid FE");
            h.assertTrue(CoilEnergy.INSTANCE.tryConsume(coil,null,89)&&CoilEnergy.read(coil)==123456700L,"old excess remains spendable");
            var restored=ItemStack.parse(h.getLevel().registryAccess(),coil.save(h.getLevel().registryAccess())).orElseThrow();
            h.assertTrue(CoilEnergy.read(restored)==123456700L,"coil excess survives component serialization");
        }));h.succeed();
    }

    @GameTest(template="empty")
    public static void configuredCoilEffectsKeepPlayersSavedSelections(GameTestHelper h){
        var coil=new ItemStack(ModContent.COIL.get());CoilModules.setCore(coil,com.moakiee.ae2lt.registry.ModItems.ULTIMATE_OVERLOAD_CORE.toStack());
        for(var type:List.of(CoilModuleItem.Type.MIMIC,CoilModuleItem.Type.EFFICIENCY,CoilModuleItem.Type.FORTUNE))CoilModules.install(coil,ModContent.COIL_UPGRADES.get(type).toStack());
        coil.set(ModContent.COIL_SETTINGS.get(),new CoilSettings(false,true,10,5,false));
        withValue(MachineSettings.COIL_EFFICIENCY_LIMIT,2,()->withValue(MachineSettings.COIL_FORTUNE_LIMIT,1,()->withValue(MachineSettings.COIL_BASE_MINING_SPEED,4.0,()->{
            h.assertTrue(coil.getDestroySpeed(Blocks.STONE.defaultBlockState())==9,"configured base speed plus effective efficiency two gives nine");
            h.assertTrue(CoilSettings.read(coil).fortuneLevel(coil)==1,"effective fortune obeys server ceiling");
            h.assertTrue(CoilSettings.read(coil).efficiency()==10&&CoilSettings.read(coil).fortune()==5,"lower effect limits leave stored player choices unchanged");
        })));h.succeed();
    }
}
