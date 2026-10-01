package dev.overloadsim.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ModeScrollTest {
    @Test void rawQuarterStepsAreProcessedBeforeVanillaFiltering(){var a=new ModeScroll();assertEquals(0,a.scroll(.25,1));assertEquals(0,a.scroll(.25,2));assertEquals(0,a.scroll(.25,3));assertEquals(1,a.scroll(.25,4));assertEquals(0,a.scroll(.75,5));assertEquals(2,a.scroll(1.75,6));}
    @Test void fractionalWheelAccumulatesAndReverses(){var a=new ModeScroll();assertEquals(0,a.scroll(.25,1));assertEquals(0,a.scroll(.25,2));assertEquals(1,a.scroll(.5,3));assertEquals(-2,a.scroll(-2,4));assertEquals(0,a.scroll(-.5,5));assertEquals(-1,a.scroll(-.5,6));}
    @Test void inactivityAndResetDiscardRemainder(){var a=new ModeScroll();assertEquals(0,a.scroll(.5,1));assertEquals(0,a.scroll(.5,22));a.reset();assertEquals(0,a.scroll(.5,23));}
    @Test void invalidAndExtremeDeltasAreBounded(){var a=new ModeScroll();assertEquals(0,a.scroll(Double.NaN,1));assertEquals(0,a.scroll(Double.POSITIVE_INFINITY,2));assertEquals(8,a.scroll(999,3));assertEquals(-8,a.scroll(-999,4));}
}
