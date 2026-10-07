package dev.overloadsim.gametest;

import dev.overloadsim.ModContent;
import dev.overloadsim.OverloadSimulation;
import dev.overloadsim.api.LaserExtensions;
import dev.overloadsim.laser.LaserKind;
import dev.overloadsim.laser.LaserMachineBlockEntity;
import dev.overloadsim.laser.LaserNetwork;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;

import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Each hook uses a different GameTest-only block and accepts only its exact fixture position. */
@GameTestHolder(OverloadSimulation.ID)
@PrefixGameTestTemplate(false)
public final class LaserTargetLifecycleGameTests {
    private static LaserMachineBlockEntity source(GameTestHelper h) {
        var source = LaserMachineGameTests.build(h, h.absolutePos(new BlockPos(4, 5, 8)), LaserKind.EMITTER, Direction.WEST);
        source.checkStructure();
        source.energy().load(4_000_000L);
        h.assertTrue(source.formed(), "lifecycle fixture emitter forms before its first paid pulse");
        return source;
    }

    private static BlockPos targetPosition(LaserMachineBlockEntity source) { return source.center().east(5); }

    private static TargetProbe target(GameTestHelper h, LaserMachineBlockEntity source, String fixtureId) {
        var position = targetPosition(source);
        var id = ModContent.id(fixtureId);
        var block = BuiltInRegistries.BLOCK.get(id);
        h.assertTrue(!block.defaultBlockState().isAir(), "a dedicated GameTest block exists for " + fixtureId);
        h.getLevel().setBlockAndUpdate(position, block.defaultBlockState());
        var probe = new TargetProbe(h, position);
        LaserExtensions.registerTarget(id, probe);
        return probe;
    }

    private static final class TargetProbe implements LaserExtensions.LaserTarget {
        private final GameTestHelper helper;
        private final BlockPos position;
        private final List<LaserExtensions.TargetContext> accepted = new ArrayList<>();
        private final List<LaserExtensions.TargetContext> deactivated = new ArrayList<>();
        private Runnable onAccept = () -> {};

        private TargetProbe(GameTestHelper helper, BlockPos position) { this.helper = helper; this.position = position; }

        @Override public boolean accept(LaserExtensions.TargetContext context) {
            if (context.level() != helper.getLevel() || !context.position().equals(position)) return false;
            accepted.add(context);
            onAccept.run();
            return true;
        }

        @Override public void deactivate(LaserExtensions.TargetContext context) {
            if (context.level() == helper.getLevel() && context.position().equals(position)) deactivated.add(context);
        }
    }

    private static void assertSource(GameTestHelper h, LaserMachineBlockEntity source, LaserExtensions.TargetContext context) {
        h.assertTrue(source.getBlockPos().equals(context.source()) && source.identity().equals(context.sourceIdentity()),
                "target lifecycle carries the emitter controller position and persistent UUID");
        h.assertTrue(context.position().equals(targetPosition(source)) && context.incoming() == Direction.EAST,
                "target lifecycle retains the actual target cell and beam direction");
    }

    @GameTest(template = "laser_empty")
    public static void multipliedBeamTargetReceivesItsActualEmitterIdentity(GameTestHelper h) {
        var source = source(h);
        var probe = target(h, source, "test_refined_block");
        var multiplierPos = source.center().east(3);
        var multiplierId = ModContent.id("test_raw_block_other");
        h.getLevel().setBlockAndUpdate(multiplierPos, BuiltInRegistries.BLOCK.get(multiplierId).defaultBlockState());
        LaserExtensions.registerModifier(multiplierId, context -> context.level() == h.getLevel() && context.position().equals(multiplierPos)
                ? new LaserExtensions.ModifierResult(context.energy() * 2, context.range()) : null);
        try {
            LaserNetwork.processEmitter(source);
            h.assertTrue(probe.accepted.size() == 1, "real optical traversal reaches the registered target after its multiplier");
            var context = probe.accepted.getFirst();
            assertSource(h, source, context);
            h.assertTrue(context.energy() == 200_000L && context.batchTick() == h.getLevel().getGameTime(),
                    "the target sees the doubled paid pulse and its actual server batch time");
            source.setEnabled(false);
            h.assertTrue(probe.deactivated.size() == 1, "stopping the multiplied path revokes the target once");
            assertSource(h, source, probe.deactivated.getFirst());
        } finally { source.setEnabled(false); }
        h.succeed();
    }

    @GameTest(template = "laser_empty")
    public static void switchingSourceOffRevokesTheAcceptedTargetExactlyOnce(GameTestHelper h) {
        var source = source(h);
        var probe = target(h, source, "test_cold_block");
        try {
            LaserNetwork.processEmitter(source);
            h.assertTrue(probe.accepted.size() == 1 && probe.deactivated.isEmpty(), "the initial paid pulse remains active");
            source.setEnabled(false);
            h.assertTrue(probe.deactivated.size() == 1 && source.segments().isEmpty(), "source stop immediately revokes the target and clears the beam");
            source.setEnabled(false);
            LaserNetwork.unregister(source);
            h.assertTrue(probe.deactivated.size() == 1, "repeated stop and cleanup never revoke the same accepted path twice");
            assertSource(h, source, probe.deactivated.getFirst());
        } finally { source.setEnabled(false); }
        h.succeed();
    }

    @GameTest(template = "laser_empty")
    public static void removingSourceRevokesTheAcceptedTargetImmediately(GameTestHelper h) {
        var source = source(h);
        var probe = target(h, source, "test_conflict_block_a");
        try {
            LaserNetwork.processEmitter(source);
            h.assertTrue(probe.accepted.size() == 1, "the target received one paid source pulse");
            h.getLevel().setBlockAndUpdate(source.getBlockPos(), Blocks.AIR.defaultBlockState());
            h.assertTrue(source.isRemoved() && probe.deactivated.size() == 1, "real controller removal revokes its target before another tick");
            LaserNetwork.unregister(source);
            h.assertTrue(probe.deactivated.size() == 1, "the removal and node cleanup callbacks do not duplicate revocation");
            assertSource(h, source, probe.deactivated.getFirst());
        } finally { source.setEnabled(false); }
        h.succeed();
    }

    @GameTest(template = "laser_empty")
    public static void blockingAnAcceptedPathRevokesItsTargetOnTheNextSourceTick(GameTestHelper h) {
        var source = source(h);
        var probe = target(h, source, "test_conflict_block_b");
        try {
            LaserNetwork.processEmitter(source);
            h.assertTrue(probe.accepted.size() == 1, "a clear path reaches the target");
            var obstacle = source.center().east(3);
            h.getLevel().setBlockAndUpdate(obstacle, Blocks.STONE.defaultBlockState());
            source.tick();
            h.assertTrue(probe.deactivated.size() == 1 && source.segments().isEmpty(), "a new collision invalidates the retained path and revokes the target immediately");
            h.assertTrue(h.getLevel().getBlockState(obstacle).is(Blocks.STONE), "path revocation preserves the blocking player's block");
            source.tick();
            h.assertTrue(probe.deactivated.size() == 1, "an already invalidated path does not revoke on every later tick");
        } finally { source.setEnabled(false); }
        h.succeed();
    }

    @GameTest(template = "laser_empty", timeoutTicks = 60)
    public static void continuousTwentyTickRenewalDoesNotDeactivateTheSameTarget(GameTestHelper h) {
        var source = source(h);
        var probe = target(h, source, "test_ambiguous_block");
        LaserNetwork.processEmitter(source);
        long initialBatch = h.getLevel().getGameTime();
        h.assertTrue(probe.accepted.size() == 1, "the target has an initial accepted path before renewal");
        h.runAtTickTime(35, () -> {
            try {
                LaserNetwork.processEmitter(source);
                h.assertTrue(probe.accepted.size() >= 2 && probe.accepted.stream().anyMatch(context -> context.batchTick() >= initialBatch + 20),
                        "real source ticks and a later paid batch renew the same target after twenty ticks");
                h.assertTrue(probe.deactivated.isEmpty(), "continuous paid batches never send a stop callback between renewals");
                source.setEnabled(false);
                h.assertTrue(probe.deactivated.size() == 1, "the final stop revokes the continuously renewed source exactly once");
            } finally { source.setEnabled(false); }
            h.succeed();
        });
    }

    @GameTest(template = "laser_empty")
    public static void reentrantRemovalDuringFirstAcceptanceRevokesWithoutPublishingTheBeam(GameTestHelper h) {
        var source = source(h);
        var probe = target(h, source, "test_denied_block");
        probe.onAccept = () -> h.getLevel().setBlockAndUpdate(source.getBlockPos(), Blocks.AIR.defaultBlockState());
        try {
            LaserNetwork.processEmitter(source);
            h.assertTrue(probe.accepted.size() == 1 && source.isRemoved(), "the target callback removes the emitter while its first path is still being computed");
            h.assertTrue(probe.deactivated.size() == 1 && source.segments().isEmpty() && source.currentPower() == 0,
                    "the aborted path immediately revokes its accepted target and cannot publish stale work");
            LaserNetwork.unregister(source);
            h.assertTrue(probe.deactivated.size() == 1, "post-abort cleanup cannot revoke the callback a second time");
            assertSource(h, source, probe.deactivated.getFirst());
        } finally { source.setEnabled(false); }
        h.succeed();
    }

    @GameTest(template = "laser_empty")
    public static void reentrantRemovalDuringRenewalDeduplicatesOldAndNewTargetRevocation(GameTestHelper h) {
        var source = source(h);
        var probe = target(h, source, "test_explicit_block");
        try {
            LaserNetwork.processEmitter(source);
            h.assertTrue(probe.accepted.size() == 1 && probe.deactivated.isEmpty(), "the first target acceptance creates a retained live path");
            probe.onAccept = () -> h.getLevel().setBlockAndUpdate(source.getBlockPos(), Blocks.AIR.defaultBlockState());
            LaserNetwork.processEmitter(source);
            h.assertTrue(probe.accepted.size() == 2 && source.isRemoved(), "the renewed target acceptance removes the source reentrantly");
            h.assertTrue(probe.deactivated.size() == 1 && source.segments().isEmpty(),
                    "the previous retained path and aborted new path revoke their shared target only once");
            LaserNetwork.unregister(source);
            h.assertTrue(probe.deactivated.size() == 1, "cleanup after an aborted renewal remains idempotent");
        } finally { source.setEnabled(false); }
        h.succeed();
    }
}
