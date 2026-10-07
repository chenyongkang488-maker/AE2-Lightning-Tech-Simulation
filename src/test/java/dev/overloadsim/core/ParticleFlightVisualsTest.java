package dev.overloadsim.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ParticleFlightVisualsTest {
    @Test void retiredSnapshotCannotReturnAndFreshTimestampSurvivesAClockRewind() throws Exception {
        var type=Class.forName("dev.overloadsim.core.ParticleFlightVisuals$Clock");
        var clock=type.getConstructor().newInstance();
        var frame=type.getMethod("beginFrame",double.class,long[].class);
        var visible=type.getMethod("visibleCount",long.class,long.class,double.class);
        frame.invoke(clock,104d,new long[]{100});
        assertEquals(1,visible.invoke(clock,1L,100L,104d));
        frame.invoke(clock,105d,new long[]{});
        frame.invoke(clock,99d,new long[]{});
        frame.invoke(clock,104d,new long[]{100});
        assertEquals(0,visible.invoke(clock,1L,100L,104d),"A retired snapshot stays retired after empty state and clock rewind");
        frame.invoke(clock,99d,new long[]{99});
        assertEquals(2,visible.invoke(clock,2L,99L,99d),"A fresh unseen production timestamp launches immediately after a rewind");
    }

    @Test void observedFlightCannotReplayWhenARewoundClockCatchesUp() throws Exception {
        var type=Class.forName("dev.overloadsim.core.ParticleFlightVisuals$Clock");
        var clock=type.getConstructor().newInstance();
        var frame=type.getMethod("beginFrame",double.class,long[].class);
        var visible=type.getMethod("visibleCount",long.class,long.class,double.class);
        frame.invoke(clock,104d,new long[]{100});
        assertEquals(1,visible.invoke(clock,1L,100L,104d));
        frame.invoke(clock,99d,new long[]{100});
        assertEquals(0,visible.invoke(clock,1L,100L,99d));
        frame.invoke(clock,104d,new long[]{100});
        assertEquals(0,visible.invoke(clock,1L,100L,104d),"The old event stays finished when world time catches up");
        frame.invoke(clock,104d,new long[]{100});
        assertEquals(0,visible.invoke(clock,1L,100L,104d),"Repeating its snapshot cannot restart a rejected flight");
        frame.invoke(clock,105d,new long[]{105});
        assertEquals(2,visible.invoke(clock,2L,105L,105d),"Fresh production remains visible after a rewind");
    }

    private static int count(long particles, long launch, double time) throws Exception {
        return ((Number) Class.forName("dev.overloadsim.core.ParticleFlightVisuals")
                .getMethod("visibleCount", long.class, long.class, double.class).invoke(null, particles, launch, time)).intValue();
    }

    @Test void realBatchDrawsExactlyOnceAndNeverLoops() throws Exception {
        assertEquals(0, count(0, 100, 100));
        assertEquals(1, count(1, 100, 100));
        assertEquals(3, count(3, 100, 104));
        assertEquals(3, count(3, 100, 104), "Repeating a snapshot samples the same existing flight");
        assertEquals(0, count(3, 100, 108));
        assertEquals(0, count(3, 100, 120));
        assertEquals(0, count(3, 100, 1_000));
    }

    @Test void maximumBatchIsExactAndMalformedOrRewoundTimesAreInvisible() throws Exception {
        assertEquals(1_954, count(1_954, 100, 103.5));
        assertEquals(0, count(1_955, 100, 103.5));
        assertEquals(0, count(-1, 100, 103.5));
        assertEquals(0, count(1, -1, 103.5));
        assertEquals(0, count(1, 100, 99.99));
        assertEquals(0, count(1, Long.MAX_VALUE, Double.MAX_VALUE));
        assertEquals(0, count(1, 100, Double.NaN));
        assertEquals(0, count(1, 100, Double.POSITIVE_INFINITY));
    }
}
