package dev.overloadsim.core;

import java.util.ArrayList;
import java.util.List;

/** Precomputed cosmetic geometry; coordinates are relative to the receiver center. */
public final class ReceiverShellMesh {
    public static final double RADIUS = 3.3;
    public static final double RIB_RADIUS = .12;
    private static final int LATITUDES = 12, LONGITUDES = 24, RING_SEGMENTS = 24, RIB_SIDES = 4;
    public record Vertex(double x, double y, double z, double nx, double ny, double nz, double u, double v) {}
    public record Quad(Vertex a, Vertex b, Vertex c, Vertex d) {}
    private static final List<Quad> GLASS = glassMesh();
    private static final List<List<Quad>> RIBS = List.of(ribMesh(0, false), ribMesh(1, false), ribMesh(2, false));
    private static final List<List<Quad>> CHANNELS = List.of(ribMesh(0, true), ribMesh(1, true), ribMesh(2, true));
    private ReceiverShellMesh() {}

    public static List<Quad> glass() { return GLASS; }
    public static List<Quad> rib(int axis) { return RIBS.get(axis); }
    public static List<Quad> channel(int axis) { return CHANNELS.get(axis); }

    private static List<Quad> glassMesh() {
        var result = new ArrayList<Quad>(LATITUDES * LONGITUDES);
        for (int latitude = 0; latitude < LATITUDES; latitude++) {
            for (int longitude = 0; longitude < LONGITUDES; longitude++) {
                result.add(flat(new Quad(sphere(latitude, longitude), sphere(latitude, longitude + 1),
                        sphere(latitude + 1, longitude + 1), sphere(latitude + 1, longitude))));
            }
        }
        return List.copyOf(result);
    }

    /** A single outward normal gives each pane an intentional low-poly reflection. */
    private static Quad flat(Quad quad) {
        double nx=quad.a.nx+quad.b.nx+quad.c.nx+quad.d.nx;
        double ny=quad.a.ny+quad.b.ny+quad.c.ny+quad.d.ny;
        double nz=quad.a.nz+quad.b.nz+quad.c.nz+quad.d.nz;
        double length=Math.sqrt(nx*nx+ny*ny+nz*nz);
        return new Quad(normal(quad.a,nx/length,ny/length,nz/length),normal(quad.b,nx/length,ny/length,nz/length),
                normal(quad.c,nx/length,ny/length,nz/length),normal(quad.d,nx/length,ny/length,nz/length));
    }
    private static Vertex normal(Vertex v,double nx,double ny,double nz) {
        return new Vertex(v.x,v.y,v.z,nx,ny,nz,v.u,v.v);
    }

    private static Vertex sphere(int latitude, int longitude) {
        double phi = latitude * Math.PI / LATITUDES;
        double theta = longitude % LONGITUDES * Math.PI * 2 / LONGITUDES;
        double sin = latitude == 0 || latitude == LATITUDES ? 0 : Math.sin(phi);
        double x = sin * Math.cos(theta), y = Math.cos(phi), z = sin * Math.sin(theta);
        return new Vertex(RADIUS * x, RADIUS * y, RADIUS * z, x, y, z,
                longitude / (double) LONGITUDES, latitude / (double) LATITUDES);
    }

    private static List<Quad> ribMesh(int axis, boolean channel) {
        int sides = channel ? 2 : RIB_SIDES;
        var result = new ArrayList<Quad>(RING_SEGMENTS * sides);
        for (int segment = 0; segment < RING_SEGMENTS; segment++) {
            for (int side = 0; side < sides; side++) {
                double phi = channel ? (side - 1) * Math.PI / 8 : side * Math.PI * 2 / RIB_SIDES;
                double next = channel ? side * Math.PI / 8 : (side + 1) * Math.PI * 2 / RIB_SIDES;
                double minor = RIB_RADIUS + (channel ? .003 : 0);
                result.add(new Quad(torus(axis, segment, phi, minor, 0, 0), torus(axis, segment + 1, phi, minor, 1, 0),
                        torus(axis, segment + 1, next, minor, 1, 1), torus(axis, segment, next, minor, 0, 1)));
            }
        }
        return List.copyOf(result);
    }

    private static Vertex torus(int axis, int segment, double phi, double minor, double u, double v) {
        double theta = segment % RING_SEGMENTS * Math.PI * 2 / RING_SEGMENTS;
        double radial = RADIUS + minor * Math.cos(phi), axial = minor * Math.sin(phi);
        double c = Math.cos(theta), s = Math.sin(theta), nc = Math.cos(phi), ns = Math.sin(phi);
        return switch (axis) {
            case 0 -> new Vertex(axial, radial * c, radial * s, ns, nc * c, nc * s, u, v);
            case 1 -> new Vertex(radial * c, -axial, radial * s, nc * c, -ns, nc * s, u, v);
            case 2 -> new Vertex(radial * c, radial * s, axial, nc * c, nc * s, ns, u, v);
            default -> throw new IllegalArgumentException("rib axis");
        };
    }
}
