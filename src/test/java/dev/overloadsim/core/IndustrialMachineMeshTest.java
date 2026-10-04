package dev.overloadsim.core;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class IndustrialMachineMeshTest {
    @Test void receiverPlinthContainsNegativeCoordinatesAndKeepsControllerOpening() throws Exception {
        var mesh = mesh("receiverPlinth");
        assertFalse(mesh.isEmpty());
        double minimumX = Double.POSITIVE_INFINITY, minimumY = Double.POSITIVE_INFINITY;
        for (var surface : mesh) for (var vertex : vertices(surface)) {
            double x = number(vertex, "x"), y = number(vertex, "y"), z = number(vertex, "z");
            minimumX = Math.min(minimumX, x); minimumY = Math.min(minimumY, y);
            assertTrue(x >= -3.6 && x <= 3.6 && y >= -5.6 && y <= -3.4 && z >= -3.6 && z <= 3.6);
        }
        assertTrue(minimumX < -3 && minimumY < -5);
        // Controller center is (0,-4,-3); an outward-facing plate here would hide its screen.
        for (var surface : mesh) {
            var vertices = vertices(surface);
            boolean coversController = vertices.stream().allMatch(v -> {
                try { return number(v, "z") <= -3.49; } catch (Exception failure) { throw new AssertionError(failure); }
            }) && min(vertices, "x") < .49 && max(vertices, "x") > -.49
                    && min(vertices, "y") < -3.51 && max(vertices, "y") > -4.49;
            assertFalse(coversController, "The front controller opening must stay unobstructed");
        }
    }

    @Test void armourRingsHaveRealPanelGapsAndBoundedOutwardGeometry() throws Exception {
        var type = Class.forName("dev.overloadsim.core.IndustrialMachineMesh");
        for (int axis = 0; axis < 3; axis++) {
            var mesh = (List<?>) type.getMethod("armouredRing", int.class, double.class).invoke(null, axis, 3.3);
            assertTrue(mesh.size() > 100 && mesh.size() < 600, "Mechanical detailing must have a fixed vertex budget");
            boolean glow = false, vent = false, screw = false;
            for (var surface : mesh) {
                int tile = ((Number) surface.getClass().getMethod("tile").invoke(surface)).intValue();
                glow |= tile == 35; vent |= tile == 33; screw |= tile == 34;
                for (var vertex : vertices(surface)) {
                    double x = number(vertex, "x"), y = number(vertex, "y"), z = number(vertex, "z");
                    assertTrue(Double.isFinite(x + y + z));
                    assertTrue(x * x + y * y + z * z < 3.7 * 3.7);
                    double nx = number(vertex, "nx"), ny = number(vertex, "ny"), nz = number(vertex, "nz");
                    assertEquals(1, nx * nx + ny * ny + nz * nz, 1.0e-9);
                }
            }
            assertTrue(glow && vent && screw, "Armour panels need distinct ventilation, bolts and guide strips");
        }
    }

    @Test void optionalInterfaceOpeningClipsMergedPlinthWithoutRemovingWholeTopPanels() throws Exception {
        var type = Class.forName("dev.overloadsim.core.IndustrialMachineMesh");
        var original = mesh("receiverPlinth");
        var clipped = (List<?>) type.getMethod("withoutCell", List.class, double.class, double.class, double.class)
                .invoke(null, original, 2.0, -4.0, 0.0);
        assertFalse(clipped.isEmpty());
        boolean retainedTop = false;
        for (var surface : clipped) {
            var points = vertices(surface);
            double x=0,y=0,z=0;
            for (var point : points) { x+=number(point,"x")/4; y+=number(point,"y")/4; z+=number(point,"z")/4; }
            assertFalse(x>1.495 && x<2.505 && y>-4.505 && y<-3.495 && z>-.505 && z<.505,
                    "Merged plates must leave all faces of the real network interface visible");
            retainedTop |= Math.abs(y+3.5)<1e-9 && (x<1 || x>3 || Math.abs(z)>1);
        }
        assertTrue(retainedTop, "An interface opening must preserve the surrounding merged top");
    }

    private static List<?> mesh(String method) throws Exception {
        return (List<?>) Class.forName("dev.overloadsim.core.IndustrialMachineMesh").getMethod(method).invoke(null);
    }
    private static List<?> vertices(Object surface) throws Exception {
        Object quad = surface.getClass().getMethod("quad").invoke(surface);
        return List.of(quad.getClass().getMethod("a").invoke(quad), quad.getClass().getMethod("b").invoke(quad),
                quad.getClass().getMethod("c").invoke(quad), quad.getClass().getMethod("d").invoke(quad));
    }
    private static double min(List<?> vertices, String name) throws Exception {
        double result = Double.POSITIVE_INFINITY;
        for (var vertex : vertices) result = Math.min(result, number(vertex, name));
        return result;
    }
    private static double max(List<?> vertices, String name) throws Exception {
        double result = Double.NEGATIVE_INFINITY;
        for (var vertex : vertices) result = Math.max(result, number(vertex, name));
        return result;
    }
    private static double number(Object value, String name) throws Exception {
        return ((Number) value.getClass().getMethod(name).invoke(value)).doubleValue();
    }
}
