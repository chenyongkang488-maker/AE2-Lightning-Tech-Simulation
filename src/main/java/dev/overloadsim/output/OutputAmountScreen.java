package dev.overloadsim.output;

import appeng.client.gui.AESubScreen;
import appeng.menu.SlotSemantics;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

/** A client editor only: applying an amount remains a validated server menu action. */
final class OutputAmountScreen extends AESubScreen<OutputMenu, OutputScreen> {
    private final int slot;
    private EditBox amount;
    private Button apply;
    OutputAmountScreen(OutputScreen parent, int slot) {
        super(parent, "/screens/overload_sim/output_amount.json"); this.slot = slot;
    }
    @Override protected boolean shouldAddToolbar() { return false; }
    @Override protected void init() {
        super.init();
        for (var semantic : OutputSlotSemantics.CONFIG) setSlotsHidden(semantic, true);
        for (var semantic : OutputSlotSemantics.STORAGE) setSlotsHidden(semantic, true);
        for (var semantic : java.util.List.of(SlotSemantics.UPGRADE, SlotSemantics.PLAYER_INVENTORY, SlotSemantics.PLAYER_HOTBAR)) setSlotsHidden(semantic, true);
        var configured = OutputScreen.displayedStack(menu.getAllConfigSlots().get(slot));
        amount = new EditBox(font, leftPos + 16, topPos + 43, 144, 18, OutputScreen.text("amount"));
        amount.setMaxLength(19); amount.setFilter(value -> value.chars().allMatch(Character::isDigit));
        amount.setValue(Long.toString(configured == null ? 1 : Math.max(1, configured.amount()))); addRenderableWidget(amount);
        apply = addRenderableWidget(Button.builder(OutputScreen.text("apply"), button -> {
            menu.setAmount(slot, amount.getValue()); returnToParent();
        }).bounds(leftPos + 16, topPos + 71, 68, 20).build());
        addRenderableWidget(Button.builder(OutputScreen.text("cancel"), button -> returnToParent()).bounds(leftPos + 92, topPos + 71, 68, 20).build());
        setInitialFocus(amount);
    }
    @Override protected void updateBeforeRender() {
        super.updateBeforeRender(); apply.active = OutputMenuRules.parseAmount(amount.getValue(), Long.MAX_VALUE) > 0;
    }
    @Override public void drawFG(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
        g.drawString(font, OutputScreen.text("amount_title", slot + 1), 8, 8, 0x413F54, false);
        var configured = OutputScreen.displayedStack(menu.getAllConfigSlots().get(slot));
        if (configured != null) g.drawString(font, font.plainSubstrByWidth(configured.what().getDisplayName().getString(), 160), 8, 26, 0x413F54, false);
        g.drawString(font, OutputScreen.text("amount_units"), 8, 101, 0x413F54, false);
    }
    @Override public void onClose() { returnToParent(); }
    @Override public boolean keyPressed(int key, int scan, int modifiers) {
        if (key == 256) { returnToParent(); return true; }
        if (key == 257 && apply.active) { menu.setAmount(slot, amount.getValue()); returnToParent(); return true; }
        return super.keyPressed(key, scan, modifiers);
    }
}
