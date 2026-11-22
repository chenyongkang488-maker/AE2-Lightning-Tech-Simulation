package dev.overloadsim.mixin.jei;

import com.lowdragmc.lowdraglib2.gui.ui.elements.Scene;
import com.moakiee.ae2lt.integration.recipeviewer.multiblock.MultiblockStructureRecipe;
import dev.overloadsim.compat.MultiblockPreviewLayout;
import java.util.Collection;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.moakiee.ae2lt.integration.recipeviewer.multiblock.LdlibMultiblockScene", remap = false)
public abstract class MultiblockSceneFitMixin {
    @Shadow @Final private Scene scene;
    @Shadow @Final private Map<BlockPos, MultiblockStructureRecipe.Cell> cells;
    @Shadow private float framedZoom;
    @Unique private float overloadsim$originalFramedZoom;
    @Unique private int overloadsim$sizeX, overloadsim$sizeY, overloadsim$sizeZ;
    @Unique private int overloadsim$width = -1, overloadsim$height = -1;
    @Unique private MultiblockStructureRecipe.Cell overloadsim$hoveredCell;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void rememberBounds(MultiblockStructureRecipe recipe, CallbackInfo ci) {
        overloadsim$originalFramedZoom = framedZoom;
        overloadsim$sizeX = recipe.sizeX();
        overloadsim$sizeY = recipe.sizeY();
        overloadsim$sizeZ = recipe.sizeZ();
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void fitViewport(GuiGraphics g, int x, int y, int width, int height, double mx, double my,
            int visibleKey, Collection<BlockPos> visiblePositions, float yaw, float pitch, CallbackInfo ci) {
        if (width == overloadsim$width && height == overloadsim$height) return;
        overloadsim$width = width;
        overloadsim$height = height;
        float zoom = MultiblockPreviewLayout.frameZoom(overloadsim$originalFramedZoom,
                overloadsim$sizeX, overloadsim$sizeY, overloadsim$sizeZ, width, height);
        float relativeZoom = scene.getZoom() / framedZoom;
        framedZoom = zoom;
        scene.setZoom(zoom * relativeZoom);
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void rememberVisibleHit(GuiGraphics g, int x, int y, int width, int height, double mx, double my,
            int visibleKey, Collection<BlockPos> visiblePositions, float yaw, float pitch, CallbackInfo ci) {
        overloadsim$hoveredCell = null;
        if (mx < x || mx >= x + width || my < y || my >= y + height) return;
        var renderer = scene.getRenderer();
        var hit = renderer == null ? null : renderer.getLastTraceResult();
        if (hit != null && hit.getType() == HitResult.Type.BLOCK && visiblePositions.contains(hit.getBlockPos())) {
            overloadsim$hoveredCell = cells.get(hit.getBlockPos());
        }
    }

    @Inject(method = "pick", at = @At("HEAD"), cancellable = true)
    private void pickStandaloneScene(CallbackInfoReturnable<MultiblockStructureRecipe.Cell> cir) {
        // The standalone renderer has a ray hit, but no LDLib widget tree supplies Scene.isHover().
        cir.setReturnValue(overloadsim$hoveredCell);
    }
}
