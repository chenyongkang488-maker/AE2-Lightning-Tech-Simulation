package dev.overloadsim.core;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GateMatrixMeshTest {
    @Test void outerCollarsHaveDistinctProfilesAndBoundedPixelGeometry() throws Exception {
        var type=Class.forName("dev.overloadsim.core.GateMatrixMesh");
        var constraint=(List<IndustrialMachineMesh.Surface>)type.getMethod("outer",boolean.class).invoke(null,false);
        var focus=(List<IndustrialMachineMesh.Surface>)type.getMethod("outer",boolean.class).invoke(null,true);
        assertNotEquals(constraint,focus);
        for(var mesh:List.of(constraint,focus)) {
            assertTrue(mesh.size()>100&&mesh.size()<4000);
            boolean rune=false,outer=false;
            for(var face:mesh)for(var v:List.of(face.quad().a(),face.quad().b(),face.quad().c(),face.quad().d())) {
                for(double coordinate:new double[]{v.x(),v.y(),v.z()}) {
                    assertTrue(Double.isFinite(coordinate)&&Math.abs(coordinate)<4.2);
                    assertEquals(Math.rint(coordinate*16),coordinate*16,1e-9);
                }
                rune|=face.glow()&&face.tile()>=46;
                outer|=Math.hypot(v.x(),v.y())>3.5;
            }
            assertTrue(rune&&outer);
            var clipped=IndustrialMachineMesh.withoutCell(mesh,0,-3,0);
            for(var face:clipped) {
                var q=face.quad();
                double x=(q.a().x()+q.b().x()+q.c().x()+q.d().x())/4;
                double y=(q.a().y()+q.b().y()+q.c().y()+q.d().y())/4;
                double z=(q.a().z()+q.b().z()+q.c().z()+q.d().z())/4;
                assertFalse(Math.abs(x)<.505&&Math.abs(y+3)<.505&&Math.abs(z)<.505);
            }
        }
    }
}
