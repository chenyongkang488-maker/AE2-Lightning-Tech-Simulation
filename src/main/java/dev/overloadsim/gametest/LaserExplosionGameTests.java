package dev.overloadsim.gametest;

import dev.overloadsim.OverloadSimulation;
import dev.overloadsim.api.LaserEvents;
import dev.overloadsim.laser.*;
import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(OverloadSimulation.ID)
@PrefixGameTestTemplate(false)
public final class LaserExplosionGameTests {
    private static LaserMachineBlockEntity receiver(GameTestHelper h) {
        var host = LaserMachineGameTests.build(h, h.absolutePos(new BlockPos(14, 12, 14)), LaserKind.RECEIVER, Direction.WEST);
        host.checkStructure();
        h.assertTrue(host.formed(), "the receiver fixture is formed");
        return host;
    }

    private static Bat bat(GameTestHelper h, Vec3 position) {
        var bat = EntityType.BAT.create(h.getLevel());
        bat.setNoAi(true); bat.setNoGravity(true); bat.setResting(false); bat.setPos(position);
        h.getLevel().addFreshEntity(bat);
        return bat;
    }

    private record AnimationObserver(ServerPlayer player, List<ClientboundLevelParticlesPacket> packets) implements AutoCloseable {
        @Override public void close() { player.serverLevel().removePlayerImmediately(player, Entity.RemovalReason.DISCARDED); }
    }

    private static AnimationObserver observeAnimation(GameTestHelper h, Vec3 position) {
        var cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "blast-observer"), false);
        var level = h.getLevel();
        var player = new ServerPlayer(level.getServer(), level, cookie.gameProfile(), cookie.clientInformation());
        var packets = new ArrayList<ClientboundLevelParticlesPacket>();
        // Record the real server's outgoing vanilla particle packets at the connection boundary.
        new ServerGamePacketListenerImpl(level.getServer(), new Connection(PacketFlow.SERVERBOUND), player, cookie) {
            @Override public void send(Packet<?> packet) {
                if (packet instanceof ClientboundLevelParticlesPacket particles) packets.add(particles);
            }
        };
        player.setPos(position);
        level.addNewPlayer(player);
        return new AnimationObserver(player, packets);
    }

    @GameTest(template = "laser_empty", batch = "laser_explosion")
    public static void blastUsesOpticalCenterAndSphereAndHonorsProtectionOnce(GameTestHelper h) {
        var host = receiver(h);
        var center = host.center();
        var boundary = center.east(12);
        var above = center.above(10);
        var outside = center.east(13);
        var corner = center.offset(9, 0, 9);
        var protectedCell = center.west(8);
        var bedrock = center.south(8);
        for (var pos : Set.of(boundary, above, outside, corner, protectedCell)) h.getLevel().setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
        h.getLevel().setBlockAndUpdate(bedrock, Blocks.BEDROCK.defaultBlockState());
        var insideEntity = bat(h, center.getCenter().add(12, 0, 0));
        var outsideEntity = bat(h, center.getCenter().add(12.01, 0, 0));
        var cornerEntity = bat(h, center.getCenter().add(9, 0, 9));
        var triggers = new AtomicInteger();
        var sounds = new AtomicInteger();
        Consumer<LaserEvents.BeforeExplosion> observer = event -> { if (event.position.equals(host.getBlockPos())) triggers.incrementAndGet(); };
        Consumer<PlayLevelSoundEvent.AtPosition> audio = event -> {
            if (event.getLevel() == h.getLevel() && SoundEvents.GENERIC_EXPLODE.equals(event.getSound())) {
                sounds.incrementAndGet();
                h.assertTrue(event.getPosition().equals(center.getCenter()), "the explosion sound uses the exact optical center");
            }
        };
        // Keep this destructive test confined to its selected markers, including the host.
        Consumer<BlockEvent.BreakEvent> protection = event -> { if (!Set.of(boundary, above, outside, corner, bedrock).contains(event.getPos())) event.setCanceled(true); };
        NeoForge.EVENT_BUS.addListener(observer);
        NeoForge.EVENT_BUS.addListener(protection);
        NeoForge.EVENT_BUS.addListener(audio);
        try (var animation = observeAnimation(h, center.getCenter().add(0, 0, 16))) {
            int loadedChunks = h.getLevel().getChunkSource().getLoadedChunksCount();
            host.particles().load(10);
            LaserExplosion.trigger(host);
            LaserExplosion.trigger(host);
            for (int i = 0; i < 80; i++) LaserExplosion.tick(new LevelTickEvent.Post(() -> true, h.getLevel()));
            h.assertTrue(triggers.get() == 1 && host.explosionTriggered() && host.particles().stored() == 0 && !host.enabled(), "one host settles exactly one explosion");
            h.assertTrue(sounds.get() == 1, "one explosion broadcasts its native sound exactly once");
            var animations = animation.packets().stream().filter(packet -> packet.getParticle().getType() == ParticleTypes.EXPLOSION_EMITTER).toList();
            h.assertTrue(animations.size() == 1, "one explosion sends exactly one native explosion animation to a nearby client");
            var packet = animations.getFirst();
            h.assertTrue(new Vec3(packet.getX(), packet.getY(), packet.getZ()).equals(center.getCenter()) && packet.getCount() == 1, "the vanilla animation packet uses the same optical center as damage and sound");
            h.assertTrue(h.getLevel().getChunkSource().getLoadedChunksCount() == loadedChunks, "destruction does not load or generate extra chunks");
            h.assertTrue(h.getLevel().getBlockState(boundary).isAir() && h.getLevel().getBlockState(above).isAir(), "the inclusive 12-block boundary and the optical-center upper sphere are destroyed");
            h.assertTrue(h.getLevel().getBlockState(outside).is(Blocks.STONE) && h.getLevel().getBlockState(corner).is(Blocks.STONE), "outside distance and the cube's corner remain untouched");
            h.assertTrue(h.getLevel().getBlockState(protectedCell).is(Blocks.STONE) && h.getLevel().getBlockState(bedrock).is(Blocks.BEDROCK), "event protection and unbreakable cells remain intact");
            h.assertTrue(!insideEntity.isAlive(), "an entity exactly on the radius boundary is damaged");
            h.assertTrue(outsideEntity.isAlive() && outsideEntity.getHealth() == outsideEntity.getMaxHealth() && cornerEntity.isAlive() && cornerEntity.getHealth() == cornerEntity.getMaxHealth(), "entities just outside the sphere and in its cube corner remain unharmed");
        } finally {
            // Drain even when a red assertion fails, while marker-only protection is still installed.
            for (int i = 0; i < 80; i++) LaserExplosion.tick(new LevelTickEvent.Post(() -> true, h.getLevel()));
            NeoForge.EVENT_BUS.unregister(observer); NeoForge.EVENT_BUS.unregister(protection);
            NeoForge.EVENT_BUS.unregister(audio);
            insideEntity.discard(); outsideEntity.discard(); cornerEntity.discard();
        }
        h.succeed();
    }

    @GameTest(template = "laser_empty", batch = "laser_explosion")
    public static void disabledExplosionKeepsBlocksAndEntitiesAndSendsNoExplosionEventOrSound(GameTestHelper h) {
        var host = receiver(h);
        var value = LaserSettings.SPEC.getValues().get("explosionEnabled");
        h.assertTrue(value instanceof ModConfigSpec.BooleanValue, "the laser config exposes explosionEnabled");
        var enabled = (ModConfigSpec.BooleanValue) value;
        boolean previous = enabled.get();
        var marker = host.center().east(8);
        h.getLevel().setBlockAndUpdate(marker, Blocks.STONE.defaultBlockState());
        var subject = bat(h, host.center().getCenter());
        var triggers = new AtomicInteger();
        var sounds = new AtomicInteger();
        Consumer<LaserEvents.BeforeExplosion> observer = event -> { if (event.position.equals(host.getBlockPos())) triggers.incrementAndGet(); };
        Consumer<PlayLevelSoundEvent.AtPosition> audio = event -> { if (event.getLevel() == h.getLevel() && SoundEvents.GENERIC_EXPLODE.equals(event.getSound())) sounds.incrementAndGet(); };
        Consumer<BlockEvent.BreakEvent> protection = event -> event.setCanceled(true);
        NeoForge.EVENT_BUS.addListener(observer); NeoForge.EVENT_BUS.addListener(audio); NeoForge.EVENT_BUS.addListener(protection);
        try (var animation = observeAnimation(h, host.center().getCenter().add(0, 0, 16))) {
            enabled.set(false);
            int loadedChunks = h.getLevel().getChunkSource().getLoadedChunksCount();
            host.particles().load(10);
            LaserExplosion.trigger(host); LaserExplosion.trigger(host);
            // Exercise the real charged-containment path as well as direct triggers.
            h.getLevel().setBlockAndUpdate(host.center().north(3), Blocks.AIR.defaultBlockState());
            host.checkStructure();
            for (int i = 0; i < 80; i++) LaserExplosion.tick(new LevelTickEvent.Post(() -> true, h.getLevel()));
            h.assertTrue(!host.explosionTriggered() && host.particles().stored() == 10, "disabled destruction does not settle an explosion or consume the stored particles");
            h.assertTrue(h.getLevel().getBlockState(marker).is(Blocks.STONE) && subject.isAlive() && subject.getHealth() == subject.getMaxHealth(), "the config switch prevents block destruction and entity damage");
            h.assertTrue(triggers.get() == 0 && sounds.get() == 0, "the disabled path never posts an explosion or broadcasts an explosion sound");
            h.assertTrue(animation.packets().stream().noneMatch(packet -> packet.getParticle().getType() == ParticleTypes.EXPLOSION_EMITTER || packet.getParticle().getType() == ParticleTypes.EXPLOSION), "the disabled path sends no native explosion animation");
            h.assertTrue(h.getLevel().getChunkSource().getLoadedChunksCount() == loadedChunks, "the disabled path does not load or generate extra chunks");
        } finally {
            enabled.set(previous); subject.discard();
            NeoForge.EVENT_BUS.unregister(observer); NeoForge.EVENT_BUS.unregister(audio); NeoForge.EVENT_BUS.unregister(protection);
        }
        h.succeed();
    }

    @GameTest(template = "laser_empty", batch = "laser_explosion")
    public static void explosionUsesRegisteredVanillaSoundAndAnimation(GameTestHelper h) {
        h.assertTrue(BuiltInRegistries.SOUND_EVENT.getKey(SoundEvents.GENERIC_EXPLODE.value()).equals(ResourceLocation.parse("minecraft:entity.generic.explode")), "the explosion sound resolves to its vanilla registered sound");
        h.assertTrue(BuiltInRegistries.PARTICLE_TYPE.getKey(ParticleTypes.EXPLOSION_EMITTER).equals(ResourceLocation.parse("minecraft:explosion_emitter")) && ParticleTypes.EXPLOSION_EMITTER.getOverrideLimiter(), "the native explosion animation is registered and always renderable");
        h.succeed();
    }

    @GameTest(template = "laser_empty", batch = "laser_explosion")
    public static void disablingConfigDiscardsTheRemainingDestructionJob(GameTestHelper h) {
        var host = receiver(h);
        var enabled = (ModConfigSpec.BooleanValue) LaserSettings.SPEC.getValues().get("explosionEnabled");
        boolean previous = enabled.get();
        var marker = host.center().east(8);
        h.getLevel().setBlockAndUpdate(marker, Blocks.STONE.defaultBlockState());
        var attempts = new AtomicInteger();
        Consumer<BlockEvent.BreakEvent> protection = event -> { attempts.incrementAndGet(); event.setCanceled(true); };
        NeoForge.EVENT_BUS.addListener(protection);
        try {
            enabled.set(true);
            LaserExplosion.trigger(host);
            enabled.set(false);
            LaserExplosion.tick(new LevelTickEvent.Post(() -> true, h.getLevel()));
            enabled.set(true);
            for (int i = 0; i < 80; i++) LaserExplosion.tick(new LevelTickEvent.Post(() -> true, h.getLevel()));
            h.assertTrue(attempts.get() == 0 && h.getLevel().getBlockState(marker).is(Blocks.STONE), "disabling config clears the queued blast rather than resuming it after re-enabling");
        } finally {
            enabled.set(previous); NeoForge.EVENT_BUS.unregister(protection);
        }
        h.succeed();
    }

    @SuppressWarnings("deprecation")
    @GameTest(template = "laser_empty", batch = "laser_explosion")
    public static void explosionEventSupportsBlockRadiusAndLegacyChunkHandlers(GameTestHelper h) {
        var host = receiver(h);
        var legacy = new LaserEvents.BeforeExplosion(h.getLevel(), host.getBlockPos(), "compatibility", 1);
        h.assertTrue(legacy.radiusBlocks() == 12 && legacy.radiusChunks() == 1, "existing four-argument event construction selects the bounded twelve-block sphere");
        legacy.setRadiusChunks(0);
        h.assertTrue(legacy.radiusBlocks() == 0 && legacy.radiusChunks() == 0, "the historical setter and getter remain available with zero-radius mapping");
        legacy.setRadiusChunks(1);
        boolean rejected = false;
        try { legacy.setRadiusBlocks(13); } catch (IllegalArgumentException expected) { rejected = true; }
        h.assertTrue(rejected && legacy.radiusBlocks() == 12, "event handlers cannot expand a receiver blast beyond twelve blocks");
        var boundary = host.center().east(5);
        var outside = host.center().east(6);
        var corner = host.center().offset(4, 0, 4);
        for (var pos : Set.of(boundary, outside, corner)) h.getLevel().setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
        var subject = bat(h, host.center().getCenter().add(6, 0, 0));
        Consumer<LaserEvents.BeforeExplosion> limiter = event -> {
            if (event.position.equals(host.getBlockPos())) {
                h.assertTrue(event.center.equals(host.center()), "the new event exposes the optical center while position identifies the controller");
                event.setRadiusBlocks(5);
            }
        };
        Consumer<BlockEvent.BreakEvent> protection = event -> { if (!Set.of(boundary, outside, corner).contains(event.getPos())) event.setCanceled(true); };
        NeoForge.EVENT_BUS.addListener(limiter); NeoForge.EVENT_BUS.addListener(protection);
        try {
            LaserExplosion.trigger(host);
            for (int i = 0; i < 80; i++) LaserExplosion.tick(new LevelTickEvent.Post(() -> true, h.getLevel()));
            h.assertTrue(h.getLevel().getBlockState(boundary).isAir() && h.getLevel().getBlockState(outside).is(Blocks.STONE) && h.getLevel().getBlockState(corner).is(Blocks.STONE), "a block-radius handler actually narrows the spherical destruction boundary");
            h.assertTrue(subject.isAlive() && subject.getHealth() == subject.getMaxHealth(), "the reduced event radius also bounds real entity damage");
        } finally {
            for (int i = 0; i < 80; i++) LaserExplosion.tick(new LevelTickEvent.Post(() -> true, h.getLevel()));
            NeoForge.EVENT_BUS.unregister(limiter); NeoForge.EVENT_BUS.unregister(protection); subject.discard();
        }
        h.succeed();
    }

    @GameTest(template = "laser_empty", batch = "laser_explosion")
    public static void canceledExplosionLeavesResourcesAndWorldUntouched(GameTestHelper h) {
        var host = receiver(h);
        var marker = host.center().east(8);
        h.getLevel().setBlockAndUpdate(marker, Blocks.STONE.defaultBlockState());
        var subject = bat(h, host.center().getCenter());
        Consumer<LaserEvents.BeforeExplosion> cancel = event -> { if (event.position.equals(host.getBlockPos())) event.setCanceled(true); };
        Consumer<BlockEvent.BreakEvent> protection = event -> event.setCanceled(true);
        NeoForge.EVENT_BUS.addListener(cancel); NeoForge.EVENT_BUS.addListener(protection);
        try (var animation = observeAnimation(h, host.center().getCenter().add(0, 0, 16))) {
            host.particles().load(10);
            LaserExplosion.trigger(host);
            for (int i = 0; i < 80; i++) LaserExplosion.tick(new LevelTickEvent.Post(() -> true, h.getLevel()));
            h.assertTrue(!host.explosionTriggered() && host.particles().stored() == 10 && host.enabled(), "canceling the optional event preserves the enabled receiver and its particles");
            h.assertTrue(h.getLevel().getBlockState(marker).is(Blocks.STONE) && subject.isAlive() && subject.getHealth() == subject.getMaxHealth(), "event cancellation prevents destruction and damage");
            h.assertTrue(animation.packets().stream().noneMatch(packet -> packet.getParticle().getType() == ParticleTypes.EXPLOSION_EMITTER), "event cancellation prevents the native explosion animation");
        } finally {
            NeoForge.EVENT_BUS.unregister(cancel); NeoForge.EVENT_BUS.unregister(protection); subject.discard();
        }
        h.succeed();
    }
}
