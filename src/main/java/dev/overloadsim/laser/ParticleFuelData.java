package dev.overloadsim.laser;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.overloadsim.api.LaserExtensions;
import dev.overloadsim.core.ParticleFuelRules;
import dev.overloadsim.core.ParticleClock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Pack rules at data/[namespace]/particle_conversion/[name].json. */
public final class ParticleFuelData extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(ParticleFuelData.class);
    private record Snapshot(Map<ResourceLocation, Recipe> recipes, Map<ResourceLocation, ResourceLocation> tags,
                            List<String> diagnostics, long generation) {}
    private static volatile Snapshot snapshot = new Snapshot(Map.of(), Map.of(), List.of(), 0);

    public record Recipe(ResourceLocation id, Ingredient ingredient, int consumableDurationTicks,
                         int particleIntervalTicks, long energyPerParticle, long inputEnergyPerBatch) {
        public Recipe(ResourceLocation id, Ingredient ingredient, int duration, int interval, long energy) {
            this(id, ingredient, duration, interval, energy, 0);
        }
        public Recipe {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(ingredient, "ingredient");
            new ParticleClock.Fuel(consumableDurationTicks, particleIntervalTicks, energyPerParticle, inputEnergyPerBatch);
        }
        public ParticleClock.Fuel fuel() {
            return new ParticleClock.Fuel(consumableDurationTicks, particleIntervalTicks, energyPerParticle, inputEnergyPerBatch);
        }
    }

    public ParticleFuelData() { super(new Gson(), "particle_conversion"); }
    public static long generation() { return snapshot.generation(); }

    public static Optional<Recipe> match(ItemStack stack) {
        if (stack.isEmpty()) return Optional.empty();
        var current = snapshot;
        for (var entry : current.recipes().entrySet()) {
            if (tagAvailable(current, entry.getKey()) && entry.getValue().ingredient().test(stack)) return Optional.of(entry.getValue());
        }
        return LaserExtensions.matchFuel(stack);
    }

    public static Optional<Recipe> resolve(ResourceLocation id) {
        var current = snapshot;
        var recipe = current.recipes().get(id);
        if (recipe != null && tagAvailable(current, id)) return Optional.of(recipe);
        return LaserExtensions.resolveFuel(id);
    }

    /** Includes malformed files and missing optional ingredients without aborting other rules. */
    public static List<String> diagnostics() {
        var current = snapshot;
        var messages = new ArrayList<>(current.diagnostics());
        current.tags().forEach((id, tag) -> {
            if (!tagAvailable(current, id)) messages.add(id + ": unavailable optional item tag " + tag);
        });
        return List.copyOf(messages);
    }

    private static boolean tagAvailable(Snapshot current, ResourceLocation recipe) {
        var tag = current.tags().get(recipe);
        if (tag == null) return true;
        return BuiltInRegistries.ITEM.getTag(TagKey.create(Registries.ITEM, tag)).filter(values -> values.size() > 0).isPresent();
    }

    @Override protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        var recipes = new LinkedHashMap<ResourceLocation, Recipe>();
        var tags = new LinkedHashMap<ResourceLocation, ResourceLocation>();
        var messages = new ArrayList<String>();
        files.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            try {
                var json = entry.getValue().getAsJsonObject();
                var fuel = readFuel(json);
                var source = json.getAsJsonObject("ingredient");
                if (source == null || source.has("item") == source.has("tag")) throw new IllegalArgumentException("ingredient needs exactly one item or tag");
                Ingredient ingredient;
                if (source.has("item")) {
                    var itemId = ResourceLocation.parse(source.get("item").getAsString());
                    var item = BuiltInRegistries.ITEM.getOptional(itemId)
                            .orElseThrow(() -> new IllegalArgumentException("unavailable optional item " + itemId));
                    if (item == net.minecraft.world.item.Items.AIR) throw new IllegalArgumentException("air is not conversion fuel");
                    ingredient = Ingredient.of(item);
                } else {
                    var tagId = ResourceLocation.parse(source.get("tag").getAsString());
                    ingredient = Ingredient.of(TagKey.create(Registries.ITEM, tagId));
                    // Item tags finish binding after reload listeners. Resolve availability at use time.
                    tags.put(entry.getKey(), tagId);
                }
                recipes.put(entry.getKey(), new Recipe(entry.getKey(), ingredient, fuel.consumableDurationTicks(),
                        fuel.particleIntervalTicks(), fuel.energyPerParticle(), fuel.inputEnergyPerBatch()));
            } catch (RuntimeException failure) {
                String message = entry.getKey() + ": " + failure.getMessage();
                messages.add(message);
                LOGGER.warn("Skipped particle conversion rule {}", message);
            }
        });
        snapshot = new Snapshot(Collections.unmodifiableMap(recipes), Collections.unmodifiableMap(tags),
                List.copyOf(messages), snapshot.generation() + 1);
    }

    /** Registry-independent numeric validation, also useful to validate pack-generated rules. */
    public static ParticleClock.Fuel readFuel(JsonObject json) {
        return ParticleFuelRules.readFuel(json);
    }
}
