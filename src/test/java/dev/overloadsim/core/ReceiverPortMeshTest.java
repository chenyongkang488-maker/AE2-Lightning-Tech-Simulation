package dev.overloadsim.core;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ReceiverPortMeshTest {
    @Test void formedLensesFitTheSphereInsteadOfKeepingAVoxelCube() throws Exception {
        var type = assertDoesNotThrow(() -> Class.forName("dev.overloadsim.core.ReceiverPortMesh"),
                "The receiver needs a formed optical collar without the protruding loose-port cube");
        for (int axis=0;axis<3;axis++) for (int sign:new int[]{-1,1}) {
            @SuppressWarnings("unchecked") var armour=(List<IndustrialMachineMesh.Surface>)type
                    .getMethod("surfaces",int.class,int.class).invoke(null,axis,sign);
            @SuppressWarnings("unchecked") var lenses=(List<ReceiverShellMesh.Quad>)type
                    .getMethod("glass",int.class,int.class).invoke(null,axis,sign);
            assertFalse(armour.isEmpty());assertEquals(2,lenses.size());
            assertTrue(armour.size()<80,"Each aperture stays bounded");
            for(var surface:armour) for(var vertex:points(surface.quad())) {
                double radial=axis==0?vertex.x():axis==1?vertex.y():vertex.z();
                assertTrue(radial*sign>=3 && radial*sign<3.4,
                        "The trim stays flush to the 3.3-radius sphere, inside the old 3.5 block cap");
                assertTrue(Math.abs(vertex.x())<3.4&&Math.abs(vertex.y())<3.4&&Math.abs(vertex.z())<3.4);
            }
            for(var lens:lenses) {
                var a=lens.a();var b=lens.b();var c=lens.c();
                double bx=b.x()-a.x(),by=b.y()-a.y(),bz=b.z()-a.z();
                double cx=c.x()-a.x(),cy=c.y()-a.y(),cz=c.z()-a.z();
                assertTrue((by*cz-bz*cy)*a.nx()+(bz*cx-bx*cz)*a.ny()+(bx*cy-by*cx)*a.nz()>0,
                        "Both sides of the lens retain outward winding after changing the aperture axis");
            }
        }
    }
    private static List<ReceiverShellMesh.Vertex> points(ReceiverShellMesh.Quad q) {
        return List.of(q.a(),q.b(),q.c(),q.d());
    }
}
