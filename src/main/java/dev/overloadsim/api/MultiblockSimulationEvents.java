package dev.overloadsim.api;
import dev.overloadsim.multiblock.*;
import net.neoforged.bus.api.*;
public final class MultiblockSimulationEvents {
    public static class BeforeStructureForm extends Event implements ICancellableEvent {
        public final SimulationControllerBlockEntity controller;public final SimulationStructure structure;
        public BeforeStructureForm(SimulationControllerBlockEntity c,SimulationStructure s){controller=c;structure=s;}
    }
    public static class Formed extends Event {
        public final SimulationControllerBlockEntity controller;public final SimulationStructure structure;
        public Formed(SimulationControllerBlockEntity c,SimulationStructure s){controller=c;structure=s;}
    }
    public static class Invalidated extends Event {
        public final SimulationControllerBlockEntity controller;public final SimulationStructure structure;
        public Invalidated(SimulationControllerBlockEntity c,SimulationStructure s){controller=c;structure=s;}
    }
    public static class BeforeBatchStart extends Event implements ICancellableEvent {
        public final SimulationControllerBlockEntity controller;public final SimulationBatch batch;
        public BeforeBatchStart(SimulationControllerBlockEntity c,SimulationBatch b){controller=c;batch=b;}
    }
    public static class Completed extends Event {
        public final SimulationControllerBlockEntity controller;public final SimulationBatch batch;
        public Completed(SimulationControllerBlockEntity c,SimulationBatch b){controller=c;batch=b;}
    }
    public static class BeforeBatchCommit extends Event implements ICancellableEvent {
        public final SimulationControllerBlockEntity controller;public final SimulationBatch batch;
        public BeforeBatchCommit(SimulationControllerBlockEntity c,SimulationBatch b){controller=c;batch=b;}
    }
    public static class BatchAborted extends Event {
        public final SimulationControllerBlockEntity controller;public final SimulationBatch batch;public final String reason;
        public BatchAborted(SimulationControllerBlockEntity c,SimulationBatch b,String reason){controller=c;batch=b;this.reason=reason;}
    }
}
