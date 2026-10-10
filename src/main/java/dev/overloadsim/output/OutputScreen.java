// SPDX-License-Identifier: LGPL-3.0-only
package dev.overloadsim.output;

import appeng.api.config.FuzzyMode;
import appeng.api.config.Settings;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.api.upgrades.Upgrades;
import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.Icon;
import appeng.client.gui.style.StyleManager;
import appeng.client.gui.widgets.IconButton;
import appeng.client.gui.widgets.SettingToggleButton;
import appeng.client.gui.widgets.UpgradesPanel;
import appeng.menu.SlotSemantics;
import com.moakiee.ae2lt.api.frequency.FrequencyApi;
import dev.overloadsim.client.TextureToggleButton;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/**
 * Original interface spacing and amount controls adapted from AE2LT's
 * OverloadedInterfaceScreen (Moakiee and contributors, LGPL-3.0-only).
 * The upstream GUI texture is referenced from AE2LT and is not redistributed.
 */
public final class OutputScreen extends AEBaseScreen<OutputMenu> {
    private static final int TEXT = 0x413F54;
    private final List<Slot> configSlots;
    private final List<OutputButton> amountButtons = new ArrayList<>();
    private final List<Button> removeButtons = new ArrayList<>();
    private final TextureToggleButton modeButton, exportButton, speedButton;
    private final SettingToggleButton<FuzzyMode> fuzzyButton;
    private final OutputButton prevPage, nextPage, energyButton, targetsButton, clearButton;
    private Button prevSlot, nextSlot, prevTargetPage, nextTargetPage;

    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(OutputContent.MENU.get(), OutputScreen::new);
    }

    public OutputScreen(OutputMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, StyleManager.loadStyleDoc("/screens/overload_sim/output_interface.json"));
        configSlots = menu.getAllConfigSlots();
        widgets.add("upgrades", new UpgradesPanel(menu.getSlots(SlotSemantics.UPGRADE),
                () -> Upgrades.getTooltipLinesForMachine(menu.getUpgrades().getUpgradableItem())));
        var frequencyButton = new TextureToggleButton(TextureToggleButton.ButtonType.FREQUENCY_BIND,
                button -> FrequencyApi.openBindingScreen(menu));
        frequencyButton.setTooltipAt(0, List.of(text("frequency"))); addToLeftToolbar(frequencyButton);
        fuzzyButton = new SettingToggleButton<>(Settings.FUZZY_MODE, FuzzyMode.IGNORE_ALL, (button, backwards) -> menu.cycleFuzzyMode());
        addToLeftToolbar(fuzzyButton);
        prevPage = toolbar(Icon.ARROW_LEFT, "previous_page", () -> menu.setPage(menu.currentPage - 1));
        nextPage = toolbar(Icon.ARROW_RIGHT, "next_page", () -> menu.setPage(menu.currentPage + 1));
        modeButton = new TextureToggleButton(TextureToggleButton.ButtonType.MODE, button -> menu.cycleInterfaceMode());
        modeButton.setTooltipOn(List.of(Component.translatable("ae2lt.gui.interface_mode.wireless")));
        modeButton.setTooltipOff(List.of(Component.translatable("ae2lt.gui.interface_mode.normal"))); addToLeftToolbar(modeButton);
        exportButton = new TextureToggleButton(TextureToggleButton.ButtonType.AUTO_EXPORT, button -> menu.cycleExportMode());
        exportButton.setTooltipOn(List.of(Component.translatable("ae2lt.gui.export_mode.auto")));
        exportButton.setTooltipOff(List.of(Component.translatable("ae2lt.gui.export_mode.off"))); addToLeftToolbar(exportButton);
        speedButton = new TextureToggleButton(TextureToggleButton.ButtonType.SPEED, button -> menu.cycleIOSpeed());
        speedButton.setTooltipOn(List.of(Component.translatable("ae2lt.gui.io_speed.fast")));
        speedButton.setTooltipOff(List.of(Component.translatable("ae2lt.gui.io_speed.normal"))); addToLeftToolbar(speedButton);
        energyButton = toolbar(Icon.POWER_UNIT_AE, "energy_direction", menu::cycleEnergyDirection);
        targetsButton = toolbar(Icon.COG, "targets", menu::toggleTargetsView);
        clearButton = toolbar(Icon.CLEAR, "clear_targets", () -> menu.clearTargets(menu.selectedTargetSlot));
        for (int i = 0; i < configSlots.size(); i++) {
            int slot = i;
            var button = new OutputButton(Icon.COG, b -> {
                if (hasShiftDown()) menu.toggleUnlimited(slot);
                else switchToScreen(new OutputAmountScreen(this, slot));
            });
            button.setDisableBackground(true); button.setMessage(text("amount"));
            button.setTooltip(Tooltip.create(text("amount_tooltip"))); amountButtons.add(button);
        }
    }

    private OutputButton toolbar(Icon icon, String key, Runnable action) {
        var button = new OutputButton(icon, b -> action.run());
        button.setMessage(text(key)); addToLeftToolbar(button); return button;
    }
    static Component text(String key, Object... arguments) {
        return Component.translatable("gui.overload_sim.output." + key, arguments);
    }
    static GenericStack displayedStack(Slot slot) {
        var item = slot.getItem(); var wrapped = GenericStack.unwrapItemStack(item);
        if (wrapped != null) return wrapped;
        var key = AEItemKey.of(item); return key == null ? null : new GenericStack(key, item.getCount());
    }

    @Override protected void init() {
        super.init();
        for (var button : amountButtons) addRenderableWidget(button);
        removeButtons.clear();
        for (int row = 0; row < OutputMenu.TARGET_ROWS; row++) {
            int index = row;
            var button = Button.builder(Component.literal("×"), b -> {
                var rows = menu.getTargetRows(); if (index < rows.size()) menu.removeTarget(rows.get(index).token());
            }).bounds(leftPos + 150, topPos + 64 + row * 16, 16, 14).tooltip(Tooltip.create(text("remove_target"))).build();
            removeButtons.add(addRenderableWidget(button));
        }
        prevSlot = addRenderableWidget(Button.builder(Component.literal("<"), b -> menu.selectTargetSlot(menu.selectedTargetSlot - 1))
                .bounds(leftPos + 8, topPos + 34, 16, 16).build());
        nextSlot = addRenderableWidget(Button.builder(Component.literal(">"), b -> menu.selectTargetSlot(menu.selectedTargetSlot + 1))
                .bounds(leftPos + 152, topPos + 34, 16, 16).build());
        prevTargetPage = addRenderableWidget(Button.builder(Component.literal("<"), b -> menu.setTargetPage(menu.targetPage - 1))
                .bounds(leftPos + 8, topPos + 143, 16, 16).build());
        nextTargetPage = addRenderableWidget(Button.builder(Component.literal(">"), b -> menu.setTargetPage(menu.targetPage + 1))
                .bounds(leftPos + 152, topPos + 143, 16, 16).build());
    }

    @Override protected void updateBeforeRender() {
        super.updateBeforeRender(); menu.applyPageVisibility();
        fuzzyButton.set(menu.fuzzyMode); fuzzyButton.setVisibility(menu.fuzzyInstalled);
        modeButton.setState(menu.interfaceMode == 1); exportButton.setState(menu.exportMode == 1); speedButton.setState(menu.ioSpeedMode == 1);
        energyButton.setMessage(text("energy_direction_value", menu.energyDirOrdinal < 0 ? text("direction_none")
                : Component.translatable("direction.overload_sim." + menu.energyDirOrdinal)));
        targetsButton.setMessage(text(menu.targetsView ? "configuration" : "targets"));
        clearButton.setVisibility(menu.targetsView); clearButton.active = menu.targetCount > 0 && menu.targetToolHeld;
        clearButton.setMessage(text(menu.targetToolHeld ? "clear_targets" : "need_tool"));
        prevPage.setVisibility(!menu.targetsView && menu.currentPage > 0);
        nextPage.setVisibility(!menu.targetsView && menu.currentPage < 1);
        for (int i = 0; i < amountButtons.size(); i++) {
            var button = amountButtons.get(i); int local = i % 18;
            button.setPosition(leftPos + 8 + local % 9 * 18, topPos + (local < 9 ? 35 : 95));
            button.visible = !menu.targetsView && OutputMenuRules.visibleSlot(i, menu.currentPage) && !configSlots.get(i).getItem().isEmpty();
        }
        var rows = menu.getTargetRows();
        for (int i = 0; i < removeButtons.size(); i++) {
            var button = removeButtons.get(i);
            button.visible = menu.targetsView && i < rows.size(); button.active = menu.targetToolHeld;
            button.setTooltip(Tooltip.create(text(menu.targetToolHeld ? "remove_target" : "need_tool")));
        }
        prevSlot.visible = nextSlot.visible = prevTargetPage.visible = nextTargetPage.visible = menu.targetsView;
        prevSlot.active = menu.selectedTargetSlot > 0; nextSlot.active = menu.selectedTargetSlot < 35;
        prevTargetPage.active = menu.targetPage > 0; nextTargetPage.active = menu.targetPage + 1 < menu.targetPages();
    }

    @Override public void drawBG(GuiGraphics g, int x, int y, int mouseX, int mouseY, float partial) {
        super.drawBG(g, x, y, mouseX, mouseY, partial);
        // Native geometry adds the outward arrow and a pink accent to the existing white interface.
        g.fill(x + 147, y + 17, x + 160, y + 19, 0xFFD981AC);
        g.fill(x + 156, y + 14, x + 159, y + 22, 0xFFD981AC);
        g.fill(x + 159, y + 16, x + 162, y + 20, 0xFFD981AC);
        if (menu.targetsView) {
            g.fill(x + 6, y + 32, x + 170, y + 160, 0xFFD9A2BE);
            g.fill(x + 7, y + 33, x + 169, y + 159, 0xFFF4F0F4);
            for (int row = 0; row < OutputMenu.TARGET_ROWS; row++)
                g.fill(x + 8, y + 63 + row * 16, x + 168, y + 79 + row * 16, row % 2 == 0 ? 0xFFE9E1EA : 0xFFF4F0F4);
        }
    }

    @Override public void drawFG(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
        super.drawFG(g, x, y, mouseX, mouseY);
        if (menu.targetsView) {
            centered(g, text("source_slot", menu.selectedTargetSlot + 1, 36), 38);
            var stack = displayedStack(configSlots.get(menu.selectedTargetSlot));
            String resource = stack == null ? text("empty_slot").getString() : stack.what().getDisplayName().getString();
            g.drawString(font, font.plainSubstrByWidth(resource, 154), 10, 53, TEXT, false);
            var rows = menu.getTargetRows();
            for (int i = 0; i < rows.size(); i++) {
                var row = rows.get(i); int top = 65 + i * 16;
                g.drawString(font, font.plainSubstrByWidth(row.dimension(), 136), 10, top, TEXT, false);
                var location = text("target_position", row.x(), row.y(), row.z(), Component.translatable("direction.overload_sim." + row.face()));
                g.drawString(font, font.plainSubstrByWidth(location.getString(), 136), 10, top + 8, TEXT, false);
            }
            if (rows.isEmpty()) centered(g, text("no_targets"), 91);
            centered(g, text("target_page", menu.targetPage + 1, menu.targetPages(), menu.targetCount), 147);
        } else {
            centered(g, Component.literal((menu.currentPage + 1) + "/2"), 18);
            for (int i = menu.currentPage * 18; i < menu.currentPage * 18 + 18; i++) if (menu.isSlotUnlimited(i)) {
                var slot = configSlots.get(i); if (!slot.getItem().isEmpty()) g.drawString(font, "∞", slot.x + 9, slot.y - 10, 0xBF397A, true);
            }
        }
    }
    private void centered(GuiGraphics g, Component value, int y) {
        g.drawString(font, value, (imageWidth - font.width(value)) / 2, y, TEXT, false);
    }
    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        super.render(g, mouseX, mouseY, partial);
        if (!menu.targetsView) return;
        var rows = menu.getTargetRows();
        if (mouseX >= leftPos + 8 && mouseX < leftPos + 149) for (int i = 0; i < rows.size(); i++) {
            if (mouseY < topPos + 63 + i * 16 || mouseY >= topPos + 79 + i * 16) continue;
            var row = rows.get(i);
            g.renderTooltip(font, List.of(Component.literal(row.dimension()), text("target_position", row.x(), row.y(), row.z(),
                    Component.translatable("direction.overload_sim." + row.face()))), java.util.Optional.empty(), mouseX, mouseY);
        }
    }
    private static final class OutputButton extends IconButton {
        private final Icon icon;
        OutputButton(Icon icon, OnPress action) { super(action); this.icon = icon; }
        @Override protected Icon getIcon() { return icon; }
    }
}
