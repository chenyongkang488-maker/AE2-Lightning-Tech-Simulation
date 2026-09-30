package dev.overloadsim.machine;
import appeng.menu.AEBaseMenu;
import appeng.menu.implementations.MenuTypeBuilder;
import appeng.menu.guisync.GuiSync;
import com.moakiee.ae2lt.api.frequency.FrequencyBindingMenuHost;
import dev.overloadsim.ModContent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;
public class SimulationMenu extends AEBaseMenu implements FrequencyBindingMenuHost {
    @GuiSync(30) public int fe;
    @GuiSync(31) public int remaining;
    @GuiSync(32) public int duration;
    @GuiSync(33) public int parallel;
    @GuiSync(34) public int maximum;
    @GuiSync(35) public int status;
    @GuiSync(36) public boolean eject;
    @GuiSync(37) public int outputMask;
    @GuiSync(38) public int frequency;
    private final SimulationChamberBlockEntity host;
    public static MenuType<SimulationMenu> createType(){return MenuTypeBuilder.create(SimulationMenu::new,SimulationChamberBlockEntity.class).withMenuTitle(h->Component.translatable("block.overload_sim.overload_simulation_chamber")).buildUnregistered(ModContent.id("overload_simulation_chamber"));}
    public SimulationMenu(int id,Inventory inv,SimulationChamberBlockEntity host){super(ModContent.MENU.get(),id,inv,host);this.host=host;
        for(int slot=0;slot<4;slot++){final int index=slot;addSlot(new SlotItemHandler(host.inventory(),slot,18+slot*24,36){@Override public boolean mayPickup(Player p){return !host.busy();}@Override public boolean mayPlace(ItemStack s){return !host.busy()&&super.mayPlace(s);}@Override public int getMaxStackSize(){return host.inventory().getSlotLimit(index);}});}
        for(int i=0;i<9;i++)addSlot(new SlotItemHandler(host.inventory(),i+4,126+(i%3)*18,28+(i/3)*18){@Override public boolean mayPlace(ItemStack s){return false;}});
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inv,col+row*9+9,18+col*18,132+row*18));
        for(int col=0;col<9;col++)addSlot(new Slot(inv,col,18+col*18,190));
        registerClientAction("eject",host::toggleEject);registerClientAction("side",Integer.class,host::toggleSide);
    }
    public void toggleEject(){sendClientAction("eject");}public void toggleSide(int side){sendClientAction("side",side);}
    @Override public void broadcastChanges(){if(isServerSide()){fe=host.energy().getEnergyStored();remaining=host.remaining();duration=host.totalTicks();parallel=host.actualParallel();maximum=host.maximumParallel();status=host.status();eject=host.eject();outputMask=host.outputMask();frequency=host.getFrequencyId();}super.broadcastChanges();}
    @Override public boolean stillValid(Player player){return !host.isRemoved()&&player.level()==host.getLevel()&&player.distanceToSqr(host.getBlockPos().getCenter())<=64;}
    @Override public ItemStack quickMoveStack(Player player,int index){if(index<0||index>=slots.size())return ItemStack.EMPTY;var source=slots.get(index);if(!source.hasItem()||!source.mayPickup(player))return ItemStack.EMPTY;var stack=source.getItem();var original=stack.copy();boolean moved=index<13?moveItemStackTo(stack,13,slots.size(),true):moveItemStackTo(stack,0,4,false);if(!moved)return ItemStack.EMPTY;if(stack.isEmpty())source.set(ItemStack.EMPTY);else source.setChanged();return original;}
}
