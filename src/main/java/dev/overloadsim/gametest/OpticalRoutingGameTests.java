package dev.overloadsim.gametest;

import dev.overloadsim.OverloadSimulation;
import dev.overloadsim.laser.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(OverloadSimulation.ID)
@PrefixGameTestTemplate(false)
public final class OpticalRoutingGameTests {
    private static int bit(Direction side) { return 1 << side.get3DDataValue(); }

    private static void configure(GameTestHelper h, LaserMachineBlockEntity deflector, int inputs, int outputs) {
        var tag = deflector.saveMachine(h.getLevel().registryAccess());
        tag.putInt("OpticalInputMask", inputs); tag.putInt("OpticalOutputMask", outputs);
        deflector.loadMachine(tag, h.getLevel().registryAccess()); deflector.checkStructure();
    }

    private static LaserMachineBlockEntity[] westInput(GameTestHelper h) {
        var source = LaserMachineGameTests.build(h, h.absolutePos(new BlockPos(3,5,12)), LaserKind.EMITTER, Direction.WEST);
        var deflector = LaserMachineGameTests.build(h, h.absolutePos(new BlockPos(13,2,12)), LaserKind.DEFLECTOR, Direction.SOUTH);
        source.checkStructure(); deflector.checkStructure();
        h.assertTrue(source.formed() && deflector.formed(), "independent separated emitter and deflector fixtures form");
        return new LaserMachineBlockEntity[]{source, deflector};
    }

    private static boolean segment(LaserMachineBlockEntity source, BlockPos from, BlockPos to, long energy) {
        return source.segments().stream().anyMatch(s -> s.start().equals(from.getCenter()) && s.end().equals(to.getCenter()) && s.energy() == energy);
    }

    @GameTest(template="laser_empty")
    public static void newDeflectorHasFourHorizontalPortsAndStartsOff(GameTestHelper h) {
        var host = LaserMachineGameTests.build(h, h.absolutePos(new BlockPos(13,2,12)), LaserKind.DEFLECTOR, Direction.NORTH);
        host.checkStructure();
        h.assertTrue(host.formed() && host.outputMask() == 0, "fresh deflector forms with every side disabled");
        h.assertTrue(h.getLevel().getBlockState(host.center().above(3)).is(LaserContent.FRAME.get()), "upper housing is ordinary frame");
        h.assertTrue(h.getLevel().getBlockState(host.center().below(3)).is(LaserContent.machine(LaserKind.DEFLECTOR)), "lower housing is the controller");
        for (var side : Direction.Plane.HORIZONTAL) h.assertTrue(h.getLevel().getBlockState(host.center().relative(side,3)).is(LaserContent.PORT.get()), "four horizontal apertures remain physical ports");
        h.succeed();
    }

    @GameTest(template="laser_empty")
    public static void configuredSideInputTurnsThroughCrystalAndConservesSplit(GameTestHelper h) {
        var pair = westInput(h); var source = pair[0]; var deflector = pair[1];
        configure(h, deflector, bit(Direction.WEST), bit(Direction.NORTH) | bit(Direction.SOUTH));
        source.energy().load(100_000); LaserNetwork.processEmitter(source);
        var center = deflector.center();
        h.assertTrue(deflector.lastPower() == 100_000, "a configured west input accepts eastward travel independently of controller facing");
        h.assertTrue(segment(source,center.west(3),center,100_000), "incoming full energy reaches the crystal along a straight internal axis");
        h.assertTrue(segment(source,center,center.north(3),50_000) && segment(source,center,center.south(3),50_000), "each output begins at the crystal with a conserved half share");
        h.assertTrue(segment(source,center.north(3),center.north(9),50_000) && segment(source,center.south(3),center.south(9),50_000), "both outgoing beams reach six blocks beyond their aperture");
        h.assertTrue(source.segments().stream().allMatch(s -> s.start().x == s.end().x || s.start().z == s.end().z), "redirected segments never cut diagonally through opaque housing");
        Runnable validate = () -> {
            LaserNetwork.tick(source);
            h.assertTrue(!source.segments().isEmpty() && deflector.currentPower() == 100_000, "solid owned ports do not invalidate the internal crystal path");
            deflector.setSideMode(Direction.WEST,LaserMachineBlockEntity.SIDE_OFF);
            h.assertTrue(source.segments().isEmpty() && deflector.currentPower() == 0, "disabling an active input clears its existing upstream route immediately");
            source.setEnabled(false); h.succeed();
        };
        if (h.getLevel().getGameTime() % 20 == 0) h.runAfterDelay(1,validate); else validate.run();
    }

    @GameTest(template="laser_empty")
    public static void offOrOutputSideRejectsIncomingBeam(GameTestHelper h) {
        var pair = westInput(h); var source = pair[0]; var deflector = pair[1];
        configure(h, deflector, bit(Direction.SOUTH), bit(Direction.WEST) | bit(Direction.NORTH));
        source.energy().load(100_000); LaserNetwork.processEmitter(source);
        h.assertTrue(deflector.lastPower() == 0 && source.segments().stream().noneMatch(s -> s.start().equals(deflector.center().getCenter())), "an output face cannot receive an incoming beam");
        configure(h, deflector, bit(Direction.SOUTH), bit(Direction.NORTH));
        source.energy().load(100_000); LaserNetwork.processEmitter(source);
        h.assertTrue(deflector.lastPower() == 0 && source.segments().stream().noneMatch(s -> s.start().equals(deflector.center().getCenter())), "an off face cannot receive an incoming beam");
        source.setEnabled(false); h.succeed();
    }

    @GameTest(template="laser_empty")
    public static void legacyPortsAndResourceStateSurviveMigration(GameTestHelper h) {
        var host = LaserMachineGameTests.build(h, h.absolutePos(new BlockPos(13,2,12)), LaserKind.DEFLECTOR, Direction.SOUTH);
        h.getLevel().setBlockAndUpdate(host.center().above(3), LaserContent.PORT.get().defaultBlockState());
        var old = host.saveMachine(h.getLevel().registryAccess());
        old.remove("OpticalInputMask"); old.remove("OpticalOutputMask"); old.remove("DeflectorShapeVersion"); old.remove("DeflectorLegacyLayout");
        old.putInt("OutputMask",63); old.putBoolean("HadStructure",true); old.putLong("Energy",123456); old.putLong("ExtremeLightning",456);
        host.loadMachine(old,h.getLevel().registryAccess()); host.checkStructure();
        h.assertTrue(host.formed() && h.getLevel().getBlockState(host.center().above(3)).is(LaserContent.PORT.get()), "legacy upper port remains in the world and the valid saved structure remains usable");
        h.assertTrue(host.outputMask() == (bit(Direction.WEST) | bit(Direction.EAST) | bit(Direction.NORTH)), "legacy outputs lose vertical routing and exclude the legacy south input");
        h.assertTrue(host.energy().stored() == 123456 && host.extremeLightning() == 456, "shape migration never debits stored resources");
        var migrated = host.saveMachine(h.getLevel().registryAccess());
        h.assertTrue(migrated.getInt("OpticalInputMask") == bit(Direction.SOUTH) && migrated.getInt("DeflectorShapeVersion") == 1, "legacy facing input becomes an explicit independently persisted side mode");
        host.loadMachine(migrated,h.getLevel().registryAccess()); host.checkStructure();
        h.assertTrue(host.formed() && host.outputMask() == 52 && host.energy().stored() == 123456, "migrated layout and explicit modes survive another save and load");
        h.succeed();
    }

    @GameTest(template="laser_empty")
    public static void multipleConfiguredInputsCannotDuplicateOneBatch(GameTestHelper h) {
        var pair = westInput(h); var source = pair[0]; var deflector = pair[1];
        var other = LaserMachineGameTests.build(h, h.absolutePos(new BlockPos(13,5,22)), LaserKind.EMITTER, Direction.SOUTH);
        other.checkStructure();
        configure(h, deflector, bit(Direction.WEST) | bit(Direction.SOUTH), bit(Direction.NORTH));
        source.energy().load(100_000); other.energy().load(100_000);
        LaserNetwork.processEmitter(source); LaserNetwork.processEmitter(other);
        h.assertTrue(segment(source,deflector.center(),deflector.center().north(3),100_000), "first input owns the batch");
        h.assertTrue(other.segments().stream().noneMatch(s -> s.start().equals(deflector.center().getCenter())), "second enabled input cannot multiply the first batch's output");
        source.setEnabled(false); other.setEnabled(false); h.succeed();
    }

    @GameTest(template="laser_empty")
    public static void housingObstructionStopsOutgoingBeam(GameTestHelper h) {
        var pair = westInput(h); var source = pair[0]; var deflector = pair[1];
        configure(h, deflector, bit(Direction.WEST), bit(Direction.NORTH));
        var obstacle = deflector.center().north(5); h.getLevel().setBlockAndUpdate(obstacle,Blocks.STONE.defaultBlockState());
        source.energy().load(100_000); LaserNetwork.processEmitter(source);
        h.assertTrue(segment(source,deflector.center().north(3),obstacle,100_000), "outgoing beam stops at its first opaque world obstacle");
        h.assertTrue(source.segments().stream().noneMatch(s -> s.end().equals(deflector.center().north(9).getCenter())), "the aperture exception cannot let light pass through unrelated housing");
        source.setEnabled(false); h.succeed();
    }

    @GameTest(template="laser_empty")
    public static void sideModesPersistAndRejectVerticalOrInvalidConfiguration(GameTestHelper h) {
        var host = LaserMachineGameTests.build(h,h.absolutePos(new BlockPos(13,2,12)),LaserKind.DEFLECTOR,Direction.EAST);
        host.setSideMode(Direction.NORTH,LaserMachineBlockEntity.SIDE_INPUT);
        host.setSideMode(Direction.EAST,LaserMachineBlockEntity.SIDE_INPUT);
        host.setSideMode(Direction.WEST,LaserMachineBlockEntity.SIDE_OUTPUT);
        host.setSideMode(Direction.SOUTH,LaserMachineBlockEntity.SIDE_OUTPUT);
        host.setSideMode(Direction.UP,LaserMachineBlockEntity.SIDE_INPUT);
        host.setSideMode(Direction.DOWN,LaserMachineBlockEntity.SIDE_OUTPUT);
        host.setSideMode(Direction.NORTH,3);
        var tag=host.saveMachine(h.getLevel().registryAccess()); host.loadMachine(tag,h.getLevel().registryAccess()); host.checkStructure();
        h.assertTrue(host.inputMask()==36 && host.outputMask()==24 && host.sideMode(Direction.UP)==0 && host.sideMode(Direction.DOWN)==0, "four independent horizontal modes round-trip while vertical and invalid requests stay inert");
        host.setSideMode(Direction.NORTH,LaserMachineBlockEntity.SIDE_OUTPUT);
        h.assertTrue(host.inputMask()==32 && host.outputMask()==28, "changing one side removes its former mode without changing the other three");
        tag.putInt("OpticalInputMask",-1); tag.putInt("OpticalOutputMask",-1); host.loadMachine(tag,h.getLevel().registryAccess());
        h.assertTrue(host.inputMask()==60 && host.outputMask()==0, "malformed overlapping persisted masks cannot create a simultaneous input/output or vertical endpoint");
        h.succeed();
    }

    @GameTest(template="laser_empty")
    public static void legacyVerticalDeflectorRetainsAnchorWithoutOpticalController(GameTestHelper h) {
        var controller=h.absolutePos(new BlockPos(13,5,15)); var center=controller.north(3);
        // Independent historical fixture: the old UP-facing controller sits on the south pole.
        for(int x=-3;x<=3;x++)for(int y=-3;y<=3;y++)for(int z=-3;z<=3;z++){
            int distance=x*x+y*y+z*z;
            var block=distance>=7&&distance<=12?LaserContent.FRAME.get():Blocks.AIR;
            if(Math.abs(x)+Math.abs(y)+Math.abs(z)==3&&(Math.abs(x)==3||Math.abs(y)==3||Math.abs(z)==3))block=LaserContent.PORT.get();
            var pos=center.offset(x,y,z); var state=block.defaultBlockState();
            if(pos.equals(controller))state=LaserContent.machine(LaserKind.DEFLECTOR).defaultBlockState().setValue(LaserMachineBlock.FACING,Direction.UP);
            h.getLevel().setBlockAndUpdate(pos,state);
        }
        var host=(LaserMachineBlockEntity)h.getLevel().getBlockEntity(controller);
        var old=host.saveMachine(h.getLevel().registryAccess()); old.remove("OpticalInputMask"); old.remove("OpticalOutputMask"); old.remove("DeflectorShapeVersion"); old.remove("DeflectorLegacyLayout");
        old.putInt("OutputMask",63); old.putBoolean("HadStructure",true); old.putLong("Energy",9876);
        host.loadMachine(old,h.getLevel().registryAccess()); host.checkStructure();
        h.assertTrue(host.formed()&&host.center().equals(center)&&host.inputMask()==0&&host.outputMask()==60, "old vertical orientation keeps its physical center and converts no vertical input");
        host.setSideMode(Direction.WEST,LaserMachineBlockEntity.SIDE_INPUT); host.setSideMode(Direction.SOUTH,LaserMachineBlockEntity.SIDE_OUTPUT);
        host.setSideMode(Direction.EAST,LaserMachineBlockEntity.SIDE_OFF); host.setSideMode(Direction.NORTH,LaserMachineBlockEntity.SIDE_OFF);
        var source=LaserMachineGameTests.build(h,h.absolutePos(new BlockPos(3,5,12)),LaserKind.EMITTER,Direction.WEST); source.checkStructure();
        source.energy().load(100_000); LaserNetwork.processEmitter(source);
        h.assertTrue(segment(source,center.west(3),center,100_000)&&source.segments().stream().noneMatch(s->s.start().equals(center.getCenter())&&s.end().equals(controller.getCenter())), "the legacy controller stays control-only even when its horizontal side was saved as output");
        h.assertTrue(host.energy().stored()==9876&&h.getLevel().getBlockState(center.above(3)).is(LaserContent.PORT.get()), "legacy vertical members and stored resources stay untouched");
        source.setEnabled(false); h.succeed();
    }

    @GameTest(template="laser_empty")
    public static void redirectedLoopStopsAtFirstRepeatedController(GameTestHelper h) {
        var pair=westInput(h); var source=pair[0]; var a=pair[1];
        var b=LaserMachineGameTests.build(h,h.absolutePos(new BlockPos(23,2,12)),LaserKind.DEFLECTOR,Direction.SOUTH);
        var c=LaserMachineGameTests.build(h,h.absolutePos(new BlockPos(23,2,22)),LaserKind.DEFLECTOR,Direction.SOUTH);
        var d=LaserMachineGameTests.build(h,h.absolutePos(new BlockPos(13,2,22)),LaserKind.DEFLECTOR,Direction.SOUTH);
        configure(h,a,bit(Direction.WEST)|bit(Direction.SOUTH),bit(Direction.EAST));
        configure(h,b,bit(Direction.WEST),bit(Direction.SOUTH)); configure(h,c,bit(Direction.NORTH),bit(Direction.WEST)); configure(h,d,bit(Direction.EAST),bit(Direction.NORTH));
        source.energy().load(100_000); LaserNetwork.processEmitter(source);
        h.assertTrue(segment(source,b.center(),b.center().south(3),100_000)&&segment(source,c.center(),c.center().west(3),100_000)&&segment(source,d.center(),d.center().north(3),100_000), "beam traverses each physical leg of the closed horizontal loop");
        h.assertTrue(source.segments().stream().filter(s->s.start().equals(a.center().getCenter())).count()==1&&source.segments().size()<20, "a revisited controller cannot create another output or unbounded segments");
        source.setEnabled(false); h.succeed();
    }

    @GameTest(template="laser_empty")
    public static void newDeflectorPlacementKeepsHorizontalControlFacing(GameTestHelper h) {
        var player=h.makeMockPlayer(net.minecraft.world.level.GameType.CREATIVE);
        var clicked=h.absolutePos(new BlockPos(13,2,12)); h.getLevel().setBlockAndUpdate(clicked,Blocks.STONE.defaultBlockState());
        player.setPos(clicked.getCenter()); player.setYRot(0); player.setXRot(-90);
        var hit=new net.minecraft.world.phys.BlockHitResult(clicked.getCenter(),Direction.UP,clicked,false);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(LaserContent.machine(LaserKind.DEFLECTOR)));
        var context=new net.minecraft.world.item.context.BlockPlaceContext(new net.minecraft.world.item.context.UseOnContext(player,net.minecraft.world.InteractionHand.MAIN_HAND,hit));
        var deflector=LaserContent.machine(LaserKind.DEFLECTOR).getStateForPlacement(context);
        h.assertTrue(deflector.getValue(LaserMachineBlock.FACING)==Direction.NORTH, "looking vertically still places a deflector with horizontal control facing");
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(LaserContent.machine(LaserKind.EMITTER)));
        var emitter=LaserContent.machine(LaserKind.EMITTER).getStateForPlacement(context);
        h.assertTrue(emitter.getValue(LaserMachineBlock.FACING)==Direction.DOWN, "emitter retains its existing six-direction placement");
        h.succeed();
    }
}
