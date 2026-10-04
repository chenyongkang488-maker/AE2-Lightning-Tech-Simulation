package dev.overloadsim.client;
import dev.overloadsim.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterRenderBuffersEvent;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.inventory.InventoryMenu;
@EventBusSubscriber(modid=OverloadSimulation.ID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public class ClientRegistration {
    @SubscribeEvent public static void receiverGlassBuffer(RegisterRenderBuffersEvent event){
        var glass=RenderType.entityTranslucent(InventoryMenu.BLOCK_ATLAS);
        // Keep glass queued until LevelRenderer has flushed the opaque block-atlas sheet.
        var buffer=new ByteBufferBuilder(glass.bufferSize());
        try{event.registerRenderBuffer(glass,buffer);}
        catch(IllegalStateException duplicate){
            // NeoForge throws only for an existing key here: another mod already supplied the fixed buffer.
            buffer.close();
        }
    }
    @SubscribeEvent public static void shellModels(net.neoforged.neoforge.client.event.ModelEvent.ModifyBakingResult event){
        for(var state:dev.overloadsim.multiblock.MultiblockContent.FRAME.get().getStateDefinition().getPossibleStates())wrap(event,state,SimulationShellModel.Kind.FRAME);
        for(var state:dev.overloadsim.multiblock.MultiblockContent.CONTROLLER.get().getStateDefinition().getPossibleStates())wrap(event,state,SimulationShellModel.Kind.CONTROLLER);
        var port=net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.parse("ae2lt:overloaded_interface"));
        for(var state:port.getStateDefinition().getPossibleStates())wrap(event,state,SimulationShellModel.Kind.PORT);
        for(var block:java.util.List.of(dev.overloadsim.laser.LaserContent.FRAME.get(),dev.overloadsim.laser.LaserContent.GLASS.get()))
            for(var state:block.getStateDefinition().getPossibleStates())
                event.getModels().computeIfPresent(net.minecraft.client.renderer.block.BlockModelShaper.stateToModelLocation(state),(key,model)->new ReceiverShellModel(model));
    }
    private static void wrap(net.neoforged.neoforge.client.event.ModelEvent.ModifyBakingResult event,net.minecraft.world.level.block.state.BlockState state,SimulationShellModel.Kind kind){
        event.getModels().computeIfPresent(net.minecraft.client.renderer.block.BlockModelShaper.stateToModelLocation(state),(key,model)->new SimulationShellModel(model,kind));
    }
    @SubscribeEvent public static void screens(RegisterMenuScreensEvent event){event.register(ModContent.MENU.get(),SimulationScreen::new);event.register(ModContent.COIL_MENU.get(),CoilScreen::new);event.register(dev.overloadsim.multiblock.MultiblockContent.MENU.get(),MultiblockSimulationScreen::new);event.register(dev.overloadsim.laser.LaserContent.MENU.get(),LaserScreen::new);}
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event){event.registerBlockEntityRenderer(ModContent.CHAMBER_ENTITY.get(),SimulationChamberRenderer::new);event.registerBlockEntityRenderer(dev.overloadsim.multiblock.MultiblockContent.CONTROLLER_ENTITY.get(),MultiblockSimulationRenderer::new);event.registerBlockEntityRenderer(dev.overloadsim.laser.LaserContent.MACHINE_ENTITY.get(),LaserRenderer::new);}
    @SubscribeEvent public static void extraModels(net.neoforged.neoforge.client.event.ModelEvent.RegisterAdditional event){
        for(var name:java.util.List.of("simulation_emitter","simulation_shell_atlas","simulation_energy_orb","simulation_energy_orb_orange","simulation_controller_ecg"))
            event.register(net.minecraft.client.resources.model.ModelResourceLocation.standalone(ModContent.id("block/"+name)));
    }
}
