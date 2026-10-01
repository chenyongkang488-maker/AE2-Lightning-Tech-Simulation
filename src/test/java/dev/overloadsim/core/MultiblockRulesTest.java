package dev.overloadsim.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MultiblockRulesTest {
    private Class<?> rules() throws Exception {
        return Class.forName("dev.overloadsim.multiblock.MultiblockRules");
    }
    @Test void durationAndCaps() throws Exception {
        var method = rules().getMethod("duration", int.class, int.class, int.class, boolean.class);
        assertEquals(180, method.invoke(null, 0, 0, 0, false));
        assertEquals(38, method.invoke(null, 0, 0, 13, true));
        assertEquals(76, method.invoke(null, 100, 100, 100, false));
    }
    @Test void fortuneAndCosts() throws Exception {
        assertEquals(1024L, rules().getMethod("multiplier", int.class).invoke(null, 40));
        Object cost = rules().getMethod("costs", int.class, boolean.class, boolean.class).invoke(null, 49, true, true);
        assertEquals(49000L, cost.getClass().getMethod("fe").invoke(cost));
        assertEquals(147L, cost.getClass().getMethod("hv").invoke(cost));
        assertEquals(49L, cost.getClass().getMethod("ehv").invoke(cost));
    }
}
