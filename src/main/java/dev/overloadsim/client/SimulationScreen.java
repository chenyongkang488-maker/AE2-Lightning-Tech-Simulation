package dev.overloadsim.client;
import com.moakiee.ae2lt.api.frequency.FrequencyApi;
import dev.overloadsim.machine.SimulationMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class SimulationScreen extends AbstractContainerScreen<SimulationMenu> {
    private Button ejectButton;private final Button[] sides=new Button[6];
    public SimulationScreen(SimulationMenu menu,Inventory inventory,Component title){super(menu,inventory,title);imageWidth=198;imageHeight=216;inventoryLabelY=120;}
    @Override protected void init(){super.init();addRenderableWidget(Button.builder(Component.translatable("gui.overload_sim.frequency"),b->FrequencyApi.openBindingScreen(menu)).bounds(leftPos+10,topPos+90,80,18).build());ejectButton=addRenderableWidget(Button.builder(Component.translatable("gui.overload_sim.eject"),b->menu.toggleEject()).bounds(leftPos+98,topPos+90,88,18).build());
        for(int i=0;i<6;i++){final int side=i;sides[i]=addRenderableWidget(Button.builder(Component.translatable("direction.overload_sim."+i),b->menu.toggleSide(side)).bounds(leftPos+10+i*29,topPos+111,27,15).build());}
    }
    @Override protected void renderBg(GuiGraphics g,float partial,int mx,int my){g.fill(leftPos,topPos,leftPos+imageWidth,topPos+imageHeight,0xFFF5EAF0);g.fill(leftPos+2,topPos+2,leftPos+imageWidth-2,topPos+20,0xFFE3A6C5);
        for(var slot:menu.slots){g.fill(leftPos+slot.x-1,topPos+slot.y-1,leftPos+slot.x+17,topPos+slot.y+17,0xFFAA7592);g.fill(leftPos+slot.x,topPos+slot.y,leftPos+slot.x+16,topPos+slot.y+16,0xFFFFF8FC);}
        int width=menu.duration<=0?0:(int)(174L*(menu.duration-menu.remaining)/menu.duration);g.fill(leftPos+12,topPos+81,leftPos+186,topPos+86,0xFFD7CED4);g.fill(leftPos+12,topPos+81,leftPos+12+width,topPos+86,0xFFE779B2);
    }
    @Override protected void renderLabels(GuiGraphics g,int mx,int my){g.drawString(font,title,8,6,0xFF553348,false);g.drawString(font,Component.translatable("gui.overload_sim.energy",menu.fe),12,60,0xFF553348,false);g.drawString(font,Component.translatable("gui.overload_sim.parallel",menu.parallel,menu.maximum),12,71,0xFF553348,false);g.drawString(font,Component.translatable("status.overload_sim."+menu.status),12,24,0xFF553348,false);}
    @Override public void render(GuiGraphics g,int mx,int my,float partial){
        if(ejectButton!=null)ejectButton.setMessage(Component.translatable("gui.overload_sim.eject").append(": ").append(Component.translatable(menu.eject?"gui.overload_sim.enabled":"gui.overload_sim.disabled")));
        for(int i=0;i<6;i++)if(sides[i]!=null)sides[i].setMessage(Component.literal((menu.outputMask&(1<<i))!=0?"§d":"§7").append(Component.translatable("direction.overload_sim."+i)));
        super.render(g,mx,my,partial);renderTooltip(g,mx,my);
        if(hoveredSlot!=null&&hoveredSlot.index<4&&hoveredSlot.getItem().isEmpty())g.renderTooltip(font,Component.translatable("gui.overload_sim.slot."+hoveredSlot.index),mx,my);
    }
}
