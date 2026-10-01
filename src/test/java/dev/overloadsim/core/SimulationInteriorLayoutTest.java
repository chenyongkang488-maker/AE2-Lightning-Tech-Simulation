package dev.overloadsim.core;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class SimulationInteriorLayoutTest {
    @Test void fourCoilsFitAboveTheOrbWithoutOverlappingForEverySupportedSize() throws Exception {
        for(int n=3;n<=7;n++){
            var c=Class.forName("dev.overloadsim.core.SimulationInteriorLayout").getConstructor(int.class).newInstance(n);
            float low=value(c,"coilLow"),high=value(c,"coilHigh"),scale=value(c,"coilScale"),y=value(c,"coilY");
            assertTrue(low>=1 && high+scale<=n-1,"coils inside chamber "+n);
            assertTrue(high-low>=scale,"four separate coils "+n);
            assertTrue(y>n/2f+(n-2)*.3f,"coils above floating orb "+n);
            assertTrue(y+scale*3/16<=n-1,"below frame roof "+n);
        }
    }
    private float value(Object c,String name)throws Exception{return (float)c.getClass().getMethod(name).invoke(c);}
}
