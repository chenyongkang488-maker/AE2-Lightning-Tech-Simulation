package dev.overloadsim.core;

import java.util.ArrayList;
import java.util.List;
import dev.overloadsim.core.ReceiverShellMesh.Vertex;
import dev.overloadsim.core.ReceiverShellMesh.Quad;

/** Fixed pixel geometry relative to the seven-block chamber's centre: right/up/inward. */
public final class WorkstationVisualMesh {
    private static final List<IndustrialMachineMesh.Surface> STATION = build();
    private static final List<IndustrialMachineMesh.Surface> SHELL = buildShell();
    private static final List<Quad> GLASS = buildGlass();
    private WorkstationVisualMesh() {}
    public static List<IndustrialMachineMesh.Surface> surfaces() { return STATION; }
    public static List<IndustrialMachineMesh.Surface> shellSurfaces() { return SHELL; }
    public static List<Quad> glass() { return GLASS; }

    private static List<IndustrialMachineMesh.Surface> build() {
        var result = new ArrayList<IndustrialMachineMesh.Surface>();
        // The receiving manifold sits inside the physical rear wall; four crystals mount flush to it.
        for (int sign : new int[]{-1,1}) {
            double edge=sign<0?-.625:.5;
            glowBox(result,41,edge,-.625,2.375,edge+.125,.625,2.5);
            glowBox(result,41,-.5,edge,2.375,.5,edge+.125,2.5);
            double low=sign<0?-2.1875:.6875,high=sign<0?-.6875:2.1875;
            IndustrialMachineMesh.box(result,38,low,-.1875,2.4375,high,.1875,2.5);
            glowBox(result,41,low,-.03125,2.40625,high,.03125,2.4375);
        }
        for(int sx:new int[]{-1,1})for(int sy:new int[]{-1,1})
            for(int cornerX:new int[]{-1,1})for(int cornerY:new int[]{-1,1}) {
                double x=sx*1.125+cornerX*.40625,y=sy*1.125+cornerY*.40625;
                IndustrialMachineMesh.box(result,44,x-.0625,y-.0625,2.4375,x+.0625,y+.0625,2.5);
            }
        // Raised workstation plinth, distinct from the simulation chamber's floating sphere.
        IndustrialMachineMesh.box(result,5,-1.625,-2.5,-1.625,1.625,-2.25,1.625);
        IndustrialMachineMesh.box(result,44,-1.4375,-2.25,-1.4375,1.4375,-2.125,1.4375);
        IndustrialMachineMesh.box(result,7,-1.3125,-2.125,-1.3125,1.3125,-2,1.3125);
        for (int sign : new int[]{-1,1}) {
            double x0 = sign < 0 ? -1.5 : 1.4375;
            IndustrialMachineMesh.box(result,32,x0,-2.1875,-1.4375,x0+.0625,-2.125,1.4375);
            double z0 = sign < 0 ? -1.5 : 1.4375;
            IndustrialMachineMesh.box(result,32,-1.4375,-2.1875,z0,1.4375,-2.125,z0+.0625);
        }
        // Nine visible holders align with the menu's actual 3x3 material slots.
        for (int x=-1;x<=1;x++) for (int z=-1;z<=1;z++)
            IndustrialMachineMesh.box(result,38,x*.8125-.3125,-2,z*.8125-.3125,
                    x*.8125+.3125,-1.9375,z*.8125+.3125);
        return List.copyOf(result);
    }

    private static List<IndustrialMachineMesh.Surface> buildShell() {
        var result=new ArrayList<IndustrialMachineMesh.Surface>();
        var floor=new ArrayList<IndustrialMachineMesh.Surface>();
        IndustrialMachineMesh.box(floor,32,-3.5,-3.5,-3.5,3.5,-2.5,3.5);
        // Only the front-facing floor plate opens for the physical controller. The opaque caps stay whole.
        for(var surface:floor)if(surface.quad().a().nz()!=-1)result.add(surface);
        result.add(new IndustrialMachineMesh.Surface(front(-3.5,-3.5,-.5,-2.5,-3.5),32,false));
        result.add(new IndustrialMachineMesh.Surface(front(.5,-3.5,3.5,-2.5,-3.5),32,false));
        IndustrialMachineMesh.box(result,32,-3.5,2.5,-3.5,3.5,3.5,3.5);
        for(int sx:new int[]{-1,1})for(int sz:new int[]{-1,1}) {
            double x=sx<0?-3.5:2.5,z=sz<0?-3.5:2.5;
            IndustrialMachineMesh.box(result,32,x,-2.5,z,x+1,2.5,z+1);
            // Continuous light-grey corner panels inside a narrow stepped navy rim.
            frontPanel(result,x+.03125,-2.46875,x+.96875,2.46875,sz);
            sidePanel(result,z+.03125,-2.46875,z+.96875,2.46875,sx);
            for(int end:new int[]{-1,1}) {
                double y=end<0?-2.3125:2.21875;
                double z0=sz<0?-3.5625:3.546875;
                IndustrialMachineMesh.box(result,44,x+.1875,y,z0,x+.28125,y+.09375,z0+.015625);
            }
        }
        // Opaque rear wall surrounds its one real optical aperture, including both outward and inward faces.
        IndustrialMachineMesh.box(result,38,-2.5,-2.5,2.5,-.5,2.5,3.5);
        IndustrialMachineMesh.box(result,38,.5,-2.5,2.5,2.5,2.5,3.5);
        IndustrialMachineMesh.box(result,38,-.5,-2.5,2.5,.5,-.5,3.5);
        IndustrialMachineMesh.box(result,38,-.5,.5,2.5,.5,2.5,3.5);
        // The receiving face repeats the emitter's stepped rings and capacitor fittings,
        // compressed into a shallow intake on the wall. Its real central aperture stays open.
        intakeRing(result,7,false,1.625,1.3125,3.5,3.53125);
        intakeRing(result,5,false,1.3125,1,3.5,3.546875);
        intakeRing(result,41,true,1,.8125,3.546875,3.5625);
        for(int[] xy:new int[][]{{0,28},{20,20},{28,0},{20,-20},{0,-28},{-20,-20},{-28,0},{-20,20}}) {
            double x=xy[0]/16.0,y=xy[1]/16.0;
            IndustrialMachineMesh.box(result,44,x-.125,y-.125,3.5,x+.125,y+.125,3.546875);
            glowBox(result,41,x-.0625,y-.0625,3.546875,x+.0625,y+.0625,3.5625);
        }
        // Dark narrow horizontal trims run continuously along all four faces.
        // The lower front remains split around the physical controller bay.
        for(int end:new int[]{-1,1}) {
            double low=end<0?-3.5:2.5,high=end<0?-2.5:3.5;
            for(int direction:new int[]{-1,1}) {
                if(end<0&&direction<0) {
                    frontPanel(result,-3.46875,low+.03125,-.53125,high-.03125,direction);
                    frontPanel(result,.53125,low+.03125,3.46875,high-.03125,direction);
                } else frontPanel(result,-3.46875,low+.03125,3.46875,high-.03125,direction);
                sidePanel(result,-2.46875,low+.03125,2.46875,high-.03125,direction);
            }
            // Paired recessed ventilation grilles: small grey slats over navy backing.
            for(int side:new int[]{-1,1}) {
                double x0=side<0?-2.25:.75;
                frontVent(result,x0,low+.25,x0+1.5,low+.75);
            }
        }
        return List.copyOf(result);
    }

    private static void frontPanel(List<IndustrialMachineMesh.Surface> result,
            double x0,double y0,double x1,double y1,int direction) {
        double z=direction<0?-3.53125:3.5;
        IndustrialMachineMesh.box(result,5,x0,y0,z,x1,y1,z+.03125);
        z=direction<0?-3.546875:3.53125;
        IndustrialMachineMesh.box(result,7,x0+.0625,y0+.0625,z,x1-.0625,y1-.0625,z+.015625);
        z=direction<0?-3.5546875:3.546875;
        IndustrialMachineMesh.box(result,38,x0+.09375,y0+.09375,z,x1-.09375,y1-.09375,z+.0078125);
    }

    private static void sidePanel(List<IndustrialMachineMesh.Surface> result,
            double z0,double y0,double z1,double y1,int direction) {
        double x=direction<0?-3.53125:3.5;
        IndustrialMachineMesh.box(result,5,x,y0,z0,x+.03125,y1,z1);
        x=direction<0?-3.546875:3.53125;
        IndustrialMachineMesh.box(result,7,x,y0+.0625,z0+.0625,x+.015625,y1-.0625,z1-.0625);
        x=direction<0?-3.5546875:3.546875;
        IndustrialMachineMesh.box(result,38,x,y0+.09375,z0+.09375,x+.0078125,y1-.09375,z1-.09375);
    }

    private static void frontVent(List<IndustrialMachineMesh.Surface> result,
            double x0,double y0,double x1,double y1) {
        IndustrialMachineMesh.box(result,5,x0,y0,-3.55859375,x1,y1,-3.546875);
        for(int slat=0;slat<4;slat++) {
            double y=y0+.0625+slat*.109375;
            IndustrialMachineMesh.box(result,7,x0+.0625,y,-3.5625,x1-.0625,y+.03125,-3.55859375);
        }
    }

    private static List<Quad> buildGlass() {
        var panes=new ArrayList<Quad>();
        addPane(panes,front(-2.5,-2.5,2.5,2.5,-3.46875));
        for(int side:new int[]{-1,1}) {
            double x=side*3.46875;
            var a=vertex(x,-2.5,-2.5,side,0,0,0,0);
            var b=vertex(x,-2.5,2.5,side,0,0,1,0);
            var c=vertex(x,2.5,2.5,side,0,0,1,1);
            var d=vertex(x,2.5,-2.5,side,0,0,0,1);
            addPane(panes,side>0?new Quad(d,c,b,a):new Quad(a,b,c,d));
        }
        // The back aperture keeps a clear exterior lens instead of turning the opaque receiving wall into glass.
        addPane(panes,front(-.5,-.5,.5,.5,3.46875));
        return List.copyOf(panes);
    }

    private static Quad front(double x0,double y0,double x1,double y1,double z) {
        return new Quad(vertex(x0,y0,z,0,0,-1,0,0),vertex(x0,y1,z,0,0,-1,0,1),
                vertex(x1,y1,z,0,0,-1,1,1),vertex(x1,y0,z,0,0,-1,1,0));
    }

    private static Vertex vertex(double x,double y,double z,double nx,double ny,double nz,double u,double v) {
        return new Vertex(x,y,z,nx,ny,nz,u,v);
    }

    private static void addPane(List<Quad> panes,Quad quad) {
        panes.add(quad);
        panes.add(new Quad(reverse(quad.d()),reverse(quad.c()),reverse(quad.b()),reverse(quad.a())));
    }

    private static Vertex reverse(Vertex vertex) {
        return new Vertex(vertex.x(),vertex.y(),vertex.z(),-vertex.nx(),-vertex.ny(),-vertex.nz(),vertex.u(),vertex.v());
    }

    private static void glowBox(List<IndustrialMachineMesh.Surface> result,int tile,double x0,double y0,double z0,
            double x1,double y1,double z1) {
        var faces=new ArrayList<IndustrialMachineMesh.Surface>();
        IndustrialMachineMesh.box(faces,tile,x0,y0,z0,x1,y1,z1);
        for(var face:faces)result.add(new IndustrialMachineMesh.Surface(face.quad(),tile,true));
    }

    private static void intakeRing(List<IndustrialMachineMesh.Surface> result,int tile,boolean glow,
            double outer,double inner,double z0,double z1) {
        double step=.25;
        int radius=(int)Math.ceil(outer/step);
        for(int y=-radius;y<radius;y++) {
            int start=Integer.MIN_VALUE;
            for(int x=-radius;x<=radius;x++) {
                double distance=Math.pow((x+.5)*step,2)+Math.pow((y+.5)*step,2);
                boolean filled=x<radius&&distance<=outer*outer&&distance>=inner*inner;
                if(filled&&start==Integer.MIN_VALUE)start=x;
                if(!filled&&start!=Integer.MIN_VALUE) {
                    if(glow)glowBox(result,tile,start*step,y*step,z0,x*step,(y+1)*step,z1);
                    else IndustrialMachineMesh.box(result,tile,start*step,y*step,z0,x*step,(y+1)*step,z1);
                    start=Integer.MIN_VALUE;
                }
            }
        }
    }
}
