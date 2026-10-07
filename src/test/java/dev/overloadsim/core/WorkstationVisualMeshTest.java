package dev.overloadsim.core;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WorkstationVisualMeshTest {
    @Test void mergedOpaqueCapsCoverTheWholeFloorAndRoof() throws Exception {
        var shell = surfaces("shellSurfaces");
        for (double height : new double[]{-3.5, 3.5})
            for (int x = -3; x <= 3; x++) for (int z = -3; z <= 3; z++) {
                boolean covered = false;
                for (var surface : shell) {
                    var points = points(surface);
                    if (flat(points, "y", height) && contains(points, "x", x) && contains(points, "z", z)) {
                        covered = true;
                        break;
                    }
                }
                assertTrue(covered, "Merged opaque cap must cover " + x + "," + height + "," + z);
            }
    }

    @Test void controllerScreenBayRemainsExposedInTheMergedFront() throws Exception {
        for (var surface : surfaces("shellSurfaces")) {
            var points = points(surface);
            boolean facesScreen = points.stream().allMatch(v -> value(v, "z") < -3.47)
                    && contains(points, "x", 0) && contains(points, "y", -3);
            assertFalse(facesScreen, "The integrated shell must not cover the real controller's screen");
        }
    }

    @Test void transparentWindowsUseLargeContinuousPanes() throws Exception {
        var type = Class.forName("dev.overloadsim.core.WorkstationVisualMesh");
        var panes = (List<?>) type.getMethod("glass").invoke(null);
        assertTrue(panes.size() >= 3 && panes.size() <= 12, "Three windows need a bounded set of merged panes");
        int large = 0;
        for (var pane : panes) {
            var points = quadPoints(pane);
            double width = span(points, "x"), height = span(points, "y"), depth = span(points, "z");
            if (Math.max(width * height, depth * height) >= 20) large++;
        }
        assertTrue(large >= 3, "Front and both sides need continuous glass instead of a grid of voxel borders");
    }

    @Test void geometryStaysInsideTheFittedSevenBlockBodyWithoutExternalRods() throws Exception {
        for (var method : new String[]{"surfaces", "shellSurfaces"})
            for (var surface : surfaces(method)) for (var point : points(surface))
                for (var axis : new String[]{"x", "y", "z"}) {
                    double coordinate = value(point, axis);
                    assertTrue(Double.isFinite(coordinate) && Math.abs(coordinate) <= 3.5625,
                            "No collector or exterior rod may protrude beyond the fitted body: " + axis + "=" + coordinate);
                }
    }

    private static List<?> surfaces(String method) throws Exception {
        return (List<?>) Class.forName("dev.overloadsim.core.WorkstationVisualMesh").getMethod(method).invoke(null);
    }

    private static List<?> points(Object surface) throws Exception {
        return quadPoints(surface.getClass().getMethod("quad").invoke(surface));
    }

    private static List<?> quadPoints(Object quad) throws Exception {
        return List.of(quad.getClass().getMethod("a").invoke(quad), quad.getClass().getMethod("b").invoke(quad),
                quad.getClass().getMethod("c").invoke(quad), quad.getClass().getMethod("d").invoke(quad));
    }

    private static double value(Object vertex, String axis) {
        try { return ((Number) vertex.getClass().getMethod(axis).invoke(vertex)).doubleValue(); }
        catch (ReflectiveOperationException failure) { throw new AssertionError(failure); }
    }

    private static double minimum(List<?> points, String axis) {
        return points.stream().mapToDouble(point -> value(point, axis)).min().orElseThrow();
    }

    private static double maximum(List<?> points, String axis) {
        return points.stream().mapToDouble(point -> value(point, axis)).max().orElseThrow();
    }

    private static double span(List<?> points, String axis) { return maximum(points, axis) - minimum(points, axis); }

    private static boolean contains(List<?> points, String axis, double coordinate) {
        return minimum(points, axis) <= coordinate && maximum(points, axis) >= coordinate;
    }

    private static boolean flat(List<?> points, String axis, double coordinate) {
        return points.stream().allMatch(point -> Math.abs(value(point, axis) - coordinate) < 1e-9);
    }
}
