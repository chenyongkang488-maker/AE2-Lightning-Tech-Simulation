package dev.overloadsim.client;
import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.Icon;
import appeng.client.gui.style.StyleManager;
import dev.overloadsim.multiblock.MultiblockSimulationMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
public final class MultiblockSimulationScreen extends AEBaseScreen<MultiblockSimulationMenu> {
    public MultiblockSimulationScreen(MultiblockSimulationMenu menu,Inventory inventory,Component title){super(menu,inventory,title,StyleManager.loadStyleDoc("/screens/overload_sim/multiblock.json"));}
    @Override protected void init(){
        super.init();
        addRenderableWidget(Button.builder(Component.literal("‹"),b->menu.changePage(Math.max(0,menu.page-1))).bounds(leftPos+181,topPos+112,22,16).build());
        addRenderableWidget(Button.builder(Component.literal("›"),b->menu.changePage(Math.min(3,menu.page+1))).bounds(leftPos+302,topPos+112,22,16).build());
    }
    @Override public void drawBG(GuiGraphics g,int x,int y,int mx,int my,float partial){
        super.drawBG(g,x,y,mx,my,partial);
        for(var slot:menu.slots)if(slot.isActive())SimulationScreen.icon(g,Icon.SLOT_BACKGROUND,x+slot.x-1,y+slot.y-1);
        for(int i=0;i<49;i++)if(i>=menu.capacity&&menu.slots.get(i).getItem().isEmpty()){
            var slot=menu.slots.get(i);g.fill(x+slot.x,y+slot.y,x+slot.x+16,y+slot.y+16,0xBB64667C);
        }
        g.fill(x+16,y+137,x+336,y+141,0xFF999DB4);int width=menu.remaining==0?0:(int)(320L*(menu.duration-menu.remaining)/Math.max(1,menu.duration));g.fill(x+16,y+137,x+16+width,y+141,0xFFEB9EC8);
    }
    @Override public void drawFG(GuiGraphics g,int x,int y,int mx,int my){
        int color=0x413F54;
        g.drawString(font,Component.translatable("gui.overload_sim.multiblock.crystals",menu.participants,menu.capacity),16,16,color,false);
        g.drawString(font,Component.translatable("gui.overload_sim.multiblock.outputs",menu.page+1),178,16,color,false);
        g.drawString(font,Component.literal(menu.duration+" tick · ×"+menu.multiplier),180,100,color,false);
        g.drawString(font,Component.literal("FE "+menu.fe+" / 2000000"),16,145,color,false);
        g.drawString(font,Component.literal("HV "+amount(menu.hv)+"   EHV "+amount(menu.ehv)),180,145,color,false);
    }
    private static String amount(long value){if(value>=Long.MAX_VALUE/2)return "∞";if(value>=1_000_000_000)return String.format(java.util.Locale.ROOT,"%.1fG",value/1_000_000_000d);if(value>=1_000_000)return String.format(java.util.Locale.ROOT,"%.1fM",value/1_000_000d);if(value>=1000)return String.format(java.util.Locale.ROOT,"%.1fk",value/1000d);return Long.toString(value);}
    @Override public void renderSlot(GuiGraphics g,net.minecraft.world.inventory.Slot slot){
        if(slot.index<49||slot.index>=81){super.renderSlot(g,slot);return;}
        var item=slot.getItem();if(item.isEmpty())return;
        g.renderItem(item.copyWithCount(1),slot.x,slot.y);
        int quantity=menu.quantity(slot.index-49);if(quantity<=0)return;
        g.pose().pushPose();g.pose().translate(0,0,200);g.pose().scale(.6f,.6f,1);
        String count=""+quantity;g.drawString(font,count,(int)((slot.x+17-font.width(count)*.6f)/.6f),(int)((slot.y+11)/.6f),0xFFFFFF,true);g.pose().popPose();
    }
    @Override public boolean mouseClicked(double x,double y,int button){
        var slot=getSlotUnderMouse();
        if(slot!=null&&slot.index>=49&&slot.index<81&&(button==0||button==1)){menu.requestTake(slot.index-49,hasShiftDown()?2:button);return true;}
        return super.mouseClicked(x,y,button);
    }
    @Override public void render(GuiGraphics g,int mx,int my,float partial){
        super.render(g,mx,my,partial);
        var slot=getSlotUnderMouse();
        if(slot!=null&&slot.index>=49&&slot.index<81&&!slot.getItem().isEmpty())
            g.renderTooltip(font,java.util.List.of(slot.getItem().getHoverName(),Component.literal(menu.quantity(slot.index-49)+" / 1024"),Component.translatable("gui.overload_sim.multiblock.take")),java.util.Optional.empty(),mx,my);
        if(mx>=leftPos+16&&mx<leftPos+336&&my>=topPos+137&&my<topPos+156)
            g.renderTooltip(font,java.util.List.of(Component.translatable("status.overload_sim."+menu.status),Component.translatable("gui.overload_sim.multiblock.error",menu.problem),Component.literal("FE "+menu.feCost+" · HV "+menu.hvCost+" · EHV "+menu.ehvCost),Component.literal("HV "+menu.hv),Component.literal("EHV "+menu.ehv),Component.translatable("gui.overload_sim.multiblock.frequency",menu.frequency)),java.util.Optional.empty(),mx,my);
    }
}
