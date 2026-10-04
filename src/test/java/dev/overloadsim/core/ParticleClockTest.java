package dev.overloadsim.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ParticleClockTest {
    @Test void firstEffectiveTickPaysFuelAndExactIntervalProducesOneParticle() {
        var clock = new ParticleClock();
        var fuel = new ParticleClock.Fuel(100, 20, 512_000_000_000L);
        var start = clock.advance(fuel, true, 5, Long.MAX_VALUE);
        assertTrue(start.consumedItem());
        assertEquals(0, start.particlesUsed());
        assertEquals(new ParticleClock.State(99, 19), clock.state());
        for (int tick = 2; tick < 20; tick++) assertEquals(0, clock.advance(fuel, false, 5, Long.MAX_VALUE).particlesUsed());
        var output = clock.advance(fuel, false, 5, Long.MAX_VALUE);
        assertFalse(output.consumedItem());
        assertEquals(1, output.particlesUsed());
        assertEquals(512_000_000_000L, output.producedFe());
        assertEquals(new ParticleClock.State(80, 20), clock.state());
    }
    @Test void allDefaultIntervalsUseOneItemPer100EffectiveTicks() {
        for (int interval : new int[]{100, 20, 10, 2, 1}) {
            var clock = new ParticleClock();
            var fuel = new ParticleClock.Fuel(100, interval, 512_000_000_000L);
            int consumedItems = 0;
            long used = 0;
            for (int tick = 0; tick < 100; tick++) {
                var result = clock.advance(fuel, true, 1_000, Long.MAX_VALUE);
                if (result.consumedItem()) consumedItems++;
                used += result.particlesUsed();
            }
            assertEquals(1, consumedItems);
            assertEquals(100 / interval, used);
            assertEquals(0, clock.state().fuelRemaining());
        }
    }
    @Test void absentParticlesAndInsufficientSpacePauseBothClocksWithoutPayingFuel() {
        var clock = new ParticleClock();
        var fuel = new ParticleClock.Fuel(100, 20, 512_000_000_000L);
        assertFalse(clock.advance(fuel, true, 0, Long.MAX_VALUE).consumedItem());
        assertFalse(clock.advance(fuel, true, 1, 511_999_999_999L).consumedItem());
        assertEquals(new ParticleClock.State(0, 0), clock.state());
        clock.advance(fuel, true, 5, Long.MAX_VALUE);
        var saved = clock.state();
        for (int tick = 0; tick < 500; tick++) clock.advance(fuel, true, 0, Long.MAX_VALUE);
        for (int tick = 0; tick < 500; tick++) clock.advance(fuel, true, 5, 1);
        assertEquals(saved, clock.state());
    }
    @Test void reloadRestoresPaidWindowWithoutChargingAnotherItem() {
        var clock = new ParticleClock();
        clock.restore(81, 1);
        var result = clock.advance(new ParticleClock.Fuel(100, 20, 512_000_000_000L), false, 1, 512_000_000_000L);
        assertFalse(result.consumedItem());
        assertEquals(1, result.particlesUsed());
        assertEquals(new ParticleClock.State(80, 20), clock.state());
    }
    @Test void exhaustedWindowWaitsForFuelAndNextWindowPaysOnce() {
        var clock = new ParticleClock();
        var fuel = new ParticleClock.Fuel(100, 1, 4_096_000_000_000L);
        clock.restore(1, 1);
        assertEquals(4_096_000_000_000L, clock.advance(fuel, false, 1, Long.MAX_VALUE).producedFe());
        assertEquals(0, clock.advance(fuel, false, 1, Long.MAX_VALUE).producedFe());
        var next = clock.advance(fuel, true, 1, Long.MAX_VALUE);
        assertTrue(next.consumedItem());
        assertEquals(4_096_000_000_000L, next.producedFe());
        assertEquals(new ParticleClock.State(99, 1), clock.state());
    }
    @Test void invalidClockValuesFailWithoutMutatingState() {
        var clock = new ParticleClock();
        clock.restore(81, 1);
        assertThrows(IllegalArgumentException.class, () -> clock.restore(-1, 1));
        assertEquals(new ParticleClock.State(81, 1), clock.state());
        assertThrows(IllegalArgumentException.class, () -> new ParticleClock.Fuel(0, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> new ParticleClock.Fuel(100, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> new ParticleClock.Fuel(100, 101, 1));
        assertThrows(IllegalArgumentException.class, () -> new ParticleClock.Fuel(100, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> clock.advance(new ParticleClock.Fuel(100, 1, 1), true, -1, 0));
    }
}
