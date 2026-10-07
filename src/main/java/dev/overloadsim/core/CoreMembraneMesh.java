package dev.overloadsim.core;

import java.util.ArrayList;
import java.util.List;

/** Two cached one-pixel-thick stepped annular membranes, with no frame-dependent noise. */
public final class CoreMembraneMesh {
    private static final List<List<IndustrialMachineMesh.Surface>> RINGS = List.of(build(23,21), build(26,24));
    private CoreMembraneMesh() {}
    public static List<IndustrialMachineMesh.Surface> ring(int index) { return RINGS.get(index); }

    private static List<IndustrialMachineMesh.Surface> build(int outer, int inner) {
        var pixels = new ArrayList<PixelAssemblyMesh.Box>();
        for (int y=-outer; y<outer; y++) {
            int start=Integer.MIN_VALUE;
            for (int x=-outer; x<=outer; x++) {
                double radius=(x+.5)*(x+.5)+(y+.5)*(y+.5);
                boolean filled=x<outer&&radius<=outer*outer&&radius>=inner*inner;
                if (filled&&start==Integer.MIN_VALUE) start=x;
                if (!filled&&start!=Integer.MIN_VALUE) {
                    pixels.add(new PixelAssemblyMesh.Box(0,false,start/16d,y/16d,0,x/16d,(y+1)/16d,1/16d));
                    start=Integer.MIN_VALUE;
                }
            }
        }
        return PixelAssemblyMesh.build(pixels);
    }
}
