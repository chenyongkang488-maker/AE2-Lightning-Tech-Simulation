package dev.overloadsim.gametest;

import com.mojang.authlib.GameProfile;
import dev.overloadsim.OverloadSimulation;
import dev.overloadsim.laser.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(OverloadSimulation.ID)
@PrefixGameTestTemplate(false)
public final class LaserReceiverPedestalGameTests {
    // Independent sphere fixture; its bottom center is FRAME rather than the old controller.
    private static final String[][] SPHERE = {
            {".......", ".......", "..GFG..", "..FFF..", "..GFG..", ".......", "......."},
            {".......", ".GGFGG.", ".G...G.", ".F...F.", ".G...G.", ".GGFGG.", "......."},
            {"..GFG..", ".G...G.", "G.....G", "F.....F", "G.....G", ".G...G.", "..GFG.."},
            {"..FPF..", ".F...F.", "F.....F", "F.....F", "F.....F", ".F...F.", "..FFF.."},
            {"..GFG..", ".G...G.", "G.....G", "F.....F", "G.....G", ".G...G.", "..GFG.."},
            {".......", ".GGFGG.", ".G...G.", ".F...F.", ".G...G.", ".GGFGG.", "......."},
            {".......", ".......", "..GFG..", "..FFF..", "..GFG..", ".......", "......."}
    };

    private static BlockPos local(Direction facing, int right, int up, int forward) {
        return switch (facing) {
            case NORTH -> new BlockPos(right, up, forward);
            case SOUTH -> new BlockPos(-right, up, -forward);
            case WEST -> new BlockPos(forward, up, -right);
            case EAST -> new BlockPos(-forward, up, right);
            case UP -> new BlockPos(-right, -forward, -up);
            case DOWN -> new BlockPos(right, forward, -up);
        };
    }

    private static Map<BlockPos, Character> fixture(Direction facing) {
        var result = new LinkedHashMap<BlockPos, Character>();
        for (int y = -1; y <= 0; y++) for (int z = 0; z <= 6; z++) for (int x = -3; x <= 3; x++)
            result.put(local(facing, x, y, z), x == 0 && y == 0 && z == 0 ? 'C' : 'F');
        for (int y = -3; y <= 3; y++) for (int z = -3; z <= 3; z++) for (int x = -3; x <= 3; x++)
            result.put(local(facing, x, y + 4, z + 3), SPHERE[y + 3][z + 3].charAt(x + 3));
        return result;
    }

    private static LaserMachineBlockEntity pedestal(GameTestHelper h, BlockPos controller, Direction facing) {
        for (var entry : fixture(facing).entrySet()) {
            var block = switch (entry.getValue()) {
                case 'C' -> LaserContent.machine(LaserKind.RECEIVER);
                case 'F' -> LaserContent.FRAME.get();
                case 'G' -> LaserContent.GLASS.get();
                case 'P' -> LaserContent.PORT.get();
                default -> Blocks.AIR;
            };
            var state = block.defaultBlockState();
            if (entry.getValue() == 'C') state = state.setValue(LaserMachineBlock.FACING, facing);
            h.getLevel().setBlockAndUpdate(controller.offset(entry.getKey()), state);
        }
        return (LaserMachineBlockEntity) h.getLevel().getBlockEntity(controller);
    }

    private static void forms(GameTestHelper h, Direction facing) {
        var pos = h.absolutePos(new BlockPos(12, 10, 12));
        var host = pedestal(h, pos, facing);
        host.checkStructure();
        h.assertTrue(host.formed(), "independent pedestal receiver forms facing " + facing + " (status=" + host.status() + ")");
        h.assertTrue(host.center().equals(pos.offset(local(facing, 0, 4, 3))), "sphere center is three forward and four up from its accessible controller");
        h.assertTrue(host.structure().members().size() == 441, "both pedestal layers and complete sphere envelope are reserved");
        var expected = new LinkedHashMap<BlockPos, LaserStructureGeometry.Part>();
        fixture(facing).forEach((offset, part) -> expected.put(offset, switch (part) {
            case 'C' -> LaserStructureGeometry.Part.CONTROLLER;
            case 'F' -> LaserStructureGeometry.Part.FRAME;
            case 'G' -> LaserStructureGeometry.Part.GLASS;
            case 'P' -> LaserStructureGeometry.Part.PORT;
            default -> LaserStructureGeometry.Part.AIR;
        }));
        h.assertTrue(host.receiverPedestalLayout() && host.structureTemplate().equals(expected), "published host template exactly matches independently placed pedestal cells");
        h.assertTrue(h.getLevel().getBlockState(pos.offset(local(facing, 0, 1, 3))).is(LaserContent.FRAME.get()), "former sphere-bottom controller is a frame");
        h.assertTrue(h.getLevel().getBlockState(pos.offset(local(facing, 0, 4, 0))).is(LaserContent.PORT.get()), "sphere keeps the upstream optical port");
        h.getLevel().setBlockAndUpdate(pos.offset(local(facing, -3, -1, 6)), Blocks.AIR.defaultBlockState());
        host.checkStructure();
        h.assertTrue(!host.formed(), "removing a lower rear pedestal corner invalidates containment");
        h.succeed();
    }

    @GameTest(template="laser_pedestal_empty") public static void receiverPedestalFacesNorth(GameTestHelper h) { forms(h, Direction.NORTH); }
    @GameTest(template="laser_pedestal_empty") public static void receiverPedestalFacesSouth(GameTestHelper h) { forms(h, Direction.SOUTH); }
    @GameTest(template="laser_pedestal_empty") public static void receiverPedestalFacesEast(GameTestHelper h) { forms(h, Direction.EAST); }
    @GameTest(template="laser_pedestal_empty") public static void receiverPedestalFacesWest(GameTestHelper h) { forms(h, Direction.WEST); }
    @GameTest(template="laser_pedestal_empty") public static void receiverPedestalFacesUp(GameTestHelper h) { forms(h, Direction.UP); }
    @GameTest(template="laser_pedestal_empty") public static void receiverPedestalFacesDown(GameTestHelper h) { forms(h, Direction.DOWN); }

    @GameTest(template="laser_pedestal_empty")
    public static void pedestalLayoutReloadPreservesPaidResources(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(12, 10, 12));
        var host = pedestal(h, pos, Direction.WEST); host.checkStructure(); host.setEnabled(false);
        host.energy().load(1_024_000_000_000L); host.particles().load(7);
        host.fuelInput().insertItem(0, new ItemStack(Items.NETHER_STAR, 2), false);
        var saved = host.saveMachine(h.getLevel().registryAccess());
        h.assertTrue(saved.getBoolean("ReceiverPedestalLayout"), "newly placed receivers persist their own layout marker");
        saved.putInt("FuelRemaining", 72); saved.putInt("ParticleRemaining", 12); saved.putString("ActiveFuel", "overload_sim:nether_star");
        host.loadMachine(saved, h.getLevel().registryAccess()); host.tick();
        h.assertTrue(host.formed() && !host.explosionTriggered() && host.center().equals(pos.east(3).above(4)), "saved pedestal reloads in its original geometry");
        h.assertTrue(host.energy().stored() == 1_024_000_000_000L && host.particles().stored() == 7 && host.fuelInput().getStackInSlot(0).getCount() == 2 && host.fuelRemaining() == 72 && host.particleRemaining() == 12, "layout reload preserves inventory and already-paid clocks");
        host.particles().load(0); h.succeed();
    }

    private static LaserMachineBlockEntity copy(GameTestHelper h, LaserMachineBlockEntity source) {
        var target = new LaserMachineBlockEntity(source.getBlockPos(), source.getBlockState());
        target.setLevel(h.getLevel());
        return target;
    }

    private static Object invokeSnapshot(LaserMachineBlockEntity host, String name, Class<?> argumentType, Object argument) throws ReflectiveOperationException {
        var method = LaserMachineBlockEntity.class.getDeclaredMethod(name, argumentType);
        method.setAccessible(true); return method.invoke(host, argument);
    }

    private static void assertSnapshot(GameTestHelper h, LaserMachineBlockEntity copy, boolean pedestal, BlockPos center) throws ReflectiveOperationException {
        h.assertTrue(copy.receiverPedestalLayout() == pedestal && copy.center().equals(center), "snapshot preserves the receiver layout used to derive its geometry");
        var field = LaserMachineBlockEntity.class.getDeclaredField("visualCenter"); field.setAccessible(true);
        h.assertTrue(center.equals(field.get(copy)), "snapshot transmits the authoritative sphere center");
        h.assertTrue(copy.structureTemplate().size() == (pedestal ? 441 : 343), "rendering receives exactly the matching per-host template");
    }

    @GameTest(template="laser_pedestal_empty")
    public static void streamSnapshotRetainsBothReceiverLayouts(GameTestHelper h) throws ReflectiveOperationException {
        var pos = h.absolutePos(new BlockPos(12, 10, 12));
        var source = pedestal(h, pos, Direction.DOWN); source.checkStructure();
        var buffer = new RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), h.getLevel().registryAccess());
        try {
            invokeSnapshot(source, "writeToStream", RegistryFriendlyByteBuf.class, buffer);
            var target = copy(h, source); invokeSnapshot(target, "readFromStream", RegistryFriendlyByteBuf.class, buffer);
            assertSnapshot(h, target, true, pos.above(3).north(4));
            h.assertTrue(target.receiverVisualFacing() == Direction.DOWN && buffer.readableBytes() == 0, "snapshot retains vertical controller facing and consumes its complete packet");
            source.invalidateStructure();
            var legacy = source.saveMachine(h.getLevel().registryAccess()); legacy.remove("ReceiverPedestalLayout");
            source.loadMachine(legacy, h.getLevel().registryAccess());
            buffer.clear(); invokeSnapshot(source, "writeToStream", RegistryFriendlyByteBuf.class, buffer);
            target = copy(h, source); invokeSnapshot(target, "readFromStream", RegistryFriendlyByteBuf.class, buffer);
            assertSnapshot(h, target, false, pos.north(3));
            h.assertTrue(buffer.readableBytes() == 0, "legacy receiver snapshot consumes the same packet schema");
        } finally { buffer.release(); }
        h.succeed();
    }

    @GameTest(template="laser_pedestal_empty")
    public static void visualTagSnapshotRetainsBothReceiverLayouts(GameTestHelper h) throws ReflectiveOperationException {
        var pos = h.absolutePos(new BlockPos(12, 10, 12));
        var source = pedestal(h, pos, Direction.WEST); source.checkStructure();
        var tag = new net.minecraft.nbt.CompoundTag();
        invokeSnapshot(source, "saveVisualState", net.minecraft.nbt.CompoundTag.class, tag);
        var target = copy(h, source); invokeSnapshot(target, "loadVisualState", net.minecraft.nbt.CompoundTag.class, tag);
        assertSnapshot(h, target, true, pos.east(3).above(4));
        source.invalidateStructure();
        var legacy = source.saveMachine(h.getLevel().registryAccess()); legacy.remove("ReceiverPedestalLayout");
        source.loadMachine(legacy, h.getLevel().registryAccess());
        tag = new net.minecraft.nbt.CompoundTag(); invokeSnapshot(source, "saveVisualState", net.minecraft.nbt.CompoundTag.class, tag);
        target = copy(h, source); invokeSnapshot(target, "loadVisualState", net.minecraft.nbt.CompoundTag.class, tag);
        assertSnapshot(h, target, false, pos.above(3));
        h.succeed();
    }

    @GameTest(template="laser_empty")
    public static void receiverWithoutLayoutMarkerKeepsChargedLegacySphere(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(8, 2, 8));
        var host = LaserMachineGameTests.build(h, pos, LaserKind.RECEIVER, Direction.WEST); host.checkStructure(); host.setEnabled(false);
        host.energy().load(1_024_000_000_000L); host.particles().load(7);
        host.fuelInput().insertItem(0, new ItemStack(Items.NETHER_STAR, 2), false);
        var old = host.saveMachine(h.getLevel().registryAccess()); old.remove("ReceiverPedestalLayout");
        old.putInt("FuelRemaining", 72); old.putInt("ParticleRemaining", 12); old.putString("ActiveFuel", "overload_sim:nether_star");
        host.loadMachine(old, h.getLevel().registryAccess()); host.tick();
        h.assertTrue(host.formed() && !host.explosionTriggered() && host.center().equals(pos.above(3)) && host.structure().members().size() == 343, "unmarked charged saves retain the valid legacy sphere without reconstruction or explosion");
        h.assertTrue(host.energy().stored() == 1_024_000_000_000L && host.particles().stored() == 7 && host.fuelInput().getStackInSlot(0).getCount() == 2 && host.fuelRemaining() == 72 && host.particleRemaining() == 12, "legacy load preserves resources and paid fuel windows");
        var saved = host.saveMachine(h.getLevel().registryAccess());
        h.assertTrue(saved.contains("ReceiverPedestalLayout") && !saved.getBoolean("ReceiverPedestalLayout"), "a subsequent save explicitly retains the legacy layout");
        host.loadMachine(saved, h.getLevel().registryAccess()); host.checkStructure();
        h.assertTrue(host.formed() && !host.explosionTriggered() && host.center().equals(pos.above(3)), "explicit legacy layout survives another reload");
        host.particles().load(0); h.succeed();
    }

    @GameTest(template="laser_pedestal_empty")
    public static void survivalBuildUsesPedestalMaterialsWithoutResettingResources(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(12, 10, 12));
        h.getLevel().setBlockAndUpdate(pos, LaserContent.machine(LaserKind.RECEIVER).defaultBlockState().setValue(LaserMachineBlock.FACING, Direction.NORTH));
        var host = (LaserMachineBlockEntity) h.getLevel().getBlockEntity(pos);
        var player = FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.randomUUID(), "pedestal-builder"));
        player.setGameMode(GameType.SURVIVAL); player.getInventory().clearContent(); player.setPos(pos.getCenter());
        h.getLevel().setBlockAndUpdate(pos.offset(-3, -1, 6), LaserContent.FRAME.get().defaultBlockState());
        player.getInventory().add(new ItemStack(LaserContent.FRAME.get(), 64));
        player.getInventory().add(new ItemStack(LaserContent.FRAME.get(), 64));
        player.getInventory().add(new ItemStack(LaserContent.FRAME.get(), 9));
        player.getInventory().add(new ItemStack(LaserContent.GLASS.get(), 56));
        player.getInventory().add(new ItemStack(LaserContent.PORT.get(), 1));
        host.energy().load(1234); host.particles().load(4);
        var result = LaserAutoConstruction.build(player, host);
        h.assertTrue(result.successful() && result.placed() == 194 && host.formed(), "one existing pedestal frame reduces the 195 missing blocks to 194 (result=" + result + ")");
        h.assertTrue(player.getInventory().isEmpty() && host.energy().stored() == 1234 && host.particles().stored() == 4, "both pedestal layers consume exact survival materials and preserve stored resources");
        host.particles().load(0); h.succeed();
    }
}
