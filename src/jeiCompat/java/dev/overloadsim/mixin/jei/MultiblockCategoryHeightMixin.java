package dev.overloadsim.mixin.jei;

import com.moakiee.ae2lt.integration.jei.category.MultiblockStructureCategory;
import dev.overloadsim.compat.MultiblockPreviewLayout;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = MultiblockStructureCategory.class, remap = false)
public abstract class MultiblockCategoryHeightMixin {
    @Shadow private int layoutHeight;

    @Inject(method = "setAvailableHeight", at = @At("HEAD"), cancellable = true)
    private void fitHeight(int availableHeight, int padding, CallbackInfoReturnable<Boolean> cir) {
        int height = MultiblockPreviewLayout.fitHeight(availableHeight, padding);
        boolean changed = layoutHeight != height;
        layoutHeight = height;
        cir.setReturnValue(changed);
    }
}
