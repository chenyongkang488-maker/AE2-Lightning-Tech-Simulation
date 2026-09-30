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
        var semantics=new appeng.menu.SlotSemantic[]{SlotSemantics.STORAGE_CELL,SlotSemantics.CONFIG,SlotSemantics.MACHINE_INPUT};
        var inputs=new int[]{0,1,3};
        for(int i=0;i<inputs.length;i++)addSlot(new AppEngSlot(wrapped,inputs[i]){@Override public boolean mayPickup(Player p){return !host.busy();}@Override public boolean mayPlace(ItemStack s){return !host.busy()&&super.mayPlace(s);}},semantics[i]);
        for(int i=0;i<9;i++)addSlot(new AppEngSlot(wrapped,i+4){@Override public boolean mayPlace(ItemStack s){return false;}},SlotSemantics.MACHINE_OUTPUT);
        for(int i=0;i<4;i++){var upgrade=new AppEngSlot(host.getUpgrades(),i){@Override public boolean mayPickup(Player p){return !host.busy();}};upgrade.setIcon(appeng.client.gui.Icon.BACKGROUND_UPGRADE);addSlot(upgrade,SlotSemantics.UPGRADE);}
        createPlayerInventorySlots(inv);
        registerClientAction("eject",host::toggleEject);registerClientAction("side",Integer.class,host::toggleSide);
        registerClientAction("clearSides",()->{for(int side=0;side<6;side++)if((host.outputMask()&(1<<side))!=0)host.toggleSide(side);});
    }
    public void toggleEject(){sendClientAction("eject");}public void toggleSide(int side){sendClientAction("side",side);}
    public void clearSides(){sendClientAction("clearSides");}
    public ItemStack neighborIcon(int side){var level=host.getLevel();var pos=host.getBlockPos().relative(net.minecraft.core.Direction.from3DDataValue(side));return level!=null&&level.hasChunkAt(pos)?new ItemStack(level.getBlockState(pos).getBlock()):ItemStack.EMPTY;}
    @Override public void broadcastChanges(){if(isServerSide()){fe=host.energy().getEnergyStored();remaining=host.remaining();duration=host.totalTicks();parallel=host.actualParallel();maximum=host.maximumParallel();status=host.status();eject=host.eject();outputMask=host.outputMask();frequency=host.getFrequencyId();highVoltage=dev.overloadsim.compat.LightningNetwork.extract(host,Long.MAX_VALUE,true);}super.broadcastChanges();}
    @Override public boolean stillValid(Player player){return !host.isRemoved()&&player.level()==host.getLevel()&&player.distanceToSqr(host.getBlockPos().getCenter())<=64;}
    @Override protected java.util.List<Slot> getQuickMoveDestinationSlots(ItemStack stack,boolean fromPlayerSide){
        if(!fromPlayerSide)return super.getQuickMoveDestinationSlots(stack,false);
        var targets=stack.is(appeng.core.definitions.AEItems.SPEED_CARD.asItem())?getSlots(SlotSemantics.UPGRADE).stream():java.util.stream.Stream.of(getSlots(SlotSemantics.STORAGE_CELL),getSlots(SlotSemantics.CONFIG),getSlots(SlotSemantics.MACHINE_INPUT)).flatMap(java.util.Collection::stream);
        return targets.filter(slot->slot.mayPlace(stack)).toList();
    }
}
