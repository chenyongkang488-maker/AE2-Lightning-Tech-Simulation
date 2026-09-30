package dev.overloadsim.machine;
import appeng.menu.AEBaseMenu;
import appeng.menu.implementations.MenuTypeBuilder;
import appeng.menu.guisync.GuiSync;
import appeng.menu.SlotSemantics;
import appeng.menu.slot.AppEngSlot;
import appeng.api.inventories.PlatformInventoryWrapper;
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
    @GuiSync(39) public long highVoltage;
    private final SimulationChamberBlockEntity host;
    public static MenuType<SimulationMenu> createType(){return MenuTypeBuilder.create(SimulationMenu::new,SimulationChamberBlockEntity.class).withMenuTitle(h->Component.translatable("block.overload_sim.overload_simulation_chamber")).buildUnregistered(ModContent.id("overload_simulation_chamber"));}
    public SimulationMenu(int id,Inventory inv,SimulationChamberBlockEntity host){super(ModContent.MENU.get(),id,inv,host);this.host=host;
        var wrapped=new PlatformInventoryWrapper(host.inventory());
        var semantics=new appeng.menu.SlotSemantic[]{SlotSemantics.STORAGE_CELL,SlotSemantics.CONFIG,SlotSemantics.UPGRADE,SlotSemantics.MACHINE_INPUT};
        for(int slot=0;slot<4;slot++)addSlot(new AppEngSlot(wrapped,slot){@Override public boolean mayPickup(Player p){return !host.busy();}@Override public boolean mayPlace(ItemStack s){return !host.busy()&&super.mayPlace(s);}},semantics[slot]);
        for(int i=0;i<9;i++)addSlot(new AppEngSlot(wrapped,i+4){@Override public boolean mayPlace(ItemStack s){return false;}},SlotSemantics.MACHINE_OUTPUT);
        createPlayerInventorySlots(inv);
        registerClientAction("eject",host::toggleEject);registerClientAction("side",Integer.class,host::toggleSide);
        registerClientAction("clearSides",()->{for(int side=0;side<6;side++)if((host.outputMask()&(1<<side))!=0)host.toggleSide(side);});
    }
    public void toggleEject(){sendClientAction("eject");}public void toggleSide(int side){sendClientAction("side",side);}
    public void clearSides(){sendClientAction("clearSides");}
    public ItemStack neighborIcon(int side){var level=host.getLevel();var pos=host.getBlockPos().relative(net.minecraft.core.Direction.from3DDataValue(side));return level!=null&&level.hasChunkAt(pos)?new ItemStack(level.getBlockState(pos).getBlock()):ItemStack.EMPTY;}
    @Override public void broadcastChanges(){if(isServerSide()){fe=host.energy().getEnergyStored();remaining=host.remaining();duration=host.totalTicks();parallel=host.actualParallel();maximum=host.maximumParallel();status=host.status();eject=host.eject();outputMask=host.outputMask();frequency=host.getFrequencyId();highVoltage=dev.overloadsim.compat.LightningNetwork.extract(host,Long.MAX_VALUE,true);}super.broadcastChanges();}
    @Override public boolean stillValid(Player player){return !host.isRemoved()&&player.level()==host.getLevel()&&player.distanceToSqr(host.getBlockPos().getCenter())<=64;}
    @Override public ItemStack quickMoveStack(Player player,int index){if(index<0||index>=slots.size())return ItemStack.EMPTY;var source=slots.get(index);if(!source.hasItem()||!source.mayPickup(player))return ItemStack.EMPTY;var stack=source.getItem();var original=stack.copy();boolean moved=index<13?moveItemStackTo(stack,13,slots.size(),true):moveItemStackTo(stack,0,4,false);if(!moved)return ItemStack.EMPTY;if(stack.isEmpty())source.set(ItemStack.EMPTY);else source.setChanged();return original;}
}
