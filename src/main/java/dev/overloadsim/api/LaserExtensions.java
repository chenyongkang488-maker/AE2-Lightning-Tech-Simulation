package dev.overloadsim.api;

import dev.overloadsim.core.LaserMath;
import dev.overloadsim.laser.ParticleFuelData;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

/** Named server hooks. Register during common setup; IDs cannot silently replace other hooks. */
public final class LaserExtensions {
    private LaserExtensions() {}
    public record BeamContext(ServerLevel level, BlockPos position, Direction incoming, long energy, int range, long batchTick) {}
    public record ModifierResult(long energy, int range) {
        public ModifierResult {
            if (energy < 0 || energy > LaserMath.MAX_BEAM || range < 0 || range > 64)
                throw new IllegalArgumentException("invalid laser modifier result");
        }
    }
    public record TargetContext(ServerLevel level, BlockPos position, Direction incoming, long energy, long batchTick) {}
    @FunctionalInterface public interface LaserModifier { ModifierResult modify(BeamContext context); }
    /** Return true when this target has handled and terminated the beam. */
    @FunctionalInterface public interface LaserTarget { boolean accept(TargetContext context); }
    /** Providers must resolve stable recipe IDs after paid windows are restored from disk. */
    public interface ParticleConversionProvider {
        Optional<ParticleFuelData.Recipe> match(ItemStack stack);
        Optional<ParticleFuelData.Recipe> resolve(ResourceLocation recipeId);
    }
    /** Return actual accepted FE in [0, amount]; simulate must cause no resource mutation. */
    @FunctionalInterface public interface EnergySink {
        long insert(ServerLevel level, BlockPos position, long amount, boolean simulate);
    }
    private static final Map<ResourceLocation, LaserModifier> MODIFIERS = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, LaserTarget> TARGETS = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, ParticleConversionProvider> FUELS = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, EnergySink> SINKS = new ConcurrentHashMap<>();

    public static void registerModifier(ResourceLocation id, LaserModifier hook) { register(MODIFIERS, id, hook); }
    public static void registerTarget(ResourceLocation id, LaserTarget hook) { register(TARGETS, id, hook); }
    public static void registerFuelProvider(ResourceLocation id, ParticleConversionProvider hook) { register(FUELS, id, hook); }
    public static void registerEnergySink(ResourceLocation id, EnergySink hook) { register(SINKS, id, hook); }
    public static LaserModifier modifier(ResourceLocation id) { return MODIFIERS.get(id); }
    public static LaserTarget target(ResourceLocation id) { return TARGETS.get(id); }
    public static List<EnergySink> energySinks() {
        return SINKS.entrySet().stream().sorted(Map.Entry.comparingByKey()).map(Map.Entry::getValue).toList();
    }
    public static Optional<ParticleFuelData.Recipe> matchFuel(ItemStack stack) {
        for (var entry : FUELS.entrySet().stream().sorted(Map.Entry.comparingByKey()).toList()) {
            var match = Objects.requireNonNull(entry.getValue().match(stack));
            if (match.isPresent()) return match;
        }
        return Optional.empty();
    }
    public static Optional<ParticleFuelData.Recipe> resolveFuel(ResourceLocation id) {
        for (var entry : FUELS.entrySet().stream().sorted(Map.Entry.comparingByKey()).toList()) {
            var resolved = Objects.requireNonNull(entry.getValue().resolve(id));
            if (resolved.isPresent()) {
                if (!resolved.get().id().equals(id)) throw new IllegalStateException("fuel provider returned a different recipe ID");
                return resolved;
            }
        }
        return Optional.empty();
    }
    private static <T> void register(Map<ResourceLocation, T> hooks, ResourceLocation id, T hook) {
        if (hooks.putIfAbsent(Objects.requireNonNull(id), Objects.requireNonNull(hook)) != null)
            throw new IllegalArgumentException("duplicate laser extension " + id);
    }
}
