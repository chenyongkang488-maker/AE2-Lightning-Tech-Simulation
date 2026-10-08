package dev.overloadsim.core;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ParticleThroughputTest {
    private static ParticleClock.Fuel fuel(String rate, String yield) {
        return ParticleFuelRules.readFuel(JsonParser.parseString("{\"consumable_duration_ticks\":100,\"input_energy_per_batch\":\""
                + rate + "\",\"energy_per_particle\":\"" + yield + "\"}").getAsJsonObject());
    }

    @Test void everyLaserStrengthConsumesExactlyItsParticlesAcrossTwentyTickBatches() throws Exception {
        String[] rates = {"1GFE", "16GFE", "256GFE", "1024GFE", "0.125PFE", "1PFE"};
        String[] yields = {"512GFE", "512GFE", "256GFE", "1024GFE", "2048GFE", "4096GFE"};
        int[] ticks = {10240, 640, 40, 20, 1280, 160};
        long[] particles = {1, 1, 1, 2, 15625, 15625};
        long[] outputs = {512_000_000_000L, 512_000_000_000L, 256_000_000_000L,
                2_048_000_000_000L, 32_000_000_000_000_000L, 64_000_000_000_000_000L};
        int[] items = {103, 7, 1, 1, 13, 2};
        String[] files = {"nether_star", "netherite_block", "singularity", "overload_singularity", "transcendent_crystal", "antimatter_pellet"};
        for (int row = 0; row < rates.length; row++) {
            var clock = new ParticleClock();
            var recipe = ParticleFuelRules.readFuel(JsonParser.parseString(java.nio.file.Files.readString(
                    java.nio.file.Path.of("src/main/resources/data/overload_sim/particle_conversion", files[row] + ".json"))).getAsJsonObject());
            long used = 0, produced = 0;
            int paid = 0;
            for (int tick = 0; tick < ticks[row]; tick++) {
                var result = clock.advance(recipe, true, 125_000, Long.MAX_VALUE);
                used += result.particlesUsed(); produced += result.producedFe();
                if (result.consumedItem()) paid++;
            }
            assertEquals(particles[row], used, rates[row]);
            assertEquals(outputs[row], produced, rates[row]);
            assertEquals(items[row], paid, rates[row]);
        }
    }

    @Test void savedFractionalQuotaSurvivesAWindowBoundaryWithoutRenewingFuel() {
        var original = new ParticleClock(); var reloaded = new ParticleClock();
        var recipe = fuel("16GFE", "512GFE");
        for (int tick = 0; tick < 99; tick++) original.advance(recipe, true, 100, Long.MAX_VALUE);
        var saved = original.state();
        reloaded.restore(saved.fuelRemaining(), saved.particleRemaining(), saved.processingCredit());
        for (int tick = 0; tick < 700; tick++) {
            assertEquals(original.advance(recipe, true, 100, Long.MAX_VALUE), reloaded.advance(recipe, true, 100, Long.MAX_VALUE));
            assertEquals(original.state(), reloaded.state());
        }
    }

    @Test void particleCountdownTracksTheFractionalProcessingQuota() {
        String[] rates = {"1GFE", "16GFE", "256GFE", "1024GFE", "0.125PFE", "1PFE"};
        int[] remaining = {10239, 639, 39, 9, 1, 1};
        for (int row = 0; row < rates.length; row++) {
            var clock = new ParticleClock();
            var recipe = fuel(rates[row], "512GFE");
            clock.advance(recipe, true, 125_000, Long.MAX_VALUE);
            assertEquals(remaining[row], clock.state().particleRemaining(), rates[row]);
            var saved = clock.state();
            clock.advance(recipe, true, 0, Long.MAX_VALUE);
            assertEquals(saved, clock.state(), "paused countdown: " + rates[row]);
        }
    }

    @Test void pausedInventoryOrOutputDoesNotAccumulateThroughputOrConsumeFuel() {
        var actual = new ParticleClock();
        var expected = new ParticleClock();
        var recipe = fuel("0.125PFE", "2048GFE");
        for (int tick = 0; tick < 37; tick++) {
            assertEquals(expected.advance(recipe, true, 125_000, Long.MAX_VALUE),
                    actual.advance(recipe, true, 125_000, Long.MAX_VALUE));
        }
        var saved = actual.state();
        for (int tick = 0; tick < 500; tick++) {
            assertEquals(new ParticleClock.Result(false, 0, 0), actual.advance(recipe, true, 0, Long.MAX_VALUE));
            assertEquals(new ParticleClock.Result(false, 0, 0), actual.advance(recipe, true, 125_000, 1));
        }
        assertEquals(saved, actual.state());
        for (int tick = 0; tick < 100; tick++) {
            assertEquals(expected.advance(recipe, true, 125_000, Long.MAX_VALUE),
                    actual.advance(recipe, true, 125_000, Long.MAX_VALUE));
        }
    }

    @Test void capacityLimitsDiscardWholeUnusedQuotaRatherThanSavingAFutureBurst() {
        var clock = new ParticleClock();
        var recipe = fuel("1PFE", "4096GFE");
        assertEquals(1, clock.advance(recipe, true, 1, Long.MAX_VALUE).particlesUsed());
        var next = clock.advance(recipe, false, 125_000, Long.MAX_VALUE);
        assertEquals(98, next.particlesUsed());
        var limited = clock.advance(recipe, false, 125_000, 4_096_000_000_000L);
        assertEquals(1, limited.particlesUsed());
        assertTrue(clock.advance(recipe, false, 125_000, Long.MAX_VALUE).particlesUsed() <= 98);
    }

    @Test void ambiguousOrInvalidRateRulesAreRejected() {
        for (String value : new String[]{"0FE", "1.000000000000001PFE", "1.1PFE", "-1FE"}) {
            assertThrows(IllegalArgumentException.class, () -> fuel(value, "512GFE"));
        }
        var ambiguous = JsonParser.parseString("{\"input_energy_per_batch\":\"1GFE\",\"particle_interval_ticks\":1,\"energy_per_particle\":\"512GFE\"}").getAsJsonObject();
        assertThrows(IllegalArgumentException.class, () -> ParticleFuelRules.readFuel(ambiguous));
    }
}
