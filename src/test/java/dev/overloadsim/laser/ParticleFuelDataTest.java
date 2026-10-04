package dev.overloadsim.laser;

import com.google.gson.JsonParser;
import dev.overloadsim.core.ParticleFuelRules;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ParticleFuelDataTest {
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
