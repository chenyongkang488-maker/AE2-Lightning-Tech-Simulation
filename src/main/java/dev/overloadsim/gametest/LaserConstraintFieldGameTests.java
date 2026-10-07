package dev.overloadsim.gametest;

import dev.overloadsim.OverloadSimulation;
import dev.overloadsim.laser.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(OverloadSimulation.ID)
@PrefixGameTestTemplate(false)
public final class LaserConstraintFieldGameTests {
    private static LaserMachineBlockEntity matrix(GameTestHelper h) {
        var matrix = LaserMachineGameTests.build(h, h.absolutePos(new BlockPos(9,2,10)), LaserKind.CONSTRAINT, Direction.WEST);
        matrix.checkStructure();
        matrix.energy().load(1_000_000);
        return matrix;
    }

    private static Bat bat(GameTestHelper h, Vec3 position, boolean add) {
        var bat = EntityType.BAT.create(h.getLevel());
        bat.setNoAi(true); bat.setNoGravity(true); bat.setResting(false);
        bat.setPos(position);
        if (add) h.getLevel().addFreshEntity(bat);
        return bat;
    }

    private static MobSpawnEvent.PositionCheck check(GameTestHelper h, Vec3 position, MobSpawnType type) {
        var event = new MobSpawnEvent.PositionCheck(bat(h, position, false), h.getLevel(), type, null);
        NeoForge.EVENT_BUS.post(event);
        return event;
    }

    @GameTest(template="laser_empty",timeoutTicks=20)
    public static void activeConstraintDamagesInteriorBeyondTheBeamButNotOutside(GameTestHelper h) {
        var source = LaserMachineGameTests.build(h, h.absolutePos(new BlockPos(3,5,10)), LaserKind.EMITTER, Direction.WEST);
        var matrix = matrix(h);
        source.checkStructure(); source.energy().load(1_000_000);
        LaserNetwork.processEmitter(source);
        h.assertTrue(matrix.currentPower() > 0, "the real paid beam activates the constraint matrix");
        // Queued BE onLoad callbacks deliberately clear transient leases; pay after those callbacks settle.
        int primeDelay=(h.getLevel().getGameTime()+3)%20==0?4:3;
        h.runAfterDelay(primeDelay, () -> {
            LaserNetwork.processEmitter(source);
            h.assertTrue(matrix.currentPower()>0,"the loaded field has a fresh real paid lease");
            var center=matrix.center().getCenter();
            var inside=bat(h,center.add(0,-.2,2),true);
            // z=3.2 overlaps the solid ring at z=3..4; keep the control subject beyond that frame.
            var outside=bat(h,center.add(0,-.2,4.2),true);
            h.assertTrue(!ConstraintFields.intersects(matrix.center(),outside.getBoundingBox()),"the control subject is outside the field sphere");
            h.assertTrue(h.getLevel().noCollision(outside),"the control subject does not intersect the physical frame");
            h.runAfterDelay(3, () -> {
                try {
                    h.assertTrue(matrix.currentPower()>0&&source.currentPower()>0,"damage is checked while the paid field remains active");
                    h.assertTrue(!inside.isAlive(), "the active spherical interior damages living entities beyond the beam cylinder"
                            + " (health="+inside.getHealth()+", source="+source.currentPower()+"/"+source.status()+", segments="+source.segments().size()
                            + ", field="+matrix.currentPower()+"/"+matrix.status()+", inSphere="+ConstraintFields.intersects(matrix.center(),inside.getBoundingBox())+")");
                    h.assertTrue(outside.isAlive()&&outside.getHealth()==outside.getMaxHealth(),"outside the constraint sphere remains harmless"
                            + " (health="+outside.getHealth()+", damage="+(outside.getLastDamageSource()==null?"none":outside.getLastDamageSource().getMsgId())
                            + ", position="+outside.position()+")");
                }finally{source.setEnabled(false);inside.discard();outside.discard();}
                h.succeed();
            });
        });
    }

    @GameTest(template="laser_empty",timeoutTicks=20)
    public static void idleFormedConstraintDoesNotDamageInterior(GameTestHelper h) {
        var matrix = matrix(h);
        var inside = bat(h, matrix.center().getCenter().add(0,-.2,2), true);
        h.runAfterDelay(3, () -> {
            try { h.assertTrue(inside.isAlive() && inside.getHealth() == inside.getMaxHealth(), "an idle formed matrix does not damage existing living entities"); }
            finally { inside.discard(); }
            h.succeed();
        });
    }

    @GameTest(template="laser_empty")
    public static void formedIdleConstraintPreventsOnlyInteriorNaturalSpawning(GameTestHelper h) {
        var matrix = matrix(h);
        var center = matrix.center().getCenter();
        h.assertTrue(check(h, center.add(0,-.2,2), MobSpawnType.NATURAL).getResult() == MobSpawnEvent.PositionCheck.Result.FAIL,
                "forming the matrix prevents interior natural spawning even while idle");
        h.assertTrue(check(h, center.add(0,-.2,3.2), MobSpawnType.NATURAL).getResult() == MobSpawnEvent.PositionCheck.Result.DEFAULT,
                "natural spawning outside the sphere retains the normal spawn rules");
        h.assertTrue(check(h, center, MobSpawnType.COMMAND).getResult() == MobSpawnEvent.PositionCheck.Result.DEFAULT
                        && check(h, center, MobSpawnType.SPAWN_EGG).getResult() == MobSpawnEvent.PositionCheck.Result.DEFAULT,
                "commands and spawn eggs remain available inside a formed matrix");
        matrix.setEnabled(false);
        h.assertTrue(check(h, center, MobSpawnType.NATURAL).getResult() == MobSpawnEvent.PositionCheck.Result.FAIL,
                "formation, not the working switch, controls natural-spawn prevention");
        h.succeed();
    }
}
