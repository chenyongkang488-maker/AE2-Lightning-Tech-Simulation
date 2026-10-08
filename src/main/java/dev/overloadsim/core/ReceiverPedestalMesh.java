package dev.overloadsim.core;

import dev.overloadsim.core.IndustrialMachineMesh.Surface;
import dev.overloadsim.core.ReceiverShellMesh.Quad;
import dev.overloadsim.core.ReceiverShellMesh.Vertex;
import java.util.ArrayList;
import java.util.List;

/** Continuous chamber-style ceramic base, in coordinates relative to the receiver sphere. */
public final class ReceiverPedestalMesh {
    private static final int SIDE = 50, TOP = 51, PINK_RAIL = 35, RECESS = 39;
    private static final double[][] RIM = {{-3.16, -3.5}, {3.16, -3.5}, {3.5, -3.16}, {3.5, 3.16},
            {3.16, 3.5}, {-3.16, 3.5}, {-3.5, 3.16}, {-3.5, -3.16}};
    private static final List<Surface> SURFACES = build();
    private static final Quad SCREEN = quad(p(-.3125, -4.25, -3.4375), p(-.3125, -3.75, -3.4375),
            p(.3125, -3.75, -3.4375), p(.3125, -4.25, -3.4375));

    private ReceiverPedestalMesh() {}

    public static List<Surface> surfaces() { return SURFACES; }

    /** Draw using the existing ECG sprite; the underlying controller remains the interaction target. */
    public static Quad screen() { return SCREEN; }

    private static List<Surface> build() {
        var result = new ArrayList<Surface>();
        double inset = 3.375 / 3.5;
        for (int edge = 0; edge < RIM.length; edge++) {
            var a = RIM[edge];
            var b = RIM[(edge + 1) % RIM.length];
            // Long plates and actual rail bands cross the block boundaries without seams or coplanar overlays.
            side(result, edge, SIDE, false, -5.375, -5.3125);
            side(result, edge, PINK_RAIL, true, -5.3125, -5.25);
            if (edge == 0) {
                front(result, SIDE, -3.16, 3.16, -5.25, -4.3125);
                front(result, SIDE, -3.16, -.375, -4.3125, -3.6875);
                front(result, SIDE, .375, 3.16, -4.3125, -3.6875);
            } else side(result, edge, SIDE, false, -5.25, -3.6875);
            side(result, edge, SIDE, false, -3.6875, -3.65625);
            side(result, edge, PINK_RAIL, true, -3.65625, -3.625);

            face(result, TOP, false, p(a[0], -3.625, a[1]), p(a[0] * inset, -3.5, a[1] * inset),
                    p(b[0] * inset, -3.5, b[1] * inset), p(b[0], -3.625, b[1]));
            face(result, SIDE, false, p(a[0] * inset, -5.5, a[1] * inset), p(a[0], -5.375, a[1]),
                    p(b[0], -5.375, b[1]), p(b[0] * inset, -5.5, b[1] * inset));
            face(result, TOP, false, p(0, -3.5, 0), p(b[0] * inset, -3.5, b[1] * inset),
                    p(a[0] * inset, -3.5, a[1] * inset), p(0, -3.5, 0));
            face(result, SIDE, false, p(0, -5.5, 0), p(a[0] * inset, -5.5, a[1] * inset),
                    p(b[0] * inset, -5.5, b[1] * inset), p(0, -5.5, 0));
        }
        // A ceramic lip leads into a small dark recess rather than exposing the separate controller body.
        recess(result, SIDE, .375, .3125, -3.5, .34375, .28125, -3.46875);
        recess(result, RECESS, .34375, .28125, -3.46875, .3125, .25, -3.4375);
        face(result, RECESS, false, p(-.3125, -4.25, -3.421875), p(-.3125, -3.75, -3.421875),
                p(.3125, -3.75, -3.421875), p(.3125, -4.25, -3.421875));
        return List.copyOf(result);
    }

    private static void side(List<Surface> result, int edge, int tile, boolean glow, double low, double high) {
        var a = RIM[edge];
        var b = RIM[(edge + 1) % RIM.length];
        face(result, tile, glow, p(a[0], low, a[1]), p(a[0], high, a[1]),
                p(b[0], high, b[1]), p(b[0], low, b[1]));
    }

    private static void front(List<Surface> result, int tile, double left, double right, double low, double high) {
        face(result, tile, false, p(left, low, -3.5), p(left, high, -3.5),
                p(right, high, -3.5), p(right, low, -3.5));
    }

    private static void recess(List<Surface> result, int tile, double outerX, double outerY, double outerZ,
            double innerX, double innerY, double innerZ) {
        face(result, tile, false, p(-outerX, -4 - outerY, outerZ), p(-outerX, -4 + outerY, outerZ),
                p(-innerX, -4 + innerY, innerZ), p(-innerX, -4 - innerY, innerZ));
        face(result, tile, false, p(outerX, -4 - outerY, outerZ), p(innerX, -4 - innerY, innerZ),
                p(innerX, -4 + innerY, innerZ), p(outerX, -4 + outerY, outerZ));
        face(result, tile, false, p(-outerX, -4 + outerY, outerZ), p(outerX, -4 + outerY, outerZ),
                p(innerX, -4 + innerY, innerZ), p(-innerX, -4 + innerY, innerZ));
        face(result, tile, false, p(-outerX, -4 - outerY, outerZ), p(-innerX, -4 - innerY, innerZ),
                p(innerX, -4 - innerY, innerZ), p(outerX, -4 - outerY, outerZ));
    }

    private static void face(List<Surface> result, int tile, boolean glow, double[] a, double[] b, double[] c, double[] d) {
        var face = quad(a, b, c, d);
        // Every vertical piece shares the slab's material height. Restarting the full
        // striped tile on the controller cut-outs made the bands jump at its corners.
        if (tile == SIDE && Math.abs(face.a().ny()) < 1e-9)
            face = new Quad(sideUv(face.a()), sideUv(face.b()), sideUv(face.c()), sideUv(face.d()));
        result.add(new Surface(face, tile, glow));
    }

    private static Vertex sideUv(Vertex v) {
        return new Vertex(v.x(),v.y(),v.z(),v.nx(),v.ny(),v.nz(),v.u(),(-3.625-v.y())/1.75);
    }

    private static Quad quad(double[] a, double[] b, double[] c, double[] d) {
        double bx = b[0] - a[0], by = b[1] - a[1], bz = b[2] - a[2];
        double cx = c[0] - a[0], cy = c[1] - a[1], cz = c[2] - a[2];
        double nx = by * cz - bz * cy, ny = bz * cx - bx * cz, nz = bx * cy - by * cx;
        double length = Math.sqrt(nx * nx + ny * ny + nz * nz);
        nx /= length; ny /= length; nz /= length;
        // Horizontal and vertical material coordinates follow the physical face's winding.
        return new Quad(vertex(a, nx, ny, nz, 0, 1), vertex(b, nx, ny, nz, 0, 0),
                vertex(c, nx, ny, nz, 1, 0), vertex(d, nx, ny, nz, 1, 1));
    }

    private static Vertex vertex(double[] p, double nx, double ny, double nz, double u, double v) {
        return new Vertex(p[0], p[1], p[2], nx, ny, nz, u, v);
    }

    private static double[] p(double x, double y, double z) { return new double[]{x, y, z}; }
}
