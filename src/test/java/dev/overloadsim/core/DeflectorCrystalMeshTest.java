package dev.overloadsim.core;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DeflectorCrystalMeshTest {
    @Test void crystalHasClosedFacetedPrismAndBothPointedTips() {
        var mesh=assertDoesNotThrow(()->Class.forName("dev.overloadsim.core.DeflectorCrystalMesh").getMethod("surfaces").invoke(null),"central crystal geometry exists");
        @SuppressWarnings("unchecked") var faces=(List<IndustrialMachineMesh.Surface>)mesh;
        assertTrue(faces.size()>=12&&faces.size()<=24,"bounded faceted crystal");
        var edges=new HashMap<String,Integer>();
        double low=Double.POSITIVE_INFINITY,high=Double.NEGATIVE_INFINITY;
        for(var face:faces){
            var q=face.quad();var ring=new ArrayList<>(List.of(q.a(),q.b(),q.c(),q.d()));
            for(int i=ring.size()-1;i>0;i--)if(key(ring.get(i)).equals(key(ring.get(i-1))))ring.remove(i);
            if(key(ring.getLast()).equals(key(ring.getFirst())))ring.removeLast();
            assertTrue(ring.size()>=3);
            for(int i=0;i<ring.size();i++){
                var v=ring.get(i);low=Math.min(low,v.y());high=Math.max(high,v.y());
                assertTrue(Double.isFinite(v.x()+v.y()+v.z()+v.nx()+v.ny()+v.nz()));
                assertTrue(v.x()*v.x()+v.z()*v.z()<=.7*.7,"crystal fits the internal cavity");
                assertTrue(Math.abs(v.nx()*v.nx()+v.ny()*v.ny()+v.nz()*v.nz()-1)<.00001,"facet normals are normalized");
                String a=key(v),b=key(ring.get((i+1)%ring.size()));edges.merge(a.compareTo(b)<0?a+"|"+b:b+"|"+a,1,Integer::sum);
            }
        }
        assertTrue(high>=1&&low<=-1,"both elongated pointed tips");
        assertTrue(edges.values().stream().allMatch(count->count==2),"every edge shared by two facets, no gaps");
        assertTrue(faces.stream().allMatch(IndustrialMachineMesh.Surface::glow));
    }
    private static String key(ReceiverShellMesh.Vertex v){return Math.round(v.x()*100000)+":"+Math.round(v.y()*100000)+":"+Math.round(v.z()*100000);}
}
