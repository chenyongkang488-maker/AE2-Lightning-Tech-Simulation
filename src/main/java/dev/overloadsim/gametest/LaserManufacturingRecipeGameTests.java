package dev.overloadsim.gametest;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.storage.StorageCells;
import appeng.blockentity.storage.DriveBlockEntity;
import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEItems;
import com.moakiee.ae2lt.blockentity.OverloadProcessingFactoryBlockEntity;
import com.moakiee.ae2lt.machine.lightningassembly.LightningAssemblyChamberInventory;
import com.moakiee.ae2lt.machine.lightningassembly.recipe.LightningAssemblyRecipe;
import com.moakiee.ae2lt.machine.lightningassembly.recipe.LightningAssemblyRecipeInput;
import com.moakiee.ae2lt.machine.overloadfactory.OverloadProcessingFactoryInventory;
import com.moakiee.ae2lt.machine.overloadfactory.recipe.OverloadProcessingRecipe;
import com.moakiee.ae2lt.machine.overloadfactory.recipe.OverloadProcessingRecipeInput;
import com.moakiee.ae2lt.me.key.LightningKey;
import dev.overloadsim.ModContent;
import dev.overloadsim.OverloadSimulation;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Exercises upstream recipe matching and a real grid-connected processing factory. */
@GameTestHolder(OverloadSimulation.ID)
@PrefixGameTestTemplate(false)
public final class LaserManufacturingRecipeGameTests {
    private record Material(String id, int count) {}
    private record Assembly(String id, long fe, int lightning, LightningKey.Tier tier, List<Material> materials) {}
    private static final LightningKey.Tier HV = LightningKey.Tier.HIGH_VOLTAGE;
    private static final LightningKey.Tier EHV = LightningKey.Tier.EXTREME_HIGH_VOLTAGE;
    private static Material material(String id, int count) { return new Material(id, count); }
    private static Assembly specification(String id, long fe, int lightning, LightningKey.Tier tier, Material... materials) {
        return new Assembly(id, fe, lightning, tier, List.of(materials));
    }
    private static final List<Assembly> ASSEMBLIES = List.of(
        specification("laser_optical_port",400_000,128,HV,material("overload_sim:laser_glass",8),material("ae2lt:firmament_alloy_ingot",8),material("ae2lt:ultimate_overload_core",1)),
        specification("high_energy_laser_emitter",1_000_000,128,EHV,material("overload_sim:laser_frame",4),material("ae2lt:overload_processor",64),material("ae2lt:overload_alloy_plate",64),material("ae2lt:firmament_superconducting_wire",32),material("ae2lt:lightning_collapse_matrix",1)),
        specification("laser_constraint_matrix",400_000,32,EHV,material("overload_sim:laser_frame",4),material("ae2lt:firmament_alloy_ingot",4),material("ae2lt:firmament_superconducting_wire",4)),
        specification("laser_enhancement_matrix",800_000,32,EHV,material("overload_sim:laser_frame",4),material("ae2lt:ultimate_overload_core",4),material("ae2lt:firmament_superconducting_wire",4)),
        specification("laser_burst_matrix",2_000_000,128,EHV,material("overload_sim:laser_frame",4),material("ae2lt:lightning_collapse_matrix",4),material("ae2lt:firmament_superconducting_wire",16)),
        specification("laser_focus_matrix",800_000,64,EHV,material("overload_sim:laser_frame",8),material("overload_sim:laser_glass",8),material("ae2lt:overload_processor",32)),
        specification("laser_deflector_matrix",400_000,32,EHV,material("overload_sim:laser_frame",8),material("overload_sim:laser_glass",32),material("ae2lt:overload_crystal_block",32)),
        specification("laser_source_crystal",2_000_000,256,EHV,material("overload_sim:laser_frame",2),material("ae2lt:flawless_budding_overload_crystal",1),material("ae2lt:overload_crystal_block",32)),
        specification("collapse_core",2_000_000,256,EHV,material("overload_sim:laser_frame",2),material("minecraft:heavy_core",1),material("ae2:singularity",512),material("minecraft:netherite_block",64)),
        specification("particle_constrainer",2_000_000,256,EHV,material("overload_sim:laser_frame",8),material("minecraft:heavy_core",1),material("ae2lt:overload_processor",64),material("ae2:dense_energy_cell",64),material("ae2lt:firmament_alloy_ingot",64)),
        specification("high_energy_workstation",2_000_000,256,EHV,material("ae2lt:overload_processing_factory",64),material("ae2lt:lightning_collapse_matrix",16),material("ae2lt:overload_processor",64))
    );

    private static Recipe<?> recipe(GameTestHelper h, String id) {
        return h.getLevel().getRecipeManager().byKey(ModContent.id(id)).orElseThrow().value();
    }
    private static ItemStack stack(String id, int count) {
        return new ItemStack(BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(id)).orElseThrow(), count);
    }
    private static LightningAssemblyRecipe assembly(GameTestHelper h, String id) {
        var raw = recipe(h, id);
        h.assertTrue(raw instanceof LightningAssemblyRecipe, id+" uses the upstream assembly serializer");
        return (LightningAssemblyRecipe) raw;
    }
    private static LightningAssemblyChamberInventory inventory(Assembly specification) {
        var inventory = new LightningAssemblyChamberInventory(null);
        for (int slot=0; slot<specification.materials.size(); slot++) {
            var material = specification.materials.get(slot);
            inventory.setStackInSlot(slot, stack(material.id, material.count));
        }
        return inventory;
    }

    @GameTest(template="empty")
    public static void everyLaserAssemblyRequiresEachFullMaterialCount(GameTestHelper h) {
        for (var specification : ASSEMBLIES) {
            var recipe = assembly(h, specification.id);
            var inventory = inventory(specification);
            h.assertTrue(recipe.matches(LightningAssemblyRecipeInput.fromInventory(inventory), h.getLevel()), specification.id+" accepts its requested materials");
            var match = recipe.planMatch(LightningAssemblyRecipeInput.fromInventory(inventory)).orElseThrow();
            for (int slot=0; slot<specification.materials.size(); slot++) {
                var material = specification.materials.get(slot);
                h.assertTrue(match.getConsumptionForSlot(slot)==material.count, specification.id+" consumes the full count of "+material.id);
                var original = inventory.getStackInSlot(slot).copy();
                var shortStack = original.copy();
                shortStack.shrink(1);
                inventory.setStackInSlot(slot, shortStack);
                h.assertTrue(!recipe.matches(LightningAssemblyRecipeInput.fromInventory(inventory), h.getLevel()), specification.id+" rejects one missing "+material.id);
                inventory.setStackInSlot(slot, stack("minecraft:dirt", material.count));
                h.assertTrue(!recipe.matches(LightningAssemblyRecipeInput.fromInventory(inventory), h.getLevel()), specification.id+" rejects an incorrect material");
                inventory.setStackInSlot(slot, original);
            }
            h.assertTrue(recipe.totalEnergy()==specification.fe && recipe.lightningCost()==specification.lightning && recipe.lightningTier()==specification.tier, specification.id+" retains requested FE and lightning costs");
            var output = recipe.assemble(LightningAssemblyRecipeInput.fromInventory(inventory), h.getLevel().registryAccess());
            h.assertTrue(output.is(BuiltInRegistries.ITEM.get(ModContent.id(specification.id))) && output.getCount()==1, specification.id+" produces exactly one item");
        }
        h.succeed();
    }

    @GameTest(template="empty")
    public static void collapseCoreRequires512SingularitiesAcrossLargeNativeSlots(GameTestHelper h) {
        var specification = ASSEMBLIES.stream().filter(s -> s.id.equals("collapse_core")).findFirst().orElseThrow();
        var recipe = assembly(h, specification.id);
        var inventory = inventory(specification);
        inventory.setStackInSlot(2, stack("ae2:singularity",300));
        inventory.setStackInSlot(4, stack("ae2:singularity",212));
        var match = recipe.planMatch(LightningAssemblyRecipeInput.fromInventory(inventory)).orElseThrow();
        h.assertTrue(match.getConsumptionForSlot(2)+match.getConsumptionForSlot(4)==512, "native matching consumes all 512 split singularities");
        inventory.setStackInSlot(4, stack("ae2:singularity",211));
        h.assertTrue(!recipe.matches(LightningAssemblyRecipeInput.fromInventory(inventory),h.getLevel()), "511 singularities cannot make a collapse core");
        h.succeed();
    }

    @GameTest(template="empty")
    public static void particleConstrainerConsumes64AlloyIngotsAndRejectsWire(GameTestHelper h) {
        var specification = ASSEMBLIES.stream().filter(s -> s.id.equals("particle_constrainer")).findFirst().orElseThrow();
        var recipe = assembly(h,specification.id);
        var inventory = inventory(specification);
        var match = recipe.planMatch(LightningAssemblyRecipeInput.fromInventory(inventory)).orElseThrow();
        h.assertTrue(match.getConsumptionForSlot(4)==64,"native assembly consumes the confirmed 64 firmament alloy ingots");
        inventory.setStackInSlot(4,stack("ae2lt:firmament_superconducting_wire",64));
        h.assertTrue(!recipe.matches(LightningAssemblyRecipeInput.fromInventory(inventory),h.getLevel()),"superconducting wire does not replace the user's confirmed alloy ingots");
        h.succeed();
    }

    @GameTest(template="empty")
    public static void frameRecipeRequiresLavaAndCountedFactoryIngredients(GameTestHelper h) {
        var raw = recipe(h,"laser_frame");
        h.assertTrue(raw instanceof OverloadProcessingRecipe, "frame uses the native overload processing serializer");
        var recipe = (OverloadProcessingRecipe) raw;
        var inventory = new OverloadProcessingFactoryInventory(null);
        var materials = List.of(material("ae2lt:overload_alloy_plate",8),material("minecraft:netherite_ingot",2),material("minecraft:iron_block",2));
        for (int slot=0; slot<materials.size(); slot++) inventory.setStackInSlot(slot,stack(materials.get(slot).id,materials.get(slot).count));
        var lava = new FluidStack(Fluids.LAVA,4000);
        h.assertTrue(recipe.matches(OverloadProcessingRecipeInput.fromInventory(inventory,lava),h.getLevel()), "4000mB lava and full materials match");
        for (var invalid : List.of(FluidStack.EMPTY,new FluidStack(Fluids.LAVA,3999),new FluidStack(Fluids.WATER,4000))) {
            h.assertTrue(!recipe.matches(OverloadProcessingRecipeInput.fromInventory(inventory,invalid),h.getLevel()), "missing, insufficient or incorrect fluid prevents manufacturing");
        }
        var match = recipe.planMatch(OverloadProcessingRecipeInput.fromInventory(inventory,lava),1).orElseThrow();
        for (int slot=0; slot<materials.size(); slot++) {
            h.assertTrue(match.getConsumptionForSlot(slot)==materials.get(slot).count, "native factory plans the requested material count");
            var original = inventory.getStackInSlot(slot).copy();
            var shortStack = original.copy(); shortStack.shrink(1);
            inventory.setStackInSlot(slot,shortStack);
            h.assertTrue(!recipe.matches(OverloadProcessingRecipeInput.fromInventory(inventory,lava),h.getLevel()), "each counted factory material is necessary");
            inventory.setStackInSlot(slot,original);
        }
        h.assertTrue(recipe.totalEnergy()==100_000 && recipe.lightningCost()==32 && recipe.lightningTier()==HV,"frame costs 100k FE and 32 HV");
        h.succeed();
    }

    @GameTest(template="laser_empty",timeoutTicks=400)
    public static void actualFactoryConsumesFourBucketsAndMakesOneFrame(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(4,2,4));
        h.getLevel().setBlockAndUpdate(pos,BuiltInRegistries.BLOCK.get(ResourceLocation.parse("ae2lt:overload_processing_factory")).defaultBlockState());
        h.getLevel().setBlockAndUpdate(pos.west(),AEBlocks.CREATIVE_ENERGY_CELL.block().defaultBlockState());
        h.getLevel().setBlockAndUpdate(pos.east(),AEBlocks.DRIVE.block().defaultBlockState());
        var drive = (DriveBlockEntity) h.getLevel().getBlockEntity(pos.east());
        var cell = stack("ae2lt:infinite_storage_cell",1);
        drive.getInternalInventory().setItemDirect(0,cell);
        var storage = StorageCells.getCellInventory(cell,drive::saveChanges);
        h.assertTrue(storage.insert(LightningKey.HIGH_VOLTAGE,32,Actionable.MODULATE,IActionSource.empty())==32,"native cell stores 32 HV");
        storage.persist();
        var factory = (OverloadProcessingFactoryBlockEntity) h.getLevel().getBlockEntity(pos);
        h.runAtTickTime(40, () -> {
            h.assertTrue(factory.getMainNode().isActive(),"native factory joins a powered real grid");
            h.assertTrue(factory.getUpgrades().addItems(AEItems.SPEED_CARD.stack(4)).isEmpty(),"four native speed cards keep the processing test bounded");
            var inventory = factory.getInventory();
            inventory.setStackInSlot(0,stack("ae2lt:overload_alloy_plate",9));
            inventory.setStackInSlot(1,stack("minecraft:netherite_ingot",3));
            inventory.setStackInSlot(2,stack("minecraft:iron_block",3));
            var fluidHandler = factory.getFluidHandlerCapability(null);
            h.assertTrue(fluidHandler.fill(new FluidStack(Fluids.LAVA,3999),FluidAction.EXECUTE)==3999,"native input tank accepts lava");
            h.assertTrue(factory.findProcessableRecipe().isEmpty(),"native machine refuses 3999mB before spending inputs");
            fluidHandler.fill(new FluidStack(Fluids.LAVA,101),FluidAction.EXECUTE);
            var candidate = factory.findProcessableRecipe().orElseThrow();
            h.assertTrue(candidate.recipe().id().equals(ModContent.id("laser_frame")),"native service chooses the requested frame recipe");
            h.assertTrue(factory.getEnergyStorage().receiveEnergy(100_000,false)==100_000,"native FE buffer receives one operation's cost");
        });
        h.succeedWhen(() -> {
            var output = factory.getInventory().getStackInSlot(OverloadProcessingFactoryInventory.SLOT_OUTPUT_0);
            h.assertTrue(output.is(BuiltInRegistries.ITEM.get(ModContent.id("laser_frame"))) && output.getCount()==1,"native server processing produces exactly one laser frame");
            for (int slot=0; slot<3; slot++) h.assertTrue(factory.getInventory().getStackInSlot(slot).getCount()==1,"native processing consumes precisely each recipe count");
            h.assertTrue(factory.getInputFluid().getFluid()==Fluids.LAVA && factory.getInputFluid().getAmount()==100,"native processing drains exactly 4000mB lava");
            h.assertTrue(factory.getEnergyStorage().getEnergyStored()==0,"native processing spends exactly 100k FE");
            h.assertTrue(factory.getAvailableHighVoltage()==0,"native processing spends exactly 32 HV");
        });
    }

    @GameTest(template="empty")
    public static void laserGlassUsesAFrameSurroundedByEightQuartzGlass(GameTestHelper h) {
        var raw = recipe(h,"laser_glass");
        h.assertTrue(raw instanceof ShapedRecipe,"glass uses shaped crafting");
        var recipe = (ShapedRecipe) raw;
        var grid = new ArrayList<ItemStack>();
        for (int slot=0; slot<9; slot++) grid.add(stack(slot==4?"overload_sim:laser_frame":"ae2:quartz_glass",1));
        h.assertTrue(recipe.matches(CraftingInput.of(3,3,grid),h.getLevel()),"frame in the center and eight quartz glasses craft");
        var output = recipe.assemble(CraftingInput.of(3,3,grid),h.getLevel().registryAccess());
        h.assertTrue(output.is(BuiltInRegistries.ITEM.get(ModContent.id("laser_glass"))) && output.getCount()==1,"crafting yields one glass");
        for (int slot=0; slot<9; slot++) {
            var original = grid.get(slot);
            grid.set(slot,ItemStack.EMPTY);
            h.assertTrue(!recipe.matches(CraftingInput.of(3,3,grid),h.getLevel()),"all nine crafting positions are required");
            grid.set(slot,original);
        }
        grid.set(0,stack("ae2:quartz_vibrant_glass",1));
        h.assertTrue(!recipe.matches(CraftingInput.of(3,3,grid),h.getLevel()),"vibrant glass cannot replace the specified quartz glass");
        h.succeed();
    }

    @GameTest(template="empty")
    public static void legacyWorkstationPartRecoveryRecipesAreRemoved(GameTestHelper h) {
        for (var id : List.of("high_energy_workstation_frame_to_laser_frame","high_energy_workstation_glass_to_laser_glass","high_energy_workstation_receiver_to_laser_optical_port")) {
            h.assertTrue(h.getLevel().getRecipeManager().byKey(ModContent.id(id)).isEmpty(),"removed legacy recovery recipe stays absent: "+id);
        }
        h.succeed();
    }
}
