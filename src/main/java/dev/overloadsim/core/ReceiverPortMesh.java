package dev.overloadsim.core;

import dev.overloadsim.core.IndustrialMachineMesh.Surface;
import dev.overloadsim.core.ReceiverShellMesh.Quad;
import dev.overloadsim.core.ReceiverShellMesh.Vertex;
import java.util.ArrayList;
import java.util.List;

/** Formed-only lenses fitted into the spherical receiver; the real optical blocks keep their role. */
public final class ReceiverPortMesh {
    private static final List<List<Surface>> ARMOUR = armour();
    private static final List<List<Quad>> GLASS = lenses();
    private ReceiverPortMesh() {}

    public static List<Surface> surfaces(int axis,int sign) { return ARMOUR.get(index(axis,sign)); }
    public static List<Quad> glass(int axis,int sign) { return GLASS.get(index(axis,sign)); }

    private static int index(int axis,int sign) {
        if(axis<0||axis>2||Math.abs(sign)!=1)throw new IllegalArgumentException("aperture direction");
        return axis*2+(sign>0?1:0);
    }

    private static List<List<Surface>> armour() {
        var boxes=List.of(new PixelAssemblyMesh.Box(32,false,-.5625,-.5625,3.0625,-.375,.5625,3.3125),
                new PixelAssemblyMesh.Box(32,false,.375,-.5625,3.0625,.5625,.5625,3.3125),
                new PixelAssemblyMesh.Box(32,false,-.375,-.5625,3.0625,.375,-.375,3.3125),
                new PixelAssemblyMesh.Box(32,false,-.375,.375,3.0625,.375,.5625,3.3125));
        var base=new ArrayList<>(PixelAssemblyMesh.build(boxes));
        for(int side:new int[]{-1,1})
            IndustrialMachineMesh.box(base,35,side*.46875-.015625,-.40625,3.3125,
                    side*.46875+.015625,.40625,3.328125);
        var variants=new ArrayList<List<Surface>>();
        for(int axis=0;axis<3;axis++)for(int sign:new int[]{-1,1}) {
            var result=new ArrayList<Surface>();
            for(var face:base)result.add(new Surface(turn(face.quad(),axis,sign),face.tile(),face.glow()));
            variants.add(List.copyOf(result));
        }
        return List.copyOf(variants);
    }

    private static List<List<Quad>> lenses() {
        var front=new Quad(new Vertex(-.375,-.375,3.28125,0,0,1,0,1),
                new Vertex(.375,-.375,3.28125,0,0,1,1,1),
                new Vertex(.375,.375,3.28125,0,0,1,1,0),
                new Vertex(-.375,.375,3.28125,0,0,1,0,0));
        var back=new Quad(reverse(front.d()),reverse(front.c()),reverse(front.b()),reverse(front.a()));
        var variants=new ArrayList<List<Quad>>();
        for(int axis=0;axis<3;axis++)for(int sign:new int[]{-1,1})
            variants.add(List.of(turn(front,axis,sign),turn(back,axis,sign)));
        return List.copyOf(variants);
    }

    private static Vertex reverse(Vertex v) {
        return new Vertex(v.x(),v.y(),v.z(),-v.nx(),-v.ny(),-v.nz(),v.u(),v.v());
    }
    private static Quad turn(Quad q,int axis,int sign) {
        return new Quad(turn(q.a(),axis,sign),turn(q.b(),axis,sign),turn(q.c(),axis,sign),turn(q.d(),axis,sign));
    }
    private static Vertex turn(Vertex p,int axis,int sign) {
        // A proper half-turn, rather than a reflection, preserves the winding of negative-axis ports.
        double x=p.x()*sign,y=p.y(),z=p.z()*sign,nx=p.nx()*sign,ny=p.ny(),nz=p.nz()*sign;
        return switch(axis) {
            case 0 -> new Vertex(z,x,y,nz,nx,ny,p.u(),p.v());
            case 1 -> new Vertex(x,z,-y,nx,nz,-ny,p.u(),p.v());
            default -> new Vertex(x,y,z,nx,ny,nz,p.u(),p.v());
        };
    }
}
