package dev.overloadsim.gametest;

import dev.overloadsim.OverloadSimulation;
import dev.overloadsim.laser.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(OverloadSimulation.ID)
@PrefixGameTestTemplate(false)
public final class LaserMachineGameTests {
    public static LaserMachineBlockEntity build(GameTestHelper h, BlockPos controller, LaserKind kind, Direction facing) {
        for (var entry : LaserStructureGeometry.template(kind, facing).entrySet()) {
            var block = switch (entry.getValue()) {
                case CONTROLLER -> LaserContent.machine(kind);
                case FRAME -> LaserContent.FRAME.get();
                case GLASS -> LaserContent.GLASS.get();
                case SOURCE -> LaserContent.SOURCE.get();
                case PORT -> LaserContent.PORT.get();
                case AIR -> Blocks.AIR;
            };
            var state = block.defaultBlockState();
            if (entry.getValue() == LaserStructureGeometry.Part.CONTROLLER) state = state.setValue(LaserMachineBlock.FACING, facing);
            h.getLevel().setBlockAndUpdate(controller.offset(entry.getKey()), state);
        }
        var host = (LaserMachineBlockEntity) h.getLevel().getBlockEntity(controller);
        if (kind == LaserKind.RECEIVER) {
            // This helper represents already-existing spherical machines for runtime regression tests.
            var legacy = host.saveMachine(h.getLevel().registryAccess());
            legacy.remove("ReceiverPedestalLayout"); host.loadMachine(legacy, h.getLevel().registryAccess());
        }
        return host;
    }

    @GameTest(template = "laser_empty")
    public static void rotatedRingRequiresClearAperture(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(8, 2, 8));
        var ring = build(h, pos, LaserKind.CONSTRAINT, Direction.EAST);
        ring.checkStructure();
        h.assertTrue(ring.formed() && ring.center().equals(pos.above(3)) && ring.forward() == Direction.WEST, "east facing controller forms westward ring with bottom controller");
        h.getLevel().setBlockAndUpdate(pos.above(3), Blocks.STONE.defaultBlockState());
        ring.tick();
        h.assertTrue(!ring.formed() && ring.segments().isEmpty(), "a blocked ring aperture immediately stops its structure and beam");
        h.succeed();
    }

    @GameTest(template = "laser_empty")
    public static void emitterRequiresCentralSource(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(8, 3, 8));
        var emitter = build(h, pos, LaserKind.EMITTER, Direction.WEST);
        emitter.checkStructure();
        h.assertTrue(emitter.formed() && emitter.center().equals(pos.east(2)), "emitter source is two blocks forward from its controller");
        h.getLevel().setBlockAndUpdate(pos.east(2), Blocks.AIR.defaultBlockState());
        emitter.tick();
        h.assertTrue(!emitter.formed(), "removing source invalidates emitter");
        h.succeed();
    }

    @GameTest(template = "laser_empty")
    public static void deflectorRequiresSixFacePorts(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(8, 2, 8));
        var deflector = build(h, pos, LaserKind.DEFLECTOR, Direction.NORTH);
        deflector.checkStructure();
        h.assertTrue(deflector.formed(), "voxel sphere forms with optical ports");
        for (var face : Direction.values()) h.assertTrue(h.getLevel().getBlockState(pos.above(3).relative(face, 3)).is(LaserContent.PORT.get()) || pos.above(3).relative(face, 3).equals(pos), "each central face has a port or the bottom controller");
        h.getLevel().setBlockAndUpdate(pos.above(6), LaserContent.FRAME.get().defaultBlockState());
        deflector.tick();
        h.assertTrue(!deflector.formed(), "replacing an optical port with ordinary frame invalidates sphere");
        h.succeed();
    }

    @GameTest(template = "laser_empty")
    public static void receiverRequiresUpstreamPort(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(8, 2, 8));
        var receiver = build(h, pos, LaserKind.RECEIVER, Direction.WEST);
        receiver.checkStructure();
        h.assertTrue(receiver.formed() && receiver.center().equals(pos.above(3)), "receiver cage forms");
        h.assertTrue(h.getLevel().getBlockState(pos.above(3).west(3)).is(LaserContent.PORT.get()), "receiver port faces upstream toward core");
        h.getLevel().setBlockAndUpdate(pos.above(3).west(3), LaserContent.GLASS.get().defaultBlockState());
        receiver.tick();
        h.assertTrue(!receiver.formed(), "glass cannot replace receiver input port");
        h.succeed();
    }

    @GameTest(template = "laser_empty")
    public static void crossedRingsCannotShareMembers(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(8, 2, 8));
        var first = build(h, pos, LaserKind.CONSTRAINT, Direction.NORTH);
        first.checkStructure();
        h.assertTrue(first.formed(), "first ring forms");
        var second = build(h, pos.above(3).south(3), LaserKind.CONSTRAINT, Direction.DOWN);
        second.checkStructure();
        h.assertTrue(!second.formed() && first.formed(), "perpendicular rings cannot claim their shared frame cells");
        h.succeed();
    }

    @GameTest(template = "laser_empty")
    public static void ringsNeedOneAirCellBetweenTheirPlanes(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(8, 2, 8));
        var first = build(h, pos, LaserKind.CONSTRAINT, Direction.NORTH); first.checkStructure();
        var adjacent = build(h, pos.south(), LaserKind.ENHANCEMENT, Direction.NORTH); adjacent.checkStructure();
        h.assertTrue(first.formed() && !adjacent.formed(), "neighboring ring planes with no clear gap cannot form");
        for (var offset : LaserStructureGeometry.template(LaserKind.ENHANCEMENT, Direction.NORTH).keySet()) h.getLevel().setBlockAndUpdate(pos.south().offset(offset), Blocks.AIR.defaultBlockState());
        var separated = build(h, pos.south(2), LaserKind.ENHANCEMENT, Direction.NORTH); separated.checkStructure();
        h.assertTrue(first.formed() && separated.formed(), "one whole air plane between rings permits both structures");
        h.succeed();
    }

    @GameTest(template = "laser_empty")
    public static void aStructureAllowsOnlyOneUpstreamInterface(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(8, 2, 8));
        var ring = build(h, pos, LaserKind.CONSTRAINT, Direction.NORTH);
        var interfaceBlock = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.parse("ae2lt:overloaded_interface"));
        h.getLevel().setBlockAndUpdate(pos.above(3).east(3), interfaceBlock.defaultBlockState()); ring.checkStructure();
        h.assertTrue(ring.formed() && ring.structure().networkInterface().equals(pos.above(3).east(3)), "one overloaded interface can replace a frame");
        h.getLevel().setBlockAndUpdate(pos.above(3).west(3), interfaceBlock.defaultBlockState()); ring.checkStructure();
        h.assertTrue(!ring.formed(), "second overloaded interface is rejected");
        h.succeed();
    }

    @GameTest(template = "laser_empty")
    public static void receiverLongStateRoundTripsAndClampsMalformedValues(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(8, 2, 8));
        var receiver = build(h, pos, LaserKind.RECEIVER, Direction.NORTH);
        receiver.energy().load(1_024_000_000_000_000_000L);
        receiver.particles().load(125_000L);
        receiver.setExtremeLightning(9_000_000_000L);
        var tag = receiver.saveMachine(h.getLevel().registryAccess());
        receiver.energy().load(0); receiver.particles().load(0); receiver.setExtremeLightning(0);
        receiver.loadMachine(tag, h.getLevel().registryAccess());
        h.assertTrue(receiver.energy().stored() == 1_024_000_000_000_000_000L && receiver.particles().stored() == 125_000 && receiver.extremeLightning() == 9_000_000_000L, "long energy, particles and lightning survive exact save and reload");
        tag.putInt("FuelRemaining", 72); tag.putInt("ParticleRemaining", 12); tag.putString("ActiveFuel", "overload_sim:netherite_block");
        receiver.loadMachine(tag, h.getLevel().registryAccess());
        h.assertTrue(receiver.fuelRemaining() == 72 && receiver.particleRemaining() == 12 && receiver.activeFuel().equals("overload_sim:netherite_block"), "paid fuel clocks and stable recipe ID survive reload without renewing a window");
        tag.putLong("Energy", Long.MAX_VALUE); tag.putLong("Particles", -1); tag.putLong("Boost", Long.MAX_VALUE); tag.putInt("OutputMask", -1); tag.getCompound("FuelInput").putInt("Size", Integer.MAX_VALUE);
        receiver.loadMachine(tag, h.getLevel().registryAccess());
        h.assertTrue(receiver.energy().stored() == 1_024_000_000_000_000_000L && receiver.particles().stored() == 0 && receiver.boost() == 9_000_000 && receiver.outputMask() == 63 && receiver.fuelInput().getSlots() == 4, "malformed resources and advertised inventory size are clamped before allocation");
        h.succeed();
    }

    @GameTest(template = "laser_empty")
    public static void fuelInputRejectsEveryExtractionRoute(GameTestHelper h) {
        var receiver = build(h, h.absolutePos(new BlockPos(8, 2, 8)), LaserKind.RECEIVER, Direction.NORTH);
        var remainder = receiver.fuelInput().insertItem(0, new ItemStack(Items.NETHER_STAR, 2), false);
        h.assertTrue(remainder.isEmpty(), "known receiver fuel inserts");
        h.assertTrue(receiver.fuelInput().extractItem(0, 1, true).isEmpty() && receiver.fuelInput().extractItem(0, 1, false).isEmpty() && receiver.fuelInput().getStackInSlot(0).getCount() == 2, "simulation and actual automation cannot extract inserted fuel");
        var capability = h.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, receiver.getBlockPos(), Direction.NORTH);
        h.assertTrue(capability != null && !(capability instanceof net.neoforged.neoforge.items.IItemHandlerModifiable) && capability.extractItem(0, 64, false).isEmpty(), "the published automation capability cannot replace or take a paid fuel stack");
        var exposed = capability.getStackInSlot(0); exposed.shrink(1);
        h.assertTrue(receiver.fuelInput().getStackInSlot(0).getCount() == 2, "mutating a published stack cannot consume or extract stored fuel");
        h.succeed();
    }

    @GameTest(template = "laser_empty")
    public static void unpaidBurstPreservesAllLocalResources(GameTestHelper h) {
        var burst = build(h, h.absolutePos(new BlockPos(8, 2, 8)), LaserKind.BURST, Direction.NORTH);
        burst.energy().load(100_000); burst.setExtremeLightning(63);
        h.assertTrue(!burst.tryPay(100_000, 64, false) && burst.energy().stored() == 100_000 && burst.extremeLightning() == 63, "missing lightning never debits available FE");
        burst.setExtremeLightning(64);
        h.assertTrue(burst.tryPay(100_000, 64, true) && burst.energy().stored() == 100_000 && burst.extremeLightning() == 64, "payment simulation preserves resources");
        h.assertTrue(burst.tryPay(100_000, 64, false) && burst.energy().stored() == 0 && burst.extremeLightning() == 0, "successful payment debits exactly once");
        h.succeed();
    }
}
