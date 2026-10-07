package dev.overloadsim.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CoreMembraneMeshTest {
    @Test void bothMembranesAreCachedPixelThinAndStayOutsideTheSettledSphere() {
        for (int ring=0; ring<2; ring++) {
            var surfaces=CoreMembraneMesh.ring(ring);
            assertSame(surfaces,CoreMembraneMesh.ring(ring));
            assertFalse(surfaces.isEmpty());
            assertTrue(surfaces.size()<1_024,"Thin membranes retain a fixed small geometry budget");
            for (var surface:surfaces) {
                var q=surface.quad();
                for (var p:new ReceiverShellMesh.Vertex[]{q.a(),q.b(),q.c(),q.d()}) {
                    assertEquals(Math.rint(p.x()*16),p.x()*16,1e-9);
                    assertEquals(Math.rint(p.y()*16),p.y()*16,1e-9);
                    assertTrue(p.z()==0||p.z()==1/16d,"The membrane occupies one pixel of depth");
                    double radius=Math.sqrt(p.x()*p.x()+p.y()*p.y());
                    assertTrue(radius>1.25&&radius<1.70,"Membrane annuli clear the orb and fit its render bounds");
                }
            }
        }
    }
}
