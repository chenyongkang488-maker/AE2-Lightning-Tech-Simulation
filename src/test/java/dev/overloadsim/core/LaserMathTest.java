package dev.overloadsim.core;

import java.util.Arrays;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LaserMathTest {
    @Test void splitAssignsRemainderDeterministicallyWithoutDuplicatingEnergy() {
        assertArrayEquals(new long[]{4, 3, 3}, LaserMath.split(10, 3));
        assertArrayEquals(new long[]{1, 1, 0, 0, 0}, LaserMath.split(2, 5));
        assertEquals(1_000_000_000_000_000L, Arrays.stream(LaserMath.split(1_000_000_000_000_000L, 5)).sum());
    }
    @Test void splitRejectsNegativeInputAndMissingOutputs() {
        assertThrows(IllegalArgumentException.class, () -> LaserMath.split(-1, 2));
        assertThrows(IllegalArgumentException.class, () -> LaserMath.split(10, 0));
        assertThrows(IllegalArgumentException.class, () -> LaserMath.split(10, 6));
    }
    @Test void amplificationRejectsAnUnpayableBeamBeforeArithmeticOverflow() {
        assertEquals(1_000_000_000_000_000L, LaserMath.amplify(500_000_000_000_000L));
        assertThrows(IllegalArgumentException.class, () -> LaserMath.amplify(500_000_000_000_001L));
        assertThrows(IllegalArgumentException.class, () -> LaserMath.amplify(Long.MAX_VALUE));
    }
    @Test void boostRejectsCapOverflowRatherThanSilentlyDiscardingPaidEnergy() {
        assertEquals(9_100_000, LaserMath.boost(100_000, 9_000_000));
        assertThrows(IllegalArgumentException.class, () -> LaserMath.boost(1_000_000_000_000_000L, 1));
        assertThrows(IllegalArgumentException.class, () -> LaserMath.boost(1, Long.MAX_VALUE));
        assertThrows(IllegalArgumentException.class, () -> LaserMath.boost(-1, 1));
    }
    @Test void coreCarriesExactRemainderAcrossBatches() {
        var first = LaserMath.accumulate(0, 511_000_000_000L);
        assertEquals(0, first.particles());
        assertEquals(511_000_000_000L, first.remainder());
        var second = LaserMath.accumulate(first.remainder(), 1_000_000_003L);
        assertEquals(1, second.particles());
        assertEquals(3, second.remainder());
    }
    @Test void accumulationHandlesLongMaximumWithoutWrappingAndRejectsInvalidRemainder() {
        var result = LaserMath.accumulate(511_999_999_999L, Long.MAX_VALUE);
        assertEquals(18_014_399, result.particles());
        assertEquals(260_854_775_806L, result.remainder());
        assertThrows(IllegalArgumentException.class, () -> LaserMath.accumulate(512_000_000_000L, 1));
        assertThrows(IllegalArgumentException.class, () -> LaserMath.accumulate(0, -1));
    }
    @Test void unitParsingPreserves1024PAndExactFractionalUnits() {
        assertEquals(1_024_000_000_000_000_000L, LaserMath.parseEnergy("1024PFE"));
        assertEquals(512_000_000_000L, LaserMath.parseEnergy("512GFE"));
        assertEquals(100_000L, LaserMath.parseEnergy("100kFE"));
        assertEquals(1_500L, LaserMath.parseEnergy("1.5kFE"));
        assertEquals(9_223_372_036_854_775_807L, LaserMath.parseEnergy("9223372036854775807FE"));
    }
    @Test void unitParsingRejectsLossyMalformedAndOutOfRangeValues() {
        for (String input : new String[]{"0.1FE", "-1FE", "NaN", "1e20FE", "9223372036854775808FE", "1TFE", "", "512G junk"}) {
            assertThrows(IllegalArgumentException.class, () -> LaserMath.parseEnergy(input), input);
        }
    }
}
