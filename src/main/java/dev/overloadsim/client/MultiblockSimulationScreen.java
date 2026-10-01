package dev.overloadsim.client;
import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.Icon;
import appeng.client.gui.widgets.IconButton;
import appeng.client.gui.style.StyleManager;
import appeng.menu.SlotSemantics;
import dev.overloadsim.core.MultiblockMenuLayout;
import dev.overloadsim.multiblock.MultiblockSimulationMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
public final class MultiblockSimulationScreen extends AEBaseScreen<MultiblockSimulationMenu> {
    private MultiblockMenuLayout layout;private boolean recovery;private final Button recoveryButton;
    public MultiblockSimulationScreen(MultiblockSimulationMenu menu,Inventory inventory,Component title){
        super(menu,inventory,title,StyleManager.loadStyleDoc("/screens/overload_sim/multiblock.json"));
        // AE2 populates its persistent toolbar during init; register this control exactly once.
        recoveryButton=addToLeftToolbar(new IconButton(b->{recovery=!recovery;init(minecraft,width,height);}){
            @Override protected Icon getIcon(){return Icon.BACK;}
        });
        recoveryButton.setMessage(Component.translatable("gui.overload_sim.multiblock.recovery"));
        recoveryButton.setTooltip(Tooltip.create(Component.translatable("gui.overload_sim.multiblock.recovery")));
    }
    @Override protected void init(){
        layout=MultiblockMenuLayout.of(menu.capacity,recovery);imageWidth=layout.width();imageHeight=layout.height();
        super.init();setTextHidden(TEXT_ID_DIALOG_TITLE,true);int right=layout.outputLeft();
        addRenderableWidget(Button.builder(Component.literal("‹"),b->menu.changePage(Math.max(0,menu.page-1))).bounds(leftPos+right,topPos+108,20,16).build());
        addRenderableWidget(Button.builder(Component.literal("›"),b->menu.changePage(Math.min(3,menu.page+1))).bounds(leftPos+right+122,topPos+108,20,16).build());
        arrange();
    }
    private void arrange(){
        for(int i=0;i<49;i++){var slot=(dev.overloadsim.multiblock.SimulationCrystalSlot)menu.slots.get(i);boolean visible=i<layout.side()*layout.side();slot.setVisible(visible);position(slot,layout.input(i));}
        for(int i=0;i<32;i++)position(menu.slots.get(49+i),layout.output(i));
        var inventory=menu.getSlots(SlotSemantics.PLAYER_INVENTORY);for(int i=0;i<inventory.size();i++)position(inventory.get(i),layout.inventory(i));
        var hotbar=menu.getSlots(SlotSemantics.PLAYER_HOTBAR);for(int i=0;i<hotbar.size();i++)position(hotbar.get(i),layout.hotbar(i));
        boolean extra=false;for(int i=menu.capacity;i<49;i++)if(!menu.slots.get(i).getItem().isEmpty()){extra=true;break;}
        recoveryButton.visible=recovery||menu.capacity>0&&extra;recoveryButton.active=recoveryButton.visible;
    }
    private static void position(Slot slot,MultiblockMenuLayout.Point p){slot.x=p.x();slot.y=p.y();}
    @Override protected void updateBeforeRender(){
        super.updateBeforeRender();var next=MultiblockMenuLayout.of(menu.capacity,recovery);
        if(layout==null||layout.side()!=next.side())init(minecraft,width,height);else arrange();
    }
    @Override public void drawBG(GuiGraphics g,int x,int y,int mx,int my,float partial){
        int lower=x+layout.inventoryPanelLeft(),upperBottom=y+layout.upperHeight();
        g.fill(x,y,x+imageWidth,upperBottom,0xFF77768C);g.fill(lower,upperBottom,lower+194,y+imageHeight,0xFF77768C);
        g.fill(x+1,y+1,x+imageWidth-1,upperBottom-1,0xFFFDF8FC);g.fill(lower+1,upperBottom-1,lower+193,y+imageHeight-1,0xFFFDF8FC);
        g.fill(x+3,y+3,x+imageWidth-3,upperBottom-3,0xFFD8D7E1);g.fill(lower+3,upperBottom-3,lower+191,y+imageHeight-3,0xFFD8D7E1);
        for(var slot:menu.slots)if(slot.isActive())SimulationScreen.icon(g,Icon.SLOT_BACKGROUND,x+slot.x-1,y+slot.y-1);
        if(recovery||menu.capacity==0)for(int i=menu.capacity;i<49;i++){var slot=menu.slots.get(i);g.fill(x+slot.x,y+slot.y,x+slot.x+16,y+slot.y+16,0x4468667D);}
        int px=x+layout.progressLeft(),height=122;g.fill(px,y+32,px+6,y+32+height,0xFF9391A8);
        int fill=menu.remaining<=0?0:(int)(height*(long)Math.clamp(menu.duration-menu.remaining,0,menu.duration)/Math.max(1,menu.duration));
        g.fill(px+1,y+32+height-fill,px+5,y+32+height,0xFFF0A6CF);
    }
    @Override public void drawFG(GuiGraphics g,int x,int y,int mx,int my){
        int color=0x413F54,right=layout.outputLeft(),present=0;
        for(int i=0;i<menu.capacity;i++)if(!menu.slots.get(i).getItem().isEmpty())present++;
        g.drawString(font,title,8,6,color,false);
        g.drawString(font,menu.capacity==0?Component.translatable("gui.overload_sim.multiblock.unformed"):Component.translatable("gui.overload_sim.multiblock.crystals",present,menu.capacity),10,20,color,false);
        g.drawString(font,Component.translatable("gui.overload_sim.multiblock.outputs",menu.page+1),right,20,color,false);
        g.drawString(font,Component.literal(menu.duration+" tick · ×"+menu.multiplier),right,128,color,false);
        g.drawString(font,Component.literal("FE "+menu.fe+" / 2000000"),right,141,color,false);
    }
    @Override public void renderSlot(GuiGraphics g,Slot slot){
        if(slot.index<49||slot.index>=81){super.renderSlot(g,slot);return;}
        var item=slot.getItem();if(item.isEmpty())return;g.renderItem(item.copyWithCount(1),slot.x,slot.y);
        int quantity=menu.quantity(slot.index-49);if(quantity<=0)return;
        g.pose().pushPose();g.pose().translate(0,0,200);g.pose().scale(.6f,.6f,1);
        String count=""+quantity;g.drawString(font,count,(int)((slot.x+17-font.width(count)*.6f)/.6f),(int)((slot.y+11)/.6f),0xFFFFFF,true);g.pose().popPose();
    }
    @Override public boolean mouseClicked(double x,double y,int button){
        var slot=getSlotUnderMouse();if(slot!=null&&slot.index>=49&&slot.index<81&&(button==0||button==1)){menu.requestTake(slot.index-49,hasShiftDown()?2:button);return true;}
        return super.mouseClicked(x,y,button);
    }
    @Override protected boolean hasClickedOutside(double mx,double my,int guiLeft,int guiTop,int button){
        if(super.hasClickedOutside(mx,my,guiLeft,guiTop,button))return true;
        return my>=topPos+layout.upperHeight()&&(mx<leftPos+layout.inventoryPanelLeft()||mx>=leftPos+layout.inventoryPanelLeft()+194);
    }
    @Override public void render(GuiGraphics g,int mx,int my,float partial){
        super.render(g,mx,my,partial);var slot=getSlotUnderMouse();
        if(slot!=null&&slot.index>=49&&slot.index<81&&!slot.getItem().isEmpty())g.renderTooltip(font,java.util.List.of(slot.getItem().getHoverName(),Component.literal(menu.quantity(slot.index-49)+" / 1024"),Component.translatable("gui.overload_sim.multiblock.take")),java.util.Optional.empty(),mx,my);
        int px=leftPos+layout.progressLeft();if(mx>=px-2&&mx<px+8&&my>=topPos+32&&my<topPos+154)
            g.renderTooltip(font,java.util.List.of(Component.translatable("status.overload_sim."+menu.status),Component.translatable("gui.overload_sim.multiblock.error",menu.problem),Component.literal("FE "+menu.feCost+" · HV "+menu.hvCost+" · EHV "+menu.ehvCost),Component.translatable("gui.overload_sim.multiblock.frequency",menu.frequency)),java.util.Optional.empty(),mx,my);
    }
}
