package dev.overloadsim.client;

import dev.overloadsim.OverloadSimulation;
import dev.overloadsim.laser.ReceiverShellVisuals;
import dev.overloadsim.workstation.WorkstationShellVisuals;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;

@EventBusSubscriber(modid = OverloadSimulation.ID, value = Dist.CLIENT)
public final class ReceiverShellClientEvents {
    @SubscribeEvent public static void unloaded(ChunkEvent.Unload event) {
        if (event.getLevel() instanceof Level level && level.isClientSide) {
            ReceiverShellVisuals.unloadChunk(level, event.getChunk().getPos().x, event.getChunk().getPos().z);
            WorkstationShellVisuals.unloadChunk(level, event.getChunk().getPos().x, event.getChunk().getPos().z);
        }
    }
    @SubscribeEvent public static void loaded(ChunkEvent.Load event) {
        if (event.getLevel() instanceof Level level && level.isClientSide) {
            ReceiverShellVisuals.loadedChunk(level);
            WorkstationShellVisuals.loadedChunk(level);
        }
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        ReceiverShellVisuals.refreshPending();
        WorkstationShellVisuals.refreshPending();
    }
}
