package dev.overloadsim.gametest;

import com.mojang.authlib.GameProfile;
import dev.overloadsim.OverloadSimulation;
import dev.overloadsim.laser.LaserAutoConstruction;
import dev.overloadsim.laser.LaserContent;
import dev.overloadsim.laser.LaserKind;
import dev.overloadsim.laser.LaserMachineBlock;
import dev.overloadsim.laser.LaserMachineBlockEntity;
import dev.overloadsim.laser.LaserStructureGeometry;
import dev.overloadsim.laser.ReceiverOpticalPorts;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(OverloadSimulation.ID)
@PrefixGameTestTemplate(false)
public final class LaserReceiverPortGameTests {
    private static BlockPos controller(GameTestHelper h) {
        return h.absolutePos(new BlockPos(12, 10, 12));
    }

    private static LaserMachineBlockEntity placeController(GameTestHelper h) {
        var pos = controller(h);
        h.getLevel().setBlockAndUpdate(pos, LaserContent.machine(LaserKind.RECEIVER).defaultBlockState()
                .setValue(LaserMachineBlock.FACING, Direction.NORTH));
        return (LaserMachineBlockEntity) h.getLevel().getBlockEntity(pos);
    }

    // Independent north-facing pedestal fixture. Port choices replace only equatorial face centres.
    private static LaserMachineBlockEntity receiver(GameTestHelper h, Set<Direction> ports) {
        var pos = controller(h);
        for (int x = -3; x <= 3; x++) for (int y = -1; y <= 0; y++) for (int z = 0; z <= 6; z++)
            h.getLevel().setBlockAndUpdate(pos.offset(x, y, z), LaserContent.FRAME.get().defaultBlockState());
        var center = pos.south(3).above(4);
        for (int x = -3; x <= 3; x++) for (int y = -3; y <= 3; y++) for (int z = -3; z <= 3; z++) {
            int distance = x * x + y * y + z * z;
            var block = distance >= 7 && distance <= 12
                    ? x == 0 || y == 0 || z == 0 ? LaserContent.FRAME.get() : LaserContent.GLASS.get()
                    : Blocks.AIR;
            h.getLevel().setBlockAndUpdate(center.offset(x, y, z), block.defaultBlockState());
        }
        for (var side : ports)
            h.getLevel().setBlockAndUpdate(center.relative(side, 3), LaserContent.PORT.get().defaultBlockState());
        return placeController(h);
    }

    private static void solePortForms(GameTestHelper h, Direction side) {
        var host = receiver(h, EnumSet.of(side));
        host.checkStructure();
        h.assertTrue(host.formed(), "the sole " + side + " optical port forms independently of pedestal controller facing (status=" + host.status() + ")");
        h.assertTrue(host.center().equals(controller(h).south(3).above(4)) && host.structure().members().size() == 441,
                "selection keeps the established sphere centre and complete pedestal ownership");
        for (var other : Direction.Plane.HORIZONTAL)
            h.assertTrue(h.getLevel().getBlockState(host.center().relative(other, 3)).is(other == side ? LaserContent.PORT.get() : LaserContent.FRAME.get()),
                    "unselected horizontal face " + other + " stays a solid frame");
        h.succeed();
    }

    @GameTest(template = "laser_pedestal_empty") public static void soleNorthPortForms(GameTestHelper h) { solePortForms(h, Direction.NORTH); }
    @GameTest(template = "laser_pedestal_empty") public static void soleEastPortForms(GameTestHelper h) { solePortForms(h, Direction.EAST); }
    @GameTest(template = "laser_pedestal_empty") public static void soleSouthPortForms(GameTestHelper h) { solePortForms(h, Direction.SOUTH); }
    @GameTest(template = "laser_pedestal_empty") public static void soleWestPortForms(GameTestHelper h) { solePortForms(h, Direction.WEST); }

    @GameTest(template = "laser_pedestal_empty")
    public static void twoSelectedPortsFormWithoutCanonicalPort(GameTestHelper h) {
        var host = receiver(h, EnumSet.of(Direction.EAST, Direction.WEST));
        host.checkStructure();
        h.assertTrue(host.formed(), "two chosen side ports form with a frame on the canonical north face");
        h.assertTrue(h.getLevel().getBlockState(host.center().north(3)).is(LaserContent.FRAME.get()), "formation does not insert a default port");
        h.succeed();
    }

    @GameTest(template = "laser_pedestal_empty")
    public static void allFourSelectedPortsForm(GameTestHelper h) {
        var host = receiver(h, EnumSet.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST));
        host.checkStructure();
        h.assertTrue(host.formed(), "four horizontal optical ports may share the same spherical containment shell");
        h.succeed();
    }

    @GameTest(template = "laser_pedestal_empty")
    public static void noOpticalPortCannotForm(GameTestHelper h) {
        var host = receiver(h, EnumSet.noneOf(Direction.class));
        host.checkStructure();
        h.assertTrue(!host.formed(), "a complete all-frame shell requires at least one optical port");
        h.succeed();
    }

    @GameTest(template = "laser_pedestal_empty")
    public static void nonCentralAndVerticalPortsCannotReplaceFrames(GameTestHelper h) {
        var host = receiver(h, EnumSet.of(Direction.NORTH));
        h.getLevel().setBlockAndUpdate(host.center().east(3).above(), LaserContent.PORT.get().defaultBlockState());
        host.checkStructure();
        h.assertTrue(!host.formed(), "an off-centre shell cell is not an optional optical port");
        h.getLevel().setBlockAndUpdate(host.center().east(3).above(), LaserContent.FRAME.get().defaultBlockState());
        h.getLevel().setBlockAndUpdate(host.center().above(3), LaserContent.PORT.get().defaultBlockState());
        host.checkStructure();
        h.assertTrue(!host.formed(), "horizontal pedestal receivers do not gain top ports");
        h.succeed();
    }

    @GameTest(template = "laser_pedestal_empty")
    public static void alternatePortSurvivalBuildPreservesSelectionAndExactMaterials(GameTestHelper h) {
        var host = placeController(h);
        var center = controller(h).south(3).above(4);
        h.getLevel().setBlockAndUpdate(center.east(3), LaserContent.PORT.get().defaultBlockState());
        h.getLevel().setBlockAndUpdate(center.north(3), LaserContent.FRAME.get().defaultBlockState());
        var player = FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.randomUUID(), "receiver-port-builder"));
        player.setGameMode(GameType.SURVIVAL); player.getInventory().clearContent(); player.setPos(controller(h).getCenter());
        player.getInventory().add(new ItemStack(LaserContent.FRAME.get(), 64));
        player.getInventory().add(new ItemStack(LaserContent.FRAME.get(), 64));
        player.getInventory().add(new ItemStack(LaserContent.FRAME.get(), 9));
        player.getInventory().add(new ItemStack(LaserContent.GLASS.get(), 56));
        host.energy().load(1234); host.particles().load(4);
        try {
            var result = LaserAutoConstruction.build(player, host);
            h.assertTrue(result.successful() && host.formed() && result.placed() == 193,
                    "existing side port and north frame leave exactly 193 missing frame/glass blocks (result=" + result + ")");
            h.assertTrue(player.getInventory().isEmpty(), "selected side port requires no replacement optical-port item");
            h.assertTrue(h.getLevel().getBlockState(center.east(3)).is(LaserContent.PORT.get())
                    && h.getLevel().getBlockState(center.north(3)).is(LaserContent.FRAME.get()), "auto-build preserves the chosen face");
            h.assertTrue(host.energy().stored() == 1234 && host.particles().stored() == 4, "construction preserves paid resources");
        } finally { host.particles().load(0); player.discard(); }
        h.succeed();
    }

    @GameTest(template = "laser_pedestal_empty")
    public static void multiplePortsAutoBuildAddsNoUnselectedPort(GameTestHelper h) {
        var host = placeController(h);
        var center = controller(h).south(3).above(4);
        h.getLevel().setBlockAndUpdate(center.east(3), LaserContent.PORT.get().defaultBlockState());
        h.getLevel().setBlockAndUpdate(center.west(3), LaserContent.PORT.get().defaultBlockState());
        var player = FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.randomUUID(), "receiver-multi-builder"));
        player.setGameMode(GameType.CREATIVE); player.setPos(controller(h).getCenter());
        try {
            var result = LaserAutoConstruction.build(player, host);
            h.assertTrue(result.successful() && host.formed() && result.placed() == 193,
                    "existing two side ports are preserved during completion (result=" + result + ")");
            h.assertTrue(h.getLevel().getBlockState(center.north(3)).is(LaserContent.FRAME.get())
                    && h.getLevel().getBlockState(center.south(3)).is(LaserContent.FRAME.get()), "completion fills unselected centres with frames");
        } finally { player.discard(); }
        h.succeed();
    }

    @GameTest(template = "laser_pedestal_empty")
    public static void chosenPortsReloadWithoutResettingPaidState(GameTestHelper h) {
        var host = receiver(h, EnumSet.of(Direction.EAST, Direction.SOUTH));
        host.checkStructure(); host.setEnabled(false);
        host.energy().load(1_024_000_000_000L); host.particles().load(7);
        host.fuelInput().insertItem(0, new ItemStack(Items.NETHER_STAR, 2), false);
        var saved = host.saveMachine(h.getLevel().registryAccess());
        saved.putInt("FuelRemaining", 72); saved.putInt("ParticleRemaining", 12); saved.putString("ActiveFuel", "overload_sim:nether_star");
        try {
            host.loadMachine(saved, h.getLevel().registryAccess()); host.tick();
            h.assertTrue(host.formed() && !host.explosionTriggered(), "selected side ports reload as the same valid pedestal structure");
            h.assertTrue(host.center().equals(controller(h).south(3).above(4)) && host.energy().stored() == 1_024_000_000_000L
                    && host.particles().stored() == 7 && host.fuelInput().getStackInSlot(0).getCount() == 2
                    && host.fuelRemaining() == 72 && host.particleRemaining() == 12, "reload keeps anchor, inventory, energy and paid clocks");
        } finally { host.particles().load(0); }
        h.succeed();
    }

    @GameTest(template = "laser_empty")
    public static void historicalVerticalApertureAndControllerRemainValid(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(8, 8, 8));
        var host = LaserMachineGameTests.build(h, pos, LaserKind.RECEIVER, Direction.UP);
        host.checkStructure();
        h.assertTrue(host.formed() && host.center().equals(pos.north(3)), "historical vertical receiver retains its original sphere anchor");
        h.assertTrue(ReceiverOpticalPorts.accepts(host, pos.north(3).above(3), Direction.DOWN), "historical axial optical port still accepts its original incoming direction");
        h.assertTrue(!ReceiverOpticalPorts.compatible(host, pos), "a horizontal face occupied by the historical controller is excluded from port choices");
        h.getLevel().setBlockAndUpdate(host.center().east(3), LaserContent.PORT.get().defaultBlockState());
        host.checkStructure();
        h.assertTrue(host.formed() && host.structureTemplate().get(BlockPos.ZERO) == LaserStructureGeometry.Part.CONTROLLER,
                "adding a compatible horizontal port preserves the old controller block");
        h.assertTrue(ReceiverOpticalPorts.accepts(host, host.center().east(3), Direction.WEST), "legacy sphere gains an independent compatible horizontal input");
        h.succeed();
    }

    @GameTest(template = "laser_pedestal_empty")
    public static void frameAtFormerDefaultPortKeepsFrameMenuOwnership(GameTestHelper h) {
        var host = receiver(h, EnumSet.of(Direction.EAST));
        host.checkStructure();
        var frame = controller(h).above(4);
        h.assertTrue(dev.overloadsim.laser.LaserStructureIndex.frameOwner(h.getLevel(), frame) == host,
                "a closed canonical aperture is a normal owned frame that can open the controller menu");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void resolvingOptionalPortsNeverLoadsAbsentChunks(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(2, 1, 2)).offset(1_000_000, 0, -1_000_000);
        var state = LaserContent.machine(LaserKind.RECEIVER).defaultBlockState().setValue(LaserMachineBlock.FACING, Direction.NORTH);
        var host = new LaserMachineBlockEntity(pos, state); host.setLevel(h.getLevel());
        int x = pos.getX() >> 4, z = pos.getZ() >> 4;
        h.assertTrue(!h.getLevel().getChunkSource().hasChunk(x, z), "distant receiver fixture begins outside loaded chunks");
        var canonical = LaserStructureGeometry.template(LaserKind.RECEIVER, Direction.NORTH, true);
        var resolved = ReceiverOpticalPorts.resolve(host, canonical);
        h.assertTrue(resolved.values().stream().filter(part -> part == LaserStructureGeometry.Part.PORT).count() == 1,
                "unavailable optical cells retain the ordinary one-port construction default");
        h.assertTrue(!h.getLevel().getChunkSource().hasChunk(x, z), "optional-port resolution did not request the absent receiver chunk");
        h.succeed();
    }
}
