package dev.overloadsim.core;

import java.util.ArrayList;
import java.util.List;
import dev.overloadsim.core.ReceiverShellMesh.Quad;
import dev.overloadsim.core.ReceiverShellMesh.Vertex;

/** Small immutable meshes in right/up/forward coordinates, independent of client classes. */
public final class IndustrialMachineMesh {
    public record Surface(Quad quad, int tile, boolean glow) {}
    private static final List<Surface> PLINTH = plinth();
    private static final List<List<Surface>> RECEIVER_RINGS = List.of(ring(0, 3.3), ring(1, 3.3), ring(2, 3.3));
    private static final List<Surface> MATRIX_RING = ring(2, 3.05);
    private static final List<Surface> FRAME_PANEL = framePanel();
    private static final List<Surface> CORE = core();
    private IndustrialMachineMesh() {}

    public static List<Surface> receiverPlinth() { return PLINTH; }
    public static List<Surface> armouredRing(int axis, double radius) {
        if (axis < 0 || axis > 2 || !Double.isFinite(radius) || radius <= .3)
            throw new IllegalArgumentException("ring geometry");
        return radius == 3.3 ? RECEIVER_RINGS.get(axis) : radius == 3.05 && axis == 2 ? MATRIX_RING : ring(axis, radius);
    }
    public static List<Surface> framePanelMesh() { return FRAME_PANEL; }
    public static List<Surface> coreAttachments() { return CORE; }

    /** Clip only the actual interface cell, retaining adjacent pieces of a merged platform panel. */
    public static List<Surface> withoutCell(List<Surface> mesh,double x,double y,double z) {
        var result = new ArrayList<Surface>();
        double[] center={x,y,z};
        for (var surface:mesh) {
            List<Vertex> remaining=List.of(surface.quad.a(),surface.quad.b(),surface.quad.c(),surface.quad.d());
            boolean outside=false;
            for(int axis=0;axis<3;axis++) {
                double minimum=Double.POSITIVE_INFINITY,maximum=Double.NEGATIVE_INFINITY;
                for(var vertex:remaining) {
                    double coordinate=axis==0?vertex.x():axis==1?vertex.y():vertex.z();
                    minimum=Math.min(minimum,coordinate);maximum=Math.max(maximum,coordinate);
                }
                if(maximum<=center[axis]-.505||minimum>=center[axis]+.505) { outside=true;break; }
            }
            if(outside) {result.add(surface);continue;}
            for (int plane=0;plane<6&&!remaining.isEmpty();plane++) {
                int axis=plane/2;boolean lower=plane%2==0;
                double boundary=center[axis]+(lower?-.505:.505);
                appendPolygon(result,surface,clip(remaining,axis,boundary,lower,false));
                remaining=clip(remaining,axis,boundary,lower,true);
            }
            // The remaining polygon is inside the interface's real cube and must not be drawn.
        }
        return List.copyOf(result);
    }

    private static List<Vertex> clip(List<Vertex> polygon,int axis,double boundary,boolean lower,boolean keepInside) {
        var result=new ArrayList<Vertex>();
        Vertex previous=polygon.getLast();
        double previousDistance=distance(previous,axis,boundary,lower);
        boolean previousKept=keepInside?previousDistance>=0:previousDistance<=0;
        for (var current:polygon) {
            double currentDistance=distance(current,axis,boundary,lower);
            boolean currentKept=keepInside?currentDistance>=0:currentDistance<=0;
            if(currentKept!=previousKept) {
                double t=previousDistance/(previousDistance-currentDistance);
                result.add(new Vertex(previous.x()+(current.x()-previous.x())*t,
                        previous.y()+(current.y()-previous.y())*t,previous.z()+(current.z()-previous.z())*t,
                        previous.nx(),previous.ny(),previous.nz(),previous.u()+(current.u()-previous.u())*t,
                        previous.v()+(current.v()-previous.v())*t));
            }
            if(currentKept)result.add(current);
            previous=current;previousDistance=currentDistance;previousKept=currentKept;
        }
        return result;
    }
    private static double distance(Vertex vertex,int axis,double boundary,boolean lower) {
        double coordinate=axis==0?vertex.x():axis==1?vertex.y():vertex.z();
        return lower?coordinate-boundary:boundary-coordinate;
    }
    private static void appendPolygon(List<Surface> result,Surface surface,List<Vertex> polygon) {
        for(int i=1;i+1<polygon.size();i++) {
            var a=polygon.getFirst();var b=polygon.get(i);var c=polygon.get(i+1);
            double bx=b.x()-a.x(),by=b.y()-a.y(),bz=b.z()-a.z(),cx=c.x()-a.x(),cy=c.y()-a.y(),cz=c.z()-a.z();
            if(Math.abs(by*cz-bz*cy)+Math.abs(bz*cx-bx*cz)+Math.abs(bx*cy-by*cx)>1e-10)
                result.add(new Surface(new Quad(a,b,c,c),surface.tile,surface.glow));
        }
    }

    public static List<Surface> translated(List<Surface> mesh,double x,double y,double z) {
        var result=new ArrayList<Surface>(mesh.size());
        for(var surface:mesh) {
            var q=surface.quad;
            result.add(new Surface(new Quad(move(q.a(),x,y,z),move(q.b(),x,y,z),move(q.c(),x,y,z),move(q.d(),x,y,z)),surface.tile,surface.glow));
        }
        return List.copyOf(result);
    }
    private static Vertex move(Vertex v,double x,double y,double z) {
        return new Vertex(v.x()+x,v.y()+y,v.z()+z,v.nx(),v.ny(),v.nz(),v.u(),v.v());
    }

    /** A merged two-layer octagonal platform; only the front controller bay interrupts its shell. */
    private static List<Surface> plinth() {
        var result = new ArrayList<Surface>();
        double[][] rim = {{-3.16,-3.5},{3.16,-3.5},{3.5,-3.16},{3.5,3.16},
                {3.16,3.5},{-3.16,3.5},{-3.5,3.16},{-3.5,-3.16}};
        for (int i = 0; i < rim.length; i++) {
            double[] a = rim[i], b = rim[(i + 1) % rim.length];
            face(result,39,false,p(a[0]*.995,-5.5,a[1]*.995),p(a[0]*.995,-3.5,a[1]*.995),
                    p(b[0]*.995,-3.5,b[1]*.995),p(b[0]*.995,-5.5,b[1]*.995));
            // Layer seams and side panel seams are real recesses, independent of the texture.
            int panels = i % 2 == 0 ? 6 : 1;
            for (int layer = 0; layer < 2; layer++) for (int panel = 0; panel < panels; panel++) {
                double t0 = panel / (double) panels + .006, t1 = (panel + 1) / (double) panels - .006;
                double x0 = a[0] + (b[0] - a[0]) * t0, z0 = a[1] + (b[1] - a[1]) * t0;
                double x1 = a[0] + (b[0] - a[0]) * t1, z1 = a[1] + (b[1] - a[1]) * t1;
                double low = -5.5 + layer + .035, high = -4.5 + layer - .035;
                if (i == 0 && layer == 1 && x0 < .5 && x1 > -.5) {
                    if (x0 < -.5) face(result, 38, false, p(x0,low,z0),p(x0,high,z0),p(-.5,high,z0),p(-.5,low,z0));
                    if (x1 > .5) face(result, 38, false, p(.5,low,z0),p(.5,high,z0),p(x1,high,z0),p(x1,low,z0));
                } else face(result, i % 2 == 0 && panel % 3 == 1 ? 33 : 38, false,
                        p(x0,low,z0),p(x0,high,z0),p(x1,high,z1),p(x1,low,z1));
            }
            // Thin continuous raised rail covers the recessed seam backing, except the controller bay.
            face(result,35,true,p(a[0],-3.59,a[1]),p(a[0],-3.54,a[1]),
                    p(b[0],-3.54,b[1]),p(b[0],-3.59,b[1]));
            // Top wedges leave the physical controller top exposed.
            face(result,37,false,p(0,-3.5,0),p(b[0],-3.5,b[1]),p(a[0],-3.5,a[1]),p(0,-3.5,0));
            face(result,38,false,p(0,-5.5,0),p(a[0],-5.5,a[1]),p(b[0],-5.5,b[1]),p(0,-5.5,0));
        }
        // Foundation underside and four compact load brackets, contained within the true base footprint.
        for (int x : new int[]{-1,1}) for (int z : new int[]{-1,1})
            box(result,39, x*2.82-.18,-3.55,z*2.82-.18,x*2.82+.18,-3.42,z*2.82+.18);
        return withoutCell(result,0,-4,-3);
    }

    private static List<Surface> ring(int axis, double radius) {
        var result = new ArrayList<Surface>(336);
        for (int segment = 0; segment < 24; segment++) {
            double a = segment * Math.PI / 12 + .012, b = (segment + 1) * Math.PI / 12 - .012;
            double inner = radius - .18, outer = radius + .19, depth = .20;
            ringBand(result,axis,32,false,a,b,inner,outer,depth);
            ringBand(result,axis,32,false,b,a,inner,outer,-depth);
            mappedFace(result,axis,39,false,polar(outer,a,-depth),polar(outer,b,-depth),polar(outer,b,depth),polar(outer,a,depth));
            mappedFace(result,axis,39,false,polar(inner,b,-depth),polar(inner,a,-depth),polar(inner,a,depth),polar(inner,b,depth));
            mappedFace(result,axis,32,false,polar(inner,a,-depth),polar(outer,a,-depth),polar(outer,a,depth),polar(inner,a,depth));
            mappedFace(result,axis,32,false,polar(outer,b,-depth),polar(inner,b,-depth),polar(inner,b,depth),polar(outer,b,depth));
            for (int side : new int[]{-1,1}) {
                double front = side * (depth + .003), from = side > 0 ? a : b, to = side > 0 ? b : a;
                ringBand(result,axis,35,true,from,to,outer-.065,outer-.025,front);
                double middle = (a+b)*.5, ventHalf = (b-a)*.26;
                ringBand(result,axis,33,false,middle-side*ventHalf,middle+side*ventHalf,inner+.095,outer-.11,front+.001*side);
                for (double bolt : new double[]{a+.035,b-.035})
                    ringBand(result,axis,34,false,bolt-.012*side,bolt+.012*side,inner+.023,inner+.087,front+.002*side);
            }
        }
        return List.copyOf(result);
    }

    private static List<Surface> framePanel() {
        var result = new ArrayList<Surface>();
        box(result,32,-.485,-.485,-.485,.485,.485,.485);
        // Face plates are set inside each voxel, making the machine's pixel geometry deliberate.
        for (int axis = 0; axis < 3; axis++) for (int side : new int[]{-1,1}) {
            double depth = side*.488;
            planar(result,axis,33,false,-.26,-.15,.26,.15,depth,side);
            planar(result,axis,35,true,-.34,.31,.34,.35,depth,side);
            for (int x : new int[]{-1,1}) for (int y : new int[]{-1,1})
                planar(result,axis,34,false,x*.36-.035,y*.36-.035,x*.36+.035,y*.36+.035,depth,side);
        }
        return List.copyOf(result);
    }

    private static List<Surface> core() {
        var result = new ArrayList<Surface>();
        // Do not bury the functional controller: corner clamps surround its six open face centers.
        for (int x : new int[]{-1,1}) for (int y : new int[]{-1,1}) for (int z : new int[]{-1,1}) {
            box(result,39,x*.46-.12,y*.46-.12,z*.46-.12,x*.46+.12,y*.46+.12,z*.46+.12);
            box(result,35,x*.56-.035,y*.56-.035,z*.56-.035,x*.56+.035,y*.56+.035,z*.56+.035);
        }
        return List.copyOf(result);
    }

    private static void planar(List<Surface> result,int axis,int tile,boolean glow,double x0,double y0,double x1,double y1,double z,int side) {
        if (side>0) mappedFace(result,axis,tile,glow,p(x0,y0,z),p(x1,y0,z),p(x1,y1,z),p(x0,y1,z));
        else mappedFace(result,axis,tile,glow,p(x1,y0,z),p(x0,y0,z),p(x0,y1,z),p(x1,y1,z));
    }
    private static void ringBand(List<Surface> result,int axis,int tile,boolean glow,double a,double b,double inner,double outer,double depth) {
        mappedFace(result,axis,tile,glow,polar(inner,a,depth),polar(outer,a,depth),polar(outer,b,depth),polar(inner,b,depth));
    }
    private static double[] polar(double radius,double angle,double depth) { return p(radius*Math.cos(angle),radius*Math.sin(angle),depth); }
    private static double[] p(double x,double y,double z) { return new double[]{x,y,z}; }
    private static double[] map(int axis,double[] p) {
        return switch(axis) { case 0 -> p(p[2],p[0],p[1]); case 1 -> p(p[0],p[2],-p[1]); default -> p; };
    }
    private static void mappedFace(List<Surface> result,int axis,int tile,boolean glow,double[] a,double[] b,double[] c,double[] d) {
        face(result,tile,glow,map(axis,a),map(axis,b),map(axis,c),map(axis,d));
    }
    private static void face(List<Surface> result,int tile,boolean glow,double[] a,double[] b,double[] c,double[] d) {
        double bx=b[0]-a[0],by=b[1]-a[1],bz=b[2]-a[2],cx=c[0]-a[0],cy=c[1]-a[1],cz=c[2]-a[2];
        double nx=by*cz-bz*cy,ny=bz*cx-bx*cz,nz=bx*cy-by*cx,length=Math.sqrt(nx*nx+ny*ny+nz*nz);
        if (length<=1e-12) return;
        nx/=length;ny/=length;nz/=length;
        result.add(new Surface(new Quad(vertex(a,nx,ny,nz,0,1),vertex(b,nx,ny,nz,1,1),
                vertex(c,nx,ny,nz,1,0),vertex(d,nx,ny,nz,0,0)),tile,glow));
    }
    private static Vertex vertex(double[] p,double nx,double ny,double nz,double u,double v) {
        return new Vertex(p[0],p[1],p[2],nx,ny,nz,u,v);
    }
    public static void box(List<Surface> result,int tile,double x0,double y0,double z0,double x1,double y1,double z1) {
        boolean glow=tile==35||tile==36;
        face(result,tile,glow,p(x0,y0,z0),p(x0,y1,z0),p(x1,y1,z0),p(x1,y0,z0));
        face(result,tile,glow,p(x1,y0,z1),p(x1,y1,z1),p(x0,y1,z1),p(x0,y0,z1));
        face(result,tile,glow,p(x0,y0,z1),p(x0,y1,z1),p(x0,y1,z0),p(x0,y0,z0));
        face(result,tile,glow,p(x1,y0,z0),p(x1,y1,z0),p(x1,y1,z1),p(x1,y0,z1));
        face(result,tile,glow,p(x0,y1,z0),p(x0,y1,z1),p(x1,y1,z1),p(x1,y1,z0));
        face(result,tile,glow,p(x0,y0,z1),p(x0,y0,z0),p(x1,y0,z0),p(x1,y0,z1));
    }
}
