package dev.overloadsim.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CoreCollapseCycleTest {
    @Test void continuousPulsesKeepStartAndRestartBeginsANewCycle() throws Exception {
        var type=Class.forName("dev.overloadsim.core.CoreCollapseCycle");
        var cycle=type.getConstructor().newInstance();
        var accept=type.getMethod("accept",long.class);
        assertEquals(true,accept.invoke(cycle,100L));
        assertEquals(false,accept.invoke(cycle,120L));
        assertEquals(false,accept.invoke(cycle,140L));
        assertEquals(100L,type.getMethod("startTick").invoke(cycle));
        assertEquals(true,accept.invoke(cycle,161L),"A pulse after active work expired restarts");
        type.getMethod("stop").invoke(cycle);
        assertEquals(true,accept.invoke(cycle,163L));
        assertEquals(163L,type.getMethod("startTick").invoke(cycle));
        type.getMethod("stop").invoke(cycle);
        assertEquals(true,accept.invoke(cycle,164L));
    }

    @Test void animationContractsThenExpandsAndClockErrorsAreBounded() throws Exception {
        var type=Class.forName("dev.overloadsim.core.CoreCollapseCycle");
        var radius=type.getMethod("radius",double.class);
        assertEquals(1.5,(double)radius.invoke(null,0d),1e-9);
        assertTrue((double)radius.invoke(null,18d)<.2);
        assertTrue((double)radius.invoke(null,32d)>.5);
        assertEquals(.85,(double)radius.invoke(null,1000d),1e-9);
        assertEquals(1.5,(double)radius.invoke(null,Double.NaN),1e-9);
        var cycle=type.getConstructor().newInstance();
        var accept=type.getMethod("accept",long.class);
        assertEquals(false,accept.invoke(cycle,-1L));
        assertEquals(-1L,type.getMethod("startTick").invoke(cycle));
        assertEquals(true,accept.invoke(cycle,Long.MAX_VALUE-1));
        assertEquals(false,accept.invoke(cycle,Long.MAX_VALUE));
        assertEquals(true,accept.invoke(cycle,4L),"Rewound world clock begins a valid new cycle");
    }
}
