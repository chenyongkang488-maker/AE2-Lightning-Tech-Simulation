package dev.overloadsim.core;

import dev.overloadsim.core.IndustrialMachineMesh.Surface;
import dev.overloadsim.core.ReceiverShellMesh.Quad;
import dev.overloadsim.core.ReceiverShellMesh.Vertex;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ReceiverPedestalMeshTest {
    private static final double EPSILON = 1.0e-9;

    @Test void ceramicStripesKeepTheSameHeightAcrossEverySplitAndCorner() throws Exception {
        var bands = mesh().stream().filter(s -> s.tile() == 50 && Math.abs(s.quad().a().ny()) < EPSILON).toList();
        assertTrue(bands.size() > 8);
        for (var band : bands) for (var vertex : vertices(band.quad()))
            assertEquals((-3.625 - vertex.y()) / 1.75, vertex.v(), EPSILON,
                    "Ceramic texture height must follow the complete side, including the split front bay");
    }

    @Test void ceramicBaseHasFiniteOutwardFacesInsideTheTwoLayerFootprint() throws Exception {
        var mesh = mesh();
        assertFalse(mesh.isEmpty());
        assertTrue(mesh.size() < 180, "A merged slab must keep a bounded geometry budget");
        boolean lowerEdge = false, top = false, bevel = false;
        for (var surface : mesh) {
            var q = surface.quad();
            for (var v : vertices(q)) {
                assertTrue(Double.isFinite(v.x() + v.y() + v.z() + v.u() + v.v()));
                assertTrue(v.x() >= -3.5 - EPSILON && v.x() <= 3.5 + EPSILON);
                assertTrue(v.z() >= -3.5 - EPSILON && v.z() <= 3.5 + EPSILON);
                assertTrue(v.y() >= -5.5 - EPSILON && v.y() <= -3.5 + EPSILON);
                assertEquals(1, v.nx() * v.nx() + v.ny() * v.ny() + v.nz() * v.nz(), EPSILON);
                assertTrue(v.u() >= 0 && v.u() <= 1 && v.v() >= 0 && v.v() <= 1);
                lowerEdge |= Math.abs(v.y() + 5.5) < EPSILON;
                top |= Math.abs(v.y() + 3.5) < EPSILON;
                bevel |= Math.abs(v.ny()) > .1 && Math.abs(v.ny()) < .9;
            }
            assertTrue(outwardArea(q) > EPSILON, "Faces must retain their winding and nonzero area");
        }
        assertTrue(lowerEdge && top && bevel, "The base retains both real layers and a geometric bevel");
    }

    @Test void eachLongSideAndPinkRailRemainContinuousAcrossBlockBoundaries() throws Exception {
        var mesh = mesh();
        assertTrue(mesh.stream().noneMatch(s -> s.tile() == 33 || s.tile() == 34),
                "The chamber-style ceramic base must not reuse per-block vents or bolts");
        for (var sample : List.of(new double[]{-3.5, -4.7, 0}, new double[]{3.5, -4.7, 0},
                new double[]{0, -4.7, -3.5}, new double[]{0, -4.7, 3.5})) {
            assertTrue(mesh.stream().anyMatch(s -> !s.glow() && covers(s.quad(), sample)
                            && span(s.quad(), sample[0] == 0 ? 0 : 2) > 6),
                    "Each long ceramic side must cross the voxel boundaries as one panel");
        }
        for (int axis : new int[]{0, 2}) for (int sign : new int[]{-1, 1}) {
            assertTrue(mesh.stream().anyMatch(s -> s.glow()
                            && allOn(s.quad(), axis, sign * 3.5)
                            && span(s.quad(), axis == 0 ? 2 : 0) > 6),
                    "The perimeter rail must not restart at each block");
        }
    }

    @Test void dedicatedScreenSitsInAVisibleRecessInsideTheInteractiveControllerCell() throws Exception {
        var screen = screen();
        var mesh = mesh();
        double screenZ = screen.a().z();
        for (var v : vertices(screen)) {
            assertTrue(v.x() > -.5 && v.x() < .5);
            assertTrue(v.y() > -4.5 && v.y() < -3.5);
            assertTrue(v.z() > -3.5 && v.z() < -3.3,
                    "The screen is inset behind the ceramic front, within the real controller cell");
            assertEquals(screenZ, v.z(), EPSILON);
            assertEquals(-1, v.nz(), EPSILON);
        }
        assertTrue(outwardArea(screen) > EPSILON);
        assertTrue(span(screen, 0) >= .5 && span(screen, 1) >= .375,
                "The merged controller must retain a legible screen");
        assertFalse(mesh.stream().anyMatch(s -> frontOccludes(s.quad(), 0, -4, screenZ)),
                "The ceramic shell and bezel must leave the recessed screen visible");
        assertTrue(mesh.stream().anyMatch(s -> covers(s.quad(), new double[]{-.48, -4, -3.5})));
        assertTrue(mesh.stream().anyMatch(s -> covers(s.quad(), new double[]{.48, -4, -3.5})),
                "Ceramic beside the screen must merge the controller into the base");
    }

    @Test void topInterfaceOpeningRetainsTheSurroundingMergedPlate() throws Exception {
        var clipped = IndustrialMachineMesh.withoutCell(mesh(), 2, -4, 0);
        assertFalse(clipped.stream().anyMatch(s -> covers(s.quad(), new double[]{2, -3.5, 0})),
                "The physical interface top must remain exposed");
        for (var sample : List.of(new double[]{1.4, -3.5, 0}, new double[]{2.6, -3.5, 0},
                new double[]{0, -3.5, 2}, new double[]{-2, -3.5, -2})) {
            assertTrue(clipped.stream().anyMatch(s -> covers(s.quad(), sample)),
                    "Opening one interface must preserve adjacent parts of merged top panels");
        }
    }

    @Test void sideInterfaceOpeningDoesNotEraseTheRestOfTheRailOrCeramicSide() throws Exception {
        var original = mesh();
        var clipped = IndustrialMachineMesh.withoutCell(original, 3, -4, 0);
        assertFalse(clipped.stream().anyMatch(s -> covers(s.quad(), new double[]{3.5, -4, 0})));
        for (var sample : List.of(new double[]{3.5, -4, -1}, new double[]{3.5, -4, 1},
                new double[]{3.5, -5, 0}, new double[]{-3.5, -4, 0})) {
            assertTrue(clipped.stream().anyMatch(s -> covers(s.quad(), sample)),
                    "Only the actual interface cell may disappear from the continuous side");
        }
        assertTrue(clipped.stream().anyMatch(s -> s.glow() && allOn(s.quad(), 0, -3.5)
                && span(s.quad(), 2) > 6), "The opposite pink rail must remain whole");
    }

    @SuppressWarnings("unchecked")
    private static List<Surface> mesh() throws Exception {
        return (List<Surface>) Class.forName("dev.overloadsim.core.ReceiverPedestalMesh")
                .getMethod("surfaces").invoke(null);
    }
    private static Quad screen() throws Exception {
        return (Quad) Class.forName("dev.overloadsim.core.ReceiverPedestalMesh").getMethod("screen").invoke(null);
    }
    private static List<Vertex> vertices(Quad q) { return List.of(q.a(), q.b(), q.c(), q.d()); }
    private static double coordinate(Vertex v, int axis) { return axis == 0 ? v.x() : axis == 1 ? v.y() : v.z(); }
    private static double span(Quad q, int axis) {
        return vertices(q).stream().mapToDouble(v -> coordinate(v, axis)).max().orElseThrow()
                - vertices(q).stream().mapToDouble(v -> coordinate(v, axis)).min().orElseThrow();
    }
    private static boolean allOn(Quad q, int axis, double coordinate) {
        return vertices(q).stream().allMatch(v -> Math.abs(coordinate(v, axis) - coordinate) < EPSILON);
    }
    private static double outwardArea(Quad q) {
        double[] a = point(q.a()), b = subtract(point(q.b()), a), c = subtract(point(q.c()), a);
        double[] area = cross(b, c);
        return area[0] * q.a().nx() + area[1] * q.a().ny() + area[2] * q.a().nz();
    }
    private static boolean covers(Quad q, double[] point) {
        return inTriangle(point, q.a(), q.b(), q.c()) || inTriangle(point, q.a(), q.c(), q.d());
    }
    private static boolean inTriangle(double[] p, Vertex a, Vertex b, Vertex c) {
        double[] ab = subtract(point(b), point(a)), ac = subtract(point(c), point(a));
        double[] ap = subtract(p, point(a)), normal = cross(ab, ac);
        double areaSquared = dot(normal, normal);
        if (areaSquared < EPSILON * EPSILON || Math.abs(dot(ap, normal)) > EPSILON * Math.sqrt(areaSquared)) return false;
        double u = dot(cross(ap, ac), normal) / areaSquared;
        double v = dot(cross(ab, ap), normal) / areaSquared;
        return u >= -EPSILON && v >= -EPSILON && u + v <= 1 + EPSILON;
    }
    private static boolean frontOccludes(Quad q, double x, double y, double z) {
        if (Math.abs(q.a().nz()) < EPSILON) return false;
        double hitZ = q.a().z() - (q.a().nx() * (x - q.a().x()) + q.a().ny() * (y - q.a().y())) / q.a().nz();
        return hitZ <= z + EPSILON && covers(q, new double[]{x, y, hitZ});
    }
    private static double[] point(Vertex v) { return new double[]{v.x(), v.y(), v.z()}; }
    private static double[] subtract(double[] a, double[] b) { return new double[]{a[0] - b[0], a[1] - b[1], a[2] - b[2]}; }
    private static double[] cross(double[] a, double[] b) {
        return new double[]{a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }
    private static double dot(double[] a, double[] b) { return a[0] * b[0] + a[1] * b[1] + a[2] * b[2]; }
}
