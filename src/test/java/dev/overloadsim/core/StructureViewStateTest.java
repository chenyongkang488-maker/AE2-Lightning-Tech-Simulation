package dev.overloadsim.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StructureViewStateTest {
    private Object state() {
        return assertDoesNotThrow(()->Class.forName("dev.overloadsim.core.StructureViewState").getConstructor().newInstance(),
                "structure previews need independent, bounded camera and layer state");
    }
    private Object call(Object state,String name,Class<?>[] types,Object...args) {
        return assertDoesNotThrow(()->state.getClass().getMethod(name,types).invoke(state,args));
    }
    @Test void scrollingZoomIsBoundedAndCannotCorruptAnotherPreview() {
        var a=state();var b=state();
        call(a,"zoom",new Class[]{double.class},1000d);
        assertEquals(2.5,(double)call(a,"scale",new Class[]{}));
        assertEquals(1,(double)call(b,"scale",new Class[]{}));
        call(a,"zoom",new Class[]{double.class},-1000d);
        assertEquals(.5,(double)call(a,"scale",new Class[]{}));
    }
    @Test void layersVisitActualNegativeAndPositiveHeightsAndReturnToAll() {
        var a=state();
        assertEquals(-1,(int)call(a,"layerIndex",new Class[]{}));
        for(int expected=0;expected<4;expected++) {
            call(a,"stepLayer",new Class[]{int.class,int.class},1,4);
            assertEquals(expected,(int)call(a,"layerIndex",new Class[]{}));
            assertTrue((boolean)call(a,"visible",new Class[]{int.class,int.class},expected-2,-2));
            assertFalse((boolean)call(a,"visible",new Class[]{int.class,int.class},expected-1,-2));
        }
        call(a,"stepLayer",new Class[]{int.class,int.class},1,4);
        assertEquals(-1,(int)call(a,"layerIndex",new Class[]{}));
        call(a,"stepLayer",new Class[]{int.class,int.class},-1,4);
        assertEquals(3,(int)call(a,"layerIndex",new Class[]{}));
    }
    @Test void resetRestoresCameraAndLayersAfterExtremeOrInvalidInput() {
        var a=state();
        call(a,"rotate",new Class[]{double.class,double.class},1000d,1000d);
        assertTrue(Math.abs((double)call(a,"pitch",new Class[]{}))<=85);
        call(a,"zoom",new Class[]{double.class},Double.NaN);
        assertTrue(Double.isFinite((double)call(a,"scale",new Class[]{})));
        call(a,"reset",new Class[]{});
        assertEquals(1,(double)call(a,"scale",new Class[]{}));
        assertEquals(-1,(int)call(a,"layerIndex",new Class[]{}));
        assertEquals(25,(double)call(a,"pitch",new Class[]{}));
    }
}
