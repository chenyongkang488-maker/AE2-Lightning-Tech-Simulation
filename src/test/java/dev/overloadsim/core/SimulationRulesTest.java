package dev.overloadsim.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SimulationRulesTest {
    @Test void matrixCapAndBase() {
        assertEquals(1, SimulationRules.parallel(0));
        assertEquals(4, SimulationRules.parallel(1));
        assertEquals(8, SimulationRules.parallel(2));
        assertEquals(128, SimulationRules.parallel(32));
        assertThrows(IllegalArgumentException.class, () -> SimulationRules.parallel(33));
        assertThrows(IllegalArgumentException.class, () -> SimulationRules.parallel(-1));
    }
    @Test void accelerationNeverCreatesZeroTickLoop() {
        assertEquals(13, SimulationRules.duration(200,4));
        assertEquals(1, SimulationRules.duration(1,4));
        assertEquals(200, SimulationRules.duration(200,0));
    }
    @Test void probabilityAndRadiusBoundaries() {
        assertTrue(SimulationRules.chance(0.099,0.1));
        assertFalse(SimulationRules.chance(0.1,0.1));
        assertTrue(SimulationRules.chance(0.329999,0.33));
        assertFalse(SimulationRules.chance(0.33,0.33));
        assertTrue(SimulationRules.withinRadius(25,5));
        assertFalse(SimulationRules.withinRadius(25.001,5));
    }
    @Test void parallelCostsAndOverflow() {
        assertEquals(128_000, SimulationRules.batchEnergy(1000,128));
        assertThrows(ArithmeticException.class, () -> SimulationRules.batchEnergy(Long.MAX_VALUE,2));
    }
    @Test void doubledParallelAndPercentBoundariesStayExact() {
        assertEquals(256_000, SimulationRules.batchEnergy(1000,256));
        assertEquals(200_000_000L, SimulationRules.scalePercent(1_000_000,20_000));
        assertEquals(200, SimulationRules.scalePercent(1,19_999));
        assertThrows(IllegalArgumentException.class, () -> SimulationRules.batchEnergy(1000,257));
        assertThrows(IllegalArgumentException.class, () -> SimulationRules.scalePercent(1,20_001));
        assertThrows(ArithmeticException.class, () -> SimulationRules.scalePercent(Long.MAX_VALUE,20_000));
    }
}
