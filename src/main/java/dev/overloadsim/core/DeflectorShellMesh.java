package dev.overloadsim.core;

import java.util.ArrayList;
import java.util.List;

/** Merged pixel sphere, with ceramic armour and three embedded luminous ribs. */
public final class DeflectorShellMesh {
    private static final List<IndustrialMachineMesh.Surface> SURFACES=build();
    private DeflectorShellMesh(){}
    public static List<IndustrialMachineMesh.Surface> surfaces(){return SURFACES;}
    private static List<IndustrialMachineMesh.Surface> build(){
        var boxes=new ArrayList<PixelAssemblyMesh.Box>();
        for(int x=-14;x<14;x++)for(int y=-14;y<14;y++)for(int z=-14;z<14;z++){
            double a=(x+.5)/4,b=(y+.5)/4,c=(z+.5)/4,d=a*a+b*b+c*c;
            if(d>3.35*3.35||d<2.9*2.9)continue;
            double rib=Math.min(Math.abs(a),Math.min(Math.abs(b),Math.abs(c)));int tile=rib<.14?6:rib<.39?7:5;
            boxes.add(new PixelAssemblyMesh.Box(tile,tile==6,x/4d,y/4d,z/4d,(x+1)/4d,(y+1)/4d,(z+1)/4d));
        }
        var mesh=PixelAssemblyMesh.build(boxes);
        for(int axis=0;axis<3;axis++)for(int side:new int[]{-1,1})mesh=IndustrialMachineMesh.withoutCell(mesh,axis==0?side*3:0,axis==1?side*3:0,axis==2?side*3:0);
        return mesh;
    }
}
