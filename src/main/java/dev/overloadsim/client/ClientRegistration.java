package dev.overloadsim.client;
import dev.overloadsim.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
@EventBusSubscriber(modid=OverloadSimulation.ID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public class ClientRegistration {
    @SubscribeEvent public static void screens(RegisterMenuScreensEvent event){event.register(ModContent.MENU.get(),SimulationScreen::new);}
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event){event.registerBlockEntityRenderer(ModContent.CHAMBER_ENTITY.get(),SimulationChamberRenderer::new);}
}
