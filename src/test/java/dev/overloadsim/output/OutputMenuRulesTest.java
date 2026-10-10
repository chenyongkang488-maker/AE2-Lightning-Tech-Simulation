package dev.overloadsim.output;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OutputMenuRulesTest {
    private Class<?> rules() throws Exception { return Class.forName("dev.overloadsim.output.OutputMenuRules"); }
    private long extract(boolean empty, boolean same, int carried, int max, long available, boolean half) throws Exception {
        return (long) rules().getMethod("extractionLimit", boolean.class, boolean.class, int.class, int.class, long.class, boolean.class)
                .invoke(null, empty, same, carried, max, available, half);
    }
    @Test void extractionNeverConsumesAnUnrelatedCursorAndRespectsStackSpace() throws Exception {
        assertEquals(0, extract(false, false, 12, 64, 1000, false));
        assertEquals(4, extract(false, true, 60, 64, 1000, false));
        assertEquals(0, extract(false, true, 65, 64, 1000, false));
        assertEquals(32, extract(true, false, 0, 64, Long.MAX_VALUE, true));
        assertEquals(1, extract(true, false, 0, 64, 1, true));
        assertEquals(0, extract(true, false, 0, 64, 0, false));
    }
    @Test void pagesExposeEachOfThirtySixSlotsExactlyOnceAndRejectForgedPages() throws Exception {
        var visible = rules().getMethod("visibleSlot", int.class, int.class);
        for (int slot = 0; slot < 36; slot++) {
            assertEquals(slot < 18, visible.invoke(null, slot, 0));
            assertEquals(slot >= 18, visible.invoke(null, slot, 1));
        }
        for (int slot : new int[]{-1, 36, Integer.MAX_VALUE}) assertEquals(false, visible.invoke(null, slot, 0));
        assertEquals(false, visible.invoke(null, 0, -1));
        assertEquals(false, visible.invoke(null, 18, 2));
    }
    @Test void amountInputRejectsOverflowNegativeAndAboveResourceCapacity() throws Exception {
        var parse = rules().getMethod("parseAmount", String.class, long.class);
        assertEquals(1024L, parse.invoke(null, "1024", 1024L));
        assertEquals(1L, parse.invoke(null, "1", Long.MAX_VALUE));
        for (String input : new String[]{"", "0", "-1", "1025", "9223372036854775808", "1e3", " 1"})
            assertEquals(-1L, parse.invoke(null, input, 1024L));
    }
}
