package dev.overloadsim.core;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class SimulationShellTopologyTest {
    private String role(int n,int x,int y,int z)throws Exception{return Class.forName("dev.overloadsim.core.SimulationShellTopology").getMethod("role",int.class,int.class,int.class,int.class).invoke(null,n,x,y,z).toString();}
    @Test void DistinguishesVerticalRailsRoofAndCorners()throws Exception{
        for(int n=3;n<=7;n++){
            assertEquals("VERTICAL",role(n,0,1,0));assertEquals("ROOF",role(n,1,n-1,1));assertEquals("ROOF_EDGE",role(n,1,n-1,0));
            assertEquals("ROOF_CORNER",role(n,0,n-1,0));assertEquals("HORIZONTAL",role(n,1,0,0));assertEquals("CORNER",role(n,0,0,0));
            assertEquals("GLASS",role(n,1,1,0));assertEquals("NONE",role(n,1,1,1));assertEquals("NONE",role(n,-1,0,0));
        }
    }
    @Test void onlyBottomNonCornerEdgesHaveASpecialOutwardFace()throws Exception{
        var face=Class.forName("dev.overloadsim.core.SimulationShellTopology").getMethod("outsideFace",int.class,int.class,int.class,int.class);
        assertEquals("NORTH",face.invoke(null,5,1,0,0));assertEquals("WEST",face.invoke(null,5,0,0,1));
        assertEquals("",face.invoke(null,5,0,0,0));assertEquals("",face.invoke(null,5,1,1,0));
    }
    @Test void staleOwnerRemovalCannotEraseAReplacementOrLeaveInteriorSkins()throws Exception{
        var type=Class.forName("dev.overloadsim.core.SimulationShellRegistry");var registry=type.getConstructor().newInstance();
        var replace=type.getMethod("replace",Object.class,int.class,int.class,int.class,int.class,int.class,int.class,int.class);
        var remove=type.getMethod("remove",Object.class,int.class,int.class,int.class);var lookup=type.getMethod("lookup",int.class,int.class,int.class);
        var oldOwner=new Object();var newOwner=new Object();replace.invoke(registry,oldOwner,1,0,0,0,0,0,7);
        assertNotNull(lookup.invoke(registry,6,6,6));assertNull(lookup.invoke(registry,3,3,3));
        replace.invoke(registry,newOwner,1,0,0,0,0,0,3);remove.invoke(registry,oldOwner,1,0,0);
        assertNotNull(lookup.invoke(registry,0,1,0));assertNull(lookup.invoke(registry,6,6,6));
        remove.invoke(registry,newOwner,1,0,0);assertNull(lookup.invoke(registry,0,1,0));
    }
}
