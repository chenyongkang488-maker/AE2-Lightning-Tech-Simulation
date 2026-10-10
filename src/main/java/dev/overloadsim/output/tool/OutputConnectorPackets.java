package dev.overloadsim.output.tool;

import dev.overloadsim.ModContent;
import dev.overloadsim.OverloadSimulation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@EventBusSubscriber(modid = OverloadSimulation.ID, bus = EventBusSubscriber.Bus.MOD)
public final class OutputConnectorPackets {
    public record Use(InteractionHand hand, int heldSlot, BlockPos pos, Direction face, boolean batch)
            implements CustomPacketPayload {
        public static final Type<Use> TYPE = new Type<>(ModContent.id("output_connector_use"));
        public static final StreamCodec<FriendlyByteBuf, Use> CODEC = StreamCodec.ofMember(Use::write, Use::read);
        private static Use read(FriendlyByteBuf b) {
            return new Use(b.readEnum(InteractionHand.class), b.readVarInt(), b.readBlockPos(),
                    b.readEnum(Direction.class), b.readBoolean());
        }
        private void write(FriendlyByteBuf b) {
            b.writeEnum(hand); b.writeVarInt(heldSlot); b.writeBlockPos(pos); b.writeEnum(face); b.writeBoolean(batch);
        }
        public Type<Use> type() { return TYPE; }
    }
    public record Cycle(int heldSlot, int steps) implements CustomPacketPayload {
        public static final Type<Cycle> TYPE = new Type<>(ModContent.id("output_connector_cycle"));
        public static final StreamCodec<FriendlyByteBuf, Cycle> CODEC = StreamCodec.ofMember(Cycle::write, Cycle::read);
        private static Cycle read(FriendlyByteBuf b) { return new Cycle(b.readVarInt(), b.readVarInt()); }
        private void write(FriendlyByteBuf b) { b.writeVarInt(heldSlot); b.writeVarInt(steps); }
        public Type<Cycle> type() { return TYPE; }
    }
    @SubscribeEvent public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToServer(Use.TYPE, Use.CODEC, (packet, context) -> context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player)
                OutputConnector.use(player, packet.hand(), packet.heldSlot(), packet.pos(), packet.face(), packet.batch());
        }));
        registrar.playToServer(Cycle.TYPE, Cycle.CODEC, (packet, context) -> context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player)
                OutputConnector.cycle(player, packet.heldSlot(), packet.steps());
        }));
    }
}
