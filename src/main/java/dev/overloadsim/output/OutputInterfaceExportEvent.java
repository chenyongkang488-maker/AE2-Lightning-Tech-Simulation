package dev.overloadsim.output;

import appeng.api.stacks.AEKey;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

/** Before each output insertion, on the server thread. SIMULATE must not mutate resources. */
public final class OutputInterfaceExportEvent extends Event implements ICancellableEvent {
    private final OutputInterfaceBlockEntity source;
    private final int sourceSlot;
    private final OutputConnection target;
    private final AEKey key;
    private final boolean simulate;
    private final long maximum;
    private long amount;
    public OutputInterfaceExportEvent(OutputInterfaceBlockEntity source,int sourceSlot,OutputConnection target,AEKey key,long amount,boolean simulate){this.source=source;this.sourceSlot=sourceSlot;this.target=target;this.key=key;this.amount=this.maximum=amount;this.simulate=simulate;}
    public OutputInterfaceBlockEntity getSource(){return source;}
    public int getSourceSlot(){return sourceSlot;}
    public OutputConnection getTarget(){return target;}
    public AEKey getKey(){return key;}
    public boolean isSimulation(){return simulate;}
    public long getAmount(){return amount;}
    public void setAmount(long amount){this.amount=Math.max(0,Math.min(maximum,amount));}
}
