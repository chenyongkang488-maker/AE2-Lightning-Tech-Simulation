package dev.overloadsim.gametest;
import dev.overloadsim.OverloadSimulation;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.server.*;
import net.minecraft.server.players.GameProfileCache;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;

/** Native security requires the profile cache omitted by the vanilla GameTest server. */
@EventBusSubscriber(modid=OverloadSimulation.ID)
public final class CoilGameTestEnvironment {
    @SubscribeEvent public static void prepare(ServerAboutToStartEvent event){
        var server=event.getServer();if(!(server instanceof GameTestServer)||server.getProfileCache()!=null)return;
        try{
            var field=MinecraftServer.class.getDeclaredField("services");field.setAccessible(true);var old=(Services)field.get(server);
            var cache=new GameProfileCache(old.profileRepository(),new java.io.File("coil-test-usercache.json"));
            field.set(server,new Services(old.sessionService(),old.servicesKeySet(),old.profileRepository(),cache));
        }catch(ReflectiveOperationException e){throw new IllegalStateException("Cannot prepare GameTest profile cache",e);}
    }
}
