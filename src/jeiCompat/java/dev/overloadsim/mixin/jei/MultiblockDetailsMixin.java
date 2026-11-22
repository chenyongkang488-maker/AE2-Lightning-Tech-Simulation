package dev.overloadsim.mixin.jei;

import com.moakiee.ae2lt.integration.recipeviewer.multiblock.InteractiveMultiblockPreview;
import com.moakiee.ae2lt.integration.recipeviewer.multiblock.InteractiveMultiblockPreview.SlotDelegate;
import com.moakiee.ae2lt.integration.recipeviewer.multiblock.InteractiveMultiblockPreview.SlotKind;
import com.moakiee.ae2lt.integration.recipeviewer.multiblock.InteractiveMultiblockPreview.SlotReference;
import com.moakiee.ae2lt.integration.recipeviewer.multiblock.MultiblockStructureRecipe.Cell;
import dev.overloadsim.compat.MultiblockPreviewLayout;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keep beta4's detail text and slots readable when its normal 184px canvas cannot fit. */
@Mixin(value = InteractiveMultiblockPreview.class, remap = false)
public abstract class MultiblockDetailsMixin {
    @Shadow @Final private int height;
    @Shadow @Final private int panelX;
    @Shadow @Final private SlotDelegate slots;
    @Shadow private Cell selectedCell;
    @Shadow protected abstract int panelContentTop();
    @Shadow protected abstract int panelContentBottom();
    @Shadow private static void enableLocalScissor(GuiGraphics g, int left, int top, int right, int bottom) {
        throw new AssertionError();
    }
    @Shadow private static void drawTrimmed(GuiGraphics g, Font font, Component text,
            int x, int y, int width, int color) { throw new AssertionError(); }

    @Unique private boolean overloadsim$detailsDrawn;
    @Unique private int overloadsim$detailScroll;
    @Unique private Cell overloadsim$lastSelection;
    @Unique private static final int OVERLOADSIM_DETAIL_HEIGHT = 120;

    @Inject(method = "drawMaterials", at = @At("HEAD"))
    private void materialTab(GuiGraphics g, Font font, double x, double y, CallbackInfo ci) {
        overloadsim$detailsDrawn = false;
    }

    @Inject(method = "drawDetails", at = @At("HEAD"), cancellable = true)
    private void scrollableDetails(GuiGraphics g, Font font, CallbackInfo ci) {
        overloadsim$detailsDrawn = true;
        if (height >= 184) return;
        if (selectedCell != overloadsim$lastSelection) {
            overloadsim$lastSelection = selectedCell;
            overloadsim$detailScroll = 0;
        }
        int top = panelContentTop(), bottom = panelContentBottom();
        enableLocalScissor(g, panelX + 1, top, panelX + 104, bottom);
        if (selectedCell == null) {
            int y = overloadsim$y(5);
            for (var line : font.split(Component.translatable("jei.ae2lt.multiblock.select_hint"), 95)) {
                g.drawString(font, line, panelX + 5, y, 0xFFD0D0D0, false);
                y += font.lineHeight + 2;
            }
        } else {
            Block block = selectedCell.state().getBlock();
            // Slot positions are actual screen-local positions, including the scroll offset.
            slots.drawSelectedBlockSlot(g, block, panelX + 4, overloadsim$y(2));
            drawTrimmed(g, font, block.getName(), panelX + 25, overloadsim$y(3), 78, 0xFFFFFFFF);
            drawTrimmed(g, font, Component.translatable("jei.ae2lt.multiblock.role_label", selectedCell.role()),
                    panelX + 4, overloadsim$y(24), 97, 0xFFD0D0D0);
            drawTrimmed(g, font, Component.translatable("jei.ae2lt.multiblock.position",
                    selectedCell.localPos().getX(), selectedCell.localPos().getY(), selectedCell.localPos().getZ()),
                    panelX + 4, overloadsim$y(35), 97, 0xFFD0D0D0);
            drawTrimmed(g, font, Component.translatable("jei.ae2lt.multiblock.replacements"),
                    panelX + 4, overloadsim$y(48), 97, 0xFFFFFFFF);
            List<Block> alternatives = overloadsim$alternatives();
            boolean air = selectedCell.alternatives().contains(Blocks.AIR);
            if (alternatives.size() == 1 && !air) {
                drawTrimmed(g, font, Component.translatable("jei.ae2lt.multiblock.fixed"),
                        panelX + 4, overloadsim$y(62), 97, 0xFFD0D0D0);
            } else {
                for (int i = 0; i < Math.min(alternatives.size(), slots.alternativeSlotCount()); i++) {
                    slots.drawAlternativeSlot(g, i, alternatives.get(i),
                            panelX + 4 + i % 4 * 23, overloadsim$y(63 + i / 4 * 21));
                }
                if (air) {
                    int i = Math.min(alternatives.size(), 7);
                    int x = panelX + 4 + i % 4 * 23, y = overloadsim$y(63 + i / 4 * 21);
                    g.fill(x, y, x + 18, y + 18, 0xFF262626);
                    g.renderOutline(x, y, 18, 18, 0xFF626262);
                    g.drawString(font, "X", x + (18 - font.width("X")) / 2, y + 5, 0xFFD0D0D0, false);
                }
            }
            if (!selectedCell.rules().isEmpty()) {
                drawTrimmed(g, font, selectedCell.rules().getFirst(),
                        panelX + 4, overloadsim$y(109), 97, 0xFFD0D0D0);
            }
        }
        g.disableScissor();
        int max = MultiblockPreviewLayout.clampScroll(Integer.MAX_VALUE, bottom - top, OVERLOADSIM_DETAIL_HEIGHT);
        if (max > 0) {
            int track = bottom - top - 2;
            int thumb = Math.max(12, track * (bottom - top) / OVERLOADSIM_DETAIL_HEIGHT);
            int y = top + 1 + overloadsim$detailScroll * (track - thumb) / max;
            g.fill(panelX + 101, top + 1, panelX + 103, bottom - 1, 0xFF303030);
            g.fill(panelX + 101, y, panelX + 103, y + thumb, 0xFFB0B0B0);
        }
        ci.cancel();
    }

    @Inject(method = "handleMouseScrolled", at = @At("HEAD"), cancellable = true)
    private void scrollDetails(double x, double y, double dx, double dy, CallbackInfoReturnable<Boolean> cir) {
        if (height >= 184 || !overloadsim$detailsDrawn || !overloadsim$inside(x, y)) return;
        overloadsim$detailScroll = MultiblockPreviewLayout.clampScroll(
                overloadsim$detailScroll - (int) Math.round(dy * 14),
                panelContentBottom() - panelContentTop(), OVERLOADSIM_DETAIL_HEIGHT);
        cir.setReturnValue(true);
    }

    @Inject(method = "getSlotUnderMouse", at = @At("HEAD"), cancellable = true)
    private void detailSlot(double x, double y, CallbackInfoReturnable<Optional<SlotReference>> cir) {
        if (height >= 184 || !overloadsim$detailsDrawn) return;
        Optional<SlotReference> result = Optional.empty();
        if (selectedCell != null && overloadsim$inside(x, y)) {
            if (overloadsim$slotContains(x, y, 0, 2)) {
                result = Optional.of(new SlotReference(SlotKind.SELECTED, 0));
            } else {
                List<Block> alternatives = overloadsim$alternatives();
                if (alternatives.size() != 1 || selectedCell.alternatives().contains(Blocks.AIR)) {
                    for (int i = 0; i < Math.min(alternatives.size(), slots.alternativeSlotCount()); i++) {
                        if (overloadsim$slotContains(x, y, i % 4 * 23, 63 + i / 4 * 21)) {
                            result = Optional.of(new SlotReference(SlotKind.ALTERNATIVE, i));
                            break;
                        }
                    }
                }
            }
        }
        cir.setReturnValue(result);
    }

    @Inject(method = "getTooltip", at = @At("HEAD"), cancellable = true)
    private void detailTooltip(double x, double y, CallbackInfoReturnable<List<Component>> cir) {
        if (height >= 184 || !overloadsim$detailsDrawn || !overloadsim$inside(x, y)) return;
        List<Component> tooltip = List.of();
        if (selectedCell != null) {
            int i = Math.min(overloadsim$alternatives().size(), 7);
            if (selectedCell.alternatives().contains(Blocks.AIR)
                    && overloadsim$slotContains(x, y, i % 4 * 23, 63 + i / 4 * 21)) {
                tooltip = List.of(Component.translatable("jei.ae2lt.multiblock.air"));
            } else if (y >= overloadsim$y(109)) {
                tooltip = selectedCell.rules();
            }
        }
        cir.setReturnValue(tooltip);
    }

    @Unique private boolean overloadsim$inside(double x, double y) {
        return x >= panelX + 1 && x < panelX + 104
                && MultiblockPreviewLayout.contains(panelContentTop(), panelContentBottom(), y);
    }
    @Unique private int overloadsim$y(int offset) {
        return MultiblockPreviewLayout.contentY(panelContentTop(), offset, overloadsim$detailScroll);
    }
    @Unique private boolean overloadsim$slotContains(double x, double y, int dx, int offset) {
        int left = panelX + 4 + dx, top = overloadsim$y(offset);
        return x >= left && x < left + 18 && y >= top && y < top + 18;
    }
    @Unique private List<Block> overloadsim$alternatives() {
        return selectedCell.alternatives().stream().filter(b -> b != Blocks.AIR).toList();
    }
}
