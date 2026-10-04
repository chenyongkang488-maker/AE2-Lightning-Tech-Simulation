package dev.overloadsim.core;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ReceiverShellMeshTest {
    private static final double EPSILON = 1.0e-10;

    @Test void facetedGlassSurfaceStaysClosedAndHasOutwardFaces() throws Exception {
        var mesh = Class.forName("dev.overloadsim.core.ReceiverShellMesh");
        var quads = (List<?>) mesh.getMethod("glass").invoke(null);
        assertTrue(quads.size() >= 100 && quads.size() <= 512,
                "Industrial glass must keep visible facets and a bounded mesh");
        for (var quad : quads) {
            var vertices = vertices(quad);
            for (var vertex : vertices) {
                double x = number(vertex, "x"), y = number(vertex, "y"), z = number(vertex, "z");
                assertEquals(3.3, Math.sqrt(x * x + y * y + z * z), EPSILON);
                assertEquals(1, Math.sqrt(square(number(vertex, "nx")) + square(number(vertex, "ny"))
                        + square(number(vertex, "nz"))), EPSILON);
            }
            assertTrue(outwardArea(vertices) > 0, "Every patch must face outward, including both poles");
        }
        assertClosedEdges(quads);
    }

    @Test void eachGreatCircleRibClosesWithoutStraightCylinderGaps() throws Exception {
        var mesh = Class.forName("dev.overloadsim.core.ReceiverShellMesh");
        for (int axis = 0; axis < 3; axis++) {
            var quads = (List<?>) mesh.getMethod("rib", int.class).invoke(null, axis);
            assertTrue(quads.size() >= 64 && quads.size() <= 256,
                    "The seam backing must use a few planar panels, not a smooth high-resolution torus");
            for (var quad : quads) {
                for (var vertex : vertices(quad)) {
                    double x = number(vertex, "x"), y = number(vertex, "y"), z = number(vertex, "z");
                    double radial = axis == 0 ? Math.hypot(y, z) : axis == 1 ? Math.hypot(x, z) : Math.hypot(x, y);
                    double axial = axis == 0 ? x : axis == 1 ? y : z;
                    assertTrue(radial >= 3.12 && radial <= 3.5);
                    assertTrue(Math.abs(axial) <= .16);
                }
            }
            assertClosedEdges(quads);
        }
    }

    private static void assertClosedEdges(List<?> quads) throws Exception {
        var counts = new java.util.HashMap<String, Integer>();
        for (var quad : quads) {
            var vertices = vertices(quad);
            for (int i = 0; i < 4; i++) {
                String a = pointKey(vertices.get(i)), b = pointKey(vertices.get((i + 1) % 4));
                if (a.equals(b)) continue; // Pole triangles occupy a quad with one duplicate corner.
                String key = a.compareTo(b) < 0 ? a + "/" + b : b + "/" + a;
                counts.merge(key, 1, Integer::sum);
            }
        }
        assertTrue(counts.values().stream().allMatch(count -> count == 2), "Each mesh edge belongs to two patches");
    }

    private static String pointKey(Object v) throws Exception {
        return Math.round(number(v, "x") * 1e8) + ":" + Math.round(number(v, "y") * 1e8)
                + ":" + Math.round(number(v, "z") * 1e8);
    }

    private static double outwardArea(List<?> vertices) throws Exception {
        double sum = 0;
        for (int i = 1; i < 3; i++) {
            var a = vertices.get(0); var b = vertices.get(i); var c = vertices.get(i + 1);
            double bx = number(b, "x") - number(a, "x"), by = number(b, "y") - number(a, "y"), bz = number(b, "z") - number(a, "z");
            double cx = number(c, "x") - number(a, "x"), cy = number(c, "y") - number(a, "y"), cz = number(c, "z") - number(a, "z");
            sum += (by * cz - bz * cy) * number(a, "nx") + (bz * cx - bx * cz) * number(a, "ny")
                    + (bx * cy - by * cx) * number(a, "nz");
        }
        return sum;
    }

    private static List<?> vertices(Object quad) throws Exception {
        return List.of(quad.getClass().getMethod("a").invoke(quad), quad.getClass().getMethod("b").invoke(quad),
                quad.getClass().getMethod("c").invoke(quad), quad.getClass().getMethod("d").invoke(quad));
    }
    private static double number(Object value, String name) throws Exception {
        return ((Number) value.getClass().getMethod(name).invoke(value)).doubleValue();
    }
    private static double square(double value) { return value * value; }
}
