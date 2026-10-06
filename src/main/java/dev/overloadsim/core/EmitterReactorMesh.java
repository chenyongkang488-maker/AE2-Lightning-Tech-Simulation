package dev.overloadsim.core;

import java.util.ArrayList;
import java.util.List;

/** Formed emitter skin in right/up/forward coordinates, entirely on a 1/16-block lattice. */
public final class EmitterReactorMesh {
    public record Piece(String group, int tile, boolean glow,
            double x0,double y0,double z0,double x1,double y1,double z1) {}
    private static final List<Piece> PIECES = build();
    private static final List<IndustrialMachineMesh.Surface> SURFACES = mesh();
    private EmitterReactorMesh() {}
    public static List<Piece> pieces() { return PIECES; }
    public static List<IndustrialMachineMesh.Surface> surfaces() { return SURFACES; }

    private static List<Piece> build() {
        var p = new ArrayList<Piece>();
        ring(p,"01 · 阶梯陶瓷外壳",5,false,1.75,0,-2.375,-1.5);
        ring(p,"01 · 阶梯陶瓷外壳",7,false,1.875,1.375,-2.125,-1.75);
        ring(p,"01 · 阶梯陶瓷外壳",5,false,2,1.75,-1.5,1.25);
        ring(p,"02 · 分段反应堆装甲",7,false,2.375,1.5,1.25,1.625);
        ring(p,"02 · 分段反应堆装甲",5,false,2.375,2.125,1.625,1.75);
        ring(p,"03 · 亮粉同心导光环",41,true,2.125,1.9375,1.75,1.8125);
        ring(p,"02 · 分段反应堆装甲",7,false,1.9375,1.5,1.8125,1.9375);
        ring(p,"02 · 分段反应堆装甲",5,false,1.75,1.5,1.9375,2.125);
        ring(p,"03 · 亮粉同心导光环",41,true,1.5,1.25,2.125,2.1875);
        ring(p,"02 · 分段反应堆装甲",7,false,1.25,1.125,2.1875,2.3125);
        // The real optical port ends at z=2.5. The cap sits just ahead of it;
        // the existing three-block laser still starts at the same world position.
        ring(p,"04 · 像素发光芯",42,true,1.125,0,2.5,2.5625);
        ring(p,"03 · 亮粉同心导光环",41,true,1.125,1,2.5625,2.625);
        add(p,"04 · 像素发光芯",40,true,-.3125,-.3125,2.5625,.3125,.3125,2.6875);
        // Eight rectangular capacitor pods deliberately retain visible voxel steps.
        for (int[] xy : new int[][]{{0,32},{23,23},{32,0},{23,-23},{0,-32},{-23,-23},{-32,0},{-23,23}}) {
            double x=xy[0]/16.0,y=xy[1]/16.0;
            add(p,"02 · 分段反应堆装甲",44,false,x-.25,y-.25,-1.375,x+.25,y+.25,1.5);
            add(p,"05 · 散热槽与螺栓",45,false,x-.1875,y-.1875,1.5,x+.1875,y+.1875,1.75);
            add(p,"03 · 亮粉同心导光环",41,true,x-.125,y-.125,.125,x+.125,y+.125,1.5625);
            add(p,"05 · 散热槽与螺栓",34,false,x-.125,y-.125,-1.5625,x+.125,y+.125,-1.375);
        }
        // Raised dark clamps break the rings into readable mechanical sections.
        for(int sign:new int[]{-1,1}) {
            add(p,"05 · 散热槽与螺栓",44,false,sign<0?-2.25:1.6875,-.125,1.8125,
                    sign<0?-1.6875:2.25,.125,2);
            add(p,"05 · 散热槽与螺栓",44,false,-.125,sign<0?-2.25:1.6875,1.8125,
                    .125,sign<0?-1.6875:2.25,2);
        }
        return List.copyOf(p);
    }

    /** Merge adjacent cells along each row, retaining stair-step rather than smooth circular edges. */
    private static void ring(List<Piece> p,String group,int tile,boolean glow,double outer,double inner,double z0,double z1) {
        double step=.25;
        int radius=(int)Math.ceil(outer/step);
        for(int y=-radius;y<radius;y++) {
            int start=Integer.MIN_VALUE;
            for(int x=-radius;x<=radius;x++) {
                double r2=Math.pow((x+.5)*step,2)+Math.pow((y+.5)*step,2);
                boolean filled=x<radius&&r2<=outer*outer&&r2>=inner*inner;
                if(filled&&start==Integer.MIN_VALUE)start=x;
                if(!filled&&start!=Integer.MIN_VALUE) {
                    add(p,group,tile,glow,start*step,y*step,z0,x*step,(y+1)*step,z1);
                    start=Integer.MIN_VALUE;
                }
            }
        }
    }
    private static void add(List<Piece> p,String group,int tile,boolean glow,
            double x0,double y0,double z0,double x1,double y1,double z1) {
        p.add(new Piece(group,tile,glow,x0,y0,z0,x1,y1,z1));
    }
    private static List<IndustrialMachineMesh.Surface> mesh() {
        return PixelAssemblyMesh.build(PIECES.stream().map(p->new PixelAssemblyMesh.Box(p.tile,p.glow,p.x0,p.y0,p.z0,p.x1,p.y1,p.z1)).toList());
    }
}
