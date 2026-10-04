package dev.overloadsim.gametest;

import dev.overloadsim.OverloadSimulation;
import dev.overloadsim.laser.LaserSegment;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(OverloadSimulation.ID)
@PrefixGameTestTemplate(false)
public final class LaserSegmentGameTests {
    private static LaserSegment beam(Vec3 direction, boolean focus) {
        return new LaserSegment(Vec3.ZERO, direction.scale(focus ? 7 : 6), 100_000, focus, false);
    }
    private static AABB point(Vec3 point) { return new AABB(point, point); }
    private static final Vec3[] DIRECTIONS = {
            new Vec3(1,0,0), new Vec3(-1,0,0), new Vec3(0,1,0),
            new Vec3(0,-1,0), new Vec3(0,0,1), new Vec3(0,0,-1)
    };

    @GameTest(template="laser_empty")
    public static void ordinaryBeamHitsAcrossItsCircularDiameterInEveryDirection(GameTestHelper h) {
        for (var direction : DIRECTIONS) {
            var side = direction.y != 0 ? new Vec3(1,0,0) : new Vec3(0,1,0);
            var segment = beam(direction, false);
            h.assertTrue(segment.intersects(point(direction.scale(3).add(side.scale(1.5)))), "three-block diameter " + direction);
            h.assertTrue(!segment.intersects(point(direction.scale(3).add(side.scale(1.501)))), "outside radius " + direction);
            h.assertTrue(!segment.intersects(point(direction.scale(6.001))), "flat endpoint caps stay bounded");
        }
        h.succeed();
    }

    @GameTest(template="laser_empty")
    public static void circularBeamExcludesSquareBoundingBoxCorners(GameTestHelper h) {
        var segment = beam(new Vec3(1,0,0), false);
        h.assertTrue(!segment.intersects(new AABB(2,1.1,1.1,3,1.4,1.4)), "square corner outside circular radius is excluded");
        h.assertTrue(segment.intersects(new AABB(2,1,1,3,1.4,1.4)), "closer corner inside circular radius is included");
        h.succeed();
    }

    @GameTest(template="laser_empty")
    public static void focusContractsOverSevenBlocksInEveryDirection(GameTestHelper h) {
        for (var direction : DIRECTIONS) {
            var side = direction.y != 0 ? new Vec3(1,0,0) : new Vec3(0,1,0);
            var segment = beam(direction, true);
            h.assertTrue(segment.intersects(point(direction.add(side.scale(1.2)))), "wide entrance " + direction);
            h.assertTrue(!segment.intersects(point(direction.scale(6).add(side.scale(1.2)))), "narrow endpoint " + direction);
            h.assertTrue(segment.intersects(point(direction.scale(7).add(side.scale(.017)))), "core center " + direction);
            h.assertTrue(!segment.intersects(point(direction.scale(7).add(side.scale(.02)))), "core periphery " + direction);
        }
        h.succeed();
    }

    @GameTest(template="laser_empty")
    public static void focusTestsTheBoxAtItsNearestAxialSlice(GameTestHelper h) {
        var segment = beam(new Vec3(1,0,0), true);
        h.assertTrue(segment.intersects(new AABB(1,1.2,0,2,1.4,.1)), "nearest overlapping axial slice remains wide enough");
        h.assertTrue(!segment.intersects(new AABB(2,1.2,0,3,1.4,.1)), "later overlapping axial slices contract beyond the box");
        h.succeed();
    }

    @GameTest(template="laser_empty")
    public static void blockedFocusKeepsItsPartialTaperInsteadOfCollapsingAtTheObstruction(GameTestHelper h) {
        var segment = new LaserSegment(Vec3.ZERO, new Vec3(2,0,0), 100_000, true, false);
        h.assertTrue(segment.intersects(point(new Vec3(2,.9,0))), "truncated focus keeps width at the blocked slice");
        h.assertTrue(Math.abs(segment.endRadius() - 1.0765714285714285) < 1e-12, "truncated visual radius follows full seven-block taper");
        h.succeed();
    }

    @GameTest(template="laser_empty")
    public static void sharedRenderBoundsContainTheWideEntranceAndOrdinaryBeam(GameTestHelper h) {
        for (boolean focused : new boolean[]{false, true}) {
            var segment = beam(new Vec3(1,0,0), focused);
            h.assertTrue(segment.bounds().contains(new Vec3(.01,1.49,0)), "render bounds include full beam entrance height");
            h.assertTrue(segment.bounds().contains(new Vec3(.01,0,1.49)), "render bounds include full beam entrance width");
        }
        h.succeed();
    }

    @GameTest(template="laser_empty")
    public static void particleSegmentsRemainHarmlessEvenWhenTheirBoundsCrossAnEntity(GameTestHelper h) {
        var segment = new LaserSegment(Vec3.ZERO, new Vec3(3,0,0), 100_000, false, true);
        h.assertTrue(!segment.intersects(new AABB(-1,-1,-1,4,1,1)), "cosmetic particle segments have no damaging volume");
        h.succeed();
    }
}
