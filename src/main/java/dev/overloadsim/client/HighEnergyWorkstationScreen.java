package dev.overloadsim.client;

import appeng.api.config.ActionItems;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.GenericStack;
import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.Icon;
import appeng.client.gui.style.StyleManager;
import appeng.client.gui.widgets.ActionButton;
import appeng.client.gui.widgets.IconButton;
import appeng.menu.SlotSemantics;
import com.moakiee.ae2lt.api.frequency.FrequencyApi;
import com.moakiee.ae2lt.client.TextureToggleButton;
import dev.overloadsim.workstation.HighEnergyWorkstationMenu;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;

/** Two compact bulk grids retain the familiar AE toolbar and ordinary player inventory. */
public final class HighEnergyWorkstationScreen extends AEBaseScreen<HighEnergyWorkstationMenu> {
    private static final int TEXT = 0x413F54;
    private final Button build;
    private final Button projection;

    public HighEnergyWorkstationScreen(HighEnergyWorkstationMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, StyleManager.loadStyleDoc("/screens/overload_sim/workstation.json"));
        var frequency = new TextureToggleButton(TextureToggleButton.ButtonType.FREQUENCY_BIND,
                b -> FrequencyApi.openBindingScreen(menu));
        frequency.setTooltipAt(0, List.of(Component.translatable("gui.overload_sim.frequency")));
        addToLeftToolbar(frequency);
        build = addToLeftToolbar(new IconButton(b -> menu.autoBuild()) {
            @Override protected Icon getIcon() { return Icon.PLACEMENT_BLOCK; }
            @Override public List<Component> getTooltipMessage() {
                var lines = new ArrayList<Component>();
                lines.add(Component.translatable("gui.overload_sim.construction.auto_build"));
                lines.add(Component.translatable("gui.overload_sim.construction.auto_build_hint"));
                if (!menu.buildFeedback.isEmpty() && minecraft.level != null) {
                    var feedback = Component.Serializer.fromJson(menu.buildFeedback, minecraft.level.registryAccess());
                    if (feedback != null) lines.add(feedback);
                }
                return lines;
            }
        });
        build.setMessage(Component.translatable("gui.overload_sim.construction.auto_build"));
        projection = addToLeftToolbar(new IconButton(b -> {
            ConstructionProjection.toggleWorkstation(menu.host());
            if (minecraft.player != null) minecraft.player.closeContainer();
        }) {
            @Override protected Icon getIcon() { return Icon.PATTERN_ACCESS_SHOW; }
            @Override public List<Component> getTooltipMessage() {
                return List.of(Component.translatable("gui.overload_sim.construction.projection"),
                        Component.translatable("gui.overload_sim.construction.projection_hint"));
            }
        });
        projection.setMessage(Component.translatable("gui.overload_sim.construction.projection"));
        var config = new ActionButton(ActionItems.COG, () -> switchToScreen(new WorkstationOutputConfigScreen(this)));
        config.setMessage(Component.translatable("gui.overload_sim.output_config"));
        addToLeftToolbar(config);
    }

    @Override protected void init() {
        super.init();
        setTextHidden(TEXT_ID_DIALOG_TITLE, true);
        setSlotsHidden(SlotSemantics.MACHINE_INPUT, false);
        setSlotsHidden(SlotSemantics.MACHINE_OUTPUT, false);
        setSlotsHidden(SlotSemantics.PLAYER_INVENTORY, false);
        setSlotsHidden(SlotSemantics.PLAYER_HOTBAR, false);
    }

    @Override protected void updateBeforeRender() {
        super.updateBeforeRender();
        build.active = !menu.formed;
        projection.active = !menu.formed;
    }

    @Override public void drawBG(GuiGraphics g, int x, int y, int mx, int my, float partial) {
        panel(g, x, y, imageWidth, imageHeight);
        g.fill(x + 5, y + 5, x + imageWidth - 5, y + 19, 0xFFF4D4E8);
        for (var slot : menu.slots) if (slot.isActive())
            SimulationScreen.icon(g, Icon.SLOT_BACKGROUND, x + slot.x - 1, y + slot.y - 1);
        SimulationScreen.icon(g, Icon.ARROW_RIGHT, x + 91, y + 92);
        bar(g, x + 82, y + 115, 36, Math.clamp(menu.progress, 0, 2), 2, 0xFFED99C5);
        g.fill(x + 12, y + 148, x + 188, y + 149, 0xFFB5A9BC);
        fluidSlot(g,x+16,y+128,menu.inputFluidDisplay);
        fluidSlot(g,x+128,y+128,menu.outputFluidDisplay);
    }

    private static void fluidSlot(GuiGraphics g,int x,int y,GenericStack display){
        g.fill(x,y,x+18,y+18,0xFF77768C);g.fill(x+1,y+1,x+17,y+17,0xFFBDB5C9);
        if(display==null||!(display.what() instanceof AEFluidKey key)||display.amount()<=0){
            Icon.TYPE_FILTER_FLUIDS.getBlitter().dest(x+3,y+3,12,12).blit(g);return;
        }
        var fluid=key.toStack(1);var properties=IClientFluidTypeExtensions.of(fluid.getFluid());
        var texture=properties.getStillTexture(fluid);if(texture==null)return;
        var sprite=Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(texture);
        int color=properties.getTintColor(fluid);
        int fill=Math.clamp((int)Math.ceil(display.amount()*16.0/dev.overloadsim.workstation.HighEnergyWorkstationBlockEntity.TANK_CAPACITY),1,16);
        g.blit(x+1,y+17-fill,0,16,fill,sprite,(color>>16&255)/255f,(color>>8&255)/255f,(color&255)/255f,(color>>>24)/255f);
    }

    static void panel(GuiGraphics g, int x, int y, int width, int height) {
        g.fill(x, y, x + width, y + height, 0xFF77768C);
        g.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFFFDF8FC);
        g.fill(x + 3, y + 3, x + width - 3, y + height - 3, 0xFFD8D7E1);
    }

    private static void bar(GuiGraphics g, int x, int y, int width, long amount, long capacity, int color) {
        g.fill(x, y, x + width, y + 5, 0xFF9391A8);
        int fill = capacity <= 0 ? 0 : (int) (width * Math.clamp(amount, 0, capacity) / capacity);
        g.fill(x, y, x + fill, y + 5, color);
    }

    @Override public void drawFG(GuiGraphics g, int x, int y, int mx, int my) {
        text(g, title, 8, 7, 184);
        text(g, Component.translatable("gui.overload_sim.workstation." + (menu.formed ? "formed" : "unformed")), 12, 25, 176);
        text(g, Component.translatable("gui.overload_sim.workstation.strength", LaserScreen.compact(menu.power)), 12, 38, 176);
        text(g, Component.translatable("gui.overload_sim.workstation.parallel", menu.parallel), 12, 51, 176);
        text(g, Component.translatable("gui.overload_sim.workstation.inputs"), 16, 62, 66);
        text(g, Component.translatable("gui.overload_sim.workstation.outputs"), 128, 62, 60);
        text(g, Component.literal(menu.progress + "/2"), 91, 121, 30);
        text(g, Component.literal(LaserScreen.compact(menu.inputFluidAmount)+" mB"), 37, 132, 66);
        text(g, Component.literal(LaserScreen.compact(menu.outputFluidAmount)+" mB"), 149, 132, 40);
        text(g, Component.translatable("container.inventory"), 20, 150, 162);
    }

    private void text(GuiGraphics g, Component component, int x, int y, int width) {
        g.drawString(font, font.plainSubstrByWidth(component.getString(), width), x, y, TEXT, false);
    }

    @Override public void renderSlot(GuiGraphics g, Slot slot) {
        if (slot.index < 0 || slot.index >= HighEnergyWorkstationMenu.DISPLAYS) { super.renderSlot(g, slot); return; }
        var item = slot.getItem();
        if (item.isEmpty()) return;
        g.renderItem(item.copyWithCount(1), slot.x, slot.y);
        long amount = menu.quantity(slot.index);
        if (amount <= 0) return;
        String count = amount >= 1_000_000 ? amount / 1_000_000 + "M" : amount >= 1_000 ? amount / 1_000 + "k" : Long.toString(amount);
        float scale = 0.65f;
        g.pose().pushPose();
        g.pose().translate(0, 0, 200);
        g.pose().scale(scale, scale, 1);
        g.drawString(font, count, (int) ((slot.x + 17 - font.width(count) * scale) / scale),
                (int) ((slot.y + 11) / scale), 0xFFFFFF, true);
        g.pose().popPose();
    }

    @Override public boolean mouseClicked(double x, double y, int button) {
        int fluid=fluidUnderMouse(x,y);
        if(fluid>=0){if(button==0||button==1)menu.requestFluid(fluid);return true;}
        var slot = getSlotUnderMouse();
        if (slot != null && slot.index >= 0 && slot.index < HighEnergyWorkstationMenu.DISPLAYS) {
            if (button == 0 || button == 1) menu.requestItem(slot.index, hasShiftDown() ? 2 : button);
            return true;
        }
        return super.mouseClicked(x, y, button);
    }
    private int fluidUnderMouse(double x,double y){
        if(y<topPos+128||y>=topPos+146)return -1;
        return x>=leftPos+16&&x<leftPos+34?0:x>=leftPos+128&&x<leftPos+146?1:-1;
    }

    @Override protected void slotClicked(Slot slot, int index, int button, ClickType type) {
        // Hotbar keys, middle-click cloning and drag sequences cannot pass a bulk display to vanilla.
        if (index >= 0 && index < HighEnergyWorkstationMenu.DISPLAYS) return;
        super.slotClicked(slot, index, button, type);
    }

    @Override public void render(GuiGraphics g, int mx, int my, float partial) {
        super.render(g, mx, my, partial);
        var slot = getSlotUnderMouse();
        if (slot != null && slot.index >= 0 && slot.index < HighEnergyWorkstationMenu.DISPLAYS) {
            var lines = new ArrayList<Component>();
            if (!slot.getItem().isEmpty()) lines.add(slot.getItem().getHoverName());
            lines.add(Component.translatable("gui.overload_sim.workstation.slot_count", menu.quantity(slot.index), HighEnergyWorkstationMenu.SLOT_CAPACITY));
            lines.add(Component.translatable("gui.overload_sim.workstation." + (slot.index < 9 ? "input_hint" : "output_hint")));
            g.renderTooltip(font, lines, Optional.empty(), mx, my);
        } else if (mx >= leftPos + 10 && mx < leftPos + 190 && my >= topPos + 24 && my < topPos + 64) {
            g.renderTooltip(font, List.of(Component.translatable("gui.overload_sim.workstation.exact_strength", menu.power),
                    Component.translatable("gui.overload_sim.workstation.laser_gate"),
                    Component.translatable("gui.overload_sim.workstation.cycle"),
                    Component.translatable("gui.overload_sim.workstation.last_recipe", menu.recipe.isEmpty()
                            ? Component.translatable("gui.overload_sim.workstation.no_recipe") : Component.literal(menu.recipe)),
                    Component.translatable("gui.overload_sim.multiblock.frequency", menu.frequency)), Optional.empty(), mx, my);
        } else if (mx >= leftPos + 10 && mx < leftPos + 190 && my >= topPos + 128 && my < topPos + 146) {
            boolean input = mx < leftPos + 106;
            String name = input ? menu.inputFluid : menu.outputFluid;
            long amount = input ? menu.inputFluidAmount : menu.outputFluidAmount;
            g.renderTooltip(font, List.of(name.isEmpty() ? Component.translatable("gui.overload_sim.workstation.empty_fluid") : Component.literal(name),
                    Component.translatable("gui.overload_sim.workstation.fluid_capacity", amount,dev.overloadsim.workstation.HighEnergyWorkstationBlockEntity.TANK_CAPACITY),
                    Component.translatable("gui.overload_sim.workstation.fluid_hint")), Optional.empty(), mx, my);
        }
    }
}
