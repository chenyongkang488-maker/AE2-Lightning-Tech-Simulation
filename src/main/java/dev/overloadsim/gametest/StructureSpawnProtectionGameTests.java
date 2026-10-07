package dev.overloadsim.gametest;

import dev.overloadsim.OverloadSimulation;
import dev.overloadsim.laser.LaserKind;
import dev.overloadsim.laser.LaserMachineBlockEntity;
import dev.overloadsim.multiblock.MultiblockContent;
import dev.overloadsim.multiblock.SimulationControllerBlockEntity;
import dev.overloadsim.multiblock.SimulationStructure;
import dev.overloadsim.multiblock.SimulationStructureIndex;
import dev.overloadsim.workstation.WorkstationStructure;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(OverloadSimulation.ID)
@PrefixGameTestTemplate(false)
public final class StructureSpawnProtectionGameTests {
    private static final List<EntityType<? extends Mob>> CATEGORIES = List.of(EntityType.BAT, EntityType.PIG,
            EntityType.ZOMBIE, EntityType.SQUID, EntityType.COD, EntityType.GLOW_SQUID, EntityType.AXOLOTL, EntityType.VILLAGER);

    private static MobSpawnEvent.PositionCheck.Result position(GameTestHelper h, EntityType<? extends Mob> type,
            Vec3 position, MobSpawnType reason) {
        var mob = type.create(h.getLevel());
        mob.setPos(position);
        var event = new MobSpawnEvent.PositionCheck(mob, h.getLevel(), reason, null);
        NeoForge.EVENT_BUS.post(event);
        return event.getResult();
    }

    private static MobSpawnEvent.SpawnPlacementCheck.Result placement(GameTestHelper h, BlockPos position,
            MobSpawnType reason) {
        var event = new MobSpawnEvent.SpawnPlacementCheck(EntityType.BAT, h.getLevel(), reason, position,
                h.getLevel().getRandom(), true);
        NeoForge.EVENT_BUS.post(event);
        return event.getResult();
    }

    private static void categoriesBlocked(GameTestHelper h, Vec3 position, String interior) {
        for (var type : CATEGORIES)
            h.assertTrue(position(h, type, position, MobSpawnType.NATURAL) == MobSpawnEvent.PositionCheck.Result.FAIL,
                    interior + " prevents natural " + type.getDescriptionId() + " independently of mob category");
    }

    private static void manualReasonsAllowed(GameTestHelper h, Vec3 position) {
        h.assertTrue(position(h, EntityType.BAT, position, MobSpawnType.COMMAND) == MobSpawnEvent.PositionCheck.Result.DEFAULT,
                "commands remain available inside a formed hollow structure");
        h.assertTrue(position(h, EntityType.BAT, position, MobSpawnType.SPAWN_EGG) == MobSpawnEvent.PositionCheck.Result.DEFAULT,
                "spawn eggs remain available inside a formed hollow structure");
    }

    @GameTest(template = "laser_empty")
    public static void idleWorkstationPreventsNaturalMobsOfEveryCategory(GameTestHelper h) {
        var host = WorkstationGameTests.build(h, Direction.NORTH);
        h.assertTrue(host.formed() && host.currentPower() == 0, "the unpowered workstation is formed");
        categoriesBlocked(h, host.center().getCenter().add(0, -.4, 0), "the workstation cavity");
        h.assertTrue(placement(h, host.center(), MobSpawnType.NATURAL) == MobSpawnEvent.SpawnPlacementCheck.Result.FAIL,
                "the workstation also rejects the earlier natural placement check");
        manualReasonsAllowed(h, host.center().getCenter());
        h.succeed();
    }

    @GameTest(template = "laser_empty")
    public static void workstationExteriorAndBrokenShellKeepNormalSpawnRules(GameTestHelper h) {
        var host = WorkstationGameTests.build(h, Direction.EAST);
        var interior = host.center().getCenter().add(0, -.4, 0);
        h.assertTrue(position(h, EntityType.BAT, host.center().getCenter().add(4.2, 0, 0), MobSpawnType.NATURAL)
                        == MobSpawnEvent.PositionCheck.Result.DEFAULT, "space beyond the workstation wall is not protected");
        h.assertTrue(position(h, EntityType.BAT, interior, MobSpawnType.NATURAL) == MobSpawnEvent.PositionCheck.Result.FAIL,
                "the idle formed workstation protects its cavity before the shell is broken");
        var corner = WorkstationStructure.position(host.getBlockPos(), host.facing(), 0, 0, 0);
        h.getLevel().setBlockAndUpdate(corner, Blocks.AIR.defaultBlockState());
        h.assertTrue(position(h, EntityType.BAT, interior, MobSpawnType.NATURAL) == MobSpawnEvent.PositionCheck.Result.DEFAULT,
                "a removed shell member immediately releases spawn protection");
        h.succeed();
    }

    @GameTest(template = "laser_empty")
    public static void idleSimulationChamberPreventsNaturalMobsOfEveryCategory(GameTestHelper h) {
        var min = h.absolutePos(new BlockPos(7, 1, 7));
        var host = MultiblockGameTests.build(h, min, 7);
        host.checkStructure();
        h.assertTrue(host.structure() != null && host.loaded() && !host.hasActiveCrystal(), "the empty chamber is formed and idle");
        var interior = min.offset(3, 3, 3);
        categoriesBlocked(h, interior.getCenter().add(0, -.4, 0), "the chamber cavity");
        h.assertTrue(placement(h, interior, MobSpawnType.NATURAL) == MobSpawnEvent.SpawnPlacementCheck.Result.FAIL,
                "the chamber also rejects the earlier natural placement check");
        manualReasonsAllowed(h, interior.getCenter());
        h.succeed();
    }

    @GameTest(template = "laser_empty")
    public static void chamberExteriorAndBrokenShellKeepNormalSpawnRules(GameTestHelper h) {
        var min = h.absolutePos(new BlockPos(7, 1, 7));
        var host = MultiblockGameTests.build(h, min, 7);
        host.checkStructure();
        var interior = min.offset(3, 3, 3).getCenter();
        h.assertTrue(position(h, EntityType.BAT, min.offset(-1, 3, 3).getCenter(), MobSpawnType.NATURAL)
                        == MobSpawnEvent.PositionCheck.Result.DEFAULT, "space beyond the chamber wall is not protected");
        h.assertTrue(position(h, EntityType.BAT, interior, MobSpawnType.NATURAL) == MobSpawnEvent.PositionCheck.Result.FAIL,
                "the idle formed chamber protects its cavity before the shell is broken");
        h.getLevel().setBlockAndUpdate(min, Blocks.AIR.defaultBlockState());
        h.assertTrue(position(h, EntityType.BAT, interior, MobSpawnType.NATURAL) == MobSpawnEvent.PositionCheck.Result.DEFAULT,
                "a stale chamber claim cannot protect an invalid shell");
        h.succeed();
    }

    private static LaserMachineBlockEntity laser(GameTestHelper h, LaserKind kind) {
        var host = LaserMachineGameTests.build(h, h.absolutePos(new BlockPos(12, 2, 12)), kind, Direction.NORTH);
        host.checkStructure();
        host.setEnabled(false);
        h.assertTrue(host.formed() && host.currentPower() == 0, "the disabled hollow laser structure is formed");
        return host;
    }

    private static void laserInterior(GameTestHelper h, LaserKind kind) {
        var host = laser(h, kind);
        // The emitter's centre is a solid source; its actual air cavity is the cell just above it.
        var interior = host.center().getCenter().add(0, kind == LaserKind.EMITTER ? .55 : -.4, 0);
        categoriesBlocked(h, interior, "the " + kind + " interior");
        manualReasonsAllowed(h, interior);
        h.assertTrue(position(h, EntityType.BAT, host.center().getCenter().add(4.2, 0, 0), MobSpawnType.NATURAL)
                        == MobSpawnEvent.PositionCheck.Result.DEFAULT, "space outside the laser enclosure is not protected");
        h.succeed();
    }

    @GameTest(template = "laser_empty") public static void idleReceiverPreventsFriendlyNaturalSpawning(GameTestHelper h) { laserInterior(h, LaserKind.RECEIVER); }
    @GameTest(template = "laser_empty") public static void idleDeflectorPreventsFriendlyNaturalSpawning(GameTestHelper h) { laserInterior(h, LaserKind.DEFLECTOR); }
    @GameTest(template = "laser_empty") public static void idleEmitterPreventsFriendlyNaturalSpawning(GameTestHelper h) { laserInterior(h, LaserKind.EMITTER); }
    @GameTest(template = "laser_empty") public static void idleEnhancementPreventsFriendlyNaturalSpawning(GameTestHelper h) { laserInterior(h, LaserKind.ENHANCEMENT); }
    @GameTest(template = "laser_empty") public static void idleBurstPreventsFriendlyNaturalSpawning(GameTestHelper h) { laserInterior(h, LaserKind.BURST); }
    @GameTest(template = "laser_empty") public static void idleFocusPreventsFriendlyNaturalSpawning(GameTestHelper h) { laserInterior(h, LaserKind.FOCUS); }
    @GameTest(template = "laser_empty") public static void idleConstraintStillPreventsEveryNaturalCategory(GameTestHelper h) { laserInterior(h, LaserKind.CONSTRAINT); }

    @GameTest(template = "laser_empty")
    public static void unloadedIndexedChamberOwnerDoesNotLoadItsChunk(GameTestHelper h) {
        var interior = h.absolutePos(new BlockPos(12, 3, 12));
        var controller = interior.offset(100_000, 0, 100_000);
        h.assertTrue(!h.getLevel().hasChunkAt(controller), "the indexed controller starts unloaded");
        var unloaded = new SimulationControllerBlockEntity(controller, MultiblockContent.CONTROLLER.get().defaultBlockState());
        unloaded.setLevel(h.getLevel());
        SimulationStructureIndex.bind(unloaded, new SimulationStructure(interior.offset(-1, -1, -1), 3,
                List.of(), List.of(), null, 0, 0, 0, 0, false, false));
        try {
            h.assertTrue(position(h, EntityType.BAT, interior.getCenter(), MobSpawnType.NATURAL) == MobSpawnEvent.PositionCheck.Result.DEFAULT,
                    "an unloaded owner cannot protect a cavity");
            h.assertTrue(!h.getLevel().hasChunkAt(controller), "checking the interior does not load the owner chunk");
            h.assertTrue(placement(h, controller, MobSpawnType.NATURAL) == MobSpawnEvent.SpawnPlacementCheck.Result.DEFAULT
                    && !h.getLevel().hasChunkAt(controller), "checking an unloaded spawn position does not load its chunk");
        } finally { SimulationStructureIndex.release(unloaded); }
        h.succeed();
    }

    @GameTest(template = "laser_empty", timeoutTicks = 20)
    public static void workstationSpawnProtectionDoesNotRemoveExistingFriendlyMobs(GameTestHelper h) {
        var host = WorkstationGameTests.build(h, Direction.NORTH);
        var bat = EntityType.BAT.create(h.getLevel());
        bat.setNoAi(true); bat.setNoGravity(true); bat.setResting(false);
        bat.setPos(host.center().getCenter());
        h.getLevel().addFreshEntity(bat);
        h.runAfterDelay(3, () -> {
            try { h.assertTrue(bat.isAlive() && bat.getHealth() == bat.getMaxHealth(), "idle enclosure protection leaves existing mobs unharmed"); }
            finally { bat.discard(); }
            h.succeed();
        });
    }
}
