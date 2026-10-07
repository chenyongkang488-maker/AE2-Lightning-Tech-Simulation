package dev.overloadsim.core;

import java.util.List;
import java.util.function.ToIntFunction;

/** Samples the visible side of a merged surface instead of the buried controller block. */
public final class WorkstationRenderLighting {
    public record Point(double x,double y,double z) {}
    private WorkstationRenderLighting() {}

    public static Point world(double x,double y,double z,double cx,double cy,double cz,int quarterTurns) {
        return switch(Math.floorMod(quarterTurns,4)) {
            case 0 -> new Point(cx+x,cy+y,cz+z);
            case 1 -> new Point(cx-z,cy+y,cz+x);
            case 2 -> new Point(cx-x,cy+y,cz-z);
            default -> new Point(cx+z,cy+y,cz-x);
        };
    }

    public static int sample(ReceiverShellMesh.Quad quad,double cx,double cy,double cz,int quarterTurns,
            ToIntFunction<Point> lightAt) {
        var vertices=List.of(quad.a(),quad.b(),quad.c(),quad.d());
        double x=0,y=0,z=0;
        int block=0,sky=0;
        // Broad panels span many blocks. Their centre and four corners see the actual local environment.
        for(var p:vertices) {
            x+=p.x()/4;y+=p.y()/4;z+=p.z()/4;
            int light=lightAt.applyAsInt(world(p.x()+p.nx()/32,p.y()+p.ny()/32,p.z()+p.nz()/32,cx,cy,cz,quarterTurns));
            block=Math.max(block,light&65535);sky=Math.max(sky,light>>>16);
        }
        var normal=quad.a();
        int centre=lightAt.applyAsInt(world(x+normal.nx()/32,y+normal.ny()/32,z+normal.nz()/32,cx,cy,cz,quarterTurns));
        return Math.max(block,centre&65535)|(Math.max(sky,centre>>>16)<<16);
    }
}
