package dev.overloadsim.multiblock;
import appeng.api.inventories.InternalInventory;
import appeng.menu.slot.AppEngSlot;

public class SimulationCrystalSlot extends AppEngSlot {
    public SimulationCrystalSlot(InternalInventory inventory,int slot){super(inventory,slot);}
    /** Inactive hides interaction; disabled AE slots also discard synchronization packets. */
    public void setVisible(boolean visible){setActive(visible);setSlotEnabled(true);}
}
