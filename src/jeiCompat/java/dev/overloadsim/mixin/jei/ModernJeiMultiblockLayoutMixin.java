package dev.overloadsim.mixin.jei;

import com.moakiee.ae2lt.integration.jei.category.MultiblockStructureCategory;
import java.util.List;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.common.util.ImmutableSize2i;
import mezz.jei.gui.bookmarks.BookmarkList;
import mezz.jei.gui.recipes.IRecipeLayoutWithButtons;
import mezz.jei.gui.recipes.RecipeGuiLogic;
import mezz.jei.gui.recipes.RecipesGui;
import mezz.jei.gui.recipes.layouts.IRecipeLayoutList;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The plugin enables this hook only when JEI exposes the new size-based signature. */
@Mixin(value = RecipeGuiLogic.class, remap = false)
public abstract class ModernJeiMultiblockLayoutMixin {
    @Shadow private IRecipeLayoutList cachedRecipeLayoutsWithButtons;
    @Shadow public abstract IRecipeCategory<?> getSelectedRecipeCategory();

    @Inject(method = "getVisibleRecipeLayoutsWithButtons(Lmezz/jei/common/util/ImmutableSize2i;Lnet/minecraft/world/inventory/AbstractContainerMenu;Lmezz/jei/gui/bookmarks/BookmarkList;Lmezz/jei/gui/recipes/RecipesGui;)Ljava/util/List;", at = @At("HEAD"))
    private void fitPreview(ImmutableSize2i availableSize, AbstractContainerMenu container,
            BookmarkList bookmarks, RecipesGui gui,
            CallbackInfoReturnable<List<IRecipeLayoutWithButtons<?>>> cir) {
        if (getSelectedRecipeCategory() instanceof MultiblockStructureCategory category
                && category.setAvailableHeight(availableSize.height(), 0)) {
            cachedRecipeLayoutsWithButtons = null;
        }
    }
}
