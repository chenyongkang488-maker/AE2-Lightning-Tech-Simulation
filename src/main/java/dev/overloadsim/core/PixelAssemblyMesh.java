package dev.overloadsim.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;

/** Union of pixel-grid armour boxes. Intersections carry no hidden or doubled faces. */
public final class PixelAssemblyMesh {
    public record Box(int tile,boolean glow,double x0,double y0,double z0,double x1,double y1,double z1) {}
    private PixelAssemblyMesh(){}
    public static List<IndustrialMachineMesh.Surface> build(List<Box> boxes){
        if(boxes.isEmpty())return List.of();
        int[][] bounds=new int[3][];
        for(int axis=0;axis<3;axis++){
            var coordinates=new TreeSet<Integer>();
            for(var box:boxes){coordinates.add(pixel(min(box,axis)));coordinates.add(pixel(max(box,axis)));}
            bounds[axis]=coordinates.stream().mapToInt(Integer::intValue).toArray();
        }
        int[] size={bounds[0].length-1,bounds[1].length-1,bounds[2].length-1};
        long count=(long)size[0]*size[1]*size[2];
        if(count>4_000_000)throw new IllegalArgumentException("Armour mesh exceeds fixed cell budget");
        int[] cells=new int[(int)count];
        for(var box:boxes){
            if(box.tile()<0||box.tile()>=64)throw new IllegalArgumentException("Atlas tile");
            int[] from=new int[3],to=new int[3];
            for(int axis=0;axis<3;axis++){
                from[axis]=Arrays.binarySearch(bounds[axis],pixel(min(box,axis)));
                to[axis]=Arrays.binarySearch(bounds[axis],pixel(max(box,axis)));
                if(from[axis]>=to[axis])throw new IllegalArgumentException("Empty armour box");
            }
            int material=1+box.tile()*2+(box.glow()?1:0);
            for(int x=from[0];x<to[0];x++)for(int y=from[1];y<to[1];y++)for(int z=from[2];z<to[2];z++)cells[index(x,y,z,size)]=material;
        }
        var result=new ArrayList<IndustrialMachineMesh.Surface>();
        for(int axis=0;axis<3;axis++)for(int side:new int[]{-1,1}){
            int u=(axis+1)%3,v=(axis+2)%3;
            for(int depth=0;depth<size[axis];depth++){
                int[] mask=new int[size[u]*size[v]],position=new int[3];position[axis]=depth;
                for(int y=0;y<size[v];y++)for(int x=0;x<size[u];x++){
                    position[u]=x;position[v]=y;int material=cells[index(position[0],position[1],position[2],size)];
                    int neighbour=depth+side;boolean external=neighbour<0||neighbour>=size[axis];
                    if(!external){position[axis]=neighbour;external=cells[index(position[0],position[1],position[2],size)]==0;position[axis]=depth;}
                    if(material!=0&&external)mask[y*size[u]+x]=material;
                }
                for(int y=0;y<size[v];y++)for(int x=0;x<size[u];x++){
                    int material=mask[y*size[u]+x];if(material==0)continue;
                    int width=1,height=1;
                    while(x+width<size[u]&&mask[y*size[u]+x+width]==material)width++;
                    rows:while(y+height<size[v]){
                        for(int dx=0;dx<width;dx++)if(mask[(y+height)*size[u]+x+dx]!=material)break rows;
                        height++;
                    }
                    for(int dy=0;dy<height;dy++)Arrays.fill(mask,(y+dy)*size[u]+x,(y+dy)*size[u]+x+width,0);
                    var a=vertex(bounds,axis,u,v,depth+(side>0?1:0),x,y,side,0,1);
                    var b=vertex(bounds,axis,u,v,depth+(side>0?1:0),x+width,y,side,1,1);
                    var c=vertex(bounds,axis,u,v,depth+(side>0?1:0),x+width,y+height,side,1,0);
                    var d=vertex(bounds,axis,u,v,depth+(side>0?1:0),x,y+height,side,0,0);
                    result.add(new IndustrialMachineMesh.Surface(side>0?new ReceiverShellMesh.Quad(a,b,c,d):new ReceiverShellMesh.Quad(d,c,b,a),(material-1)/2,(material-1)%2!=0));
                }
            }
        }
        return List.copyOf(result);
    }
    private static ReceiverShellMesh.Vertex vertex(int[][] bounds,int axis,int u,int v,int depth,int x,int y,int side,double tu,double tv){
        double[] p=new double[3],n=new double[3];p[axis]=bounds[axis][depth]/16d;p[u]=bounds[u][x]/16d;p[v]=bounds[v][y]/16d;n[axis]=side;
        return new ReceiverShellMesh.Vertex(p[0],p[1],p[2],n[0],n[1],n[2],tu,tv);
    }
    private static int index(int x,int y,int z,int[] size){return (x*size[1]+y)*size[2]+z;}
    private static int pixel(double value){if(!Double.isFinite(value)||Math.abs(value)>32||Math.abs(value*16-Math.rint(value*16))>1e-7)throw new IllegalArgumentException("Armour coordinates must use the pixel grid");return (int)Math.rint(value*16);}
    private static double min(Box b,int axis){return axis==0?b.x0:axis==1?b.y0:b.z0;}
    private static double max(Box b,int axis){return axis==0?b.x1:axis==1?b.y1:b.z1;}
}
