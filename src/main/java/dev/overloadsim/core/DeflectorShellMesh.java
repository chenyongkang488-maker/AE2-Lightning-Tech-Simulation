package dev.overloadsim.core;

import java.util.ArrayList;
import java.util.List;

/** Four beam-width horizontal collars; the upper shell has no optical endpoint. */
public final class DeflectorShellMesh {
    private static final List<IndustrialMachineMesh.Surface> SURFACES=build();
    private DeflectorShellMesh(){}
    public static List<IndustrialMachineMesh.Surface> surfaces(){return SURFACES;}
    private static List<IndustrialMachineMesh.Surface> build(){
        var boxes=new ArrayList<PixelAssemblyMesh.Box>();
        for(int x=-14;x<14;x++)for(int y=-14;y<14;y++)for(int z=-14;z<14;z++){
            double a=(x+.5)/4,b=(y+.5)/4,c=(z+.5)/4,d=a*a+b*b+c*c;
            if(d>3.35*3.35||d<2.9*2.9)continue;
            if(Math.abs(a)>2.25&&b*b+c*c<1.7*1.7||Math.abs(c)>2.25&&a*a+b*b<1.7*1.7)continue;
            double rib=Math.min(Math.abs(a),Math.min(Math.abs(b),Math.abs(c)));int tile=rib<.14?6:rib<.39?7:5;
            boxes.add(new PixelAssemblyMesh.Box(tile,tile==6,x/4d,y/4d,z/4d,(x+1)/4d,(y+1)/4d,(z+1)/4d));
        }
        // Pixel collars occupy their own depth instead of layering on the shell.
        for(int axis:new int[]{0,2})for(int side:new int[]{-1,1})for(int u=-8;u<8;u++)for(int v=-8;v<8;v++){
            double a=(u+.5)/4,b=(v+.5)/4,r=a*a+b*b;
            if(r<1.7*1.7||r>1.95*1.95)continue;
            double from=side<0?-3.5:2.75,to=side<0?-2.75:3.5;
            int tile=r<1.8*1.8?6:44;
            boxes.add(axis==0?new PixelAssemblyMesh.Box(tile,tile==6,from,u/4d,v/4d,to,(u+1)/4d,(v+1)/4d)
                    :new PixelAssemblyMesh.Box(tile,tile==6,u/4d,v/4d,from,(u+1)/4d,(v+1)/4d,to));
        }
        // Controller location is supplied by the saved layout, including old vertical machines.
        return PixelAssemblyMesh.build(boxes);
    }
}
