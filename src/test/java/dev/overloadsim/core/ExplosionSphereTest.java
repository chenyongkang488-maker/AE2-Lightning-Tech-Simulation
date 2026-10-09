package dev.overloadsim.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ExplosionSphereTest {
    @Test void includesTheBoundaryAndExcludesJustBeyondIt() {
        assertTrue(ExplosionSphere.contains(12, 12, 0, 0));
        assertTrue(ExplosionSphere.contains(12, 0, -12, 0));
        assertTrue(ExplosionSphere.contains(12, 0, 0, 12));
        assertFalse(ExplosionSphere.contains(12, 12.0001, 0, 0));
        assertFalse(ExplosionSphere.contains(12, 12, .0001, 0));
    }

    @Test void excludesCubeCornersWhileKeepingPointsInsideTheSphere() {
        assertFalse(ExplosionSphere.contains(12, 9, 0, 9));
        assertFalse(ExplosionSphere.contains(12, -9, -9, -9));
        assertTrue(ExplosionSphere.contains(12, 6, 6, 6));
        // The 3-4-5 triangle checks a reduced plugin radius without relying on the helper's arithmetic.
        assertTrue(ExplosionSphere.contains(5, 3, 4, 0));
        assertFalse(ExplosionSphere.contains(5, 3, 4, 1));
    }

    @Test void zeroRadiusSelectsOnlyItsCenter() {
        assertTrue(ExplosionSphere.contains(0, 0, 0, 0));
        assertFalse(ExplosionSphere.contains(0, 0, 0, .0001));
    }

    @Test void rejectsRadiusExpansionAndNeverTreatsNonFiniteCoordinatesAsInside() {
        assertThrows(IllegalArgumentException.class, () -> ExplosionSphere.contains(-1, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> ExplosionSphere.contains(65, 0, 0, 0));
        assertFalse(ExplosionSphere.contains(12, Double.NaN, 0, 0));
        assertFalse(ExplosionSphere.contains(12, Double.POSITIVE_INFINITY, 0, 0));
    }
    @Test void supportsConfiguredRadiusWithTheSameSphericalBoundary() {
        assertTrue(ExplosionSphere.contains(20, 20, 0, 0));
        assertFalse(ExplosionSphere.contains(20, 15, 0, 15));
        assertTrue(ExplosionSphere.contains(64, 0, -64, 0));
    }
}
