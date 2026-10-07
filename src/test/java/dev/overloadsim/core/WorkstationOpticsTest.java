package dev.overloadsim.core;

import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WorkstationOpticsTest {
    @Test void fourIndependentCrystalRaysMeetAtTheCentralWorkSurface() throws Exception {
        var rays=rays();assertEquals(4,rays.size());
        var starts=new HashSet<String>();
        for(var ray:rays){
            Object start=ray.getClass().getMethod("start").invoke(ray);
            Object end=ray.getClass().getMethod("end").invoke(ray);
            starts.add(start.toString());
            assertEquals(0,coordinate(end,"x"),1e-9,"Every ray converges horizontally at the centre");
            assertEquals(0,coordinate(end,"z"),1e-9,"No ray aims at an outer inventory holder");
            assertTrue(coordinate(end,"y")>-1.9375 && coordinate(end,"y")<-1.85,
                    "The focus rests just above the centre holder instead of inside the plinth");
            assertTrue(coordinate(start,"y")>coordinate(end,"y"),"Each crystal shines downward");
        }
        assertEquals(4,starts.size(),"All four native crystal clusters must emit, with no duplicated ray");
    }

    @Test void rayOriginsStayOnTheVisibleTipsOfTheWallMountedClusters() throws Exception {
        for(var ray:rays()) {
            Object start=ray.getClass().getMethod("start").invoke(ray);
            assertEquals(1.125,Math.abs(coordinate(start,"x")),1e-9);
            assertEquals(1.125,Math.abs(coordinate(start,"y")),1e-9);
            assertEquals(1.84375,coordinate(start,"z"),1e-9,"NORTH's visible native crystal tip is forward of the flush wall base");
        }
    }

    private static List<?> rays() throws Exception {
        return (List<?>)Class.forName("dev.overloadsim.core.WorkstationOptics").getMethod("rays").invoke(null);
    }
    private static double coordinate(Object point,String axis) throws Exception {
        return ((Number)point.getClass().getMethod(axis).invoke(point)).doubleValue();
    }
}
