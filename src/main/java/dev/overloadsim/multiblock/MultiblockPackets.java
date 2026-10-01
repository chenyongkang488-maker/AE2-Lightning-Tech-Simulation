package dev.overloadsim.multiblock;
import java.util.*;
import dev.overloadsim.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
@EventBusSubscriber(modid=OverloadSimulation.ID,bus=EventBusSubscriber.Bus.MOD)
public final class MultiblockPackets {
    public record Take(int menu,int page,int revision,int slot,int mode) implements CustomPacketPayload {
        public static final Type<Take> TYPE=new Type<>(ModContent.id("multiblock_take"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Take> CODEC=StreamCodec.of((buf,p)->{buf.writeVarInt(p.menu);buf.writeVarInt(p.page);buf.writeVarInt(p.revision);buf.writeVarInt(p.slot);buf.writeVarInt(p.mode);},buf->new Take(buf.readVarInt(),buf.readVarInt(),buf.readVarInt(),buf.readVarInt(),buf.readVarInt()));
        public Type<Take> type(){return TYPE;}
    }
    public record Snapshot(int menu,int page,int revision,List<ItemStack> items,List<Integer> counts) implements CustomPacketPayload {
        public static final Type<Snapshot> TYPE=new Type<>(ModContent.id("multiblock_snapshot"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Snapshot> CODEC=StreamCodec.of((buf,p)->{
            buf.writeVarInt(p.menu);buf.writeVarInt(p.page);buf.writeVarInt(p.revision);
            for(int i=0;i<32;i++){ItemStack.OPTIONAL_STREAM_CODEC.encode(buf,p.items.get(i));buf.writeVarInt(p.counts.get(i));}
        },buf->{
            int menu=buf.readVarInt(),page=buf.readVarInt(),revision=buf.readVarInt();if(page<0||page>3)throw new IllegalArgumentException("page");
            var items=new ArrayList<ItemStack>();var counts=new ArrayList<Integer>();
            for(int i=0;i<32;i++){items.add(ItemStack.OPTIONAL_STREAM_CODEC.decode(buf));int count=buf.readVarInt();if(count<0||count>1024)throw new IllegalArgumentException("quantity");counts.add(count);}
            return new Snapshot(menu,page,revision,List.copyOf(items),List.copyOf(counts));
        });
        public Type<Snapshot> type(){return TYPE;}
    }
    @SubscribeEvent public static void register(RegisterPayloadHandlersEvent event){
        var r=event.registrar("1");
        r.playToServer(Take.TYPE,Take.CODEC,(p,c)->{if(c.player() instanceof ServerPlayer player&&player.containerMenu.containerId==p.menu&&player.containerMenu instanceof MultiblockSimulationMenu menu)menu.take(player,p.page,p.revision,p.slot,p.mode);});
        r.playToClient(Snapshot.TYPE,Snapshot.CODEC,(p,c)->{if(c.player().containerMenu.containerId==p.menu&&c.player().containerMenu instanceof MultiblockSimulationMenu menu)menu.applySnapshot(p);});
    }
}
