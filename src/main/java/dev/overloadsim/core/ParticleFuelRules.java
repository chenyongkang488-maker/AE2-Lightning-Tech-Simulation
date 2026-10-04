package dev.overloadsim.core;

import com.google.gson.JsonObject;

/** Exact JSON numeric validation without Minecraft registry or loader initialization. */
public final class ParticleFuelRules {
    private ParticleFuelRules() {}

    public static ParticleClock.Fuel readFuel(JsonObject json) {
        if (json.has("type") && !"overload_sim:particle_conversion".equals(json.get("type").getAsString()))
            throw new IllegalArgumentException("unsupported conversion type");
        if (integer(json, "consumable_count", 1) != 1 || integer(json, "particles_per_operation", 1) != 1)
            throw new IllegalArgumentException("conversion uses one item per window and one particle per operation");
        int duration = integer(json, "consumable_duration_ticks", 100);
        int interval = integer(json, "particle_interval_ticks", 0);
        if (!json.has("energy_per_particle") || !json.get("energy_per_particle").isJsonPrimitive())
            throw new IllegalArgumentException("missing energy_per_particle");
        long energy = LaserMath.parseEnergy(json.get("energy_per_particle").getAsString());
        return new ParticleClock.Fuel(duration, interval, energy);
    }

    private static int integer(JsonObject json, String key, int fallback) {
        if (!json.has(key)) return fallback;
        String value = json.get(key).getAsString();
        if (!value.matches("[0-9]+")) throw new IllegalArgumentException("invalid integer " + key);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException failure) {
            throw new IllegalArgumentException("integer out of range: " + key, failure);
        }
    }
}
