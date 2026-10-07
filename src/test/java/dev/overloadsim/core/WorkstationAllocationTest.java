package dev.overloadsim.core;

import static org.junit.jupiter.api.Assertions.*;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class WorkstationAllocationTest {
    @Test void overlappingIngredientsCannotSpendTheSameItemsTwice() {
        assertNull(WorkstationAllocation.allocate(new int[]{3}, new int[]{2,2}, new boolean[][]{{true},{true}},1));
        assertArrayEquals(new int[]{4},WorkstationAllocation.allocate(new int[]{4},new int[]{2,2},new boolean[][]{{true},{true}},1));
    }
    @Test void broadIngredientLeavesRareMaterialForTheSpecificRequirement() {
        assertArrayEquals(new int[]{2,2},WorkstationAllocation.allocate(new int[]{2,2},new int[]{2,2},new boolean[][]{{true,true},{true,false}},1));
    }
    @Test void millionCountsDoNotEnumerateIndividualItems() {
        assertTimeoutPreemptively(Duration.ofSeconds(1),()->assertArrayEquals(new int[]{1_000_000,1_000_000},
                WorkstationAllocation.allocate(new int[]{1_000_000,1_000_000},new int[]{1_000_000,1_000_000},new boolean[][]{{true,true},{true,false}},1)));
    }
    @Test void parallelDemandUsesLongMathBeforeRejectingUnrepresentableRecipes() {
        assertNull(WorkstationAllocation.allocate(new int[]{1_000_000},new int[]{Integer.MAX_VALUE},new boolean[][]{{true}},1024));
        assertArrayEquals(new int[]{1024},WorkstationAllocation.allocate(new int[]{1_000_000},new int[]{1},new boolean[][]{{true}},1024));
    }
}
