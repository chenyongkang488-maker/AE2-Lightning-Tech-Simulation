package dev.overloadsim.core;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PixelAssemblyMeshTest {
    @Test void intersectingArmourHasOnlyExternalFaces() throws Exception {
        var box=Class.forName("dev.overloadsim.core.PixelAssemblyMesh$Box");
        var constructor=box.getConstructor(int.class,boolean.class,double.class,double.class,double.class,double.class,double.class,double.class);
        var mesh=(List<IndustrialMachineMesh.Surface>)Class.forName("dev.overloadsim.core.PixelAssemblyMesh")
                .getMethod("build",List.class).invoke(null,List.of(
                        constructor.newInstance(5,false,0d,0d,0d,1d,1d,1d),
                        constructor.newInstance(6,true,.5,0d,0d,1.5,1d,1d)));
        double area=0;
        for(var face:mesh){
            var q=face.quad();double ax=q.b().x()-q.a().x(),ay=q.b().y()-q.a().y(),az=q.b().z()-q.a().z();
            double bx=q.d().x()-q.a().x(),by=q.d().y()-q.a().y(),bz=q.d().z()-q.a().z();
            area+=Math.sqrt(Math.pow(ay*bz-az*by,2)+Math.pow(az*bx-ax*bz,2)+Math.pow(ax*by-ay*bx,2));
            double x=(q.a().x()+q.b().x()+q.c().x()+q.d().x())/4;
            assertFalse(x>.00001&&x<1.49999&&Math.abs(q.a().nx())>.99,"An intersection must not retain an internal end plate");
        }
        assertEquals(8,area,1e-8,"The union is a 1.5 by 1 by 1 prism, with no doubled faces");
        assertTrue(mesh.stream().anyMatch(f->f.glow()&&f.tile()==6));
    }

    @Test void emitterHasNoOverlappingCoplanarFaces() {
        var faces=EmitterReactorMesh.surfaces();
        for(int i=0;i<faces.size();i++)for(int j=i+1;j<faces.size();j++){
            var a=faces.get(i).quad();var b=faces.get(j).quad();
            int axis=Math.abs(a.a().nx())>.99?0:Math.abs(a.a().ny())>.99?1:2;
            int other=Math.abs(b.a().nx())>.99?0:Math.abs(b.a().ny())>.99?1:2;
            if(axis!=other||Math.abs(coord(a.a(),axis)-coord(b.a(),axis))>1e-8)continue;
            int u=(axis+1)%3,v=(axis+2)%3;
            assertFalse(overlap(a,b,u)>1e-8&&overlap(a,b,v)>1e-8,"Overlapping armour planes cause depth flicker");
        }
    }
    private static double overlap(ReceiverShellMesh.Quad a,ReceiverShellMesh.Quad b,int axis){
        return Math.min(max(a,axis),max(b,axis))-Math.max(min(a,axis),min(b,axis));
    }
    private static double min(ReceiverShellMesh.Quad q,int axis){return List.of(q.a(),q.b(),q.c(),q.d()).stream().mapToDouble(p->coord(p,axis)).min().orElseThrow();}
    private static double max(ReceiverShellMesh.Quad q,int axis){return List.of(q.a(),q.b(),q.c(),q.d()).stream().mapToDouble(p->coord(p,axis)).max().orElseThrow();}
    private static double coord(ReceiverShellMesh.Vertex v,int axis){return axis==0?v.x():axis==1?v.y():v.z();}
}
