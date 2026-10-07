package dev.overloadsim.gametest;

import com.moakiee.ae2lt.machine.overloadfactory.recipe.OverloadProcessingIngredient;
import com.moakiee.ae2lt.machine.overloadfactory.recipe.OverloadProcessingRecipe;
import com.moakiee.ae2lt.me.key.LightningKey;
import dev.overloadsim.ModContent;
import dev.overloadsim.OverloadSimulation;
import dev.overloadsim.laser.LaserContent;
import dev.overloadsim.laser.LaserKind;
import dev.overloadsim.laser.LaserMachineBlockEntity;
import dev.overloadsim.laser.LaserNetwork;
import dev.overloadsim.laser.LaserSettings;
import dev.overloadsim.workstation.HighEnergyWorkstationBlockEntity;
import dev.overloadsim.workstation.WorkstationRecipes;
import dev.overloadsim.workstation.WorkstationStructure;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Real paid optical paths must resolve the workstation's canonical shared port. */
@GameTestHolder(OverloadSimulation.ID)
@PrefixGameTestTemplate(false)
public final class WorkstationLaserRouteGameTests {
    private static HighEnergyWorkstationBlockEntity workstation(GameTestHelper h) {
        var controller = h.absolutePos(new BlockPos(12, 2, 19));
        for (var entry : WorkstationStructure.template(controller, Direction.SOUTH).entrySet())
            h.getLevel().setBlockAndUpdate(entry.getKey(), entry.getValue());
        var host = (HighEnergyWorkstationBlockEntity) h.getLevel().getBlockEntity(controller);
        host.checkStructure();
        h.assertTrue(host.formed() && host.receiver().equals(h.absolutePos(new BlockPos(12, 5, 13)))
                        && h.getLevel().getBlockState(host.receiver()).is(LaserContent.PORT.get()),
                "the formed workstation receives through the canonical shared optical port");
        return host;
    }

    private static LaserMachineBlockEntity emitter(GameTestHelper h) {
        var source = LaserMachineGameTests.build(h, h.absolutePos(new BlockPos(12, 5, 4)), LaserKind.EMITTER, Direction.NORTH);
        source.checkStructure(); source.energy().load(1_000_000_000L);
        h.assertTrue(source.formed(), "the real emitter forms and has finite physical FE"); return source;
    }

    private static LaserMachineBlockEntity burst(GameTestHelper h) {
        var matrix = LaserMachineGameTests.build(h, h.absolutePos(new BlockPos(12, 2, 11)), LaserKind.BURST, Direction.NORTH);
        matrix.checkStructure(); matrix.setExtremeLightning(64);
        h.assertTrue(matrix.formed(), "the nonfocused burst matrix forms with one paid lightning batch"); return matrix;
    }

    /** Legitimate common configuration is changed only during this synchronous paid batch. */
    private static void withEmitterCost(long amount, Runnable action) {
        try {
            var field = LaserSettings.class.getDeclaredField("EMITTER_FE"); field.setAccessible(true);
            var config = (ModConfigSpec.LongValue) field.get(null); long previous = config.get(); config.set(amount);
            try { action.run(); } finally { config.set(previous); }
        } catch (ReflectiveOperationException error) { throw new IllegalStateException("Cannot set the real emitter batch configuration", error); }
    }

    private static void withRecipe(GameTestHelper h, Runnable action) {
        var manager = h.getLevel().getRecipeManager(); var original = List.copyOf(manager.getRecipes());
        var recipe = new OverloadProcessingRecipe(10_000,
                List.of(new OverloadProcessingIngredient(Ingredient.of(Items.IRON_INGOT), 1)), FluidStack.EMPTY,
                List.of(new ItemStack(Items.GOLD_INGOT, 2)), FluidStack.EMPTY,
                Long.MAX_VALUE, Integer.MAX_VALUE, LightningKey.Tier.EXTREME_HIGH_VOLTAGE);
        var catalog = new ArrayList<RecipeHolder<?>>(original); catalog.add(new RecipeHolder<>(ModContent.id("workstation_test/optical_route"), recipe));
        manager.replaceRecipes(catalog);
        try { action.run(); } finally { manager.replaceRecipes(original); WorkstationRecipes.invalidate(); }
    }

    private static void terminatesAtPort(GameTestHelper h, LaserMachineBlockEntity source, HighEnergyWorkstationBlockEntity host) {
        h.assertTrue(source.segments().stream().noneMatch(segment -> segment.focused() || segment.particle()),
                "ordinary source and burst output reach the workstation without any focusing matrix");
        h.assertTrue(source.segments().stream().anyMatch(segment -> segment.end().equals(host.receiver().getCenter()))
                        && source.segments().stream().allMatch(segment -> segment.end().z <= host.receiver().getZ() + 0.5),
                "a handled beam ends at the actual receiving port without continuing into the workstation");
    }

    @GameTest(template = "laser_pedestal_empty")
    public static void directEmitterReachesCanonicalWorkstationPort(GameTestHelper h) {
        var host = workstation(h); var source = emitter(h);
        try { withEmitterCost(100_000, () -> {
            LaserNetwork.processEmitter(source);
            h.assertTrue(host.currentPower() == 100_000 && source.energy().stored() == 999_900_000L,
                    "a direct ordinary beam registers its exact strength after one real FE payment");
            terminatesAtPort(h, source, host);
        }); } finally { source.setEnabled(false); }
        h.succeed();
    }

    @GameTest(template = "laser_pedestal_empty")
    public static void nonfocusedBurstPowersCanonicalWorkstationWithoutExtraFees(GameTestHelper h) {
        var host = workstation(h); var source = emitter(h); var matrix = burst(h);
        host.inputs().insert(0, new ItemStack(Items.IRON_INGOT), 1024, false);
        try { withEmitterCost(600_000_000L, () -> withRecipe(h, () -> {
            LaserNetwork.processEmitter(source);
            h.assertTrue(host.currentPower() == 1_200_000_000L && source.energy().stored() == 400_000_000L
                            && matrix.extremeLightning() == 0,
                    "the workstation recognizes one nonfocused doubled beam after exactly 600M FE and 64 EHV are paid");
            terminatesAtPort(h, source, host); host.tick(); host.tick();
            h.assertTrue(host.inputs().count(0) == 0 && host.outputs().count(0) == 2048
                            && source.energy().stored() == 400_000_000L && matrix.extremeLightning() == 0,
                    "one qualifying beam permits one material batch without a second FE/lightning debit or duplicate output");
        })); } finally { source.setEnabled(false); }
        h.succeed();
    }

    @GameTest(template = "laser_pedestal_empty")
    public static void routedBeamAtExactThresholdCannotProcessMaterials(GameTestHelper h) {
        var host = workstation(h); var source = emitter(h); var matrix = burst(h);
        host.inputs().insert(0, new ItemStack(Items.IRON_INGOT), 1024, false);
        try { withEmitterCost(500_000_000L, () -> withRecipe(h, () -> {
            LaserNetwork.processEmitter(source);
            h.assertTrue(host.currentPower() == 1_000_000_000L, "a routed threshold-strength beam is displayed exactly");
            host.tick(); host.tick();
            h.assertTrue(host.inputs().count(0) == 1024 && host.outputs().empty() && host.progress() == 0
                            && source.energy().stored() == 500_000_000L && matrix.extremeLightning() == 0,
                    "the shared-port route preserves the strict greater-than-1GFE single-beam processing gate");
        })); } finally { source.setEnabled(false); }
        h.succeed();
    }

    @GameTest(template = "laser_pedestal_empty")
    public static void sourceStopImmediatelyRevokesCanonicalWorkstationPower(GameTestHelper h) {
        var host = workstation(h); var source = emitter(h);
        try { withEmitterCost(100_000, () -> {
            LaserNetwork.processEmitter(source); h.assertTrue(host.currentPower() == 100_000, "the direct source first owns a real power lease");
            source.setEnabled(false);
            h.assertTrue(host.currentPower() == 0 && source.segments().isEmpty() && source.energy().stored() == 999_900_000L,
                    "source stop revokes the shared-port lease immediately without charging a second pulse");
        }); } finally { source.setEnabled(false); }
        h.succeed();
    }

    @GameTest(template = "laser_pedestal_empty")
    public static void obstructionBeforeCanonicalPortCannotBeBypassed(GameTestHelper h) {
        var host = workstation(h); var source = emitter(h); var obstacle = h.absolutePos(new BlockPos(12, 5, 10));
        h.getLevel().setBlockAndUpdate(obstacle, Blocks.STONE.defaultBlockState());
        try { withEmitterCost(100_000, () -> {
            LaserNetwork.processEmitter(source);
            h.assertTrue(host.currentPower() == 0 && source.energy().stored() == 999_900_000L
                            && h.getLevel().getBlockState(obstacle).is(Blocks.STONE)
                            && source.segments().stream().allMatch(segment -> segment.end().z <= obstacle.getZ() + 0.5),
                    "a real broad-beam collision blocks both workstation power and continuation while preserving its paid source batch");
        }); } finally { source.setEnabled(false); }
        h.succeed();
    }

    @GameTest(template = "laser_pedestal_empty")
    public static void looseSharedPortCannotCreateAWorkstationLease(GameTestHelper h) {
        var source = emitter(h); var port = h.absolutePos(new BlockPos(12, 5, 13));
        h.getLevel().setBlockAndUpdate(port, LaserContent.PORT.get().defaultBlockState());
        try { withEmitterCost(100_000, () -> {
            LaserNetwork.processEmitter(source);
            h.assertTrue(source.energy().stored() == 999_900_000L
                            && source.segments().stream().anyMatch(segment -> segment.end().equals(port.getCenter()))
                            && source.segments().stream().allMatch(segment -> segment.end().z <= port.getZ() + 0.5),
                    "an unowned shared optical port remains an ordinary solid beam blocker");
        }); } finally { source.setEnabled(false); }
        h.succeed();
    }

    @GameTest(template = "laser_pedestal_empty")
    public static void ordinaryParticleReceiverKeepsItsSharedPortBehavior(GameTestHelper h) {
        var source = emitter(h);
        var receiver = LaserMachineGameTests.build(h, h.absolutePos(new BlockPos(12, 2, 16)), LaserKind.RECEIVER, Direction.NORTH);
        receiver.checkStructure(); h.assertTrue(receiver.formed(), "ordinary spherical receiver forms with its shared optical port");
        try { withEmitterCost(100_000, () -> {
            LaserNetwork.processEmitter(source);
            h.assertTrue(receiver.particles().stored() == 0 && receiver.energy().stored() == 0 && receiver.currentPower() == 0,
                    "registering the shared port as a workstation target cannot turn an ordinary broad beam into receiver resources");
            var core = LaserMachineGameTests.build(h, h.absolutePos(new BlockPos(12, 5, 10)), LaserKind.CORE, Direction.NORTH);
            core.checkStructure();
            h.assertTrue(LaserNetwork.sendParticles(core, 3, Direction.SOUTH, h.getLevel().getGameTime()) == receiver
                            && receiver.particles().stored() == 3 && source.energy().stored() == 999_900_000L,
                    "the same ordinary shared receiver port still accepts exactly three forward particles without another source charge");
        }); } finally { source.setEnabled(false); receiver.particles().load(0); }
        h.succeed();
    }
}
