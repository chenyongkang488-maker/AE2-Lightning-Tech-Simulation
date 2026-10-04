package dev.overloadsim.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Protects the stored-particle display from work-cycle flicker and unbounded geometry. */
class ReceiverParticleVisualsTest {
    private static int count(long stored, long capacity) throws Exception {
        return (Integer) Class.forName("dev.overloadsim.core.ReceiverParticleVisuals")
                .getMethod("count", long.class, long.class).invoke(null, stored, capacity);
    }

    @Test void emptyAndInvalidStorageNeverProducesDots() throws Exception {
        assertEquals(0, count(0, 1_000_000));
        assertEquals(0, count(-1, 1_000_000));
        assertEquals(0, count(Long.MIN_VALUE, Long.MAX_VALUE));
    }

    @Test void positiveStorageStaysVisibleAndNeverExceedsBudget() throws Exception {
        assertEquals(1, count(1, 1_000_000));
        assertEquals(48, count(250_000, 1_000_000));
        assertEquals(96, count(1_000_000, 1_000_000));
        assertEquals(96, count(Long.MAX_VALUE, 1_000_000));
        assertEquals(1, count(1, 0));
        assertEquals(1, count(1, -1));
    }

    @Test void increasingStoredParticlesNeverRemovesDots() throws Exception {
        int previous = 0;
        for (long stored : new long[]{0, 1, 10, 100, 1_000, 10_000, 100_000, 500_000, 1_000_000, Long.MAX_VALUE}) {
            int next = count(stored, 1_000_000);
            assertTrue(next >= previous && next <= 96);
            previous = next;
        }
    }

    @Test void everyOrbitAndTrailPositionStaysInsideGlass() throws Exception {
        var position = Class.forName("dev.overloadsim.core.ReceiverParticleVisuals")
                .getMethod("position", int.class, double.class);
        for (int index = 0; index < 96; index++) {
            for (double time : new double[]{-10_000, -.5, 0, .5, 1_000, 1.0e15, Double.NaN, Double.POSITIVE_INFINITY}) {
                var point = position.invoke(null, index, time);
                double x = component(point, "x"), y = component(point, "y"), z = component(point, "z");
                assertTrue(Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z));
                assertTrue(x * x + y * y + z * z <= 2.3 * 2.3,
                        "Dots and their trail samples must remain inside the containment glass");
            }
            assertEquals(position.invoke(null, index, 25.5), position.invoke(null, index, 25.5),
                    "Orbit phase must depend only on index and world time");
            assertNotEquals(position.invoke(null, index, 25.5), position.invoke(null, index, 26.5));
        }
    }

    private static double component(Object point, String name) throws Exception {
        return ((Number) point.getClass().getMethod(name).invoke(point)).doubleValue();
    }
}
