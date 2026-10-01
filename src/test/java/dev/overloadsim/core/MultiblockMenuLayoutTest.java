package dev.overloadsim.core;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class MultiblockMenuLayoutTest {
    private Object layout(int capacity,boolean recovery)throws Exception{return Class.forName("dev.overloadsim.core.MultiblockMenuLayout").getMethod("of",int.class,boolean.class).invoke(null,capacity,recovery);}
    private int value(Object o,String name)throws Exception{return (int)o.getClass().getMethod(name).invoke(o);}
    @Test void allFiveSizesAreSquareAndFitTheCompactViewport()throws Exception{
        for(int n=3;n<=7;n++){
            var l=layout(n*n,false);assertEquals(n,value(l,"side"));assertTrue(value(l,"height")<=240);
            int right=value(l,"outputLeft"),bar=value(l,"progressLeft");assertTrue(bar>10+n*18-2&&bar<right);
            var method=l.getClass().getMethod("input",int.class);
            for(int i=0;i<n*n;i++){var p=method.invoke(l,i);assertEquals(10+i%n*18,value(p,"x"));assertEquals(32+i/n*18,value(p,"y"));}
            assertEquals(194,value(l,"inventoryPanelWidth"));assertTrue(value(l,"width")>194);
        }
    }
    @Test void RecoveryKeepsAllStableSlotIdsAccessible()throws Exception{
        var l=layout(9,true);assertEquals(7,value(l,"side"));
        var last=l.getClass().getMethod("input",int.class).invoke(l,48);assertEquals(118,value(last,"x"));assertEquals(140,value(last,"y"));
        assertEquals(7,value(layout(0,false),"side"));
    }
}
