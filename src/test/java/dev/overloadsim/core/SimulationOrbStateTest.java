package dev.overloadsim.core;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class SimulationOrbStateTest {
    private Object state(int flags)throws Exception{return Class.forName("dev.overloadsim.core.SimulationOrbState").getMethod("of",int.class).invoke(null,flags);}
    private boolean value(Object state,String name)throws Exception{return (boolean)state.getClass().getMethod(name).invoke(state);}
    @Test void onlyWorkingBatchesShowAnOrbAndCombineModuleEffects()throws Exception{
        assertFalse(value(state(0),"visible"));assertFalse(value(state(6),"visible"));
        assertTrue(value(state(1),"visible"));assertFalse(value(state(1),"smelting"));
        assertTrue(value(state(7),"smelting"));assertTrue(value(state(7),"overload"));
    }
}
