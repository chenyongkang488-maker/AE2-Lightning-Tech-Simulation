package dev.overloadsim.core;

import java.util.List;
import java.util.function.ToIntFunction;
import java.util.function.Function;
import java.util.function.Predicate;

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
        return sample(quad,p -> world(p.x(),p.y(),p.z(),cx,cy,cz,quarterTurns),lightAt,p -> false);
    }

    /** Arbitrary machine basis; inset skins may lie inside their hidden opaque construction blocks. */
    public static int sample(ReceiverShellMesh.Quad quad,Function<Point,Point> world,
            ToIntFunction<Point> lightAt,Predicate<Point> opaqueAt) {
        var vertices=List.of(quad.a(),quad.b(),quad.c(),quad.d());
        double x=0,y=0,z=0;
        int block=0,sky=0;
        // Broad panels span many blocks. Their centre and four corners see the actual local environment.
        for(var p:vertices) {
            x+=p.x()/4;y+=p.y()/4;z+=p.z()/4;
            int light=visibleLight(p.x(),p.y(),p.z(),p.nx(),p.ny(),p.nz(),world,lightAt,opaqueAt);
            block=Math.max(block,light&65535);sky=Math.max(sky,light>>>16);
        }
        var normal=quad.a();
        int centre=visibleLight(x,y,z,normal.nx(),normal.ny(),normal.nz(),world,lightAt,opaqueAt);
        return Math.max(block,centre&65535)|(Math.max(sky,centre>>>16)<<16);
    }

    private static int visibleLight(double x,double y,double z,double nx,double ny,double nz,
            Function<Point,Point> world,ToIntFunction<Point> lightAt,Predicate<Point> opaqueAt) {
        Point sample=world.apply(new Point(x+nx/32,y+ny/32,z+nz/32));
        // A hidden frame still blocks world light. Leave that one voxel along the
        // face normal, without making the armour emissive or probing distant lamps.
        for(double offset=.25;offset<=1&&opaqueAt.test(sample);offset+=.25)
            sample=world.apply(new Point(x+nx*offset,y+ny*offset,z+nz*offset));
        return lightAt.applyAsInt(sample);
    }
}
