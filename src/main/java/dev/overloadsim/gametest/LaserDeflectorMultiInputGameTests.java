package dev.overloadsim.gametest;

import com.moakiee.ae2lt.machine.overloadfactory.recipe.OverloadProcessingIngredient;
import com.moakiee.ae2lt.machine.overloadfactory.recipe.OverloadProcessingRecipe;
import com.moakiee.ae2lt.me.key.LightningKey;
import dev.overloadsim.ModContent;
import dev.overloadsim.OverloadSimulation;
import dev.overloadsim.api.LaserExtensions;
import dev.overloadsim.core.LaserMath;
import dev.overloadsim.laser.LaserKind;
import dev.overloadsim.laser.LaserMachineBlockEntity;
import dev.overloadsim.laser.LaserNetwork;
import dev.overloadsim.workstation.HighEnergyWorkstationBlockEntity;
import dev.overloadsim.workstation.WorkstationStructure;
import dev.overloadsim.workstation.WorkstationRecipes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import dev.overloadsim.api.LaserEvents;

/** Real paid sources and deflector ports, with exact FE settlement at external target hooks. */
@GameTestHolder(OverloadSimulation.ID)
@PrefixGameTestTemplate(false)
public final class LaserDeflectorMultiInputGameTests {
    @GameTest(template="laser_empty",batch="configuration_laser")
    public static void longerConstraintRangeStillDiscoversAndSumsEverySource(GameTestHelper h){
        var setting=dev.overloadsim.config.MachineSettings.LASER_CONSTRAINT_RANGE;int old=setting.get();
        setting.set(10);setting.clearCache();
        try{
            var matrix=LaserMachineGameTests.build(h,h.absolutePos(new BlockPos(23,5,12)),LaserKind.DEFLECTOR,Direction.NORTH);
            var west=LaserMachineGameTests.build(h,h.absolutePos(new BlockPos(3,8,12)),LaserKind.EMITTER,Direction.WEST);
            var constraint=LaserMachineGameTests.build(h,h.absolutePos(new BlockPos(10,5,12)),LaserKind.CONSTRAINT,Direction.WEST);
            var north=LaserMachineGameTests.build(h,h.absolutePos(new BlockPos(23,8,2)),LaserKind.EMITTER,Direction.NORTH);
            matrix.setSideMode(Direction.WEST,LaserMachineBlockEntity.SIDE_INPUT);matrix.setSideMode(Direction.NORTH,LaserMachineBlockEntity.SIDE_INPUT);
            matrix.setSideMode(Direction.EAST,LaserMachineBlockEntity.SIDE_OUTPUT);matrix.setSideMode(Direction.SOUTH,LaserMachineBlockEntity.SIDE_OUTPUT);
            for(var node:List.of(matrix,west,constraint,north)){node.checkStructure();h.assertTrue(node.formed(),"extended-range fixture forms "+node.kind());node.energy().load(400_000);}
            var east=matrix.center().east(5);var south=matrix.center().south(5);target(h,east);target(h,south);
            try(var f=new Fixture(h,west,north,matrix,east,south)){
                LaserNetwork.processEmitter(west);LaserNetwork.processEmitter(north);
                h.assertTrue(matrix.currentPower()==200_000&&f.received(east)==100_000&&f.received(south)==100_000,"graph discovery follows the configured ten-block constraint range and merges both inputs exactly once");
                h.assertTrue(west.energy().stored()==300_000&&north.energy().stored()==300_000&&constraint.energy().stored()==300_000,"all source and modifier costs settle once");
            }
        }finally{setting.set(old);setting.clearCache();}h.succeed();
    }
    private record TargetKey(ServerLevel level, BlockPos position) {}
    private static final Map<TargetKey, List<LaserExtensions.TargetContext>> RECEIVED = new HashMap<>();
    private static final Map<TargetKey, Long> MODIFIED_POWER = new HashMap<>();
    private static final Map<TargetKey, List<LaserExtensions.TargetContext>> DEACTIVATED = new HashMap<>(), REFRESHED = new HashMap<>();
    private static final Map<TargetKey, Consumer<LaserExtensions.TargetContext>> ON_ACCEPT = new HashMap<>();
    private static final Map<TargetKey, Consumer<LaserExtensions.TargetContext>> ON_REFRESH = new HashMap<>();
    private static boolean hookRegistered;

    private record Fixture(GameTestHelper helper, LaserMachineBlockEntity west, LaserMachineBlockEntity north,
            LaserMachineBlockEntity matrix, BlockPos eastTarget, BlockPos southTarget) implements AutoCloseable {
        long received(BlockPos position) {
            return RECEIVED.get(new TargetKey(helper.getLevel(), position)).stream()
                    .mapToLong(LaserExtensions.TargetContext::energy).sum();
        }
        @Override public void close() {
            west.setEnabled(false); north.setEnabled(false);
            RECEIVED.remove(new TargetKey(helper.getLevel(), eastTarget));
            RECEIVED.remove(new TargetKey(helper.getLevel(), southTarget));
            for (var pos : List.of(eastTarget, southTarget)) {
                var key = new TargetKey(helper.getLevel(), pos); DEACTIVATED.remove(key); REFRESHED.remove(key); ON_ACCEPT.remove(key); ON_REFRESH.remove(key);
            }
        }
    }

    private static void target(GameTestHelper h, BlockPos position) {
        if (!hookRegistered) {
            LaserExtensions.registerTarget(BuiltInRegistries.BLOCK.getKey(Blocks.PINK_GLAZED_TERRACOTTA), new LaserExtensions.LaserTarget() {
                @Override public boolean accept(LaserExtensions.TargetContext context) {
                    var key = new TargetKey(context.level(), context.position()); var received = RECEIVED.get(key);
                    if (received == null) return false;
                    received.add(context); var action = ON_ACCEPT.get(key); if (action != null) action.accept(context); return true;
                }
                @Override public void deactivate(LaserExtensions.TargetContext context) {
                    var records = DEACTIVATED.get(new TargetKey(context.level(), context.position())); if (records != null) records.add(context);
                }
                @Override public void refreshLease(LaserExtensions.TargetContext context) {
                    var key = new TargetKey(context.level(), context.position());
                    var records = REFRESHED.get(key); if (records != null) records.add(context);
                    var action = ON_REFRESH.get(key); if (action != null) action.accept(context);
                }
            });
            LaserExtensions.registerModifier(BuiltInRegistries.BLOCK.getKey(Blocks.LIME_GLAZED_TERRACOTTA), context -> {
                var power = MODIFIED_POWER.get(new TargetKey(context.level(), context.position()));
                return power == null ? null : new LaserExtensions.ModifierResult(power, context.range());
            });
            hookRegistered = true;
        }
        RECEIVED.put(new TargetKey(h.getLevel(), position), new ArrayList<>());
        DEACTIVATED.put(new TargetKey(h.getLevel(), position), new ArrayList<>());
        REFRESHED.put(new TargetKey(h.getLevel(), position), new ArrayList<>());
        h.getLevel().setBlockAndUpdate(position, Blocks.PINK_GLAZED_TERRACOTTA.defaultBlockState());
    }

    private static Fixture fixture(GameTestHelper h) {
        var matrix = LaserMachineGameTests.build(h, h.absolutePos(new BlockPos(12, 2, 12)), LaserKind.DEFLECTOR, Direction.NORTH);
        var west = LaserMachineGameTests.build(h, h.absolutePos(new BlockPos(2, 5, 12)), LaserKind.EMITTER, Direction.WEST);
        var north = LaserMachineGameTests.build(h, h.absolutePos(new BlockPos(12, 5, 2)), LaserKind.EMITTER, Direction.NORTH);
        matrix.setSideMode(Direction.WEST, LaserMachineBlockEntity.SIDE_INPUT);
        matrix.setSideMode(Direction.NORTH, LaserMachineBlockEntity.SIDE_INPUT);
        matrix.setSideMode(Direction.EAST, LaserMachineBlockEntity.SIDE_OUTPUT);
        matrix.setSideMode(Direction.SOUTH, LaserMachineBlockEntity.SIDE_OUTPUT);
        for (var node : List.of(west, north, matrix)) {
            node.checkStructure();
            h.assertTrue(node.formed(), "multi-input fixture forms " + node.kind());
        }
        west.energy().load(400_000); north.energy().load(400_000);
        var east = matrix.center().east(5); var south = matrix.center().south(5);
        target(h, east); target(h, south);
        return new Fixture(h, west, north, matrix, east, south);
    }

    @GameTest(template = "laser_empty")
    public static void twoSourcesThroughDifferentInputPortsConserveTotalOutput(GameTestHelper h) {
        try (var f = fixture(h)) {
            LaserNetwork.processEmitter(f.west());
            LaserNetwork.processEmitter(f.north());
            h.assertTrue(f.received(f.eastTarget()) == 100_000 && f.received(f.southTarget()) == 100_000,
                    "each output actually settles both 50k input shares in the same server batch");
            h.assertTrue(f.matrix().currentPower() == 200_000, "matrix reports the total of both accepted inputs");
            h.assertTrue(f.west().energy().stored() == 300_000 && f.north().energy().stored() == 300_000,
                    "each source pays exactly one 100k input batch");
        }
        h.succeed();
    }

    @GameTest(template = "laser_empty")
    public static void stoppingOneSourcePreservesTheOtherSourcesMatrixLease(GameTestHelper h) {
        try (var f = fixture(h)) {
            LaserNetwork.processEmitter(f.west());
            LaserNetwork.processEmitter(f.north());
            f.west().setEnabled(false);
            h.assertTrue(f.matrix().currentPower() == 100_000 && f.matrix().activeUntil() > h.getLevel().getGameTime(),
                    "stopping one source leaves the other accepted input active at its own power");
            h.assertTrue(!f.north().segments().isEmpty(), "the surviving source retains its output segments");
            f.north().setEnabled(false);
            h.assertTrue(f.matrix().currentPower() == 0, "the final source stop clears the matrix lease");
        }
        h.succeed();
    }

    @GameTest(template = "laser_empty")
    public static void anUnpaidSourceCannotStopTheOtherPaidInput(GameTestHelper h) {
        try (var f = fixture(h)) {
            f.north().energy().load(0);
            LaserNetwork.processEmitter(f.north()); LaserNetwork.processEmitter(f.west());
            h.assertTrue(f.matrix().currentPower() == 100_000 && f.received(f.eastTarget()) == 50_000
                            && f.received(f.southTarget()) == 50_000,
                    "starting the connected batch from an unpaid emitter still settles the independent paid input");
            h.assertTrue(f.west().energy().stored() == 300_000 && f.north().energy().stored() == 0,
                    "the unpaid input does not debit or cancel the paid source");
        }
        h.succeed();
    }

    @GameTest(template = "laser_empty")
    public static void blockingOnlyOneInputDoesNotInvalidateTheSurvivingSourcePath(GameTestHelper h) {
        try (var f = fixture(h)) {
            LaserNetwork.processEmitter(f.west()); LaserNetwork.processEmitter(f.north());
            h.getLevel().setBlockAndUpdate(f.west().center().east(4), Blocks.STONE.defaultBlockState());
            LaserNetwork.tick(f.west()); LaserNetwork.tick(f.north());
            h.assertTrue(f.west().segments().isEmpty() && !f.north().segments().isEmpty() && f.matrix().currentPower() == 100_000,
                    "each source watches its own input cells, so blocking west preserves north's 100k output");
        }
        h.succeed();
    }

    @GameTest(template = "laser_empty")
    public static void enablingAnotherSourceCannotReopenTheSettledMatrixInTheSameTick(GameTestHelper h) {
        try (var f = fixture(h)) {
            f.north().setEnabled(false);
            LaserNetwork.processEmitter(f.west());
            f.north().setEnabled(true);
            LaserNetwork.processEmitter(f.north());
            h.assertTrue(f.west().energy().stored() == 300_000 && f.north().energy().stored() == 400_000,
                    "a late enabled input waits for the next batch and cannot charge the already settled source again");
            h.assertTrue(f.matrix().currentPower() == 100_000 && f.received(f.eastTarget()) == 50_000
                            && f.received(f.southTarget()) == 50_000,
                    "the same physical matrix cannot duplicate its already settled output in the same server tick");
        }
        h.succeed();
    }

    @GameTest(template = "laser_empty")
    public static void aTargetCallbackRemovingOneSourceReducesTheRemainingOutputAndAcceptedLease(GameTestHelper h) {
        try (var f = fixture(h)) {
            ON_ACCEPT.put(new TargetKey(h.getLevel(), f.southTarget()), context -> f.west().setEnabled(false));
            LaserNetwork.processEmitter(f.west());
            h.assertTrue(f.received(f.southTarget()) == 100_000 && f.received(f.eastTarget()) == 50_000,
                    "after the first output callback stops west, the later physical output contains only north's 50k share");
            var refreshed = REFRESHED.get(new TargetKey(h.getLevel(), f.southTarget()));
            h.assertTrue(refreshed.size() == 1 && refreshed.getFirst().energy() == 50_000,
                    "the already accepted target lease is refreshed to the surviving share without settling FE again");
        }
        h.succeed();
    }

    @GameTest(template = "laser_empty")
    public static void stoppingTheFinalSourceInsideLeaseRefreshRevokesEachTargetExactlyOnce(GameTestHelper h) {
        try (var f = fixture(h)) {
            var first = new TargetKey(h.getLevel(), f.southTarget()); var second = new TargetKey(h.getLevel(), f.eastTarget());
            ON_ACCEPT.put(first, context -> f.west().setEnabled(false));
            ON_REFRESH.put(first, context -> f.north().setEnabled(false));
            LaserNetwork.processEmitter(f.west());
            h.assertTrue(DEACTIVATED.get(first).size() == 1 && DEACTIVATED.get(second).size() == 1,
                    "reentrant final-source stop during lease refresh revokes both accepted targets exactly once");
            h.assertTrue(f.matrix().currentPower() == 0 && f.west().segments().isEmpty() && f.north().segments().isEmpty(),
                    "the reentrant lease callback leaves no surviving matrix power or output beam");
        }
        h.succeed();
    }

    private static BlockPos sourceFrame(GameTestHelper h, LaserMachineBlockEntity source) {
        var pos = source.center().above(2);
        h.assertTrue(h.getLevel().getBlockState(pos).is(dev.overloadsim.laser.LaserContent.FRAME.get()), "the callback removes a real required emitter frame");
        return pos;
    }

    @GameTest(template = "laser_deflector_empty")
    public static void breakingTheSourceDuringAModifierEventCannotPayOrDeliverDownstream(GameTestHelper h) {
        try (var f = fixture(h)) {
            f.north().setEnabled(false); f.matrix().setSideMode(Direction.SOUTH, LaserMachineBlockEntity.SIDE_OFF);
            var constraint = LaserMachineGameTests.build(h, f.matrix().center().east(5).below(3), LaserKind.CONSTRAINT, Direction.WEST);
            constraint.checkStructure(); constraint.energy().load(100_000);
            var endpoint = constraint.center().east(3); target(h, endpoint);
            var frame = sourceFrame(h, f.west());
            Consumer<LaserEvents.BeforeModifier> callback = event -> {
                if (event.position.equals(constraint.getBlockPos())) h.getLevel().setBlockAndUpdate(frame, Blocks.AIR.defaultBlockState());
            };
            NeoForge.EVENT_BUS.addListener(callback);
            try {
                LaserNetwork.processEmitter(f.west());
                h.assertTrue(constraint.energy().stored() == 100_000 && RECEIVED.get(new TargetKey(h.getLevel(), endpoint)).isEmpty(),
                        "an event-broken emitter aborts before downstream modifier fees or target settlement");
            } finally {
                NeoForge.EVENT_BUS.unregister(callback); RECEIVED.remove(new TargetKey(h.getLevel(), endpoint));
                DEACTIVATED.remove(new TargetKey(h.getLevel(), endpoint)); REFRESHED.remove(new TargetKey(h.getLevel(), endpoint));
            }
        }
        h.succeed();
    }

    private static void coreCallbackFixture(GameTestHelper h, boolean breakEvent) {
        try (var f = fixture(h)) {
            f.north().setEnabled(false); f.matrix().setSideMode(Direction.SOUTH, LaserMachineBlockEntity.SIDE_OFF);
            var inputModifier = inputPower(h, f.west(), 1_000_000_000L);
            var focus = LaserMachineGameTests.build(h, f.matrix().center().east(5).below(3), LaserKind.FOCUS, Direction.WEST);
            focus.checkStructure();
            var core = LaserMachineGameTests.build(h, focus.center().east(7), LaserKind.CORE, Direction.WEST); core.checkStructure();
            var frame = sourceFrame(h, f.west()); var obstruction = focus.center().east(2);
            if (breakEvent) h.getLevel().setBlockAndUpdate(obstruction, Blocks.STONE.defaultBlockState());
            Consumer<BlockEvent.BreakEvent> onBreak = event -> {
                if (event.getPos().equals(obstruction)) h.getLevel().setBlockAndUpdate(frame, Blocks.AIR.defaultBlockState());
            };
            Consumer<LaserEvents.BeforeCoreEmission> onCore = event -> {
                if (event.position.equals(core.getBlockPos())) h.getLevel().setBlockAndUpdate(frame, Blocks.AIR.defaultBlockState());
            };
            if (breakEvent) NeoForge.EVENT_BUS.addListener(onBreak); else NeoForge.EVENT_BUS.addListener(onCore);
            try {
                LaserNetwork.processEmitter(f.west());
                h.assertTrue(core.coreRemainder() == 0 && core.currentPower() == 0 && core.collapseStartTick() == -1,
                        "breaking the source inside the focus/core callback cannot commit core energy or animation");
            } finally {
                NeoForge.EVENT_BUS.unregister(breakEvent ? onBreak : onCore); MODIFIED_POWER.remove(new TargetKey(h.getLevel(), inputModifier));
            }
        }
    }
    @GameTest(template = "laser_deflector_empty")
    public static void breakingTheSourceDuringFocusBlockProtectionCannotSettleTheCore(GameTestHelper h) { coreCallbackFixture(h, true); h.succeed(); }

    @GameTest(template = "laser_deflector_empty")
    public static void breakingTheSourceDuringCoreEmissionCannotCommitCoreEnergy(GameTestHelper h) { coreCallbackFixture(h, false); h.succeed(); }

    @GameTest(template = "laser_empty")
    public static void joiningABatchRevokesTheSourcesPreviousOrdinaryTarget(GameTestHelper h) {
        var source = LaserMachineGameTests.build(h, h.absolutePos(new BlockPos(2, 5, 12)), LaserKind.EMITTER, Direction.WEST);
        source.checkStructure(); source.energy().load(400_000);
        var originalTarget = source.center().east(5); target(h, originalTarget);
        var output = h.absolutePos(new BlockPos(17, 5, 12));
        try {
            LaserNetwork.processEmitter(source);
            h.assertTrue(RECEIVED.get(new TargetKey(h.getLevel(), originalTarget)).size() == 1, "the source first owns an ordinary direct target lease");
            matrix(h, h.absolutePos(new BlockPos(12, 5, 12)), new Direction[]{Direction.WEST}, new Direction[]{Direction.EAST});
            target(h, output); LaserNetwork.processEmitter(source);
            h.assertTrue(DEACTIVATED.get(new TargetKey(h.getLevel(), originalTarget)).size() == 1,
                    "moving the same source into a real deflector batch revokes its prior ordinary target exactly once");
        } finally {
            source.setEnabled(false);
            for (var pos : List.of(originalTarget, output)) {
                var key = new TargetKey(h.getLevel(), pos); RECEIVED.remove(key); DEACTIVATED.remove(key); REFRESHED.remove(key);
            }
        }
        h.succeed();
    }

    @GameTest(template = "laser_empty")
    public static void repeatingTheSameInputPortCannotSettleAnotherBatchInTheSameTick(GameTestHelper h) {
        try (var f = fixture(h)) {
            LaserNetwork.processEmitter(f.west());
            LaserNetwork.processEmitter(f.west());
            h.assertTrue(f.received(f.eastTarget()) == 100_000 && f.received(f.southTarget()) == 100_000,
                    "a repeated source call cannot duplicate the connected circuit's paid batch");
            LaserNetwork.processEmitter(f.north());
            h.assertTrue(f.received(f.eastTarget()) == 100_000 && f.received(f.southTarget()) == 100_000,
                    "a repeated west input does not consume the independent north input claim");
        }
        h.succeed();
    }

    private static BlockPos inputPower(GameTestHelper h, LaserMachineBlockEntity source, long power) {
        var pos = source.center().relative(source.forward(), 3);
        MODIFIED_POWER.put(new TargetKey(h.getLevel(), pos), power);
        h.getLevel().setBlockAndUpdate(pos, Blocks.LIME_GLAZED_TERRACOTTA.defaultBlockState());
        return pos;
    }

    private static void withWorkstationRecipe(GameTestHelper h, Runnable action) {
        var manager = h.getLevel().getRecipeManager(); var original = List.copyOf(manager.getRecipes());
        var recipe = new OverloadProcessingRecipe(10_000,
                List.of(new OverloadProcessingIngredient(Ingredient.of(Items.IRON_INGOT), 1)), FluidStack.EMPTY,
                List.of(new ItemStack(Items.GOLD_INGOT, 2)), FluidStack.EMPTY,
                Long.MAX_VALUE, Integer.MAX_VALUE, LightningKey.Tier.EXTREME_HIGH_VOLTAGE);
        var catalog = new ArrayList<RecipeHolder<?>>(original);
        catalog.add(new RecipeHolder<>(ModContent.id("workstation_test/multi_input"), recipe)); manager.replaceRecipes(catalog);
        try { action.run(); } finally { manager.replaceRecipes(original); WorkstationRecipes.invalidate(); }
    }

    @GameTest(template = "laser_empty")
    public static void multipleInputsConserveExactlyOnePfeAtTheBoundary(GameTestHelper h) {
        try (var f = fixture(h)) {
            var westModifier = inputPower(h, f.west(), 500_000_000_000_000L);
            var northModifier = inputPower(h, f.north(), 500_000_000_000_000L);
            try {
                LaserNetwork.processEmitter(f.west());
                LaserNetwork.processEmitter(f.north());
                h.assertTrue(f.received(f.eastTarget()) == 500_000_000_000_000L
                                && f.received(f.southTarget()) == 500_000_000_000_000L,
                        "two half-PFE inputs settle exactly 1PFE, with no overflow or lost input");
                h.assertTrue(f.matrix().currentPower() == LaserMath.MAX_BEAM, "the exact combined 1PFE boundary remains active");
                h.assertTrue(f.west().segments().stream().allMatch(segment -> segment.energy() <= LaserMath.MAX_BEAM)
                                && f.north().segments().stream().allMatch(segment -> segment.energy() <= LaserMath.MAX_BEAM),
                        "every published contribution respects the existing 1PFE beam limit");
            } finally {
                MODIFIED_POWER.remove(new TargetKey(h.getLevel(), westModifier));
                MODIFIED_POWER.remove(new TargetKey(h.getLevel(), northModifier));
            }
        }
        h.succeed();
    }

    @GameTest(template = "laser_deflector_empty")
    public static void mergedOutputTraversesOrdinaryModifiersAndActuallyEmitsParticles(GameTestHelper h) {
        try (var f = fixture(h)) {
            f.matrix().setSideMode(Direction.SOUTH, LaserMachineBlockEntity.SIDE_OFF);
            var westModifier = inputPower(h, f.west(), 256_000_000_000L);
            var northModifier = inputPower(h, f.north(), 256_000_000_000L);
            var constraint = LaserMachineGameTests.build(h, f.matrix().center().east(5).below(3), LaserKind.CONSTRAINT, Direction.WEST);
            var enhancement = LaserMachineGameTests.build(h, f.matrix().center().east(7).below(3), LaserKind.ENHANCEMENT, Direction.WEST);
            var focus = LaserMachineGameTests.build(h, f.matrix().center().east(9).below(3), LaserKind.FOCUS, Direction.WEST);
            var core = LaserMachineGameTests.build(h, focus.center().east(7), LaserKind.CORE, Direction.WEST);
            var receiver = LaserMachineGameTests.build(h, core.getBlockPos().east(6).below(3), LaserKind.RECEIVER, Direction.WEST);
            for (var node : List.of(constraint, enhancement, focus, core, receiver)) {
                node.checkStructure(); h.assertTrue(node.formed(), "ordinary downstream fixture forms " + node.kind());
            }
            constraint.energy().load(100_000); enhancement.energy().load(100_000);
            try {
                LaserNetwork.processEmitter(f.west()); LaserNetwork.processEmitter(f.north());
                h.assertTrue(focus.currentPower() == 512_000_100_000L && core.currentPower() == 512_000_100_000L,
                        "one merged 512G beam crosses the real constraint, enhancement and focus exactly once");
                h.assertTrue(constraint.energy().stored() == 0 && enhancement.energy().stored() == 0,
                        "each ordinary modifier pays once for the combined batch");
                h.assertTrue(receiver.particles().stored() == 1 && core.coreRemainder() == 100_000,
                        "the real core settles one whole particle from both half-particle inputs and preserves exact remainder");
                long renderedParticles = java.util.stream.Stream.concat(f.west().segments().stream(), f.north().segments().stream())
                        .filter(segment -> segment.particle()).mapToLong(segment -> segment.particleCount()).sum();
                h.assertTrue(renderedParticles == 1, "all emitter visuals publish the one settled particle only once");
                f.west().setEnabled(false);
                h.assertTrue(focus.currentPower() == 256_000_050_000L && core.currentPower() == 256_000_050_000L
                                && receiver.currentPower() == 256_000_050_000L,
                        "stopping one contributor preserves the other contributor's downstream core and receiver lease");
            } finally {
                MODIFIED_POWER.remove(new TargetKey(h.getLevel(), westModifier)); MODIFIED_POWER.remove(new TargetKey(h.getLevel(), northModifier));
                receiver.particles().load(0);
            }
        }
        h.succeed();
    }

    @GameTest(template = "laser_deflector_empty")
    public static void twoWeakInputsMergeAboveTheWorkstationsSingleBeamThreshold(GameTestHelper h) {
        try (var f = fixture(h)) {
            f.matrix().setSideMode(Direction.SOUTH, LaserMachineBlockEntity.SIDE_OFF);
            var controller = f.matrix().center().east(11).below(3);
            for (var e : WorkstationStructure.template(controller, Direction.EAST).entrySet())
                h.getLevel().setBlockAndUpdate(e.getKey(), e.getValue());
            var workstation = (HighEnergyWorkstationBlockEntity) h.getLevel().getBlockEntity(controller);
            workstation.checkStructure();
            h.assertTrue(workstation.formed() && workstation.receiver().equals(f.eastTarget()), "merged beam meets the real workstation port");
            var westModifier = inputPower(h, f.west(), 600_000_000L);
            var northModifier = inputPower(h, f.north(), 600_000_000L);
            try {
                LaserNetwork.processEmitter(f.west()); LaserNetwork.processEmitter(f.north());
                h.assertTrue(workstation.currentPower() == 1_200_000_000L,
                        "two sub-threshold 600M inputs become one physical 1.2G workstation beam");
                withWorkstationRecipe(h, () -> {
                    workstation.inputs().insert(0, new ItemStack(Items.IRON_INGOT), 1024, false);
                    workstation.tick(); workstation.tick();
                    h.assertTrue(workstation.inputs().count(0) == 0 && workstation.outputs().count(0) == 2048,
                            "the physically merged above-threshold beam actually processes one real material batch");
                });
                f.west().setEnabled(false);
                h.assertTrue(workstation.currentPower() == 600_000_000L,
                        "stopping one contributor immediately lowers the shared workstation lease without delivering energy again");
                f.north().setEnabled(false);
                h.assertTrue(workstation.currentPower() == 0, "stopping the final contributor revokes the combined source lease");
            } finally {
                MODIFIED_POWER.remove(new TargetKey(h.getLevel(), westModifier)); MODIFIED_POWER.remove(new TargetKey(h.getLevel(), northModifier));
            }
        }
        h.succeed();
    }

    @GameTest(template = "laser_empty")
    public static void combinedInputAboveOnePfeStopsTheWholeMatrixBeforeDelivery(GameTestHelper h) {
        try (var f = fixture(h)) {
            var westModifier = inputPower(h, f.west(), 500_000_000_000_000L);
            var northModifier = inputPower(h, f.north(), 500_000_000_000_001L);
            try {
                LaserNetwork.processEmitter(f.west()); LaserNetwork.processEmitter(f.north());
                h.assertTrue(f.received(f.eastTarget()) == 0 && f.received(f.southTarget()) == 0
                                && f.matrix().currentPower() == 0 && f.matrix().status().equals("beam_limit"),
                        "1PFE plus one FE aborts the complete matrix output before any partial input is delivered");
            } finally {
                MODIFIED_POWER.remove(new TargetKey(h.getLevel(), westModifier)); MODIFIED_POWER.remove(new TargetKey(h.getLevel(), northModifier));
            }
        }
        h.succeed();
    }

    private static LaserMachineBlockEntity matrix(GameTestHelper h, BlockPos center, Direction[] inputs, Direction[] outputs) {
        var matrix = LaserMachineGameTests.build(h, center.below(3), LaserKind.DEFLECTOR, Direction.NORTH);
        for (var side : Direction.Plane.HORIZONTAL) matrix.setSideMode(side, LaserMachineBlockEntity.SIDE_OFF);
        for (var side : inputs) matrix.setSideMode(side, LaserMachineBlockEntity.SIDE_INPUT);
        for (var side : outputs) matrix.setSideMode(side, LaserMachineBlockEntity.SIDE_OUTPUT);
        matrix.checkStructure();
        h.assertTrue(matrix.formed(), "routing fixture deflector forms");
        return matrix;
    }

    @GameTest(template = "laser_deflector_empty")
    public static void twoBranchesFromOneEmitterCanEnterDifferentPortsOfTheSameMatrix(GameTestHelper h) {
        var firstCenter = h.absolutePos(new BlockPos(12, 5, 12));
        matrix(h, firstCenter, new Direction[]{Direction.WEST}, new Direction[]{Direction.EAST, Direction.SOUTH});
        matrix(h, firstCenter.east(8), new Direction[]{Direction.WEST}, new Direction[]{Direction.SOUTH});
        matrix(h, firstCenter.south(8), new Direction[]{Direction.NORTH}, new Direction[]{Direction.EAST});
        var merged = matrix(h, firstCenter.east(8).south(8), new Direction[]{Direction.NORTH, Direction.WEST}, new Direction[]{Direction.EAST});
        var source = LaserMachineGameTests.build(h, firstCenter.west(10), LaserKind.EMITTER, Direction.WEST);
        source.checkStructure(); source.energy().load(100_000);
        h.assertTrue(source.formed(), "split-and-rejoin emitter forms");
        var target = merged.center().east(5); target(h, target);
        try {
            LaserNetwork.processEmitter(source);
            var arrivals = RECEIVED.get(new TargetKey(h.getLevel(), target));
            h.assertTrue(arrivals.size() == 1 && arrivals.getFirst().energy() == 100_000,
                    "two real 50k branches rejoin into one physical 100k output beam");
            h.assertTrue(merged.currentPower() == 100_000, "same-source fan-in reports both accepted branch inputs");
        } finally {
            source.setEnabled(false); RECEIVED.remove(new TargetKey(h.getLevel(), target));
        }
        h.succeed();
    }

    @GameTest(template = "laser_deflector_empty")
    public static void aLoopReturningThroughAnotherInputPortCannotAmplifyThePaidBeam(GameTestHelper h) {
        var firstCenter = h.absolutePos(new BlockPos(12, 5, 12));
        var first = matrix(h, firstCenter, new Direction[]{Direction.WEST, Direction.SOUTH}, new Direction[]{Direction.EAST, Direction.NORTH});
        matrix(h, firstCenter.east(8), new Direction[]{Direction.WEST}, new Direction[]{Direction.SOUTH});
        matrix(h, firstCenter.east(8).south(8), new Direction[]{Direction.NORTH}, new Direction[]{Direction.WEST});
        matrix(h, firstCenter.south(8), new Direction[]{Direction.EAST}, new Direction[]{Direction.NORTH});
        var source = LaserMachineGameTests.build(h, firstCenter.west(10), LaserKind.EMITTER, Direction.WEST);
        source.checkStructure(); source.energy().load(100_000);
        h.assertTrue(source.formed(), "loop emitter forms");
        var target = first.center().north(5); target(h, target);
        try {
            LaserNetwork.processEmitter(source);
            var arrivals = RECEIVED.get(new TargetKey(h.getLevel(), target));
            h.assertTrue(arrivals.size() == 1 && arrivals.getFirst().energy() == 50_000,
                    "the returning 50k branch stops before revisiting the splitter through its other input");
        } finally {
            source.setEnabled(false); RECEIVED.remove(new TargetKey(h.getLevel(), target));
        }
        h.succeed();
    }
}
