package dev.overloadsim.client;

import java.util.List;
import java.util.Optional;
import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.Icon;
import appeng.client.gui.widgets.IconButton;
import appeng.client.gui.style.StyleManager;
import appeng.menu.SlotSemantics;
import com.moakiee.ae2lt.api.frequency.FrequencyApi;
import dev.overloadsim.laser.LaserKind;
import dev.overloadsim.laser.LaserMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Compact AE-style prototype; all controls send validated menu actions. */
public final class LaserScreen extends AEBaseScreen<LaserMenu> {
    private static final int TEXT = 0x413F54;
    private Button enable;
    private BoostSlider boostSlider;
    private final Button[] sides = new Button[6];
    private final Button build,projection;

    public LaserScreen(LaserMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, StyleManager.loadStyleDoc(menu.kind() == LaserKind.CORE
                ? "/screens/overload_sim/laser_core.json" : "/screens/overload_sim/laser.json"));
        if (menu.kind() == LaserKind.CORE) { build = null; projection = null; return; }
        var frequencyButton=new TextureToggleButton(TextureToggleButton.ButtonType.FREQUENCY_BIND,b->FrequencyApi.openBindingScreen(menu));
        frequencyButton.setTooltipAt(0,List.of(Component.translatable("gui.overload_sim.frequency")));addToLeftToolbar(frequencyButton);
        build=addToLeftToolbar(new IconButton(b->menu.autoBuild()) {
            @Override protected Icon getIcon(){return Icon.PLACEMENT_BLOCK;}
            @Override public List<Component> getTooltipMessage(){
                var lines=new java.util.ArrayList<Component>();
                lines.add(Component.translatable("gui.overload_sim.construction.auto_build"));
                lines.add(Component.translatable("gui.overload_sim.construction.auto_build_hint"));
                if(!menu.buildFeedback.isEmpty()&&minecraft.level!=null) {
                    var feedback=Component.Serializer.fromJson(menu.buildFeedback,minecraft.level.registryAccess());
                    if(feedback!=null)lines.add(feedback);
                }
                return lines;
            }
        });
        build.setMessage(Component.translatable("gui.overload_sim.construction.auto_build"));
        projection=addToLeftToolbar(new IconButton(b->{
            ConstructionProjection.toggleLaser(menu.host());
            if(minecraft.player!=null)minecraft.player.closeContainer();
        }) {
            @Override protected Icon getIcon(){return Icon.PATTERN_ACCESS_SHOW;}
            @Override public List<Component> getTooltipMessage(){return List.of(Component.translatable("gui.overload_sim.construction.projection"),Component.translatable("gui.overload_sim.construction.projection_hint"));}
        });
        projection.setMessage(Component.translatable("gui.overload_sim.construction.projection"));
    }

    @Override protected boolean shouldAddToolbar() { return menu.kind() != LaserKind.CORE; }
    @Override protected guideme.PageAnchor getHelpTopic() { return menu.kind() == LaserKind.CORE ? null : super.getHelpTopic(); }

    @Override protected void init() {
        super.init();
        setTextHidden(TEXT_ID_DIALOG_TITLE, true);
        if (menu.kind() == LaserKind.CORE) return;
        enable = addRenderableWidget(Button.builder(Component.empty(), b -> menu.toggleEnabled())
                .bounds(leftPos + 148, topPos + 25, 66, 18).build());
        if (menu.kind() == LaserKind.ENHANCEMENT) {
            boostSlider = addRenderableWidget(new BoostSlider(leftPos + 10, topPos + 99));
        }
        if (menu.kind() == LaserKind.DEFLECTOR) for (Direction direction : Direction.Plane.HORIZONTAL) {
            int side = direction.get3DDataValue();
            sides[side] = addRenderableWidget(Button.builder(Component.empty(), b -> menu.toggleSide(side))
                    .bounds(leftPos + 10 + (side-2) % 2 * 104, topPos + 99 + (side-2) / 2 * 21, 100, 18).build());
        }
        updateControls();
    }

    @Override protected void updateBeforeRender() { super.updateBeforeRender(); updateControls(); }

    // Container screens consume item drags before dispatching them to child widgets.
    @Override public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        if (button == 0 && boostSlider != null && boostSlider.dragging) {
            return boostSlider.mouseDragged(x, y, button, dx, dy);
        }
        return super.mouseDragged(x, y, button, dx, dy);
    }

    @Override public boolean mouseReleased(double x, double y, int button) {
        if (button == 0 && boostSlider != null && boostSlider.dragging) {
            boostSlider.onRelease(x, y);
            setDragging(false);
            return true;
        }
        return super.mouseReleased(x, y, button);
    }

    private void updateControls() {
        if (enable == null) return;
        enable.setMessage(Component.translatable("gui.overload_sim.laser." + (menu.enabled ? "on" : "off")));
        enable.active = menu.enabled || menu.formed;
        enable.setTooltip(Tooltip.create(Component.translatable("gui.overload_sim.laser.control_hint")));
        build.active=!menu.formed;
        projection.active=!menu.formed&&menu.kind()!=LaserKind.CORE;
        if (boostSlider != null) { boostSlider.active = menu.formed&&menu.boostMaximum>menu.boostMinimum; boostSlider.synchronize(); }
        for (int side = 0; side < 6; side++) if (sides[side] != null) {
            boolean input = (menu.inputMask&(1<<side))!=0;
            var face = Component.translatable("gui.overload_sim.laser.face." + Direction.from3DDataValue(side).getName());
            sides[side].setMessage(Component.translatable("gui.overload_sim.laser.side", face,
                    Component.translatable("gui.overload_sim.laser." + (input ? "input" : (menu.outputMask & (1 << side)) != 0 ? "output" : "off"))));
            sides[side].active = true;
            sides[side].setTooltip(Tooltip.create(Component.translatable("gui.overload_sim.laser.split_hint")));
        }
    }
    private final class BoostSlider extends AbstractSliderButton {
        private boolean dragging;
        private long requested;
        BoostSlider(int x,int y) {
            super(x,y,204,18,Component.empty(),0);
            value=normalized(menu.boost);requested=menu.boost;updateMessage();
            setTooltip(Tooltip.create(Component.translatable("gui.overload_sim.laser.boost_slider_configured",compact(menu.boostMinimum),compact(menu.boostMaximum),compact(menu.boostStep))));
        }
        private long steps(){return Math.max(0,menu.boostMaximum-menu.boostMinimum)/Math.max(1,menu.boostStep);}
        private double normalized(long amount){long steps=steps();return steps==0?0:Math.clamp((double)(amount-menu.boostMinimum)/Math.max(1,menu.boostStep)/steps,0,1);}
        private long selected(){return menu.boostMinimum+Math.round(value*steps())*Math.max(1,menu.boostStep);}
        void synchronize(){if(!dragging){value=normalized(menu.boost);requested=menu.boost;updateMessage();}setTooltip(Tooltip.create(Component.translatable("gui.overload_sim.laser.boost_slider_configured",compact(menu.boostMinimum),compact(menu.boostMaximum),compact(menu.boostStep))));}
        @Override protected void updateMessage(){setMessage(Component.literal(compact(selected())+" FE"));}
        @Override protected void applyValue(){long amount=selected();if(amount!=requested){requested=amount;menu.requestBoost(amount);}}
        @Override public void onClick(double x,double y){dragging=true;super.onClick(x,y);}
        @Override public void onRelease(double x,double y){super.onRelease(x,y);dragging=false;}
        @Override public void renderWidget(GuiGraphics g,int mouseX,int mouseY,float partial){
            super.renderWidget(g,mouseX,mouseY,partial);
            int filled=(int)Math.round(value*(width-4));
            g.fill(getX()+2,getY()+height-3,getX()+2+filled,getY()+height-2,active?0xFFEE98C4:0xFF88879B);
        }
        @Override public boolean keyPressed(int key,int scan,int modifiers){
            if(key==262||key==263){value=normalized(selected()+(key==262?menu.boostStep:-menu.boostStep));updateMessage();applyValue();return true;}
            return super.keyPressed(key,scan,modifiers);
        }
    }

    @Override public void drawBG(GuiGraphics g, int x, int y, int mx, int my, float partial) {
        g.fill(x, y, x + imageWidth, y + imageHeight, 0xFF77768C);
        g.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFFFDF8FC);
        g.fill(x + 3, y + 3, x + imageWidth - 3, y + imageHeight - 3, 0xFFD8D7E1);
        g.fill(x + 5, y + 5, x + imageWidth - 5, y + 19, 0xFFF4D4E8);
        for (var slot : menu.slots) if (slot.isActive())
            SimulationScreen.icon(g, Icon.SLOT_BACKGROUND, x + slot.x - 1, y + slot.y - 1);
        if (menu.kind() == LaserKind.RECEIVER) {
            bar(g, x + 10, y + 98, 204, menu.particleCount, menu.particleCapacity, 0xFF7AC5E8);
            bar(g, x + 10, y + 107, 204, menu.energy, menu.energyCapacity, 0xFFEE98C4);
        }
    }

    private static void bar(GuiGraphics g, int x, int y, int width, long amount, long capacity, int color) {
        g.fill(x, y, x + width, y + 6, 0xFF88879B);
        // This is display-only; synchronized inventory values remain exact longs.
        int filled = capacity <= 0 ? 0 : (int) (width * Math.clamp((double) amount / capacity, 0, 1));
        g.fill(x, y, x + filled, y + 6, color);
    }

    @Override public void drawFG(GuiGraphics g, int x, int y, int mx, int my) {
        g.drawString(font, font.plainSubstrByWidth(title.getString(), 208), 8, 7, TEXT, false);
        if (menu.kind() == LaserKind.CORE) {
            line(g, "gui.overload_sim.laser.current_strength", 10, 30, compact(menu.power));
            line(g, "gui.overload_sim.laser.core_remainder", 10, 49, compact(menu.coreRemainder));
            line(g, "gui.overload_sim.laser.core_threshold", 10, 68, compact(menu.coreThreshold));
            return;
        }
        line(g, "gui.overload_sim.laser." + (menu.formed ? "formed" : "unformed"), 10, 28);
        line(g, "gui.overload_sim.laser.network", 10, 45,
                Component.translatable("gui.overload_sim.laser." + (menu.network ? "online" : "offline")));
        line(g, matrixStrength() ? "gui.overload_sim.laser.current_strength" : "gui.overload_sim.laser.power", 10, 60, compact(menu.power));
        switch (menu.kind()) {
            case ENHANCEMENT -> {
                line(g, "gui.overload_sim.laser.boost", 10, 78, compact(menu.boost));
                line(g, "gui.overload_sim.laser.boost_cost", 10, 123);
            }
            case DEFLECTOR -> line(g, "gui.overload_sim.laser.outputs", 10, 81);
            case CORE -> {}
            case RECEIVER -> {
                line(g, "gui.overload_sim.laser.particles", 10, 76, menu.particleCount);
                line(g, "gui.overload_sim.laser.energy", 10, 89, compact(menu.energy));
                line(g, "gui.overload_sim.laser.fuel", 10, 118);
                line(g, "gui.overload_sim.laser.fuel_ticks", 92, 133, menu.fuelRemaining);
            }
            case BURST -> {
                line(g, "gui.overload_sim.laser.burst_configured", 10, 80,menu.burstMultiplier);
                line(g, "gui.overload_sim.laser.ehv", 10, 98, menu.extremeLightning);
            }
            case FOCUS -> line(g, "gui.overload_sim.laser.focus_hint", 10, 80);
            default -> line(g, "gui.overload_sim.laser.range_configured", 10, 80,menu.outputRange);
        }
        g.drawString(font, Component.translatable("container.inventory"), 31, 150, TEXT, false);
    }

    private boolean matrixStrength(){return menu.kind()==LaserKind.CONSTRAINT||menu.kind()==LaserKind.FOCUS;}

    private void line(GuiGraphics g, String key, int x, int y, Object... args) {
        g.drawString(font, font.plainSubstrByWidth(Component.translatable(key, args).getString(), 204), x, y, TEXT, false);
    }

    /** Integer display conversion avoids rounding PFE values through a double. */
    public static String compact(long value) {
        long unit = 1; String suffix = "";
        if (value >= 1_000_000_000_000_000L) { unit = 1_000_000_000_000_000L; suffix = "P"; }
        else if (value >= 1_000_000_000L) { unit = 1_000_000_000L; suffix = "G"; }
        else if (value >= 1_000_000L) { unit = 1_000_000L; suffix = "M"; }
        else if (value >= 1_000L) { unit = 1_000L; suffix = "k"; }
        if (unit == 1) return Long.toString(value);
        long hundredths = value % unit / (unit / 100);
        return value / unit + (hundredths == 0 ? "" : "." + (hundredths < 10 ? "0" : "") + hundredths) + suffix;
    }

    @Override public void render(GuiGraphics g, int mx, int my, float partial) {
        super.render(g, mx, my, partial);
        if (menu.kind() == LaserKind.CORE) {
            if (mx >= leftPos + 8 && mx <= leftPos + 215 && my >= topPos + 25 && my <= topPos + 85)
                g.renderTooltip(font, List.of(Component.translatable("gui.overload_sim.laser.exact_strength", menu.power),
                        Component.translatable("gui.overload_sim.laser.core_remainder", Long.toString(menu.coreRemainder)),
                        Component.translatable("gui.overload_sim.laser.core_threshold", Long.toString(menu.coreThreshold))),
                        Optional.empty(), mx, my);
            return;
        }
        var slot = getSlotUnderMouse();
        if (slot != null && menu.getSlotSemantic(slot) == SlotSemantics.MACHINE_INPUT)
            g.renderTooltip(font, List.of(Component.translatable("gui.overload_sim.laser.sealed_fuel"),
                    Component.translatable("gui.overload_sim.laser.active_fuel", menu.activeFuel, menu.particleRemaining)), Optional.empty(), mx, my);
        else if (mx >= leftPos + 8 && mx <= leftPos + 215 && my >= topPos + 45
                && my <= topPos + (menu.kind()==LaserKind.DEFLECTOR||menu.kind()==LaserKind.ENHANCEMENT?95:116))
            g.renderTooltip(font, List.of(Component.translatable("gui.overload_sim.laser.status", diagnostic()),
                    Component.translatable(matrixStrength()?"gui.overload_sim.laser.exact_strength":"gui.overload_sim.laser.exact_power", menu.power),
                    Component.translatable("gui.overload_sim.laser.exact_energy", menu.energy, menu.energyCapacity),
                    Component.translatable("gui.overload_sim.laser.guide_hint")), Optional.empty(), mx, my);
    }

    private Component diagnostic() {
        String key = "gui.overload_sim.laser.status." + menu.status;
        return I18n.exists(key) ? Component.translatable(key) : Component.literal(menu.status);
    }
}
