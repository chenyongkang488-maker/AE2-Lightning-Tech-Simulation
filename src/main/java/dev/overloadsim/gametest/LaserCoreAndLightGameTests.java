package dev.overloadsim.gametest;

import dev.overloadsim.OverloadSimulation;
import dev.overloadsim.core.LaserMath;
import dev.overloadsim.laser.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(OverloadSimulation.ID)
@PrefixGameTestTemplate(false)
public final class LaserCoreAndLightGameTests {
    @GameTest(template = "laser_empty")
    public static void coreMenuContainsOnlyTheLaserEnergyReadout(GameTestHelper h) {
        var core = LaserMachineGameTests.build(h, h.absolutePos(new BlockPos(8, 3, 8)), LaserKind.CORE, Direction.NORTH);
        core.checkStructure();
        core.acceptCore(LaserMath.CORE_THRESHOLD, Direction.SOUTH, h.getLevel().getGameTime());
        var player = h.makeMockPlayer(GameType.CREATIVE);
        player.setPos(core.getBlockPos().getCenter());
        var menu = new LaserMenu(1, player.getInventory(), core);
        player.containerMenu = menu;
        menu.broadcastChanges();
        h.assertTrue(menu.slots.isEmpty(), "the core readout has no player or machine inventory slots");
        h.assertTrue(menu.power == 1_000_000_000L && menu.coreRemainder == 1_000_000_000L,
                "the readout retains exact incoming energy and accumulated remainder");
        menu.receiveClientAction("laserEnabled", "null");
        menu.receiveClientAction("laserBuild", "null");
        h.assertTrue(core.enabled() && menu.buildFeedback.isEmpty(), "legacy control actions cannot change or build through the read-only core page");
        h.succeed();
    }

    @GameTest(template = "laser_empty")
    public static void coreRejectsFrequencyAndExternalEnergyWithoutDiscardingSavedResources(GameTestHelper h) {
        var core = LaserMachineGameTests.build(h, h.absolutePos(new BlockPos(8, 3, 8)), LaserKind.CORE, Direction.NORTH);
        var state = core.saveWithoutMetadata(h.getLevel().registryAccess());
        state.putInt("FrequencyId", 37);
        state.getCompound("LaserMachine").putLong("Energy", 123_456L);
        state.getCompound("LaserMachine").putLong("CoreRemainder", 999L);
        state.getCompound("LaserMachine").putBoolean("Enabled", false);
        core.loadTag(state, h.getLevel().registryAccess());
        core.checkStructure();
        core.getFrequencyBindingAccess().setFrequency(42);
        h.assertTrue(core.getFrequencyId() == -1 && !core.isFrequencyConnected(), "a core cannot acquire a wireless frequency binding");
        h.assertTrue(core.getGridConnectableSides(null).isEmpty(), "a core has no physical AE cable sides");
        h.assertTrue(h.getLevel().getCapability(appeng.api.AECapabilities.IN_WORLD_GRID_NODE_HOST, core.getBlockPos()) == null,
                "the core does not advertise an external AE node host");
        h.assertTrue(h.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, core.getBlockPos(), Direction.NORTH) == null
                        && core.feCapability().receiveEnergy(1000, false) == 0,
                "neither published nor retained FE handlers can inject power into the core");
        h.assertTrue(core.enabled() && core.energy().stored() == 123_456L && core.coreRemainder() == 999L,
                "previously disabled cores receive laser work and retain their saved resources");
        h.assertTrue(core.saveWithoutMetadata(h.getLevel().registryAccess()).getInt("FrequencyId") == 37,
                "legacy frequency data remains preserved but inactive");
        h.succeed();
    }

    @GameTest(template = "laser_empty", timeoutTicks = 70)
    public static void formedEmitterFaceProducesRealBlockLightAndCleansUp(GameTestHelper h) {
        var emitter = LaserMachineGameTests.build(h, h.absolutePos(new BlockPos(8, 5, 8)), LaserKind.EMITTER, Direction.WEST);
        emitter.tick();
        var face = emitter.center().relative(emitter.forward(), 3);
        var light = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("overload_sim:laser_light"));
        h.assertTrue(h.getLevel().getBlockState(face).getLightEmission(h.getLevel(), face) == 15,
                "the visibly glowing idle emitter face emits real level15 block light");
        h.runAtTickTime(10, () -> {
            h.assertTrue(h.getLevel().getBrightness(LightLayer.BLOCK, face.east()) >= 14,
                    "vanilla light propagation illuminates the blocks beside the emitter face");
            h.assertTrue(h.getLevel().getBlockState(face).getCollisionShape(h.getLevel(), face).isEmpty()
                            && h.getLevel().getBlockState(face).getShape(h.getLevel(), face).isEmpty(),
                    "the light source does not obstruct players, interaction rays or laser traversal");
            h.getLevel().setBlockAndUpdate(emitter.getBlockPos(), Blocks.AIR.defaultBlockState());
            h.assertTrue(!h.getLevel().getBlockState(face).is(light), "removing the source cleans its light immediately");
            h.succeed();
        });
    }

    @GameTest(template = "laser_empty")
    public static void emitterLightDoesNotReplaceWaterPlantsOrPlayerBlocks(GameTestHelper h) {
        var emitter = LaserMachineGameTests.build(h, h.absolutePos(new BlockPos(8, 5, 8)), LaserKind.EMITTER, Direction.WEST);
        emitter.checkStructure();
        var face = emitter.center().relative(emitter.forward(), 3);
        for (var block : java.util.List.of(Blocks.WATER, Blocks.SHORT_GRASS, Blocks.STONE)) {
            h.getLevel().setBlockAndUpdate(face, block.defaultBlockState());
            emitter.tick();
            h.assertTrue(h.getLevel().getBlockState(face).is(block), "light placement preserves " + block);
        }
        h.succeed();
    }

    @GameTest(template = "laser_empty")
    public static void beamLightFollowsWorkAndLeavesTheIdleEmissionFaceLit(GameTestHelper h) {
        var emitter = LaserMachineGameTests.build(h, h.absolutePos(new BlockPos(8, 5, 8)), LaserKind.EMITTER, Direction.WEST);
        emitter.energy().load(100_000);
        LaserNetwork.processEmitter(emitter);
        emitter.refreshLaserLights();
        var end = emitter.center().relative(Direction.EAST, 8);
        h.assertTrue(h.getLevel().getBlockState(end).is(LaserContent.LIGHT.get()), "working beam illuminates its far endpoint");
        emitter.setEnabled(false);
        emitter.tick();
        h.assertTrue(h.getLevel().getBlockState(end).isAir(), "switching the beam off releases its endpoint light");
        h.assertTrue(h.getLevel().getBlockState(emitter.center().east(3)).is(LaserContent.LIGHT.get()),
                "the formed idle emitter keeps only its visible emission face illuminated");
        h.succeed();
    }

    @GameTest(template = "laser_empty")
    public static void sharedLightLeasesSurviveSaveAndReleaseIndependently(GameTestHelper h) {
        var first = LaserMachineGameTests.build(h, h.absolutePos(new BlockPos(8, 3, 8)), LaserKind.CORE, Direction.NORTH);
        var second = LaserMachineGameTests.build(h, h.absolutePos(new BlockPos(10, 3, 8)), LaserKind.CORE, Direction.NORTH);
        var pos = first.getBlockPos().above();
        h.assertTrue(LaserLightManager.claim(h.getLevel(), pos, first) && LaserLightManager.claim(h.getLevel(), pos, second),
                "two independent sources acquire the same light cell");
        var light = (LaserLightBlockEntity) h.getLevel().getBlockEntity(pos);
        var saved = light.saveWithFullMetadata(h.getLevel().registryAccess());
        light.loadWithComponents(saved, h.getLevel().registryAccess());
        LaserLightManager.release(h.getLevel(), pos, first);
        h.assertTrue(h.getLevel().getBlockState(pos).is(LaserContent.LIGHT.get()), "releasing one saved lease keeps the other source lit");
        LaserLightManager.release(h.getLevel(), pos, second);
        h.assertTrue(h.getLevel().getBlockState(pos).isAir(), "the last released lease removes its source");
        h.succeed();
    }

    @GameTest(template = "laser_empty", timeoutTicks = 70)
    public static void orphanLightExpiresWithoutLoadingItsSourceChunk(GameTestHelper h) {
        var core = LaserMachineGameTests.build(h, h.absolutePos(new BlockPos(8, 3, 8)), LaserKind.CORE, Direction.NORTH);
        var pos = core.getBlockPos().above();
        LaserLightManager.claim(h.getLevel(), pos, core);
        var light = (LaserLightBlockEntity) h.getLevel().getBlockEntity(pos);
        var saved = light.saveWithFullMetadata(h.getLevel().registryAccess());
        var far = new BlockPos(29_000_000, 100, 29_000_000);
        var lease = saved.getList("Owners", net.minecraft.nbt.Tag.TAG_COMPOUND).getCompound(0);
        lease.putLong("Source", far.asLong()); lease.putLong("Until", h.getLevel().getGameTime() + 20);
        light.loadWithComponents(saved, h.getLevel().registryAccess());
        light.recover();
        h.assertTrue(h.getLevel().getBlockState(pos).is(LaserContent.LIGHT.get()) && !h.getLevel().hasChunkAt(far),
                "an unexpired saved lease defers its unloaded source without loading it");
        h.runAtTickTime(25, () -> {
            light.recover();
            h.assertTrue(h.getLevel().getBlockState(pos).isAir() && !h.getLevel().hasChunkAt(far),
                    "an expired unloaded owner cannot leave a permanent ghost light");
            h.succeed();
        });
    }

    @GameTest(template = "laser_empty")
    public static void proxyAirKeepsAperturesValidAndCleanupPreservesReplacements(GameTestHelper h) {
        var ring = LaserMachineGameTests.build(h, h.absolutePos(new BlockPos(8, 3, 8)), LaserKind.CONSTRAINT, Direction.NORTH);
        ring.checkStructure();
        h.assertTrue(LaserLightManager.claim(h.getLevel(), ring.center(), ring), "light occupies the clear central aperture");
        ring.checkStructure();
        h.assertTrue(ring.formed(), "an owned light source does not invalidate the ring aperture");
        h.getLevel().setBlockAndUpdate(ring.center(), Blocks.STONE.defaultBlockState());
        LaserLightManager.release(h.getLevel(), ring.center(), ring);
        h.assertTrue(h.getLevel().getBlockState(ring.center()).is(Blocks.STONE), "cleanup never deletes a player's replacement block");
        var far = new BlockPos(29_000_000, 100, 29_000_000);
        h.assertTrue(!LaserLightManager.claim(h.getLevel(), far, ring) && !h.getLevel().hasChunkAt(far),
                "proxy acquisition never force loads a beam endpoint");
        h.succeed();
    }
}
