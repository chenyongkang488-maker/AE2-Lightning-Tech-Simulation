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
        if(System.getProperty("neoforge.enabledGameTestNamespaces","").contains(ID))dev.overloadsim.gametest.CompatibilityFixtures.register();
        ModContent.register(bus);container.registerConfig(ModConfig.Type.COMMON,SimulationConfig.SPEC);
        dev.overloadsim.multiblock.SimulationStructureIndex.register();
        NeoForge.EVENT_BUS.addListener(dev.overloadsim.command.SimulationDiagnostics::register);
        NeoForge.EVENT_BUS.addListener((AddReloadListenerEvent e)->e.addListener(new SimulationData()));
        NeoForge.EVENT_BUS.addListener((AddReloadListenerEvent e)->e.addListener(new dev.overloadsim.data.MobSimulationData()));
        NeoForge.EVENT_BUS.addListener((AddReloadListenerEvent e)->e.addListener(new dev.overloadsim.data.MineralSimulationData()));
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.TagsUpdatedEvent e)->{SimulationData.invalidate();dev.overloadsim.binding.SimulationEntityEligibility.rebuildEggIndex();});
        NeoForge.EVENT_BUS.addListener((AddReloadListenerEvent e)->e.addListener(new dev.overloadsim.multiblock.MultiblockData()));
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.LOWEST,true,PlayerLightningHandler::struck);
        bus.addListener(ModContent::capabilities);
        bus.addListener(ModContent::setup);
    }
}
