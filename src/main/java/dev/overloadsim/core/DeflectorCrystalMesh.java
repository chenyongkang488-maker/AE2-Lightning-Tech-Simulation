package dev.overloadsim.core;

import java.util.ArrayList;
import java.util.List;
import dev.overloadsim.core.ReceiverShellMesh.Quad;
import dev.overloadsim.core.ReceiverShellMesh.Vertex;

/** Six-sided prism with two pointed ends; immutable cosmetic facets around the optical junction. */
public final class DeflectorCrystalMesh {
    private static final List<IndustrialMachineMesh.Surface> SURFACES=build();
    private DeflectorCrystalMesh(){}
    public static List<IndustrialMachineMesh.Surface> surfaces(){return SURFACES;}
    private static List<IndustrialMachineMesh.Surface> build(){
        var result=new ArrayList<IndustrialMachineMesh.Surface>();
        for(int i=0;i<6;i++){
            double a=i*Math.PI/3,b=(i+1)*Math.PI/3;
            double[] lo={.55*Math.cos(a),-.75,.55*Math.sin(a)},hi={lo[0],.75,lo[2]};
            double[] nextLo={.55*Math.cos(b),-.75,.55*Math.sin(b)},nextHi={nextLo[0],.75,nextLo[2]};
            add(result,lo,hi,nextHi,nextLo,i%2==0?52:53);
            add(result,hi,new double[]{0,1.35,0},nextHi,nextHi,i%2==0?52:53);
            add(result,lo,nextLo,new double[]{0,-1.35,0},new double[]{0,-1.35,0},i%2==0?53:52);
        }
        return List.copyOf(result);
    }
    private static void add(List<IndustrialMachineMesh.Surface> out,double[] a,double[] b,double[] c,double[] d,int tile){
        double ux=b[0]-a[0],uy=b[1]-a[1],uz=b[2]-a[2],vx=c[0]-a[0],vy=c[1]-a[1],vz=c[2]-a[2];
        double nx=uy*vz-uz*vy,ny=uz*vx-ux*vz,nz=ux*vy-uy*vx,length=Math.sqrt(nx*nx+ny*ny+nz*nz);
        out.add(new IndustrialMachineMesh.Surface(new Quad(vertex(a,nx/length,ny/length,nz/length,0,1),
                vertex(b,nx/length,ny/length,nz/length,0,0),vertex(c,nx/length,ny/length,nz/length,1,0),
                vertex(d,nx/length,ny/length,nz/length,1,1)),tile,true));
    }
    private static Vertex vertex(double[] p,double x,double y,double z,double u,double v){return new Vertex(p[0],p[1],p[2],x,y,z,u,v);}
}
