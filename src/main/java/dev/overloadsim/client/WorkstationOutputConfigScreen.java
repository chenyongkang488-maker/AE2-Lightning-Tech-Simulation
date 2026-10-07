package dev.overloadsim.client;

import appeng.client.gui.AESubScreen;
import appeng.client.gui.Icon;
import appeng.client.gui.widgets.TabButton;
import appeng.menu.SlotSemantics;
import dev.overloadsim.workstation.HighEnergyWorkstationMenu;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;

/** Configuration is an AE sub-screen of the existing menu; no reopen or separate host session. */
public final class WorkstationOutputConfigScreen extends AESubScreen<HighEnergyWorkstationMenu, HighEnergyWorkstationScreen> {
    private Button eject;
    private Button network;

    public WorkstationOutputConfigScreen(HighEnergyWorkstationScreen parent) {
        super(parent, "/screens/overload_sim/workstation_output_config.json");
        widgets.add("return", new TabButton(Icon.ARROW_LEFT, Component.translatable("gui.overload_sim.back"), b -> returnToParent()));
        for (int side = 0; side < 6; side++) widgets.add("side" + side, new SideButton(side));
    }

    @Override protected boolean shouldAddToolbar() { return false; }
    @Override protected void init() {
        super.init();
        setTextHidden(TEXT_ID_DIALOG_TITLE, true);
        for (var semantic : List.of(SlotSemantics.MACHINE_INPUT, SlotSemantics.MACHINE_OUTPUT,
                SlotSemantics.PLAYER_INVENTORY, SlotSemantics.PLAYER_HOTBAR)) setSlotsHidden(semantic, true);
        eject = addRenderableWidget(Button.builder(Component.empty(), b -> menu.toggleEject())
                .bounds(leftPos + 12, topPos + 127, 176, 20).build());
        eject.setTooltip(Tooltip.create(Component.translatable("gui.overload_sim.workstation.eject_hint")));
        network = addRenderableWidget(Button.builder(Component.empty(), b -> menu.toggleNetworkExport())
                .bounds(leftPos + 12, topPos + 151, 176, 20).build());
        network.setTooltip(Tooltip.create(Component.translatable("gui.overload_sim.workstation.network_hint")));
        updateControls();
    }

    @Override protected void updateBeforeRender() { super.updateBeforeRender(); updateControls(); }
    private void updateControls() {
        if (eject == null) return;
        eject.setMessage(Component.translatable("gui.overload_sim.workstation.eject", onOff(menu.eject)));
        network.setMessage(Component.translatable("gui.overload_sim.workstation.network_export", onOff(menu.networkExport)));
    }
    private static Component onOff(boolean state) {
        return Component.translatable(state ? "gui.overload_sim.enabled" : "gui.overload_sim.disabled");
    }

    @Override public void drawBG(GuiGraphics g, int x, int y, int mx, int my, float partial) {
        HighEnergyWorkstationScreen.panel(g, x, y, imageWidth, imageHeight);
        g.fill(x + 5, y + 5, x + imageWidth - 5, y + 19, 0xFFF4D4E8);
        g.fill(x + 12, y + 121, x + 188, y + 122, 0xFFB5A9BC);
        SimulationScreen.icon(g, Icon.COG, x + 18, y + 83);
    }

    @Override public void drawFG(GuiGraphics g, int x, int y, int mx, int my) {
        g.drawString(font, Component.translatable("gui.overload_sim.output_config"), 8, 7, 0x413F54, false);
        g.drawString(font, font.plainSubstrByWidth(Component.translatable("gui.overload_sim.workstation.sides_hint").getString(), 176),
                12, 111, 0x413F54, false);
    }

    @Override public void onClose() { returnToParent(); }
    @Override public boolean keyPressed(int key, int scan, int modifiers) {
        if (key == 256 || minecraft.options.keyInventory.matches(key, scan)) { returnToParent(); return true; }
        return super.keyPressed(key, scan, modifiers);
    }

    private final class SideButton extends Button implements appeng.client.gui.widgets.ITooltip {
        private final int side;
        private SideButton(int side) {
            super(0, 0, 52, 22, face(side), b -> menu.cycleSide(side), DEFAULT_NARRATION);
            this.side = side;
        }
        @Override protected void renderWidget(GuiGraphics g, int mx, int my, float partial) {
            int mode = menu.sideMode(side);
            g.fill(getX(), getY(), getX() + width, getY() + height, 0xFF8A8096);
            g.fill(getX() + 1, getY() + 1, getX() + width - 1, getY() + height - 1,
                    isHoveredOrFocused() ? 0xFFFFE8F4 : 0xFFFDF8FC);
            int color = switch (mode) { case 1 -> 0xFF7CBFE0; case 2 -> 0xFFEE99C5; case 3 -> 0xFFBA94D9; default -> 0xFFB8B3C3; };
            g.fill(getX() + 1, getY() + height - 3, getX() + width - 1, getY() + height - 1, color);
            g.drawString(font, font.plainSubstrByWidth(getMessage().getString(), 31), getX() + 3, getY() + 6, 0x413F54, false);
            Icon icon = switch (mode) { case 1 -> Icon.ACCESS_READ; case 2 -> Icon.ACCESS_WRITE; case 3 -> Icon.ACCESS_READ_WRITE; default -> Icon.REDSTONE_OFF; };
            SimulationScreen.icon(g, icon, getX() + 34, getY() + 2);
        }
        @Override public List<Component> getTooltipMessage() {
            return List.of(Component.translatable("gui.overload_sim.workstation.side", face(side),
                    Component.translatable("gui.overload_sim.workstation.mode." + menu.sideMode(side))),
                    Component.translatable("gui.overload_sim.workstation.sides_hint"),
                    Component.translatable("gui.overload_sim.workstation.side_rules"));
        }
        @Override public net.minecraft.client.renderer.Rect2i getTooltipArea() {
            return new net.minecraft.client.renderer.Rect2i(getX(), getY(), width, height);
        }
        @Override public boolean isTooltipAreaVisible() { return visible; }
    }

    private static Component face(int side) {
        return Component.translatable("gui.overload_sim.laser.face." + Direction.from3DDataValue(side).getName());
    }
}
