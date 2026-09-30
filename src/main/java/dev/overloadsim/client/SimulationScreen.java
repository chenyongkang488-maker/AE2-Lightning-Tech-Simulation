package dev.overloadsim.client;

import java.util.List;
import appeng.api.config.ActionItems;
import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.Icon;
import appeng.client.gui.style.Blitter;
import appeng.client.gui.style.StyleManager;
import appeng.client.gui.widgets.ActionButton;
import appeng.client.gui.widgets.UpgradesPanel;
import appeng.menu.SlotSemantics;
import com.moakiee.ae2lt.api.frequency.FrequencyApi;
import com.moakiee.ae2lt.client.TextureToggleButton;
import dev.overloadsim.machine.SimulationMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class SimulationScreen extends AEBaseScreen<SimulationMenu> {
    private static final int TEXT=0x413F54;
    private final TextureToggleButton ejectButton;
    public SimulationScreen(SimulationMenu menu,Inventory inventory,Component title){
        super(menu,inventory,title,StyleManager.loadStyleDoc("/screens/overload_sim/chamber.json"));
        widgets.add("upgrades",new UpgradesPanel(menu.getSlots(SlotSemantics.UPGRADE),()->List.of(Component.translatable("gui.overload_sim.slot.2"))));
        var frequencyButton=new TextureToggleButton(TextureToggleButton.ButtonType.FREQUENCY_BIND,b->FrequencyApi.openBindingScreen(menu));
        frequencyButton.setTooltipAt(0,List.of(Component.translatable("gui.overload_sim.frequency")));addToLeftToolbar(frequencyButton);
        ejectButton=new TextureToggleButton(TextureToggleButton.ButtonType.AUTO_EXPORT,b->menu.toggleEject());
        ejectButton.setTooltipOn(List.of(Component.translatable("gui.overload_sim.eject"),Component.translatable("gui.overload_sim.enabled")));
        ejectButton.setTooltipOff(List.of(Component.translatable("gui.overload_sim.eject"),Component.translatable("gui.overload_sim.disabled")));addToLeftToolbar(ejectButton);
        var outputs=new ActionButton(ActionItems.COG,()->switchToScreen(new SimulationOutputConfigScreen(this)));
        outputs.setMessage(Component.translatable("gui.overload_sim.output_config"));addToLeftToolbar(outputs);
    }
    @Override protected void updateBeforeRender(){super.updateBeforeRender();ejectButton.setState(menu.eject);}
    @Override public void drawBG(GuiGraphics g,int x,int y,int mouseX,int mouseY,float partial){
        super.drawBG(g,x,y,mouseX,mouseY,partial);
        for(var slot:menu.slots)if(slot.isActive()&&menu.getSlotSemantic(slot)!=SlotSemantics.UPGRADE)icon(g,Icon.SLOT_BACKGROUND,x+slot.x-1,y+slot.y-1);
        g.fill(x+10,y+26,x+23,y+80,0xFF87899F);g.fill(x+11,y+27,x+22,y+79,0xFFE2E3ED);g.fill(x+12,y+28,x+21,y+78,0xFF999DB4);
        int filled=(int)(50L*menu.fe/2_000_000);g.fill(x+12,y+78-filled,x+21,y+78,0xFF626C90);
        for(int tick=0;tick<5;tick++)g.fill(x+18,y+29+tick*10,x+22,y+30+tick*10,0xFFE2E3ED);
        g.fill(x+60,y+36,x+94,y+40,0xFF999DB4);g.fill(x+91,y+32,x+95,y+44,0xFF999DB4);g.fill(x+95,y+34,x+97,y+42,0xFF999DB4);g.fill(x+97,y+36,x+99,y+40,0xFF999DB4);
        int width=menu.duration<=0||menu.status==0?0:(int)(34L*Math.clamp(menu.duration-menu.remaining,0,menu.duration)/menu.duration);g.fill(x+60,y+36,x+60+width,y+40,0xFF636C90);
        icon(g,Icon.POWER_UNIT_AE,x+150,y+5);
    }
    @Override public void drawFG(GuiGraphics g,int x,int y,int mx,int my){g.drawString(font,Component.translatable("gui.overload_sim.parallel",menu.parallel,menu.maximum),30,79,TEXT,false);}
    @Override public void render(GuiGraphics g,int mx,int my,float partial){
        super.render(g,mx,my,partial);
        if(mx>=leftPos+10&&mx<leftPos+23&&my>=topPos+26&&my<topPos+80)g.renderTooltip(font,List.of(Component.translatable("gui.overload_sim.energy",menu.fe),Component.translatable("gui.overload_sim.network_power")),java.util.Optional.empty(),mx,my);
        if(mx>=leftPos+150&&mx<leftPos+166&&my>=topPos+5&&my<topPos+21)g.renderTooltip(font,List.of(Component.translatable("status.overload_sim."+menu.status),Component.translatable("gui.overload_sim.hv",menu.highVoltage)),java.util.Optional.empty(),mx,my);
        var slot=getSlotUnderMouse();if(slot!=null&&slot.index<4&&slot.getItem().isEmpty())g.renderTooltip(font,Component.translatable("gui.overload_sim.slot."+slot.index),mx,my);
    }
    static void icon(GuiGraphics g,Icon icon,int x,int y){Blitter.texture(Icon.TEXTURE,256,256).src(icon.x,icon.y,icon.width,icon.height).dest(x,y).blit(g);}
}
