package dev.overloadsim.gametest;

import dev.overloadsim.OverloadSimulation;
import dev.overloadsim.laser.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(OverloadSimulation.ID)
@PrefixGameTestTemplate(false)
public final class LaserGeometryGameTests {
    // Hand-authored construction fixture, independent of the production template.
    // Rows run upstream to downstream, columns left to right, layers bottom to top.
    private static final String[][] SPHERE = {
            {".......", ".......", "..GFG..", "..FCF..", "..GFG..", ".......", "......."},
            {".......", ".GGFGG.", ".G...G.", ".F...F.", ".G...G.", ".GGFGG.", "......."},
            {"..GFG..", ".G...G.", "G.....G", "F.....F", "G.....G", ".G...G.", "..GFG.."},
            {"..FPF..", ".F...F.", "F.....F", "F.....F", "F.....F", ".F...F.", "..FFF.."},
            {"..GFG..", ".G...G.", "G.....G", "F.....F", "G.....G", ".G...G.", "..GFG.."},
            {".......", ".GGFGG.", ".G...G.", ".F...F.", ".G...G.", ".GGFGG.", "......."},
            {".......", ".......", "..GFG..", "..FFF..", "..GFG..", ".......", "......."}
    };

    private static BlockPos local(Direction facing, int x, int y, int z) {
        return switch (facing) {
            case NORTH -> new BlockPos(x, y, z);
            case SOUTH -> new BlockPos(-x, y, -z);
            case WEST -> new BlockPos(z, y, -x);
            case EAST -> new BlockPos(-z, y, x);
            case UP -> new BlockPos(-x, -z, -y);
            case DOWN -> new BlockPos(x, z, -y);
        };
    }

    private static LaserMachineBlockEntity receiver(GameTestHelper h, BlockPos controller, Direction facing, boolean sphere) {
        BlockPos center = controller.offset(local(facing, 0, 3, 0));
        for (int y = -3; y <= 3; y++) for (int z = -3; z <= 3; z++) for (int x = -3; x <= 3; x++) {
            char part;
            if (sphere) part = SPHERE[y + 3][z + 3].charAt(x + 3);
            else {
                int edges = (Math.abs(x) == 3 ? 1 : 0) + (Math.abs(y) == 3 ? 1 : 0) + (Math.abs(z) == 3 ? 1 : 0);
                part = edges >= 2 ? 'F' : edges == 1 ? 'G' : '.';
                if (x == 0 && y == -3 && z == 0) part = 'C';
                if (x == 0 && y == 0 && z == -3) part = 'P';
            }
            var block = switch (part) {
                case 'F' -> LaserContent.FRAME.get();
                case 'G' -> LaserContent.GLASS.get();
                case 'P' -> LaserContent.PORT.get();
                case 'C' -> LaserContent.machine(LaserKind.RECEIVER);
                default -> Blocks.AIR;
            };
            var state = block.defaultBlockState();
            if (part == 'C') state = state.setValue(LaserMachineBlock.FACING, facing);
            h.getLevel().setBlockAndUpdate(center.offset(local(facing, x, y, z)), state);
        }
        return (LaserMachineBlockEntity) h.getLevel().getBlockEntity(controller);
    }

    private static void sphereForms(GameTestHelper h, Direction facing) {
        var pos = h.absolutePos(new BlockPos(8, 4, 8));
        var host = receiver(h, pos, facing, true); host.checkStructure();
        h.assertTrue(host.formed() && host.center().equals(pos.offset(local(facing, 0, 3, 0))), "independent sphere fixture forms facing " + facing);
        h.assertTrue(host.structure().members().size() == 343, "sphere also reserves clear envelope cells against overlap");
        h.getLevel().setBlockAndUpdate(host.center().offset(local(facing, 3, 3, 3)), LaserContent.FRAME.get().defaultBlockState());
        host.tick();
        h.assertTrue(!host.formed(), "cuboid corner cannot replace required sphere exterior air");
        h.succeed();
    }

    private static void damageAtNextEvenTick(GameTestHelper h, LaserMachineBlockEntity source, Runnable assertions) {
        long tick = h.getLevel().getGameTime();
        if (tick % 2 != 0) {
            h.runAfterDelay(1, () -> damageAtNextEvenTick(h, source, assertions));
            return;
        }
        source.energy().load(100_000);
        // Network.tick emits itself at the batch boundary; emitting twice would claim a focus input twice.
        if (tick % 20 != 0) LaserNetwork.processEmitter(source);
        LaserNetwork.tick(source);
        assertions.run();
    }

    @GameTest(template="laser_empty") public static void receiverSphereFacesNorth(GameTestHelper h) { sphereForms(h, Direction.NORTH); }
    @GameTest(template="laser_empty") public static void receiverSphereFacesSouth(GameTestHelper h) { sphereForms(h, Direction.SOUTH); }
    @GameTest(template="laser_empty") public static void receiverSphereFacesEast(GameTestHelper h) { sphereForms(h, Direction.EAST); }
    @GameTest(template="laser_empty") public static void receiverSphereFacesWest(GameTestHelper h) { sphereForms(h, Direction.WEST); }
    @GameTest(template="laser_empty") public static void receiverSphereFacesUp(GameTestHelper h) { sphereForms(h, Direction.UP); }
    @GameTest(template="laser_empty") public static void receiverSphereFacesDown(GameTestHelper h) { sphereForms(h, Direction.DOWN); }

    @GameTest(template="laser_empty")
    public static void adjacentLaserGlassHidesOnlyItsInternalFaces(GameTestHelper h) {
        var glass = LaserContent.GLASS.get().defaultBlockState();
        h.assertTrue(glass.skipRendering(glass, Direction.EAST), "adjacent glass panels merge into a clear spherical surface");
        h.assertTrue(!glass.skipRendering(Blocks.AIR.defaultBlockState(), Direction.EAST), "outer transparent faces remain visible");
        var frame = LaserContent.FRAME.get().defaultBlockState();
        h.assertTrue(!frame.skipRendering(frame, Direction.EAST), "glass face suppression does not alter rib rendering");
        h.succeed();
    }

    @GameTest(template="laser_empty")
    public static void legacyCubeCannotFormNewReceiver(GameTestHelper h) {
        var host = receiver(h, h.absolutePos(new BlockPos(8, 2, 8)), Direction.WEST, false); host.checkStructure();
        h.assertTrue(!host.formed(), "former cubic glass cage is no longer a valid new receiver");
        h.succeed();
    }

    @GameTest(template="laser_empty")
    public static void legacyChargedReceiverPausesUntilRebuiltWithoutLosingPaidState(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(8, 2, 8));
        var host = receiver(h, pos, Direction.WEST, false);
        host.energy().load(1_024_000_000_000L); host.particles().load(7);
        host.fuelInput().insertItem(0, new ItemStack(Items.NETHER_STAR, 2), false);
        var old = host.saveMachine(h.getLevel().registryAccess());
        old.remove("ReceiverShapeVersion"); old.remove("ReceiverRebuildPending"); old.putBoolean("HadStructure", true);
        old.putInt("FuelRemaining", 72); old.putInt("ParticleRemaining", 12); old.putString("ActiveFuel", "overload_sim:nether_star");
        host.loadMachine(old, h.getLevel().registryAccess()); host.tick();
        h.assertTrue(!host.formed() && !host.explosionTriggered() && host.status().equals("receiver_rebuild_required"), "upgraded legacy cage parks safely and requests spherical rebuild");
        h.assertTrue(host.energy().stored() == 1_024_000_000_000L && host.particles().stored() == 7 && host.fuelInput().getStackInSlot(0).getCount() == 2 && host.fuelRemaining() == 72 && host.particleRemaining() == 12, "upgrade preserves resources, consumables and paid clocks");
        var parked = host.saveMachine(h.getLevel().registryAccess()); host.loadMachine(parked, h.getLevel().registryAccess()); host.tick();
        h.assertTrue(!host.explosionTriggered() && host.particles().stored() == 7, "saved pending rebuild remains safe after another reload");
        receiver(h, pos, Direction.WEST, true); host.checkStructure();
        h.assertTrue(host.formed() && host.particles().stored() == 7 && host.fuelRemaining() == 72, "spherical rebuild rearms containment without erasing saved resources");
        var current = host.saveMachine(h.getLevel().registryAccess()); host.loadMachine(current, h.getLevel().registryAccess()); host.checkStructure();
        java.util.concurrent.atomic.AtomicInteger explosions = new java.util.concurrent.atomic.AtomicInteger();
        java.util.function.Consumer<dev.overloadsim.api.LaserEvents.BeforeExplosion> listener = event -> {
            if (event.position.equals(pos)) { explosions.incrementAndGet(); event.setCanceled(true); }
        };
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(listener);
        try {
            h.getLevel().setBlockAndUpdate(pos.above(3).east(2).south(2).above(), Blocks.AIR.defaultBlockState());
            host.checkStructure();
            h.assertTrue(explosions.get() == 1, "a migrated sphere still applies charged containment-break safety");
        } finally { net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(listener); host.particles().load(0); }
        h.succeed();
    }

    @GameTest(template="laser_empty",timeoutTicks=20)
    public static void ordinaryBeamDamagesAcrossThreeBlockDiameter(GameTestHelper h) {
        var source = LaserMachineGameTests.build(h, h.absolutePos(new BlockPos(4,5,8)), LaserKind.EMITTER, Direction.WEST);
        source.checkStructure();
        var inside = h.spawn(EntityType.COW, new BlockPos(11,5,10)); inside.setNoAi(true); inside.setNoGravity(true);
        inside.setPos(inside.getX(), inside.getY(), source.center().getZ() + 1.7);
        var outside = h.spawn(EntityType.COW, new BlockPos(11,5,11)); outside.setNoAi(true); outside.setNoGravity(true);
        var corner = h.spawn(EntityType.COW, new BlockPos(11,7,10)); corner.setNoAi(true); corner.setNoGravity(true);
        corner.setPos(corner.getX(), source.center().getY() + 1.7, source.center().getZ() + 2.2);
        float before = inside.getHealth(), outsideBefore = outside.getHealth();
        float cornerBefore = corner.getHealth();
        damageAtNextEvenTick(h, source, () -> {
            h.assertTrue(source.formed() && !source.segments().isEmpty(), "ordinary test emits a valid paid beam");
            h.assertTrue(source.segments().stream().anyMatch(s -> s.intersects(inside.getBoundingBox())), "ordinary test entity intersects the real segment");
            h.assertTrue(inside.getHealth() < before, "a living entity inside the three-block-wide beam takes damage");
            h.assertTrue(outside.getHealth() == outsideBefore, "an entity beyond its circular radius remains safe");
            h.assertTrue(corner.getHealth() == cornerBefore, "square search bounds do not damage entities beyond the circular beam");
            inside.discard(); outside.discard(); corner.discard(); source.setEnabled(false); h.succeed();
        });
    }

    @GameTest(template="laser_empty",timeoutTicks=20)
    public static void focusedBeamDamageContractsTowardCore(GameTestHelper h) {
        var source = LaserMachineGameTests.build(h,h.absolutePos(new BlockPos(3,5,10)),LaserKind.EMITTER,Direction.WEST);
        var focus = LaserMachineGameTests.build(h,h.absolutePos(new BlockPos(9,2,10)),LaserKind.FOCUS,Direction.WEST);
        source.checkStructure(); focus.checkStructure();
        var near = h.spawn(EntityType.COW, new BlockPos(11,5,11)); near.setNoAi(true); near.setNoGravity(true);
        var far = h.spawn(EntityType.COW, new BlockPos(15,5,11)); far.setNoAi(true); far.setNoGravity(true);
        near.setPos(near.getX(), near.getY(), focus.center().getZ() + 1.7);
        far.setPos(far.getX(), far.getY(), focus.center().getZ() + 1.7);
        float nearBefore = near.getHealth(), farBefore = far.getHealth();
        var side = focus.center().east().south(); h.getLevel().setBlockAndUpdate(side, Blocks.STONE.defaultBlockState());
        damageAtNextEvenTick(h, source, () -> {
            h.assertTrue(source.segments().stream().anyMatch(s -> s.focused() && s.intersects(near.getBoundingBox())), "focus test entity intersects the real tapered segment");
            h.assertTrue(near.getHealth() < nearBefore && far.getHealth() == farBefore, "wide focus entrance harms while its narrow endpoint excludes the same radial offset");
            h.assertTrue(h.getLevel().getBlockState(side).is(Blocks.STONE), "wider visuals and entity damage do not broaden the existing block-destruction path");
            near.discard(); far.discard(); source.setEnabled(false); h.succeed();
        });
    }
}
