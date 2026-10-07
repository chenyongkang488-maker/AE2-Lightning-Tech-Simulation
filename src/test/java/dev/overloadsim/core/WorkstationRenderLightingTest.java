package dev.overloadsim.core;

import java.util.function.ToIntFunction;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WorkstationRenderLightingTest {
    @Test void exteriorFacesUseTheirDaylightInEveryControllerOrientation() throws Exception {
        var panel = new ReceiverShellMesh.Quad(point(-3.5,-2.5,-3.5),point(-3.5,2.5,-3.5),
                point(-2.5,2.5,-3.5),point(-2.5,-2.5,-3.5));
        for(int turn=0;turn<4;turn++) {
            int light = sample(panel,turn,p -> {
                double x=coordinate(p,"x"),z=coordinate(p,"z");
                // The controller and every physical frame cell are dark; air just outside is sunlit.
                return x < -3 || x > 4 || z < -3 || z > 4 ? 15 << 20 : 0;
            });
            assertEquals(15 << 20,light,"A sunlit external panel must not inherit the buried controller's darkness");
        }
    }

    @Test void unlitRoomsStayDarkAndNearbyBlockLightIsPreserved() throws Exception {
        var panel = new ReceiverShellMesh.Quad(point(-1,-1,-1),point(-1,1,-1),point(1,1,-1),point(1,-1,-1));
        assertEquals(0,sample(panel,0,p->0),"Armour must not become emissive to mask a lighting bug");
        assertEquals(7 << 4,sample(panel,0,p->7 << 4),"The local lamp's block-light channel must survive");
    }

    private static ReceiverShellMesh.Vertex point(double x,double y,double z) {
        return new ReceiverShellMesh.Vertex(x,y,z,0,0,-1,0,0);
    }
    private static int sample(ReceiverShellMesh.Quad quad,int turns,ToIntFunction<Object> query) throws Exception {
        var type=Class.forName("dev.overloadsim.core.WorkstationRenderLighting");
        return (int)type.getMethod("sample",ReceiverShellMesh.Quad.class,double.class,double.class,double.class,
                int.class,ToIntFunction.class).invoke(null,quad,.5,.5,.5,turns,query);
    }
    private static double coordinate(Object sample,String axis) {
        try{return (double)sample.getClass().getMethod(axis).invoke(sample);}
        catch(ReflectiveOperationException error){throw new AssertionError(error);}
    }
}
