package dev.overloadsim.laser;

import com.google.gson.JsonParser;
import dev.overloadsim.core.ParticleFuelRules;
import dev.overloadsim.core.ParticleClock;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ParticleFuelDataTest {
    @Test void transcendentCrystalUsesTheSamePaidWindowAsBothNuclearPellets() throws Exception {
        var directory = Path.of("src/main/resources/data/overload_sim/particle_conversion");
        var crystal = JsonParser.parseString(Files.readString(directory.resolve("transcendent_crystal.json"))).getAsJsonObject();
        assertEquals("overload_sim:transcendent_crystal", crystal.getAsJsonObject("ingredient").get("item").getAsString());
        var fuel = ParticleFuelRules.readFuel(crystal);
        for (var original : new String[]{"plutonium_pellet", "polonium_pellet"}) {
            var nuclear = ParticleFuelRules.readFuel(JsonParser.parseString(Files.readString(directory.resolve(original + ".json"))).getAsJsonObject());
            var actual = new ParticleClock();
            var expected = new ParticleClock();
            for (int tick = 0; tick < 125; tick++) {
                long particles = tick >= 40 && tick < 60 ? 0 : 125;
                long space = tick >= 80 && tick < 85 ? 0 : Long.MAX_VALUE;
                assertEquals(expected.advance(nuclear, true, particles, space), actual.advance(fuel, true, particles, space));
                assertEquals(expected.state(), actual.state());
            }
        }
    }
    @Test void recipeNumbersPreserveExactLongEnergyAndDefaultWindow() {
        var fuel = ParticleFuelRules.readFuel(JsonParser.parseString("{\"particle_interval_ticks\":1,\"energy_per_particle\":\"1024PFE\"}").getAsJsonObject());
        assertEquals(100, fuel.consumableDurationTicks());
        assertEquals(1, fuel.particleIntervalTicks());
        assertEquals(1_024_000_000_000_000_000L, fuel.energyPerParticle());
    }
    @Test void integerJsonFieldsCannotSilentlyTruncateFractionsOrOverflow() {
        for (String interval : new String[]{"0", "-1", "1.5", "4294967297", "101"}) {
            var json = JsonParser.parseString("{\"particle_interval_ticks\":" + interval + ",\"energy_per_particle\":\"512GFE\"}").getAsJsonObject();
            assertThrows(IllegalArgumentException.class, () -> ParticleFuelRules.readFuel(json), interval);
        }
    }
    @Test void unsupportedBatchSizesAndZeroYieldAreDiagnosedInsteadOfCreatingResources() {
        for (String fields : new String[]{"\"consumable_count\":2,", "\"particles_per_operation\":2,", "\"type\":\"minecraft:crafting_shaped\","}) {
            var json = JsonParser.parseString("{" + fields + "\"particle_interval_ticks\":1,\"energy_per_particle\":\"512GFE\"}").getAsJsonObject();
            assertThrows(IllegalArgumentException.class, () -> ParticleFuelRules.readFuel(json));
        }
        var zero = JsonParser.parseString("{\"particle_interval_ticks\":1,\"energy_per_particle\":\"0FE\"}").getAsJsonObject();
        assertThrows(IllegalArgumentException.class, () -> ParticleFuelRules.readFuel(zero));
    }
}
