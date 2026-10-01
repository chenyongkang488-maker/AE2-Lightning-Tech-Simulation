package dev.overloadsim.tool;

import dev.overloadsim.ModContent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

public final class CoilMenu extends AbstractContainerMenu {
    private final Inventory inventory;private final int inventorySlot;private final ItemStack original;
    private final ContainerData data=new SimpleContainerData(8);
    public CoilMenu(int id,Inventory inventory,int inventorySlot){super(ModContent.COIL_MENU.get(),id);this.inventory=inventory;this.inventorySlot=inventorySlot;original=inventory.getItem(inventorySlot);addDataSlots(data);refresh();}
    public static void open(ServerPlayer player){
        int slot=CoilModules.isCoil(player.getMainHandItem())?player.getInventory().selected:CoilModules.isCoil(player.getOffhandItem())?40:-1;if(slot<0)return;
        player.openMenu(new MenuProvider(){public Component getDisplayName(){return Component.translatable("item.overload_sim.resonance_coil");}public AbstractContainerMenu createMenu(int id,Inventory inv,Player p){return new CoilMenu(id,inv,slot);}},buf->buf.writeVarInt(slot));
    }
    public ItemStack stack(){return inventory.getItem(inventorySlot);}
    @Override public boolean stillValid(Player player){return player.isAlive()&&(inventorySlot==40||inventory.selected==inventorySlot)&&stack()==original&&CoilModules.isCoil(stack());}
    @Override public ItemStack quickMoveStack(Player p,int slot){return ItemStack.EMPTY;}
    public int value(int index){return data.get(index);}
    public boolean installed(CoilModuleItem.Type type){return (data.get(5)&(1<<type.ordinal()))!=0;}
    /** Only the server calls this. Illegal actions never mutate the held stack. */
    public boolean configure(Player player,int action,int value){
        if(inventory.player.level().isClientSide()||!stillValid(player))return false;
        if(!CoilConfiguration.apply(stack(),action,value))return false;
        inventory.setChanged();broadcastChanges();return true;
    }

    private void refresh(){
        if(inventory.player.level().isClientSide())return;var s=stack();var settings=CoilSettings.read(s);
        data.set(0,settings.extreme()?1:0);data.set(1,settings.efficiencyEnabled()?1:0);data.set(2,settings.efficiency());data.set(3,settings.fortune());data.set(4,settings.silk()?1:0);
        int mask=0;for(var type:CoilModuleItem.Type.values())if(CoilModules.has(s,type))mask|=1<<type.ordinal();data.set(5,mask);data.set(6,CoilModules.hasCore(s)?1:0);data.set(7,(int)(CoilEnergy.read(s)*10000/CoilModules.capacity(s)));
    }
    @Override public void broadcastChanges(){refresh();super.broadcastChanges();}
}
