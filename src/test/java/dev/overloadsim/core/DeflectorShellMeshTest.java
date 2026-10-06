package dev.overloadsim.core;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DeflectorShellMeshTest {
    @Test void formedDeflectorMergesItsSphereAndKeepsAllSixOpticalEndpointsOpen() throws Exception {
        var faces=(List<IndustrialMachineMesh.Surface>)Class.forName("dev.overloadsim.core.DeflectorShellMesh").getMethod("surfaces").invoke(null);
        assertTrue(faces.size()>100&&faces.size()<7000);
        for(var surface:faces){
            var q=surface.quad();double x=(q.a().x()+q.b().x()+q.c().x()+q.d().x())/4,y=(q.a().y()+q.b().y()+q.c().y()+q.d().y())/4,z=(q.a().z()+q.b().z()+q.c().z()+q.d().z())/4;
            assertFalse(Math.abs(x)<.49&&Math.abs(y)<.49&&Math.abs(z)>2.9,"Z ports remain accessible");
            assertFalse(Math.abs(x)<.49&&Math.abs(z)<.49&&Math.abs(y)>2.9,"Y controller and port remain accessible");
            assertFalse(Math.abs(y)<.49&&Math.abs(z)<.49&&Math.abs(x)>2.9,"X ports remain accessible");
            for(var v:List.of(q.a(),q.b(),q.c(),q.d()))assertTrue(Double.isFinite(v.x()+v.y()+v.z())&&Math.abs(v.x())<=3.5&&Math.abs(v.y())<=3.5&&Math.abs(v.z())<=3.5);
        }
        assertTrue(faces.stream().anyMatch(f->f.glow()));
    }
}
