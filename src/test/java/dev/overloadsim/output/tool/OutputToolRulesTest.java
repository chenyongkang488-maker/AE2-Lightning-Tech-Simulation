package dev.overloadsim.output.tool;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OutputToolRulesTest {
    @Test void cyclesEverySlotIncludingEmptyAndWrapsInBothDirections() {
        assertEquals(35, OutputToolRules.cycle(0, -1));
        assertEquals(0, OutputToolRules.cycle(35, 1));
        assertEquals(18, OutputToolRules.cycle(17, 1));
        assertEquals(33, OutputToolRules.cycle(5, -8));
    }

    @Test void rejectsMalformedCycleAndChangedHeldSlot() {
        assertTrue(OutputToolRules.canCycle(true, false, true, true, 2, 2, 1));
        assertFalse(OutputToolRules.canCycle(false, false, true, true, 2, 2, 1));
        assertFalse(OutputToolRules.canCycle(true, true, true, true, 2, 2, 1));
        assertFalse(OutputToolRules.canCycle(true, false, false, true, 2, 2, 1));
        assertFalse(OutputToolRules.canCycle(true, false, true, false, 2, 2, 1));
        assertFalse(OutputToolRules.canCycle(true, false, true, true, 2, 3, 1));
        assertFalse(OutputToolRules.canCycle(true, false, true, true, -1, -1, 1));
        assertFalse(OutputToolRules.canCycle(true, false, true, true, 9, 9, 1));
        assertFalse(OutputToolRules.canCycle(true, false, true, true, 2, 2, 0));
        assertFalse(OutputToolRules.canCycle(true, false, true, true, 2, 2, 9));
        assertFalse(OutputToolRules.canCycle(true, false, true, true, 2, 2, Integer.MIN_VALUE));
        assertThrows(IllegalArgumentException.class, () -> OutputToolRules.cycle(36, 1));
    }

    @Test void duplicateBatchRetainsExistingBindingsAndCapsNewAdditions() {
        var plan = OutputToolRules.planAdd(List.of("a", "b"), List.of("b", "c", "c", "d", "e"), 4);
        assertEquals(List.of("c", "d"), plan.add());
        assertEquals(1, plan.existing());
        assertEquals(1, plan.full());
        var repeat = OutputToolRules.planAdd(List.of("a"), List.of("a"), 1);
        assertTrue(repeat.add().isEmpty());
        assertEquals(1, repeat.existing());
        assertEquals(0, repeat.full());
    }

    @Test void acceptsSourceSlotsOnlyWithinFixedInventory() {
        assertTrue(OutputToolRules.validSourceSlot(0));
        assertTrue(OutputToolRules.validSourceSlot(35));
        assertFalse(OutputToolRules.validSourceSlot(-1));
        assertFalse(OutputToolRules.validSourceSlot(36));
    }

    @Test void rejectedBatchCandidateDoesNotReserveRemainingCapacity() {
        var plan = OutputToolRules.planAdd(List.of("existing"), List.of("out-of-range", "valid", "existing"),
                2, target -> !target.equals("out-of-range"));
        assertEquals(List.of("valid"), plan.add());
        assertEquals(1, plan.existing());
        assertEquals(0, plan.full());
        assertEquals(1, plan.rejected());
    }
}
