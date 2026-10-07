package dev.overloadsim.gametest;

import com.moakiee.ae2lt.registry.ModRecipeTypes;
import dev.overloadsim.ModContent;
import dev.overloadsim.OverloadSimulation;
import dev.overloadsim.workstation.HighEnergyWorkstationRecipe;
import dev.overloadsim.workstation.WorkstationContent;
import dev.overloadsim.workstation.WorkstationRecipes;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.fluids.FluidStack;

@GameTestHolder(OverloadSimulation.ID)
@PrefixGameTestTemplate(false)
public final class TranscendentCrystalGameTests {
    private static final ResourceLocation RECIPE = ModContent.id("transcendent_crystal");

    private static Item overloadCrystal() {
        return BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse("ae2lt:overload_crystal")).orElseThrow();
    }

    private static RecipeHolder<?> loadedRecipe(GameTestHelper helper) {
        var loaded = helper.getLevel().getRecipeManager().byKey(RECIPE);
        helper.assertTrue(loaded.isPresent(), "the shipped transcendent crystal recipe must load from the datapack");
        helper.assertTrue(loaded.orElseThrow().value() instanceof HighEnergyWorkstationRecipe,
                "transcendent crystals require the dedicated workstation recipe type");
        return loaded.orElseThrow();
    }

    private static void withLoadedRecipe(GameTestHelper helper, Runnable action) {
        var recipe = loadedRecipe(helper);
        var manager = helper.getLevel().getRecipeManager();
        var original = List.copyOf(manager.getRecipes());
        manager.replaceRecipes(List.of(recipe));
        WorkstationRecipes.invalidate();
        try {
            action.run();
        } finally {
            manager.replaceRecipes(original);
            WorkstationRecipes.invalidate();
        }
    }

    @GameTest(template = "laser_empty")
    public static void shippedCrystalRecipeIsWorkstationOnly(GameTestHelper helper) {
        var recipe = loadedRecipe(helper);
        var manager = helper.getLevel().getRecipeManager();
        helper.assertTrue(manager.getAllRecipesFor(WorkstationContent.RECIPE_TYPE.get()).stream()
                .anyMatch(holder -> holder.id().equals(RECIPE)), "the production recipe is available to workstations");
        helper.assertTrue(manager.getAllRecipesFor(ModRecipeTypes.OVERLOAD_PROCESSING_TYPE.get()).stream()
                .noneMatch(holder -> holder.id().equals(RECIPE)), "the new recipe must not enter the original processing factory");
        helper.assertTrue(WorkstationRecipes.find(manager, RECIPE).orElseThrow().recipe()
                == ((HighEnergyWorkstationRecipe) recipe.value()).delegate(), "workstation executes the loaded production object");
        helper.succeed();
    }

    @GameTest(template = "laser_empty")
    public static void fullCatalogSelectsCrystalRecipeFromFourNormalStacks(GameTestHelper helper) {
        loadedRecipe(helper);
        var host = WorkstationGameTests.build(helper, Direction.NORTH);
        var automation = host.automation(Direction.UP);
        for (int slot = 0; slot < 4; slot++) {
            helper.assertTrue(automation.insertItem(slot, new ItemStack(overloadCrystal(), 64), false).isEmpty(),
                    "each ordinary stack of 64 overload crystals can enter an independent input slot");
        }
        var selected = WorkstationRecipes.select(helper.getLevel().getRecipeManager(), host.inputs(), host.outputs(),
                FluidStack.EMPTY, FluidStack.EMPTY);
        helper.assertTrue(selected.isPresent(), "four stacks satisfy the loaded crystal recipe in the complete production catalog");
        helper.assertTrue(selected.orElseThrow().entry().id().equals(RECIPE),
                "the complete catalog must select transcendent crystals rather than an inherited competing recipe: "
                        + selected.orElseThrow().entry().id());
        host.receiveLaser(Direction.NORTH, 1_000_000_001L, helper.getLevel().getGameTime());
        host.tick();
        for (int slot = 0; slot < 4; slot++) {
            helper.assertTrue(host.inputs().count(slot) == 64, "first tick preserves every ordinary input stack");
        }
        host.tick();
        helper.assertTrue(host.inputs().empty() && host.outputs().count(0) == 4
                && host.outputs().prototype(0).is(BuiltInRegistries.ITEM.get(RECIPE))
                && host.lastRecipe().equals(RECIPE.toString()),
                "the production workstation consumes four normal stacks and emits exactly four transcendent crystals");
        helper.succeed();
    }

    @GameTest(template = "laser_empty")
    public static void fewerThan256CrystalsCannotStart(GameTestHelper helper) {
        withLoadedRecipe(helper, () -> {
            var host = WorkstationGameTests.build(helper, Direction.NORTH);
            host.inputs().insert(0, new ItemStack(overloadCrystal()), 255, false);
            host.receiveLaser(Direction.NORTH, 1_000_000_001L, helper.getLevel().getGameTime());
            host.tick();
            host.tick();
            helper.assertTrue(host.inputs().count(0) == 255 && host.outputs().empty() && host.progress() == 0,
                    "255 overload crystals remain untouched and produce no transcendent crystal");
        });
        helper.succeed();
    }

    @GameTest(template = "laser_empty")
    public static void twoStrongTicksConvert256ToFour(GameTestHelper helper) {
        withLoadedRecipe(helper, () -> {
            var host = WorkstationGameTests.build(helper, Direction.NORTH);
            host.inputs().insert(0, new ItemStack(overloadCrystal()), 256, false);
            host.receiveLaser(Direction.NORTH, 1_000_000_000L, helper.getLevel().getGameTime());
            host.tick();
            host.tick();
            helper.assertTrue(host.inputs().count(0) == 256 && host.outputs().empty(),
                    "a beam at the exact threshold cannot produce crystals");
            host.receiveLaser(Direction.NORTH, 1_000_000_001L, helper.getLevel().getGameTime());
            host.tick();
            helper.assertTrue(host.inputs().count(0) == 256 && host.outputs().empty() && host.progress() == 1,
                    "first strong tick reserves the job without spending materials");
            host.tick();
            helper.assertTrue(host.inputs().empty() && host.outputs().count(0) == 4
                    && host.outputs().prototype(0).is(BuiltInRegistries.ITEM.get(RECIPE))
                    && host.lastParallel() == 1 && host.lastRecipe().equals(RECIPE.toString()),
                    "second strong tick spends exactly 256 overload crystals for four transcendent crystals");
            helper.assertTrue(host.outputs().prototype(0).getMaxStackSize() == 64,
                    "transcendent crystals can be extracted in ordinary stacks of 64");
        });
        helper.succeed();
    }

    @GameTest(template = "laser_empty")
    public static void maxParallelCrystalBatchUsesExactMaterialCostWithoutNetworkFees(GameTestHelper helper) {
        withLoadedRecipe(helper, () -> {
            var host = WorkstationGameTests.build(helper, Direction.NORTH);
            host.inputs().insert(0, new ItemStack(overloadCrystal()), 262_245, false);
            helper.assertTrue(!host.getMainNode().isActive(), "no network FE or lightning is available for this workstation");
            host.receiveLaser(Direction.NORTH, 1_000_000_001L, helper.getLevel().getGameTime());
            host.tick();
            helper.assertTrue(host.inputs().count(0) == 262_245 && host.outputs().empty(),
                    "the first tick never charges any of the batch materials");
            host.tick();
            helper.assertTrue(host.inputs().count(0) == 101 && host.outputs().count(0) == 4096
                    && host.outputs().prototype(0).is(BuiltInRegistries.ITEM.get(RECIPE))
                    && host.lastParallel() == 1024,
                    "1024 operations debit 262144 overload crystals and produce 4096 crystals using only laser power");
        });
        helper.succeed();
    }
}
