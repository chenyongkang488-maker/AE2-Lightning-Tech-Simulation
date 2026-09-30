package dev.overloadsim.api;

import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;

public final class SimulationEvents {
    private SimulationEvents() {}
    public static class BeforeBinding extends Event implements ICancellableEvent {
        public final ServerLevel level; public final BlockPos position; public final CrystalData data;
        public BeforeBinding(ServerLevel level, BlockPos position, CrystalData data) { this.level=level; this.position=position.immutable(); this.data=data; }
    }
    public static class BeforeCultivation extends Event implements ICancellableEvent {
        public final CrystalData data; public final ServerLevel level; public final BlockPos position;
        public BeforeCultivation(ServerLevel level, BlockPos position, CrystalData data) { this.level=level; this.position=position.immutable(); this.data=data; }
    }
    public static class BeforeSimulation extends Event implements ICancellableEvent {
        public final ServerLevel level; public final BlockPos position; public final CrystalData data; public final int parallel;
        public BeforeSimulation(ServerLevel level, BlockPos position, CrystalData data, int parallel) { this.level=level; this.position=position.immutable(); this.data=data; this.parallel=parallel; }
    }
    public static class Completed extends Event {
        public final String operation; public final CrystalData data; public final ServerLevel level; public final BlockPos position;
        public Completed(String operation, ServerLevel level, BlockPos position, CrystalData data) { this.operation=operation; this.level=level; this.position=position.immutable(); this.data=data; }
    }
}
