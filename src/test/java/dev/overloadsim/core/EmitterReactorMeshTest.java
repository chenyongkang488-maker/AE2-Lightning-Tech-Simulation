package dev.overloadsim.core;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EmitterReactorMeshTest {
    @Test void reactorUsesBoundedPixelGeometryAndAForwardEmissiveCore() throws Exception {
        var type = Class.forName("dev.overloadsim.core.EmitterReactorMesh");
        var surfaces = (List<?>) type.getMethod("surfaces").invoke(null);
        assertTrue(surfaces.size() > 100 && surfaces.size() < 3000);
        boolean brightCore = false, armour = false;
        for (var surface : surfaces) {
            boolean glow = (boolean) surface.getClass().getMethod("glow").invoke(surface);
            var quad = surface.getClass().getMethod("quad").invoke(surface);
            for (String corner : List.of("a", "b", "c", "d")) {
                var vertex = quad.getClass().getMethod(corner).invoke(quad);
                double x = number(vertex,"x"), y = number(vertex,"y"), z = number(vertex,"z");
                for (double value : new double[]{x,y,z}) {
                    assertTrue(Double.isFinite(value) && Math.abs(value) < 2.85, "Stay inside emitter culling bounds");
                    assertEquals(Math.rint(value * 16), value * 16, 1e-9, "Vertices must stay on the Minecraft pixel grid");
                }
                double nx=number(vertex,"nx"),ny=number(vertex,"ny"),nz=number(vertex,"nz");
                assertEquals(1, Math.abs(nx)+Math.abs(ny)+Math.abs(nz), 1e-9, "Keep axis-aligned voxel faces");
                brightCore |= glow && z>2.5 && Math.abs(x)<.6 && Math.abs(y)<.6;
                armour |= !glow && Math.abs(x)>1.75;
            }
        }
        assertTrue(brightCore && armour, "Reactor needs a visible luminous core in front of the optical port and stepped armour");
    }

    @Test void realBackControllerAndOptionalInterfaceRemainExposed() throws Exception {
        var type = Class.forName("dev.overloadsim.core.EmitterReactorMesh");
        var surfaces = (List<IndustrialMachineMesh.Surface>) type.getMethod("surfaces").invoke(null);
        for (double[] opening : List.of(new double[]{0,0,-2},new double[]{2,0,0})) {
            var clipped = IndustrialMachineMesh.withoutCell(surfaces,opening[0],opening[1],opening[2]);
            assertFalse(clipped.isEmpty());
            for (var face : clipped) {
                var q=face.quad();
                double x=(q.a().x()+q.b().x()+q.c().x()+q.d().x())/4;
                double y=(q.a().y()+q.b().y()+q.c().y()+q.d().y())/4;
                double z=(q.a().z()+q.b().z()+q.c().z()+q.d().z())/4;
                assertFalse(Math.abs(x-opening[0])<.505 && Math.abs(y-opening[1])<.505 && Math.abs(z-opening[2])<.505);
            }
        }
    }

    private static double number(Object object,String method) throws Exception {
        return ((Number)object.getClass().getMethod(method).invoke(object)).doubleValue();
    }
}
