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
        assertTrue(SimulationRules.withinRadius(25,5));
        assertFalse(SimulationRules.withinRadius(25.001,5));
    }
    @Test void parallelCostsAndOverflow() {
        assertEquals(128_000, SimulationRules.batchEnergy(1000,128));
        assertThrows(ArithmeticException.class, () -> SimulationRules.batchEnergy(Long.MAX_VALUE,2));
    }
}
