package dev.overloadsim.core;

import static org.junit.jupiter.api.Assertions.*;
import java.util.EnumMap;
import org.junit.jupiter.api.Test;

class WorkstationGeometryTest {
    @Test void literalSevenCubeHasCompleteOpaqueRoofFloorAndBack() {
        var counts=new EnumMap<WorkstationGeometry.Part,Integer>(WorkstationGeometry.Part.class);
        for(int x=0;x<7;x++)for(int y=0;y<7;y++)for(int z=0;z<7;z++)counts.merge(WorkstationGeometry.part(x,y,z),1,Integer::sum);
        assertEquals(343,counts.values().stream().mapToInt(Integer::intValue).sum());
        assertEquals(141,counts.get(WorkstationGeometry.Part.FRAME));
        assertEquals(75,counts.get(WorkstationGeometry.Part.GLASS));
        assertEquals(125,counts.get(WorkstationGeometry.Part.AIR));
        assertEquals(1,counts.get(WorkstationGeometry.Part.CONTROLLER));
        assertEquals(1,counts.get(WorkstationGeometry.Part.RECEIVER));
        assertEquals(WorkstationGeometry.Part.FRAME,WorkstationGeometry.part(3,6,3));
        assertEquals(WorkstationGeometry.Part.CONTROLLER,WorkstationGeometry.part(3,0,0));
        assertEquals(WorkstationGeometry.Part.RECEIVER,WorkstationGeometry.part(3,3,6));
    }
}
