package dev.overloadsim.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CoreCollapseCycleTest {
    @Test void settledVoxelEnvelopeEnclosesClampsThroughoutRotation() throws Exception {
        var resource = getClass().getResourceAsStream("/assets/overload_sim/models/block/collapse_energy_orb.json");
        assertNotNull(resource);
        com.google.gson.JsonArray boxes;
        try (var reader = new java.io.InputStreamReader(resource, java.nio.charset.StandardCharsets.UTF_8)) {
            boxes = com.google.gson.JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("elements");
        }
        double scale = CoreCollapseCycle.modelScale(1_000);
        for (int degrees=0; degrees<360; degrees+=2) {
            double angle=Math.toRadians(degrees);
            for (double x:new double[]{-.595,.595}) for (double y:new double[]{-.595,.595}) for (double z:new double[]{-.595,.595}) {
                double[] point={(x*Math.cos(angle)+z*Math.sin(angle))/scale*16+8,y/scale*16+8,
                        (-x*Math.sin(angle)+z*Math.cos(angle))/scale*16+8};
                boolean inside=false;
                for (var element:boxes) {
                    var box=element.getAsJsonObject();var from=box.getAsJsonArray("from");var to=box.getAsJsonArray("to");
                    boolean contains=true;
                    for (int axis=0; axis<3; axis++) contains &= point[axis]>=from.get(axis).getAsDouble() && point[axis]<=to.get(axis).getAsDouble();
                    if (contains) { inside=true; break; }
                }
                assertTrue(inside,"Rotating voxel steps must not expose an outer clamp corner at "+degrees+" degrees");
            }
        }
    }

    @Test void settledSphereEnclosesTheCornerClamps() {
        double radius = CoreCollapseCycle.radius(1_000);
        // The existing corner attachment reaches +/- .595 on each axis.
        assertTrue(radius * radius > 3 * .595 * .595,
                "The settled energy sphere must surround the outer attachment corners");
    }

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
        assertEquals(1.125,(double)radius.invoke(null,1000d),1e-9);
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
