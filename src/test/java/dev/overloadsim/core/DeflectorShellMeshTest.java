package dev.overloadsim.core;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DeflectorShellMeshTest {
    @Test void formedDeflectorMatchesThreeBlockBeamsAndClosesVerticalOptics() throws Exception {
        var faces=(List<IndustrialMachineMesh.Surface>)Class.forName("dev.overloadsim.core.DeflectorShellMesh").getMethod("surfaces").invoke(null);
        assertTrue(faces.size()>100&&faces.size()<7000);
        for(var surface:faces){
            var q=surface.quad();double x=(q.a().x()+q.b().x()+q.c().x()+q.d().x())/4,y=(q.a().y()+q.b().y()+q.c().y()+q.d().y())/4,z=(q.a().z()+q.b().z()+q.c().z()+q.d().z())/4;
            assertFalse(x*x+y*y<1.45*1.45&&Math.abs(z)>2.85,"Z aperture accommodates full 3-block beam");
            assertFalse(y*y+z*z<1.45*1.45&&Math.abs(x)>2.85,"X aperture accommodates full 3-block beam");
            for(var v:List.of(q.a(),q.b(),q.c(),q.d()))assertTrue(Double.isFinite(v.x()+v.y()+v.z())&&Math.abs(v.x())<=3.5&&Math.abs(v.y())<=3.5&&Math.abs(v.z())<=3.5);
        }
        assertTrue(faces.stream().anyMatch(f->f.glow()));
        assertTrue(faces.stream().anyMatch(f->{var q=f.quad();return Math.abs(q.a().x())<.3&&q.a().y()>3&&Math.abs(q.a().z())<.3;}),"upper shell has no optical aperture");
        assertTrue(faces.stream().anyMatch(f->{var q=f.quad();return Math.abs(q.a().x())<.3&&q.a().y()<-3&&Math.abs(q.a().z())<.3;}),"raw lower pole stays capped until the real controller is clipped");
    }
}
