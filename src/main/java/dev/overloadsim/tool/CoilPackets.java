package dev.overloadsim.tool;

import dev.overloadsim.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid=OverloadSimulation.ID,bus=EventBusSubscriber.Bus.MOD)
public final class CoilPackets {
    public record Open() implements CustomPacketPayload {
        public static final Type<Open> TYPE=new Type<>(ModContent.id("open_coil"));public static final StreamCodec<FriendlyByteBuf,Open> CODEC=StreamCodec.unit(new Open());
        public Type<Open> type(){return TYPE;}
    }
    public record Configure(int menuId,int action,int value) implements CustomPacketPayload {
        public static final Type<Configure> TYPE=new Type<>(ModContent.id("configure_coil"));
        public static final StreamCodec<FriendlyByteBuf,Configure> CODEC=StreamCodec.composite(ByteBufCodecs.VAR_INT,Configure::menuId,ByteBufCodecs.VAR_INT,Configure::action,ByteBufCodecs.VAR_INT,Configure::value,Configure::new);
        public Type<Configure> type(){return TYPE;}
    }
    @SubscribeEvent public static void register(RegisterPayloadHandlersEvent e){var r=e.registrar("1");
        r.playToServer(Open.TYPE,Open.CODEC,(packet,context)->{if(context.player() instanceof ServerPlayer player)CoilMenu.open(player);});
        r.playToServer(Configure.TYPE,Configure.CODEC,(packet,context)->{if(context.player() instanceof ServerPlayer player&&player.containerMenu instanceof CoilMenu menu&&menu.containerId==packet.menuId())menu.configure(player,packet.action(),packet.value());});
    }
}
