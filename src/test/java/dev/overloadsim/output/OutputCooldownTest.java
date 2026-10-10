package dev.overloadsim.output;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
class OutputCooldownTest {
    @Test void successfulOutputAlwaysRunsOnNextTickInBothModes() {
        for(boolean fast:new boolean[]{false,true}) {
            var cooldown=new OutputCooldown();cooldown.complete(100,true,fast);
            assertFalse(cooldown.isDue(100));assertTrue(cooldown.isDue(101));
        }
    }
    @Test void idlePollingBacksOffToOriginalModeMaximumAndResetsAfterSuccess() {
        for(boolean fast:new boolean[]{false,true}) {
            var cooldown=new OutputCooldown();long now=0;
            for(int attempt=0;attempt<20;attempt++){cooldown.complete(now,false,fast);now=cooldown.nextTick();}
            cooldown.complete(now,false,fast);
            assertEquals(fast?20:80,cooldown.nextTick()-now);
            cooldown.complete(now,true,fast);assertEquals(now+1,cooldown.nextTick());
        }
    }
}
