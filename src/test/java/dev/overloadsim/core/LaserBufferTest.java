package dev.overloadsim.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LaserBufferTest {
    @Test void energySimulationNeverMutatesAndAcceptsOnlyAvailableSpace() {
        var energy = new LongEnergyBuffer(1_024_000_000_000_000_000L);
        assertEquals(1_024_000_000_000_000_000L, energy.insert(Long.MAX_VALUE, true));
        assertEquals(0, energy.stored());
        assertEquals(1_024_000_000_000_000_000L, energy.insert(Long.MAX_VALUE, false));
        assertEquals(0, energy.insert(1, false));
        assertEquals(1_024_000_000_000_000_000L, energy.extract(Long.MAX_VALUE, true));
        assertEquals(1_024_000_000_000_000_000L, energy.stored());
        assertEquals(1_024_000_000_000_000_000L, energy.extract(Long.MAX_VALUE, false));
        assertEquals(0, energy.stored());
    }
    @Test void savedEnergySaturatesAtCapacityAndRejectsNegativeAmounts() {
        var energy = new LongEnergyBuffer(100);
        energy.load(Long.MAX_VALUE);
        assertEquals(100, energy.stored());
        assertThrows(IllegalArgumentException.class, () -> energy.load(-1));
        assertThrows(IllegalArgumentException.class, () -> energy.insert(-1, false));
        assertThrows(IllegalArgumentException.class, () -> energy.extract(-1, false));
        assertThrows(IllegalArgumentException.class, () -> new LongEnergyBuffer(-1));
    }
    @Test void particleOverflowRejectsEntireBatchAndRetains125000Particles() {
        var particles = new ParticleBuffer(125_000);
        assertTrue(particles.receive(124_999));
        assertFalse(particles.receive(2));
        assertEquals(124_999, particles.stored());
        assertTrue(particles.receive(1));
        assertFalse(particles.receive(Long.MAX_VALUE));
        assertEquals(125_000, particles.stored());
        assertEquals(125_000, particles.consume(Long.MAX_VALUE));
        assertEquals(0, particles.stored());
    }
    @Test void savedParticlesClampAndNegativeOperationsCannotCreateParticles() {
        var particles = new ParticleBuffer(125_000);
        particles.load(Long.MAX_VALUE);
        assertEquals(125_000, particles.stored());
        assertThrows(IllegalArgumentException.class, () -> particles.load(-1));
        assertThrows(IllegalArgumentException.class, () -> particles.receive(-1));
        assertThrows(IllegalArgumentException.class, () -> particles.consume(-1));
        assertThrows(IllegalArgumentException.class, () -> new ParticleBuffer(-1));
    }
}
