package dev.overloadsim.gametest;

import dev.overloadsim.OverloadSimulation;
import dev.overloadsim.laser.LaserContent;
import dev.overloadsim.workstation.HighEnergyWorkstationBlockEntity;
import dev.overloadsim.workstation.HighEnergyWorkstationBlock;
import dev.overloadsim.workstation.WorkstationContent;
import dev.overloadsim.workstation.WorkstationStructure;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Explicit cache publication lets a server fixture exercise the common-side client prediction data. */
@GameTestHolder(OverloadSimulation.ID)
@PrefixGameTestTemplate(false)
public final class WorkstationMenuAnchorGameTests {
    private static Class<?> cache() throws ClassNotFoundException {
        return Class.forName("dev.overloadsim.workstation.WorkstationMenuAnchors");
    }

    private static void publish(HighEnergyWorkstationBlockEntity host) throws Exception {
        cache().getMethod("publish", HighEnergyWorkstationBlockEntity.class).invoke(null, host);
    }

    private static void remove(HighEnergyWorkstationBlockEntity host) throws Exception {
        cache().getMethod("remove", HighEnergyWorkstationBlockEntity.class).invoke(null, host);
    }

    private static HighEnergyWorkstationBlockEntity owner(Level level, BlockPos pos) throws Exception {
        return (HighEnergyWorkstationBlockEntity) cache().getMethod("owner", Level.class, BlockPos.class)
                .invoke(null, level, pos);
    }

    private static BlockPos frame(HighEnergyWorkstationBlockEntity host) {
        return WorkstationStructure.position(host.getBlockPos(), host.facing(), 0, 6, 6);
    }

    @GameTest(template = "laser_empty")
    public static void formedSnapshotCachesOnlyItsFramesAndPort(GameTestHelper h) throws Exception {
        var host = WorkstationGameTests.build(h, Direction.EAST);
        h.assertTrue(host.formed(), "the canonical workstation fixture forms");
        publish(host);
        for (var entry : host.template().entrySet()) {
            boolean menuPart = entry.getValue().is(LaserContent.FRAME.get())
                    || entry.getValue().is(LaserContent.PORT.get());
            h.assertTrue(owner(h.getLevel(), entry.getKey()) == (menuPart ? host : null),
                    "only this formed template's frames and optical port predict its menu: " + entry.getKey());
        }
        var outside = host.getBlockPos().above(8);
        h.getLevel().setBlockAndUpdate(outside, LaserContent.FRAME.get().defaultBlockState());
        h.assertTrue(owner(h.getLevel(), outside) == null, "a nearby loose frame gains no cached anchor");
        h.succeed();
    }

    @GameTest(template = "laser_empty")
    public static void unformedSnapshotNeverBuildsOrClaimsTheShell(GameTestHelper h) throws Exception {
        var origin = h.absolutePos(new BlockPos(10, 2, 10));
        var state = WorkstationContent.CONTROLLER.get().defaultBlockState()
                .setValue(HighEnergyWorkstationBlock.FACING, Direction.NORTH);
        h.getLevel().setBlockAndUpdate(origin, state);
        var host = (HighEnergyWorkstationBlockEntity) h.getLevel().getBlockEntity(origin);
        var frame = frame(host);
        h.getLevel().setBlockAndUpdate(frame, LaserContent.FRAME.get().defaultBlockState());
        publish(host);
        h.assertTrue(!host.formed() && !host.structureClaimed() && owner(h.getLevel(), frame) == null,
                "cache publication cannot form an incomplete shell or grant its loose frame an anchor");
        h.succeed();
    }

    @GameTest(template = "laser_empty")
    public static void invalidatedSnapshotCannotReappearWithoutPublication(GameTestHelper h) throws Exception {
        var host = WorkstationGameTests.build(h, Direction.NORTH);
        publish(host);
        host.invalidateStructure();
        h.assertTrue(owner(h.getLevel(), frame(host)) == null, "invalidated formation rejects the old snapshot");
        host.checkStructure();
        h.assertTrue(host.formed() && owner(h.getLevel(), frame(host)) == null,
                "forming again does not revive an unpublished old client snapshot");
        publish(host);
        h.assertTrue(owner(h.getLevel(), frame(host)) == host, "a newly published formed snapshot restores its anchor");
        h.succeed();
    }

    @GameTest(template = "laser_empty")
    public static void controllerRemovalClearsAllAnchors(GameTestHelper h) throws Exception {
        var host = WorkstationGameTests.build(h, Direction.SOUTH);
        try {
            h.assertTrue(host.getMainNode().getNode() == null, "the fixture still awaits its first AE initialization");
            publish(host);
            host.setRemoved();
            h.assertTrue(owner(h.getLevel(), frame(host)) == null && owner(h.getLevel(), host.receiver()) == null,
                    "a removed controller grants no frame or optical-port menu access");
            h.getLevel().getChunkAt(host.getBlockPos()).addAndRegisterBlockEntity(host);
            host.checkStructure();
            h.assertTrue(h.getLevel().getBlockEntity(host.getBlockPos()) == host && host.formed()
                            && owner(h.getLevel(), frame(host)) == null,
                    "the removal hook discarded the old snapshot even if the same instance is reattached");
        } finally {
            // Both pending registrations must observe removal before AE2 runs its first-tick queue.
            h.getLevel().setBlockAndUpdate(host.getBlockPos(), Blocks.AIR.defaultBlockState());
        }
        h.succeed();
    }

    @GameTest(template = "laser_empty")
    public static void controllerChunkUnloadClearsAllAnchors(GameTestHelper h) throws Exception {
        var host = WorkstationGameTests.build(h, Direction.WEST);
        try {
            h.assertTrue(host.getMainNode().getNode() == null, "the fixture still awaits its first AE initialization");
            publish(host);
            host.onChunkUnloaded();
            host.setRemoved();
            h.assertTrue(owner(h.getLevel(), frame(host)) == null, "an unloaded controller grants no cached menu access");
            h.getLevel().getChunkAt(host.getBlockPos()).addAndRegisterBlockEntity(host);
            host.checkStructure();
            h.assertTrue(h.getLevel().getBlockEntity(host.getBlockPos()) == host && host.formed()
                            && owner(h.getLevel(), frame(host)) == null,
                    "the unload hook discarded the snapshot even if the same instance is reattached");
        } finally {
            h.getLevel().setBlockAndUpdate(host.getBlockPos(), Blocks.AIR.defaultBlockState());
        }
        h.succeed();
    }

    @GameTest(template = "laser_empty")
    public static void replacementInstanceCannotInheritOrLoseAnotherInstancesSnapshot(GameTestHelper h) throws Exception {
        var previous = WorkstationGameTests.build(h, Direction.NORTH);
        publish(previous);
        var saved = previous.saveMachine(h.getLevel().registryAccess());
        var origin = previous.getBlockPos();
        var state = previous.getBlockState();
        h.getLevel().removeBlockEntity(origin);
        var replacement = new HighEnergyWorkstationBlockEntity(origin, state);
        replacement.loadMachine(saved, h.getLevel().registryAccess());
        h.getLevel().setBlockEntity(replacement);
        replacement.checkStructure();
        h.assertTrue(replacement.formed() && replacement.identity().equals(previous.identity())
                        && owner(h.getLevel(), frame(replacement)) == null,
                "a replacement instance cannot inherit the prior snapshot even with the same restored UUID");
        publish(replacement);
        remove(previous);
        h.assertTrue(owner(h.getLevel(), frame(replacement)) == replacement,
                "late cleanup from the old instance cannot erase the replacement's published snapshot");
        h.succeed();
    }

    @GameTest(template = "laser_empty")
    public static void changedPartCannotKeepItsCachedMenuAnchor(GameTestHelper h) throws Exception {
        var host = WorkstationGameTests.build(h, Direction.NORTH);
        publish(host);
        var port = host.receiver();
        h.getLevel().setBlockAndUpdate(port, LaserContent.GLASS.get().defaultBlockState());
        h.assertTrue(owner(h.getLevel(), port) == null, "a former optical port replaced by glass has no cached menu anchor");
        h.succeed();
    }
}
