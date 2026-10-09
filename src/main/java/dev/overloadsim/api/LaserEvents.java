package dev.overloadsim.api;

import dev.overloadsim.core.LaserMath;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

/** Post on the NeoForge game bus before settling a server resource transaction. */
public final class LaserEvents {
    private LaserEvents() {}
    public abstract static class AtMachine extends Event implements ICancellableEvent {
        public final ServerLevel level;
        public final BlockPos position;
        protected AtMachine(ServerLevel level, BlockPos position) {
            this.level = Objects.requireNonNull(level);
            this.position = Objects.requireNonNull(position).immutable();
        }
    }
    public static final class BeforeModifier extends AtMachine {
        public final ResourceLocation modifierId;
        public final long inputEnergy;
        private long outputEnergy;
        private int range;
        public BeforeModifier(ServerLevel level, BlockPos position, ResourceLocation modifierId, long inputEnergy, long outputEnergy, int range) {
            super(level, position);
            this.modifierId = Objects.requireNonNull(modifierId);
            bounded(inputEnergy, LaserMath.MAX_BEAM);
            this.inputEnergy = inputEnergy;
            setOutputEnergy(outputEnergy);
            setRange(range);
        }
        public long outputEnergy() { return outputEnergy; }
        public void setOutputEnergy(long value) { bounded(value, LaserMath.MAX_BEAM); outputEnergy = value; }
        public int range() { return range; }
        public void setRange(int value) { bounded(value, 64); range = value; }
    }
    public static final class BeforeCoreEmission extends AtMachine {
        public final long energy;
        private final long maximumParticles;
        private final long maximumRemainder;
        private long particles;
        private long remainder;
        public BeforeCoreEmission(ServerLevel level, BlockPos position, long energy, long particles, long remainder) {
            super(level, position);
            bounded(energy, LaserMath.MAX_BEAM);
            bounded(particles, LaserMath.MAX_BEAM / LaserMath.PARTICLE_COST + 1);
            bounded(remainder, LaserMath.PARTICLE_COST - 1);
            this.energy = energy;
            maximumParticles = this.particles = particles;
            maximumRemainder = this.remainder = remainder;
        }
        public long particles() { return particles; }
        public void setParticles(long value) { bounded(value, maximumParticles); particles = value; }
        public long remainder() { return remainder; }
        public void setRemainder(long value) { bounded(value, maximumRemainder); remainder = value; }
    }
    public static final class BeforeParticleConversion extends AtMachine {
        public final ResourceLocation recipeId;
        private final long maximumParticles;
        private final long maximumEnergy;
        private long particles;
        private long energy;
        public BeforeParticleConversion(ServerLevel level, BlockPos position, ResourceLocation recipeId, long particles, long energy) {
            super(level, position);
            this.recipeId = Objects.requireNonNull(recipeId);
            bounded(particles, 125_000);
            bounded(energy, 1_024_000_000_000_000_000L);
            if (particles == 0 && energy > 0) throw new IllegalArgumentException("conversion requires particles");
            maximumParticles = this.particles = particles;
            maximumEnergy = this.energy = energy;
        }
        public long particles() { return particles; }
        public void setParticles(long value) {
            bounded(value, maximumParticles);
            particles = value;
            energy = Math.min(energy, proportionalEnergyLimit());
        }
        public long energy() { return energy; }
        public void setEnergy(long value) {
            bounded(value, proportionalEnergyLimit());
            if (particles == 0 && value > 0) throw new IllegalArgumentException("conversion requires particles");
            energy = value;
        }
        private long proportionalEnergyLimit() {
            if (maximumParticles == 0) return 0;
            // Divide first: the remainder product is bounded by 125000 squared.
            return maximumEnergy / maximumParticles * particles
                    + maximumEnergy % maximumParticles * particles / maximumParticles;
        }
    }
    public static final class BeforeExplosion extends AtMachine {
        public final String cause;
        /** Optical center block; position continues to identify the controller. */
        public final BlockPos center;
        private int radiusBlocks;
        private final int maximumRadius;
        /** @deprecated Use the block-radius constructor. A legacy chunk radius of 1 maps to 12 blocks, and 0 to 0. */
        @Deprecated
        public BeforeExplosion(ServerLevel level, BlockPos position, String cause, int radiusChunks) {
            super(level, position);
            this.cause = Objects.requireNonNull(cause);
            this.center = position.immutable();
            maximumRadius=12;
            setRadiusChunks(radiusChunks);
        }
        public BeforeExplosion(ServerLevel level, BlockPos position, BlockPos center, String cause, int radiusBlocks) {
            super(level, position);
            this.cause = Objects.requireNonNull(cause);
            this.center = Objects.requireNonNull(center).immutable();
            bounded(radiusBlocks,dev.overloadsim.core.ExplosionSphere.HARD_RADIUS_LIMIT);
            maximumRadius=radiusBlocks;
            setRadiusBlocks(radiusBlocks);
        }
        /** Handlers may reduce the configured radius, never expand it beyond the initial value (at most 64). */
        public int radiusBlocks() { return radiusBlocks; }
        public void setRadiusBlocks(int value) { bounded(value, maximumRadius); radiusBlocks = value; }
        /** @deprecated A compatibility flag: 0 for zero radius, 1 for any positive block radius. */
        @Deprecated
        public int radiusChunks() { return radiusBlocks == 0 ? 0 : 1; }
        /** @deprecated A legacy value of 1 selects the 12-block sphere, and 0 selects its center only. */
        @Deprecated
        public void setRadiusChunks(int value) { bounded(value, 1); radiusBlocks = value == 0 ? 0 : Math.min(12,maximumRadius); }
    }
    private static void bounded(long value, long maximum) {
        if (value < 0 || value > maximum) throw new IllegalArgumentException("laser event amount out of range");
    }
}
