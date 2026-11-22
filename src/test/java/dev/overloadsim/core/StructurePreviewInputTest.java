package dev.overloadsim.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StructurePreviewInputTest {
    private Object input(){return assertDoesNotThrow(()->Class.forName("dev.overloadsim.core.StructurePreviewInput").getConstructor().newInstance());}
    private Object click(Object input,double x,double y,int button,boolean simulate,boolean ring){
        return assertDoesNotThrow(()->input.getClass().getMethod("click",double.class,double.class,int.class,boolean.class,boolean.class).invoke(input,x,y,button,simulate,ring));
    }
    private String action(Object result){return assertDoesNotThrow(()->result.getClass().getMethod("action").invoke(result).toString());}
    private boolean consumed(Object result){return assertDoesNotThrow(()->(boolean)result.getClass().getMethod("consumed").invoke(result));}
    @Test void jeiSimulationDoesNotExecuteControlsAndReleaseExecutesExactlyOnce(){
        var input=input();var down=click(input,132,102,0,true,false);
        assertTrue(consumed(down));assertEquals("NONE",action(down));
        assertEquals("NEXT_LAYER",action(click(input,132,102,0,false,false)));
        assertEquals("NONE",action(click(input,132,102,0,false,false)));
    }
    @Test void dragReleaseOverAControlDoesNotActivateIt(){
        var input=input();assertTrue(consumed(click(input,80,80,0,true,true)));
        assertEquals("NONE",action(click(input,20,122,0,false,true)));
        assertFalse(consumed(click(input,220,80,0,true,true)),"material slots remain available to JEI");
    }
    @Test void flatControlIsOnlyAvailableForRingsAndInstancesDoNotSharePresses(){
        var a=input();var b=input();
        assertFalse(consumed(click(a,20,122,0,true,false)));
        assertTrue(consumed(click(a,20,122,0,true,true)));
        assertEquals("NONE",action(click(b,20,122,0,false,true)));
        assertEquals("TOGGLE_FLAT",action(click(a,20,122,0,false,true)));
    }
}
