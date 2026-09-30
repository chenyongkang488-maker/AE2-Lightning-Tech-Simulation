package dev.overloadsim.client;

import dev.overloadsim.tool.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

public final class CoilScreen extends AbstractContainerScreen<CoilMenu> {
    private Button voltage,efficiency,silk;private Button eminus,eplus,fminus,fplus;
    public CoilScreen(CoilMenu menu,Inventory inventory,Component title){super(menu,inventory,title);imageWidth=280;imageHeight=228;}
    private Button button(int x,int y,int width,String text,Button.OnPress press){return addRenderableWidget(Button.builder(Component.literal(text),press).bounds(leftPos+x,topPos+y,width,20).build());}
    private void change(int action,int value){PacketDistributor.sendToServer(new CoilPackets.Configure(menu.containerId,action,value));}
    @Override protected void init(){super.init();
        voltage=button(116,60,148,"",b->change(0,1-menu.value(0)));
        efficiency=button(116,87,148,"",b->change(1,1-menu.value(1)));
        eminus=button(190,114,34,"−",b->change(2,menu.value(2)-1));eplus=button(230,114,34,"+",b->change(2,menu.value(2)+1));
        fminus=button(190,141,34,"−",b->change(3,menu.value(3)-1));fplus=button(230,141,34,"+",b->change(3,menu.value(3)+1));
        silk=button(116,168,148,"",b->change(4,1-menu.value(4)));
        button(190,199,74,Component.translatable("gui.done").getString(),b->onClose());
    }
    private Component enabled(boolean value){return Component.translatable(value?"gui.overload_sim.coil.on":"gui.overload_sim.coil.off");}
    private void buttons(){
        voltage.active=menu.installed(CoilModuleItem.Type.EXTREME);voltage.setMessage(Component.translatable(menu.value(0)==1&&voltage.active?"gui.overload_sim.coil.natural":"gui.overload_sim.coil.artificial"));
        efficiency.active=menu.installed(CoilModuleItem.Type.EFFICIENCY);efficiency.setMessage(enabled(menu.value(1)==1));
        eminus.active=efficiency.active&&menu.value(2)>0;eplus.active=efficiency.active&&menu.value(2)<10;
        fminus.active=menu.installed(CoilModuleItem.Type.FORTUNE)&&menu.value(3)>0;fplus.active=menu.installed(CoilModuleItem.Type.FORTUNE)&&menu.value(3)<5;
        silk.active=menu.installed(CoilModuleItem.Type.SILK);silk.setMessage(enabled(menu.value(4)==1));
    }
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partial){buttons();super.render(g,mouseX,mouseY,partial);renderTooltip(g,mouseX,mouseY);}
    @Override protected void renderBg(GuiGraphics g,float partial,int mx,int my){
        g.fill(leftPos,topPos,leftPos+imageWidth,topPos+imageHeight,0xff73758b);g.fill(leftPos+2,topPos+2,leftPos+imageWidth-2,topPos+imageHeight-2,0xffedebf2);g.fill(leftPos+4,topPos+4,leftPos+imageWidth-4,topPos+imageHeight-4,0xffcfcfd9);
        g.fill(leftPos+8,topPos+30,leftPos+272,topPos+49,0xff9a9caf);g.fill(leftPos+10,topPos+32,leftPos+10+(int)(260*menu.value(7)/10000d),topPos+47,0xffe998c7);
        g.renderItem(menu.stack(),leftPos+250,topPos+8);
    }
    @Override protected void renderLabels(GuiGraphics g,int mx,int my){
        int color=0xff414354;g.drawString(font,title,12,12,color,false);
        g.drawString(font,Component.literal("FE  "+String.format(java.util.Locale.ROOT,"%.1f%%",menu.value(7)/100d)),14,36,0xffffffff,false);
        label(g,"voltage",65,color);label(g,"efficiency",92,color);
        g.drawString(font,Component.translatable("gui.overload_sim.coil.efficiency_level",menu.value(2)),12,119,color,false);
        g.drawString(font,Component.translatable("gui.overload_sim.coil.fortune_level",menu.value(3)),12,146,color,false);label(g,"silk",173,color);
        g.drawString(font,Component.translatable(menu.value(6)==1?"tooltip.overload_sim.coil.core_ready":"message.overload_sim.coil.no_core"),12,201,menu.value(6)==1?color:0xffa04d6b,false);
    }
    private void label(GuiGraphics g,String key,int y,int color){g.drawString(font,Component.translatable("gui.overload_sim.coil."+key),12,y,color,false);}
}
