package dev.overloadsim.client;

import java.util.List;
import appeng.api.config.ActionItems;
import appeng.client.gui.AESubScreen;
import appeng.client.gui.Icon;
import appeng.client.gui.widgets.ActionButton;
import appeng.client.gui.widgets.TabButton;
import appeng.menu.SlotSemantics;
import dev.overloadsim.machine.SimulationMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public class SimulationOutputConfigScreen extends AESubScreen<SimulationMenu,SimulationScreen> {
    public SimulationOutputConfigScreen(SimulationScreen parent){
        super(parent,"/screens/overload_sim/output_config.json");
        widgets.add("return",new TabButton(Icon.ARROW_LEFT,Component.translatable("gui.overload_sim.back"),b->returnToParent()));
        var clear=new ActionButton(ActionItems.CLOSE,()->menu.clearSides());clear.setHalfSize(true);clear.setDisableBackground(true);clear.setMessage(Component.translatable("gui.overload_sim.clear_sides"));widgets.add("clear",clear);
        for(int side=0;side<6;side++)widgets.add("side"+side,new SideButton(side));
    }
    @Override protected boolean shouldAddToolbar(){return false;}
    @Override protected void init(){super.init();for(var semantic:List.of(SlotSemantics.STORAGE_CELL,SlotSemantics.CONFIG,SlotSemantics.UPGRADE,SlotSemantics.MACHINE_INPUT,SlotSemantics.MACHINE_OUTPUT,SlotSemantics.PLAYER_INVENTORY,SlotSemantics.PLAYER_HOTBAR))setSlotsHidden(semantic,true);}
    @Override public void onClose(){returnToParent();}
    @Override public boolean keyPressed(int key,int scan,int modifiers){if(key==256||minecraft.options.keyInventory.matches(key,scan)){returnToParent();return true;}return super.keyPressed(key,scan,modifiers);}
    private class SideButton extends Button implements appeng.client.gui.widgets.ITooltip {
        private final int side;
        SideButton(int side){super(0,0,18,18,Component.empty(),b->menu.toggleSide(side),DEFAULT_NARRATION);this.side=side;}
        @Override protected void renderWidget(GuiGraphics g,int mx,int my,float partial){
            var background=isHoveredOrFocused()?Icon.TOOLBAR_BUTTON_BACKGROUND_HOVER:(menu.outputMask&(1<<side))!=0?Icon.TOOLBAR_BUTTON_BACKGROUND_FOCUS:Icon.TOOLBAR_BUTTON_BACKGROUND;
            background.getBlitter().dest(getX()-1,getY(),18,20).blit(g);var stack=menu.neighborIcon(side);if(!stack.isEmpty())g.renderItem(stack,getX(),getY()+1);
        }
        public List<Component> getTooltipMessage(){return List.of(Component.translatable("direction.overload_sim."+side),Component.translatable((menu.outputMask&(1<<side))!=0?"gui.overload_sim.enabled":"gui.overload_sim.disabled"));}
        public net.minecraft.client.renderer.Rect2i getTooltipArea(){return new net.minecraft.client.renderer.Rect2i(getX(),getY(),width,height);}
        public boolean isTooltipAreaVisible(){return visible;}
    }
}
