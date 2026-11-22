package dev.overloadsim.compat;

import java.util.function.BiPredicate;

/** Probe bytecode, never load optional client classes during Mixin discovery. */
public final class JeiPreviewCompatibility {
    public static final String CATEGORY = "com.moakiee.ae2lt.integration.jei.category.MultiblockStructureCategory";
    public static final String PREVIEW = "com.moakiee.ae2lt.integration.recipeviewer.multiblock.InteractiveMultiblockPreview";
    public static final String LOGIC = "mezz.jei.gui.recipes.RecipeGuiLogic";
    public static final String SCENE = "com.moakiee.ae2lt.integration.recipeviewer.multiblock.LdlibMultiblockScene";
    public static final String SCENE_RENDERER = "com.lowdragmc.lowdraglib2.client.scene.WorldSceneRenderer";
    public static final String MODERN_LAYOUT = "getVisibleRecipeLayoutsWithButtons(Lmezz/jei/common/util/ImmutableSize2i;Lnet/minecraft/world/inventory/AbstractContainerMenu;Lmezz/jei/gui/bookmarks/BookmarkList;Lmezz/jei/gui/recipes/RecipesGui;)Ljava/util/List;";

    private JeiPreviewCompatibility() {}

    public static boolean categoryPatch(BiPredicate<String, String> hasMethod) {
        return hasMethod.test(CATEGORY, "setAvailableHeight(II)Z")
                && hasMethod.test(PREVIEW, "handleMouseScrolled(DDDD)Z")
                && hasMethod.test(LOGIC, "getSelectedRecipeCategory()Lmezz/jei/api/recipe/category/IRecipeCategory;");
    }

    public static boolean modernLayoutPatch(BiPredicate<String, String> hasMethod) {
        return categoryPatch(hasMethod) && hasMethod.test(LOGIC, MODERN_LAYOUT);
    }

    public static boolean scenePatch(BiPredicate<String, String> hasMethod) {
        return categoryPatch(hasMethod) && hasMethod.test(SCENE, "resetCamera()V")
                && hasMethod.test(SCENE, "pick()Lcom/moakiee/ae2lt/integration/recipeviewer/multiblock/MultiblockStructureRecipe$Cell;")
                && hasMethod.test(SCENE_RENDERER,
                        "getLastTraceResult()Lnet/minecraft/world/phys/BlockHitResult;")
                && hasMethod.test(SCENE, "render(Lnet/minecraft/client/gui/GuiGraphics;IIIIDDILjava/util/Collection;FF)V");
    }
}
