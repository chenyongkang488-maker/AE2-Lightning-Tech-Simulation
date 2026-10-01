package dev.overloadsim;
import net.neoforged.fml.common.Mod;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import dev.overloadsim.binding.PlayerLightningHandler;
import dev.overloadsim.data.SimulationData;

@Mod(OverloadSimulation.ID)
public class OverloadSimulation {
    public static final String ID="overload_sim";
    public OverloadSimulation(IEventBus bus,ModContainer container){
        ModContent.register(bus);container.registerConfig(ModConfig.Type.COMMON,SimulationConfig.SPEC);
        NeoForge.EVENT_BUS.addListener((AddReloadListenerEvent e)->e.addListener(new SimulationData()));
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.LOWEST,true,PlayerLightningHandler::struck);
        bus.addListener(ModContent::capabilities);
        bus.addListener(ModContent::setup);
    }
}
