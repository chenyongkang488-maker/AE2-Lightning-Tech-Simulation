package dev.overloadsim.multiblock;
import java.util.*;
import appeng.menu.AEBaseMenu;
import appeng.menu.implementations.MenuTypeBuilder;
import appeng.menu.guisync.GuiSync;
import appeng.menu.SlotSemantics;
import appeng.menu.slot.AppEngSlot;
import appeng.api.inventories.PlatformInventoryWrapper;
import dev.overloadsim.ModContent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.network.PacketDistributor;
public class MultiblockSimulationMenu extends AEBaseMenu {
    public static final int OUTPUT_START=49,VISIBLE_OUTPUTS=32,PAGES=4;
    @GuiSync(30) public int capacity;
    @GuiSync(31) public int remaining;
    @GuiSync(32) public int duration;
    @GuiSync(33) public int participants;
    @GuiSync(34) public int fe;
    @GuiSync(35) public int status;
    @GuiSync(36) public int page;
    @GuiSync(37) public long multiplier;
    @GuiSync(38) public long hv;
    @GuiSync(39) public long ehv;
    private final SimulationControllerBlockEntity host;
    private final ItemStackHandler display=new ItemStackHandler(VISIBLE_OUTPUTS);
    private final int[] quantities=new int[VISIBLE_OUTPUTS];
    private int revision;private boolean force=true;
    public static MenuType<MultiblockSimulationMenu> createType(){return MenuTypeBuilder.create(MultiblockSimulationMenu::new,SimulationControllerBlockEntity.class).withMenuTitle(h->Component.translatable("block.overload_sim.simulation_controller")).buildUnregistered(ModContent.id("simulation_controller"));}
    public MultiblockSimulationMenu(int id,Inventory inventory,SimulationControllerBlockEntity host){
        super(MultiblockContent.MENU.get(),id,inventory,host);this.host=host;capacity=host.structure()==null?0:host.structure().capacity();
        var crystals=new PlatformInventoryWrapper(host.crystals());var output=new PlatformInventoryWrapper(display);
        for(int i=0;i<49;i++){final int slot=i;addSlot(new AppEngSlot(crystals,i){
            @Override public boolean mayPickup(Player p){return !host.busy();}
            @Override public boolean mayPlace(ItemStack item){return slot<capacity&&!host.busy()&&super.mayPlace(item);}
        },SlotSemantics.MACHINE_INPUT);}
        for(int i=0;i<VISIBLE_OUTPUTS;i++)addSlot(new AppEngSlot(output,i){
            @Override public boolean mayPlace(ItemStack item){return false;}
            @Override public boolean mayPickup(Player p){return false;}
        },SlotSemantics.MACHINE_OUTPUT);
        createPlayerInventorySlots(inventory);
        registerClientAction("bulkPage",Integer.class,p->{if(p>=0&&p<PAGES&&stillValid(inventory.player)){page=p;force=true;refresh();}});
    }
    public SimulationControllerBlockEntity host(){return host;}
    public int quantity(int slot){return quantities[slot];}
    public int revision(){return revision;}
    public void changePage(int value){sendClientAction("bulkPage",value);}
    public void requestTake(int visible,int mode){net.neoforged.neoforge.network.PacketDistributor.sendToServer(new MultiblockPackets.Take(containerId,page,revision,visible,mode));}
    @Override public void clicked(int slot,int button,ClickType click,Player player){if(slot>=OUTPUT_START&&slot<OUTPUT_START+VISIBLE_OUTPUTS)return;super.clicked(slot,button,click,player);}
    public void take(ServerPlayer player,int expectedPage,int expectedRevision,int visible,int mode){
        if(player.containerMenu!=this||!stillValid(player)||visible<0||visible>=VISIBLE_OUTPUTS||mode<0||mode>2)return;
        refresh();if(expectedPage!=page||expectedRevision!=revision)return;
        int slot=page*VISIBLE_OUTPUTS+visible;var buffer=host.outputs();
        if(mode==2){
            while(buffer.count(slot)>0){
                var offered=buffer.extract(slot,Integer.MAX_VALUE,true);int before=offered.getCount();player.getInventory().add(offered);
                int moved=before-offered.getCount();if(moved==0)break;buffer.debit(slot,moved);
            }
        }else{
            var prototype=buffer.prototype(slot);var carried=getCarried();
            if(prototype.isEmpty()||!carried.isEmpty()&&!ItemStack.isSameItemSameComponents(carried,prototype))return;
            int space=prototype.getMaxStackSize()-carried.getCount();
            var taken=buffer.extract(slot,Math.min(space,mode==1?1:space),false);
            if(taken.isEmpty())return;
            if(carried.isEmpty())setCarried(taken);else{carried.grow(taken.getCount());setCarried(carried);}
        }host.saveChanges();force=true;refresh();broadcastChanges();
    }
    private void refresh(){
        if(!isServerSide())return;boolean changed=force;
        for(int i=0;i<VISIBLE_OUTPUTS;i++){
            int slot=page*VISIBLE_OUTPUTS+i;var item=host.outputs().prototype(slot);int count=host.outputs().count(slot);
            if(quantities[i]!=count||!ItemStack.matches(display.getStackInSlot(i),item))changed=true;
            quantities[i]=count;display.setStackInSlot(i,item);
        }
        if(changed&&getPlayer() instanceof ServerPlayer player){
            revision++;force=false;var items=new ArrayList<ItemStack>();var counts=new ArrayList<Integer>();
            for(int i=0;i<VISIBLE_OUTPUTS;i++){items.add(display.getStackInSlot(i).copy());counts.add(quantities[i]);}
            PacketDistributor.sendToPlayer(player,new MultiblockPackets.Snapshot(containerId,page,revision,items,counts));
        }
    }
    public void applySnapshot(MultiblockPackets.Snapshot snapshot){
        if(isServerSide())return;page=snapshot.page();revision=snapshot.revision();
        for(int i=0;i<VISIBLE_OUTPUTS;i++){display.setStackInSlot(i,snapshot.items().get(i));quantities[i]=snapshot.counts().get(i);}
    }
    @Override public void broadcastChanges(){
        if(isServerSide()){
            var s=host.structure();capacity=s==null?0:s.capacity();fe=host.energy().getEnergyStored();status=host.status();
            var batch=host.batch();remaining=batch==null?0:batch.remaining;duration=batch==null?s==null?180:MultiblockData.policy().duration(s):batch.duration;
            participants=batch==null?0:batch.inputs.size();multiplier=s==null?1:MultiblockData.policy().multiplier(s);
            hv=host.bridge().extract(false,Long.MAX_VALUE,true);ehv=host.bridge().extract(true,Long.MAX_VALUE,true);refresh();
        }super.broadcastChanges();
    }
    @Override public boolean stillValid(Player player){return !host.isRemoved()&&player.level()==host.getLevel()&&player.distanceToSqr(host.getBlockPos().getCenter())<=64;}
    @Override protected List<Slot> getQuickMoveDestinationSlots(ItemStack item,boolean fromPlayer){
        if(!fromPlayer)return super.getQuickMoveDestinationSlots(item,false);
        return getSlots(SlotSemantics.MACHINE_INPUT).stream().filter(s->s.mayPlace(item)).toList();
    }
}
