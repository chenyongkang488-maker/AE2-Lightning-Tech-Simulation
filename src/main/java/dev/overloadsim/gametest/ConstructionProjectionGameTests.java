package dev.overloadsim.gametest;

import dev.overloadsim.OverloadSimulation;
import dev.overloadsim.construction.ConstructionProjectionGeometry;
import dev.overloadsim.laser.LaserKind;
import dev.overloadsim.laser.LaserStructureGeometry;
import dev.overloadsim.multiblock.MultiblockContent;
import dev.overloadsim.multiblock.SimulationAutoConstruction;
import dev.overloadsim.multiblock.SimulationControllerBlock;
import dev.overloadsim.multiblock.SimulationControllerBlockEntity;
import dev.overloadsim.multiblock.SimulationStructureValidator;
import dev.overloadsim.multiblock.MultiblockSimulationMenu;
import com.mojang.authlib.GameProfile;
import java.util.LinkedHashMap;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

@GameTestHolder(OverloadSimulation.ID)
@PrefixGameTestTemplate(false)
public final class ConstructionProjectionGameTests {
    private static final BlockPos LASER_CONTROLLER = new BlockPos(37, 64, -21);

    // A wrong origin translation or facing inversion must put these optical endpoints elsewhere.
    @GameTest(template = "empty")
    public static void emitterProjectionPlacesSourceAndExitInEachWorldDirection(GameTestHelper h) {
        var sourceOffsets = new BlockPos[] {
                new BlockPos(0, 2, 0), new BlockPos(0, -2, 0), new BlockPos(0, 0, 2),
                new BlockPos(0, 0, -2), new BlockPos(2, 0, 0), new BlockPos(-2, 0, 0)
        };
        var exitOffsets = new BlockPos[] {
                new BlockPos(0, 4, 0), new BlockPos(0, -4, 0), new BlockPos(0, 0, 4),
                new BlockPos(0, 0, -4), new BlockPos(4, 0, 0), new BlockPos(-4, 0, 0)
        };
        var facings = new Direction[] {
                Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST
        };
        for (int i = 0; i < facings.length; i++) {
            var cells = ConstructionProjectionGeometry.laserCells(LASER_CONTROLLER,
                    LaserStructureGeometry.template(LaserKind.EMITTER, facings[i]));
            h.assertTrue(cells.get(LASER_CONTROLLER.offset(sourceOffsets[i])) == LaserStructureGeometry.Part.SOURCE,
                    "projected emitter source follows " + facings[i]);
            h.assertTrue(cells.get(LASER_CONTROLLER.offset(exitOffsets[i])) == LaserStructureGeometry.Part.PORT,
                    "projected emitter exit follows " + facings[i]);
            h.assertTrue(!cells.containsKey(LASER_CONTROLLER), "the existing emitter controller is omitted");
            h.assertTrue(!cells.containsValue(LaserStructureGeometry.Part.AIR), "empty emitter envelope stays invisible");
        }
        h.succeed();
    }

    // Literal totals come from the independent 7x7x7 sphere and two 7x7 pedestal layers.
    @GameTest(template = "empty")
    public static void receiverProjectionIncludesBothPedestalLayersAndKeepsHistoricalAnchorSeparate(GameTestHelper h) {
        var facings = new Direction[] { Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST };
        var rearLowerCorners = new BlockPos[] {
                new BlockPos(-3, 6, 1), new BlockPos(3, -6, 1), new BlockPos(-3, -1, 6),
                new BlockPos(3, -1, -6), new BlockPos(6, -1, 3), new BlockPos(-6, -1, -3)
        };
        for (int i = 0; i < facings.length; i++) {
            var pedestal = ConstructionProjectionGeometry.laserCells(LASER_CONTROLLER,
                    LaserStructureGeometry.template(LaserKind.RECEIVER, facings[i], true));
            h.assertTrue(pedestal.size() == 195, "full receiver construction excludes only its existing controller");
            h.assertTrue(pedestal.values().stream().filter(p -> p == LaserStructureGeometry.Part.FRAME).count() == 138,
                    "both pedestal layers and spherical ribs require 138 frames");
            h.assertTrue(pedestal.values().stream().filter(p -> p == LaserStructureGeometry.Part.GLASS).count() == 56,
                    "receiver shell requires 56 glass cells");
            h.assertTrue(pedestal.values().stream().filter(p -> p == LaserStructureGeometry.Part.PORT).count() == 1,
                    "receiver shell has a single optical entry");
            h.assertTrue(pedestal.get(LASER_CONTROLLER.offset(rearLowerCorners[i])) == LaserStructureGeometry.Part.FRAME,
                    "rear lower pedestal corner follows " + facings[i]);
            var historical = ConstructionProjectionGeometry.laserCells(LASER_CONTROLLER,
                    LaserStructureGeometry.template(LaserKind.RECEIVER, facings[i], false));
            h.assertTrue(historical.size() == 97, "historical receiver keeps its original controller anchor");
        }
        var north = ConstructionProjectionGeometry.laserCells(LASER_CONTROLLER,
                LaserStructureGeometry.template(LaserKind.RECEIVER, Direction.NORTH, true));
        h.assertTrue(north.get(LASER_CONTROLLER.offset(-3, -1, 6)) == LaserStructureGeometry.Part.FRAME,
                "rear lower pedestal corner is projected");
        h.assertTrue(north.get(LASER_CONTROLLER.offset(3, 0, 0)) == LaserStructureGeometry.Part.FRAME,
                "front upper pedestal corner is projected");
        h.assertTrue(north.get(LASER_CONTROLLER.offset(0, 4, 0)) == LaserStructureGeometry.Part.PORT,
                "receiver entry is four blocks above its front controller");
        h.assertTrue(!north.containsKey(LASER_CONTROLLER.offset(0, 4, 3)), "empty sphere center stays invisible");
        h.succeed();
    }

    // Rendering envelope AIR or the present controller would inflate these independent totals.
    @GameTest(template = "empty")
    public static void everyInstrumentProjectsOnlyItsPhysicalConstructionFootprint(GameTestHelper h) {
        var kinds = new LaserKind[] { LaserKind.EMITTER, LaserKind.CONSTRAINT, LaserKind.ENHANCEMENT,
                LaserKind.BURST, LaserKind.DEFLECTOR, LaserKind.FOCUS, LaserKind.CORE, LaserKind.RECEIVER };
        var visibleCounts = new int[] { 38, 15, 15, 15, 97, 15, 0, 195 };
        for (int i = 0; i < kinds.length; i++) for (var facing : Direction.values()) {
            var cells = ConstructionProjectionGeometry.laserCells(LASER_CONTROLLER,
                    LaserStructureGeometry.template(kinds[i], facing, true));
            h.assertTrue(cells.size() == visibleCounts[i], "visible footprint for " + kinds[i] + " facing " + facing);
            h.assertTrue(!cells.containsValue(LaserStructureGeometry.Part.CONTROLLER), "existing controllers stay invisible");
            h.assertTrue(!cells.containsValue(LaserStructureGeometry.Part.AIR), "reserved air stays invisible");
        }
        h.succeed();
    }

    // New deflectors have four physical side ports; historical shells retain their inert pole ports.
    @GameTest(template = "empty")
    public static void deflectorProjectionTracksHorizontalPortsAndLegacyAnchors(GameTestHelper h) {
        for (var facing : Direction.values()) {
            var cells = ConstructionProjectionGeometry.laserCells(LASER_CONTROLLER,
                    LaserStructureGeometry.template(LaserKind.DEFLECTOR, facing, false, false));
            h.assertTrue(cells.values().stream().filter(p -> p == LaserStructureGeometry.Part.PORT).count() == 4,
                    "new deflector projects exactly four side ports");
            h.assertTrue(cells.get(LASER_CONTROLLER.offset(3, 3, 0)) == LaserStructureGeometry.Part.PORT,
                    "east deflector port is projected");
            h.assertTrue(cells.get(LASER_CONTROLLER.offset(-3, 3, 0)) == LaserStructureGeometry.Part.PORT,
                    "west deflector port is projected");
            h.assertTrue(cells.get(LASER_CONTROLLER.offset(0, 3, 3)) == LaserStructureGeometry.Part.PORT,
                    "south deflector port is projected");
            h.assertTrue(cells.get(LASER_CONTROLLER.offset(0, 3, -3)) == LaserStructureGeometry.Part.PORT,
                    "north deflector port is projected");
            h.assertTrue(cells.get(LASER_CONTROLLER.offset(0, 6, 0)) == LaserStructureGeometry.Part.FRAME,
                    "deflector top is a structural frame");
            var historical = ConstructionProjectionGeometry.laserCells(LASER_CONTROLLER,
                    LaserStructureGeometry.template(LaserKind.DEFLECTOR, facing, false, true));
            h.assertTrue(historical.values().stream().filter(p -> p == LaserStructureGeometry.Part.PORT).count() == 5,
                    "historical shell keeps five non-controller port cells");
        }
        h.succeed();
    }

    // A cap regression would permit arbitrary templates to allocate unbounded draw lists.
    @GameTest(template = "empty")
    public static void projectionRejectsOversizedTemplatesAndSnapshotsMutablePositions(GameTestHelper h) {
        var tooLarge = new LinkedHashMap<BlockPos, LaserStructureGeometry.Part>();
        for (int i = 1; i <= 600; i++) tooLarge.put(new BlockPos(i, 0, 0), LaserStructureGeometry.Part.FRAME);
        boolean rejected = false;
        try { ConstructionProjectionGeometry.laserCells(LASER_CONTROLLER, tooLarge); }
        catch (IllegalArgumentException expected) { rejected = true; }
        h.assertTrue(rejected, "oversized arbitrary projection templates are rejected");
        var offset = new BlockPos.MutableBlockPos(1, 2, 3);
        var template = new LinkedHashMap<BlockPos, LaserStructureGeometry.Part>();
        template.put(offset, LaserStructureGeometry.Part.FRAME);
        var cells = ConstructionProjectionGeometry.laserCells(LASER_CONTROLLER, template);
        offset.set(9, 9, 9);
        template.clear();
        h.assertTrue(cells.get(LASER_CONTROLLER.offset(1, 2, 3)) == LaserStructureGeometry.Part.FRAME,
                "input position and map changes cannot move an active projection cell");
        boolean immutable = false;
        try { cells.clear(); } catch (UnsupportedOperationException expected) { immutable = true; }
        h.assertTrue(immutable, "projection cell snapshots are immutable");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void selectedChamberSizeSurvivesMenuCloseReopenAndControllerSave(GameTestHelper h) {
        var controller = h.absolutePos(new BlockPos(3, 1, 2));
        var state = MultiblockContent.CONTROLLER.get().defaultBlockState().setValue(SimulationControllerBlock.FACING, Direction.NORTH);
        h.getLevel().setBlockAndUpdate(controller, state);
        var host = (SimulationControllerBlockEntity) h.getLevel().getBlockEntity(controller);
        var player = FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.randomUUID(), "projection-size"));
        player.setPos(controller.getCenter());
        var menu = new MultiblockSimulationMenu(1, player.getInventory(), host);
        player.containerMenu = menu;
        menu.receiveClientAction("simulationSize", "7");
        h.assertTrue(menu.buildSize == 7, "the active menu accepts the selected seven-block chamber");
        player.closeContainer();
        menu.receiveClientAction("simulationSize", "3");
        var reopened = new MultiblockSimulationMenu(2, player.getInventory(), host);
        h.assertTrue(reopened.buildSize == 7, "closing for a projection and reopening preserves size seven");
        player.containerMenu = reopened;
        reopened.broadcastChanges();
        h.assertTrue(reopened.buildSize == 7, "initial GUI synchronization sends the controller's saved choice");
        player.closeContainer();
        var saved = host.saveMachine(h.getLevel().registryAccess());
        h.getLevel().setBlockAndUpdate(controller, Blocks.AIR.defaultBlockState());
        h.getLevel().setBlockAndUpdate(controller, state);
        var restored = (SimulationControllerBlockEntity) h.getLevel().getBlockEntity(controller);
        restored.loadMachine(saved, h.getLevel().registryAccess());
        var restoredMenu = new MultiblockSimulationMenu(3, player.getInventory(), restored);
        h.assertTrue(restoredMenu.buildSize == 7, "a fresh controller restored from saved NBT retains size seven");
        player.discard();
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void chamberSelectionRejectsInvalidActionsAndClampsSavedValues(GameTestHelper h) {
        var controller = h.absolutePos(new BlockPos(3, 1, 2));
        h.getLevel().setBlockAndUpdate(controller, MultiblockContent.CONTROLLER.get().defaultBlockState());
        var host = (SimulationControllerBlockEntity) h.getLevel().getBlockEntity(controller);
        var player = FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.randomUUID(), "projection-size-bounds"));
        player.setPos(controller.getCenter());
        var menu = new MultiblockSimulationMenu(1, player.getInventory(), host);
        player.containerMenu = menu;
        menu.receiveClientAction("simulationSize", "7");
        menu.receiveClientAction("simulationSize", "2");
        menu.receiveClientAction("simulationSize", "8");
        menu.receiveClientAction("simulationSize", "null");
        player.setPos(controller.getCenter().add(9, 0, 0));
        menu.receiveClientAction("simulationSize", "4");
        player.setPos(controller.getCenter());
        h.assertTrue(new MultiblockSimulationMenu(2, player.getInventory(), host).buildSize == 7,
                "invalid, null and out-of-range menu actions cannot overwrite the controller's selected size");
        player.closeContainer();
        var registry = h.getLevel().registryAccess();
        var saved = host.saveMachine(registry);
        saved.putInt("SelectedBuildSize", -10);host.loadMachine(saved, registry);
        h.assertTrue(new MultiblockSimulationMenu(3, player.getInventory(), host).buildSize == 3,
                "saved sizes below the supported range clamp to three");
        saved.putInt("SelectedBuildSize", 99);host.loadMachine(saved, registry);
        h.assertTrue(new MultiblockSimulationMenu(4, player.getInventory(), host).buildSize == 7,
                "saved sizes above the supported range clamp to seven");
        saved.putString("SelectedBuildSize", "7");host.loadMachine(saved, registry);
        h.assertTrue(new MultiblockSimulationMenu(5, player.getInventory(), host).buildSize == 3,
                "malformed non-integer saved choices safely default to size three");
        saved.remove("SelectedBuildSize");host.loadMachine(saved, registry);
        h.assertTrue(new MultiblockSimulationMenu(6, player.getInventory(), host).buildSize == 3,
                "older saved controllers without a choice default to size three");
        player.discard();
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void chamberPreviewShowsExactShellForEverySizeAndFacing(GameTestHelper h) {
        // Independently counted size, front offset, required frames, glass and visible cells.
        int[][] fixtures = {{3,1,20,4,24},{4,1,35,16,51},{5,2,52,36,88},{6,2,71,64,135},{7,3,92,100,192}};
        var min = h.absolutePos(new BlockPos(2, 1, 2));
        var glass = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("ae2:quartz_vibrant_glass"));
        for (var fixture : fixtures) for (var facing : Direction.Plane.HORIZONTAL) {
            clear(h, min);
            int size = fixture[0];
            var controller = switch (facing) {
                case NORTH -> min.offset(fixture[1], 0, 0);
                case SOUTH -> min.offset(fixture[1], 0, size - 1);
                case WEST -> min.offset(0, 0, fixture[1]);
                case EAST -> min.offset(size - 1, 0, fixture[1]);
                default -> throw new IllegalArgumentException("horizontal fixture");
            };
            h.getLevel().setBlockAndUpdate(controller, MultiblockContent.CONTROLLER.get().defaultBlockState()
                    .setValue(SimulationControllerBlock.FACING, facing));
            var host = (SimulationControllerBlockEntity) h.getLevel().getBlockEntity(controller);
            var template = SimulationAutoConstruction.previewTemplate(controller, facing, size);
            h.assertTrue(SimulationAutoConstruction.previewMin(controller, facing, size).equals(min),
                    "preview preserves the front bottom controller anchor for " + size + " facing " + facing);
            h.assertTrue(template.size() == size * size * size, "preview retains the bounded air envelope");
            h.assertTrue(template.values().stream().filter(s -> s.is(MultiblockContent.FRAME.get())).count() == fixture[2],
                    "preview has the required frame count for size " + size);
            h.assertTrue(template.values().stream().filter(s -> s.is(glass)).count() == fixture[3],
                    "preview has the required glass count for size " + size);
            h.assertTrue(template.entrySet().stream().filter(e -> !e.getKey().equals(controller) && !e.getValue().isAir()).count() == fixture[4],
                    "visible preview skips the existing controller, floor upgrades and interior air");
            h.assertTrue(template.get(min.offset(1,0,1)).isAir()
                    && template.get(min.offset(1,size-1,1)).is(MultiblockContent.FRAME.get())
                    && template.get(min.offset(1,1,0)).is(glass), "open floor, solid roof and transparent sides have distinct roles");
            h.assertTrue(template.equals(SimulationAutoConstruction.plan(host, size).cells()),
                    "empty-world construction and preview use the same cells");
            template.forEach((pos, state) -> {
                if (!pos.equals(controller)) h.getLevel().setBlockAndUpdate(pos, state);
            });
            var validated = SimulationStructureValidator.validate(host, min, size);
            h.assertTrue(validated.valid(), "the projected shell validates independently: " + validated.error());
        }
        clear(h, min);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void previewGeometryDoesNotLoadChunksOrChangeBlocks(GameTestHelper h) {
        var witness = h.absolutePos(new BlockPos(2, 1, 2));
        h.getLevel().setBlockAndUpdate(witness, Blocks.STONE.defaultBlockState());
        var before = h.getLevel().getBlockState(witness);
        var distant = witness.offset(1_000_000, 0, -1_000_000);
        int chunkX = distant.getX() >> 4, chunkZ = distant.getZ() >> 4;
        h.assertTrue(!h.getLevel().getChunkSource().hasChunk(chunkX, chunkZ), "distant fixture starts unloaded");
        var template = SimulationAutoConstruction.previewTemplate(distant, Direction.EAST, 7);
        h.assertTrue(template.size() == 343 && template.get(distant).is(MultiblockContent.CONTROLLER.get()),
                "read-only geometry can describe an unloaded position");
        h.assertTrue(!h.getLevel().getChunkSource().hasChunk(chunkX, chunkZ), "preview does not load its distant chunk");
        h.assertTrue(h.getLevel().getBlockState(witness).equals(before), "preview has no world placement side effects");
        boolean immutable = false;
        try { template.clear(); } catch (UnsupportedOperationException expected) { immutable = true; }
        h.assertTrue(immutable, "callers cannot mutate the shared preview template");
        h.succeed();
    }

    private static void clear(GameTestHelper h, BlockPos min) {
        for (int x = 0; x < 7; x++) for (int y = 0; y < 7; y++) for (int z = 0; z < 7; z++)
            h.getLevel().setBlockAndUpdate(min.offset(x, y, z), Blocks.AIR.defaultBlockState());
    }
}
