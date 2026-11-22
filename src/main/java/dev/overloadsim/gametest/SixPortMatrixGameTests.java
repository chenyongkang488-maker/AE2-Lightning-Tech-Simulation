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
public final class SixPortMatrixGameTests {
    private static void remove(GameTestHelper h,LaserMachineBlockEntity host) {
        host.setEnabled(false);
        for(var pos:host.structureTemplate().keySet())h.getLevel().setBlockAndUpdate(host.getBlockPos().offset(pos),Blocks.AIR.defaultBlockState());
    }
    private static LaserMachineBlockEntity build(GameTestHelper h, BlockPos controller, LaserKind kind, Direction facing) {
        h.getLevel().setBlockAndUpdate(controller, LaserContent.machine(kind).defaultBlockState().setValue(LaserMachineBlock.FACING, facing));
        var host=(LaserMachineBlockEntity)h.getLevel().getBlockEntity(controller);
        for(var entry:host.structureTemplate().entrySet()) {
            if(entry.getValue()==LaserStructureGeometry.Part.CONTROLLER)continue;
            var block=switch(entry.getValue()) {
                case FRAME->LaserContent.FRAME.get(); case PORT->LaserContent.PORT.get();
                case GLASS->LaserContent.GLASS.get(); case SOURCE->LaserContent.SOURCE.get(); default->Blocks.AIR;
            };
            h.getLevel().setBlockAndUpdate(controller.offset(entry.getKey()),block.defaultBlockState());
        }
        host.checkStructure(); return host;
    }

    @GameTest(template="laser_empty")
    public static void newDeflectorHasSixRealPortsAndCornerController(GameTestHelper h) {
        var host=build(h,h.absolutePos(new BlockPos(15,5,15)),LaserKind.DEFLECTOR,Direction.NORTH);
        h.assertTrue(host.formed(),"new deflector forms");
        h.assertTrue(host.getBlockPos().subtract(host.center()).equals(new BlockPos(2,-2,2)),"controller sits on diagonal shell cell, away from all beams");
        h.assertTrue(host.inputMask()==0&&host.outputMask()==0,"new six-port machine starts entirely closed");
        for(var side:Direction.values())h.assertTrue(h.getLevel().getBlockState(host.center().relative(side,3)).is(LaserContent.PORT.get()),"real optical port on "+side);
        h.assertTrue(host.saveMachine(h.getLevel().registryAccess()).getInt("DeflectorShapeVersion")==2,"new layout saves as shape2");
        h.succeed();
    }

    @GameTest(template="laser_empty")
    public static void verticalModesRoundTripAndNeverOverlap(GameTestHelper h) {
        var host=build(h,h.absolutePos(new BlockPos(15,5,15)),LaserKind.DEFLECTOR,Direction.NORTH);
        host.setSideMode(Direction.DOWN,LaserMachineBlockEntity.SIDE_INPUT);
        host.setSideMode(Direction.UP,LaserMachineBlockEntity.SIDE_OUTPUT);
        host.setSideMode(Direction.EAST,LaserMachineBlockEntity.SIDE_OUTPUT);
        var tag=host.saveMachine(h.getLevel().registryAccess());
        host.loadMachine(tag,h.getLevel().registryAccess());host.checkStructure();
        h.assertTrue(host.formed()&&host.inputMask()==1&&host.outputMask()==34,"vertical modes and world orientation survive reload");
        tag.putInt("OpticalInputMask",-1);tag.putInt("OpticalOutputMask",-1);
        host.loadMachine(tag,h.getLevel().registryAccess());
        h.assertTrue(host.inputMask()==63&&host.outputMask()==0,"overlapping persisted six-bit masks cannot duplicate input as output");
        h.succeed();
    }

    @GameTest(template="laser_empty")
    public static void horizontalInputSplitsUpAndDownWithoutDuplicateFees(GameTestHelper h) {
        var host=build(h,h.absolutePos(new BlockPos(17,5,15)),LaserKind.DEFLECTOR,Direction.NORTH);
        var source=build(h,host.center().west(10),LaserKind.EMITTER,Direction.WEST);
        host.setSideMode(Direction.WEST,LaserMachineBlockEntity.SIDE_INPUT);
        host.setSideMode(Direction.UP,LaserMachineBlockEntity.SIDE_OUTPUT);
        host.setSideMode(Direction.DOWN,LaserMachineBlockEntity.SIDE_OUTPUT);
        source.energy().load(200_000);LaserNetwork.processEmitter(source);LaserNetwork.processEmitter(source);
        h.assertTrue(source.energy().stored()==100_000&&host.currentPower()==100_000,"one source pays once per batch");
        for(var side:new Direction[]{Direction.UP,Direction.DOWN}) {
            var end=host.center().relative(side,3).getCenter();
            h.assertTrue(source.segments().stream().anyMatch(s->s.start().equals(host.center().getCenter())&&s.end().equals(end)&&s.energy()==50_000),"conserved internal Y output on "+side);
        }
        source.setEnabled(false);
        h.assertTrue(host.currentPower()==0&&source.segments().isEmpty(),"stop clears both vertical outputs immediately");
        h.succeed();
    }

    @GameTest(template="laser_empty")
    public static void verticalAndHorizontalInputsMergeAndRetainRemainingSource(GameTestHelper h) {
        var host=build(h,h.absolutePos(new BlockPos(17,5,15)),LaserKind.DEFLECTOR,Direction.NORTH);
        var west=build(h,host.center().west(10),LaserKind.EMITTER,Direction.WEST);
        var upper=build(h,host.center().above(10),LaserKind.EMITTER,Direction.UP);
        host.setSideMode(Direction.WEST,LaserMachineBlockEntity.SIDE_INPUT);
        host.setSideMode(Direction.UP,LaserMachineBlockEntity.SIDE_INPUT);
        host.setSideMode(Direction.DOWN,LaserMachineBlockEntity.SIDE_OUTPUT);
        host.setSideMode(Direction.EAST,LaserMachineBlockEntity.SIDE_OUTPUT);
        west.energy().load(200_000);upper.energy().load(200_000);
        LaserNetwork.processEmitter(west);LaserNetwork.processEmitter(upper);
        h.assertTrue(host.currentPower()==200_000&&west.energy().stored()==100_000&&upper.energy().stored()==100_000,"both real axes merge and each source pays once");
        h.assertTrue(java.util.stream.Stream.concat(west.segments().stream(),upper.segments().stream())
                .filter(s->s.start().equals(host.center().getCenter())&&s.end().equals(host.center().below(3).getCenter())&&s.energy()==100_000).count()==1,"merged down output is published exactly once");
        west.setEnabled(false);
        h.assertTrue(host.currentPower()==100_000,"remaining vertical source retains its share");
        host.setSideMode(Direction.UP,LaserMachineBlockEntity.SIDE_OFF);
        h.assertTrue(host.currentPower()==0&&upper.segments().isEmpty(),"closing vertical input clears dependent beam");
        upper.setEnabled(false);h.succeed();
    }

    @GameTest(template="laser_empty")
    public static void missingVerticalPortAndObstructionStopLight(GameTestHelper h) {
        var host=build(h,h.absolutePos(new BlockPos(17,5,15)),LaserKind.DEFLECTOR,Direction.NORTH);
        var source=build(h,host.center().west(10),LaserKind.EMITTER,Direction.WEST);
        host.setSideMode(Direction.WEST,LaserMachineBlockEntity.SIDE_INPUT);host.setSideMode(Direction.UP,LaserMachineBlockEntity.SIDE_OUTPUT);
        var obstacle=host.center().above(5);h.getLevel().setBlockAndUpdate(obstacle,Blocks.STONE.defaultBlockState());
        source.energy().load(200_000);LaserNetwork.processEmitter(source);
        h.assertTrue(source.segments().stream().anyMatch(s->s.start().equals(host.center().above(3).getCenter())&&s.end().equals(obstacle.getCenter())),"vertical output stops at real opaque obstacle");
        h.getLevel().setBlockAndUpdate(host.center().below(3),LaserContent.FRAME.get().defaultBlockState());host.checkStructure();LaserNetwork.tick(source);
        h.assertTrue(!host.formed()&&source.segments().isEmpty(),"every optical port is a required real member, removal invalidates active route");
        source.setEnabled(false);h.succeed();
    }

    @GameTest(template="laser_empty")
    public static void shape1KeepsAnchorResourcesAndClosedVerticalModes(GameTestHelper h) {
        var host=LaserMachineGameTests.build(h,h.absolutePos(new BlockPos(15,3,15)),LaserKind.DEFLECTOR,Direction.NORTH);
        host.energy().load(123_456);host.setSideMode(Direction.WEST,LaserMachineBlockEntity.SIDE_INPUT);host.setSideMode(Direction.EAST,LaserMachineBlockEntity.SIDE_OUTPUT);
        var center=host.center();
        for(int i=0;i<2;i++) {
            var saved=host.saveMachine(h.getLevel().registryAccess());saved.putInt("OpticalInputMask",saved.getInt("OpticalInputMask")|1);saved.putInt("OpticalOutputMask",saved.getInt("OpticalOutputMask")|2);
            host.loadMachine(saved,h.getLevel().registryAccess());host.checkStructure();
            h.assertTrue(host.formed()&&host.center().equals(center)&&host.energy().stored()==123_456,"saved shape1 retains structure, resources and center");
            h.assertTrue(host.inputMask()==16&&host.outputMask()==32,"shape1 discards injected vertical settings but preserves horizontal ones");
            host.setSideMode(Direction.DOWN,LaserMachineBlockEntity.SIDE_INPUT);host.setSideMode(Direction.UP,LaserMachineBlockEntity.SIDE_OUTPUT);
            h.assertTrue(host.sideMode(Direction.UP)==0&&host.sideMode(Direction.DOWN)==0,"legacy settings cannot enable sealed ends");
        }
        h.succeed();
    }

    @GameTest(template="laser_empty")
    public static void oldDismantleSettingsDoNotCarryLayoutOrPortModes(GameTestHelper h) {
        var old=LaserMachineGameTests.build(h,h.absolutePos(new BlockPos(15,3,15)),LaserKind.DEFLECTOR,Direction.NORTH);
        old.setSideMode(Direction.NORTH,LaserMachineBlockEntity.SIDE_INPUT);old.energy().load(123_456);
        var components=net.minecraft.core.component.DataComponentMap.builder();old.exportSettings(appeng.util.SettingsFrom.DISMANTLE_ITEM,components,null);
        var settings=components.build();var center=old.center();remove(h,old);
        var pos=center.offset(2,-2,2);
        var fresh=build(h,pos,LaserKind.DEFLECTOR,Direction.SOUTH);fresh.importSettings(appeng.util.SettingsFrom.DISMANTLE_ITEM,settings,null);fresh.checkStructure();
        h.assertTrue(fresh.formed()&&fresh.center().equals(center)&&fresh.saveMachine(h.getLevel().registryAccess()).getInt("DeflectorShapeVersion")==2,"dismantle settings allow new corner placement without moving optical center");
        h.assertTrue(fresh.inputMask()==0&&fresh.outputMask()==0&&fresh.energy().stored()==0,"existing dismantling rules reset local FE and side modes");
        h.succeed();
    }

    @GameTest(template="laser_empty")
    public static void constructionPlanAndProjectionUseSixPortsAndRejectStaleLayout(GameTestHelper h) {
        var pos=h.absolutePos(new BlockPos(17,5,15));
        h.getLevel().setBlockAndUpdate(pos,LaserContent.machine(LaserKind.DEFLECTOR).defaultBlockState());
        var host=(LaserMachineBlockEntity)h.getLevel().getBlockEntity(pos);
        var plan=LaserAutoConstruction.plan(host);
        h.assertTrue(plan.available().getAsBoolean(),"new shape2 plan available");
        for(var side:Direction.values())h.assertTrue(plan.cells().get(host.center().relative(side,3)).is(LaserContent.PORT.get()),"construction includes all six endpoints");
        var projected=dev.overloadsim.construction.ConstructionProjectionGeometry.laserCells(pos,host.structureTemplate());
        h.assertTrue(projected.values().stream().filter(p->p==LaserStructureGeometry.Part.PORT).count()==6&&!projected.containsKey(pos),"projection includes six ports and omits real corner controller");
        var saved=host.saveMachine(h.getLevel().registryAccess());saved.putInt("DeflectorShapeVersion",1);host.loadMachine(saved,h.getLevel().registryAccess());
        h.assertTrue(!plan.available().getAsBoolean(),"captured construction plan refuses a changed shape version");
        h.succeed();
    }

    @GameTest(template="laser_empty",timeoutTicks=150)
    public static void allFourRingsBuildAndTransmitInEveryDirection(GameTestHelper h) {
        int delay=1;
        for(var kind:new LaserKind[]{LaserKind.CONSTRAINT,LaserKind.ENHANCEMENT,LaserKind.BURST,LaserKind.FOCUS})for(var facing:Direction.values()) {
            final int step=delay++;
            h.runAfterDelay(step,()->{
            var ring=build(h,h.absolutePos(new BlockPos(15,6,15)),kind,facing);
            var center=ring.center();var forward=facing.getOpposite();
            h.assertTrue(ring.formed(),kind+" forms facing "+facing);
            if(facing.getAxis()==Direction.Axis.Y)h.assertTrue(center.equals(ring.getBlockPos().north(3)),"flat ring uses north construction-up");
            var source=build(h,center.relative(forward,-6),LaserKind.EMITTER,facing);
            ring.energy().load(500_000);ring.setExtremeLightning(128);source.energy().load(200_000);
            LaserNetwork.processEmitter(source);
            long expected=kind==LaserKind.ENHANCEMENT||kind==LaserKind.BURST?200_000:100_000;
            h.assertTrue(ring.currentPower()==expected,kind+" transmits along "+forward+" with real charges: source="+source.formed()+"/"+source.status()+"/"+source.energy().stored()+", ring="+ring.formed()+"/"+ring.status()+"/"+ring.currentPower()+", lines="+source.segments());
            remove(h,source);remove(h,ring);
            if(step==24)h.succeed();
            });
        }
    }

    @GameTest(template="laser_empty",timeoutTicks=150)
    public static void allFourRingsAutoBuildAndInvalidateAfterRotation(GameTestHelper h) {
        var player=net.neoforged.neoforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"matrix-build"));
        player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
        for(var kind:new LaserKind[]{LaserKind.CONSTRAINT,LaserKind.ENHANCEMENT,LaserKind.BURST,LaserKind.FOCUS})for(var facing:Direction.values()) {
            var pos=h.absolutePos(new BlockPos(15,6,15));h.getLevel().setBlockAndUpdate(pos,LaserContent.machine(kind).defaultBlockState().setValue(LaserMachineBlock.FACING,facing));
            var ring=(LaserMachineBlockEntity)h.getLevel().getBlockEntity(pos);player.setPos(pos.getCenter());
            var projected=dev.overloadsim.construction.ConstructionProjectionGeometry.laserCells(pos,ring.structureTemplate());
            var plan=LaserAutoConstruction.plan(ring);
            h.assertTrue(projected.size()==15&&plan.available().getAsBoolean(),"rotated ring preview matches 15 required frame cells");
            var result=LaserAutoConstruction.build(player,ring);
            h.assertTrue(result.successful()&&ring.formed(),kind+" automatic construction faces "+facing);
            // A wrench changes the same block's FACING; a perpendicular axis must invalidate the old shell.
            var rotated=facing.getAxis()==Direction.Axis.Y?Direction.NORTH:Direction.UP;
            var oldCells=java.util.List.copyOf(ring.structureTemplate().keySet());
            h.getLevel().setBlockAndUpdate(pos,ring.getBlockState().setValue(LaserMachineBlock.FACING,rotated));ring.checkStructure();
            h.assertTrue(!ring.formed()&&!plan.available().getAsBoolean(),"rotation rejects stale shell and construction plan");
            for(var cell:oldCells)h.getLevel().setBlockAndUpdate(pos.offset(cell),Blocks.AIR.defaultBlockState());
        }
        h.succeed();
    }

    @GameTest(template="laser_empty")
    public static void sixPortClientSnapshotsRetainLayoutAndVerticalMasks(GameTestHelper h) throws ReflectiveOperationException {
        var host=build(h,h.absolutePos(new BlockPos(17,5,15)),LaserKind.DEFLECTOR,Direction.NORTH);
        host.setSideMode(Direction.DOWN,LaserMachineBlockEntity.SIDE_INPUT);host.setSideMode(Direction.UP,LaserMachineBlockEntity.SIDE_OUTPUT);
        var buffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),h.getLevel().registryAccess());
        var write=LaserMachineBlockEntity.class.getDeclaredMethod("writeToStream",net.minecraft.network.RegistryFriendlyByteBuf.class);write.setAccessible(true);
        var read=LaserMachineBlockEntity.class.getDeclaredMethod("readFromStream",net.minecraft.network.RegistryFriendlyByteBuf.class);read.setAccessible(true);
        try {
            for(int shape:new int[]{2,1}) {
                if(shape==1){var tag=host.saveMachine(h.getLevel().registryAccess());tag.putInt("DeflectorShapeVersion",1);host.loadMachine(tag,h.getLevel().registryAccess());}
                var copy=new LaserMachineBlockEntity(host.getBlockPos(),host.getBlockState());copy.setLevel(h.getLevel());buffer.clear();
                write.invoke(host,buffer);read.invoke(copy,buffer);
                h.assertTrue(buffer.readableBytes()==0&&copy.inputMask()==(shape==2?1:0)&&copy.outputMask()==(shape==2?2:0),"client consumes complete snapshot and preserves layout-specific masks");
                h.assertTrue(copy.structureTemplate().values().stream().filter(p->p==LaserStructureGeometry.Part.PORT).count()==(shape==2?6:4),"client template follows transmitted layout");
                h.assertTrue(copy.saveMachine(h.getLevel().registryAccess()).getInt("DeflectorShapeVersion")==shape,"client layout does not revert to default");
            }
        }finally{buffer.release();}
        h.succeed();
    }

    @GameTest(template="laser_empty")
    public static void verticalLoopCannotAmplifyOrLeaveBeamsAfterUnload(GameTestHelper h) {
        var center=h.absolutePos(new BlockPos(12,5,12));
        var a=build(h,center.offset(2,-2,2),LaserKind.DEFLECTOR,Direction.NORTH);
        var b=build(h,center.above(10).offset(2,-2,2),LaserKind.DEFLECTOR,Direction.NORTH);
        var c=build(h,center.above(10).east(10).offset(2,-2,2),LaserKind.DEFLECTOR,Direction.NORTH);
        var d=build(h,center.east(10).offset(2,-2,2),LaserKind.DEFLECTOR,Direction.NORTH);
        var source=build(h,center.west(10),LaserKind.EMITTER,Direction.WEST);
        a.setSideMode(Direction.WEST,LaserMachineBlockEntity.SIDE_INPUT);a.setSideMode(Direction.EAST,LaserMachineBlockEntity.SIDE_INPUT);a.setSideMode(Direction.UP,LaserMachineBlockEntity.SIDE_OUTPUT);
        b.setSideMode(Direction.DOWN,LaserMachineBlockEntity.SIDE_INPUT);b.setSideMode(Direction.EAST,LaserMachineBlockEntity.SIDE_OUTPUT);
        c.setSideMode(Direction.WEST,LaserMachineBlockEntity.SIDE_INPUT);c.setSideMode(Direction.DOWN,LaserMachineBlockEntity.SIDE_OUTPUT);
        d.setSideMode(Direction.UP,LaserMachineBlockEntity.SIDE_INPUT);d.setSideMode(Direction.WEST,LaserMachineBlockEntity.SIDE_OUTPUT);
        source.energy().load(200_000);LaserNetwork.processEmitter(source);
        h.assertTrue(java.util.List.of(a,b,c,d).stream().allMatch(m->m.currentPower()==100_000),"vertical closed loop visits each matrix with only original paid energy");
        h.assertTrue(source.segments().size()<20&&source.energy().stored()==100_000,"loop is finite and charges once");
        b.onChunkUnloaded();
        h.assertTrue(source.segments().isEmpty()&&java.util.List.of(a,b,c,d).stream().allMatch(m->m.currentPower()==0),"unload withdraws the whole dependent route without stale vertical beams");
        source.setEnabled(false);h.succeed();
    }

    @GameTest(template="laser_empty")
    public static void viewPlacementSelectsAllSixRingFacings(GameTestHelper h) {
        var player=h.makeMockPlayer(net.minecraft.world.level.GameType.CREATIVE);var clicked=h.absolutePos(new BlockPos(15,6,15));
        h.getLevel().setBlockAndUpdate(clicked,Blocks.STONE.defaultBlockState());player.setPos(clicked.getCenter());
        for(var kind:new LaserKind[]{LaserKind.CONSTRAINT,LaserKind.ENHANCEMENT,LaserKind.BURST,LaserKind.FOCUS})for(var look:Direction.values()) {
            player.setYRot(switch(look){case WEST->90;case NORTH->180;case EAST->270;default->0;});
            player.setYHeadRot(player.getYRot());
            player.setXRot(look==Direction.UP?-90:look==Direction.DOWN?90:0);
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(LaserContent.machine(kind)));
            var hit=new net.minecraft.world.phys.BlockHitResult(clicked.getCenter(),Direction.UP,clicked,false);
            var context=new net.minecraft.world.item.context.BlockPlaceContext(new net.minecraft.world.item.context.UseOnContext(player,net.minecraft.world.InteractionHand.MAIN_HAND,hit));
            h.assertTrue(LaserContent.machine(kind).getStateForPlacement(context).getValue(LaserMachineBlock.FACING)==look.getOpposite(),"view placement of "+kind+" faces opposite "+look);
        }
        h.succeed();
    }

    @GameTest(template="laser_empty")
    public static void minedLegacyControllerPlacesAsNewSixPortMachine(GameTestHelper h) {
        var old=LaserMachineGameTests.build(h,h.absolutePos(new BlockPos(15,3,15)),LaserKind.DEFLECTOR,Direction.NORTH);
        var center=old.center();var oldPos=old.getBlockPos();old.setSideMode(Direction.NORTH,LaserMachineBlockEntity.SIDE_INPUT);
        h.getLevel().destroyBlock(oldPos,true);
        var drops=h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(oldPos).inflate(2));
        var dropped=drops.stream().filter(e->e.getItem().is(LaserContent.machine(LaserKind.DEFLECTOR).asItem())).findFirst().orElseThrow();
        var item=dropped.getItem().copy();dropped.discard();remove(h,old);
        var pos=center.offset(2,-2,2);var clicked=pos.below();h.getLevel().setBlockAndUpdate(clicked,Blocks.STONE.defaultBlockState());
        var player=h.makeMockPlayer(net.minecraft.world.level.GameType.CREATIVE);player.setPos(pos.getCenter());player.setYRot(0);player.setYHeadRot(0);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,item);
        var hit=new net.minecraft.world.phys.BlockHitResult(clicked.getCenter().add(0,.5,0),Direction.UP,clicked,false);
        var placed=item.useOn(new net.minecraft.world.item.context.UseOnContext(player,net.minecraft.world.InteractionHand.MAIN_HAND,hit));
        h.assertTrue(placed.consumesAction()&&h.getLevel().getBlockEntity(pos) instanceof LaserMachineBlockEntity,"actual mined item places a controller");
        h.getLevel().setBlockAndUpdate(clicked,Blocks.AIR.defaultBlockState());
        var fresh=(LaserMachineBlockEntity)h.getLevel().getBlockEntity(pos);
        h.assertTrue(fresh.saveMachine(h.getLevel().registryAccess()).getInt("DeflectorShapeVersion")==2&&fresh.inputMask()==0&&fresh.outputMask()==0,"normal item placement initializes shape2 and clears legacy modes");
        h.succeed();
    }
}
