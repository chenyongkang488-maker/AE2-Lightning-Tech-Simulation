package dev.overloadsim.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.moakiee.ae2lt.integration.jei.category.TeslaCoilCategory;
import dev.overloadsim.integration.jei.SimulationJeiPlugin;
import java.util.Collection;
import java.util.ArrayList;
import java.util.List;
import mezz.jei.api.gui.handlers.IGuiClickableArea;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/** JEI uses the first matching area, so extend the original Tesla area instead of adding an overlap. */
@Pseudo
@Mixin(targets = "com.moakiee.ae2lt.integration.jei.JEIPlugin", remap = false)
public abstract class TeslaJeiClickAreaMixin {
    @ModifyReturnValue(method = "clickableAreaHandler(IIIILmezz/jei/api/recipe/RecipeType;)Lmezz/jei/api/gui/handlers/IGuiContainerHandler;",
            at = @At("RETURN"), require = 0)
    private static IGuiContainerHandler<?> outputRecipes(IGuiContainerHandler<?> original,
            int x, int y, int width, int height, RecipeType<?> type) {
        return withTesla(original, x, y, width, height, List.of(type));
    }

    @ModifyReturnValue(method = "clickableAreaHandler(IIII[Lmezz/jei/api/recipe/RecipeType;)Lmezz/jei/api/gui/handlers/IGuiContainerHandler;",
            at = @At("RETURN"), require = 0)
    private static IGuiContainerHandler<?> outputRecipesNew(IGuiContainerHandler<?> original,
            int x, int y, int width, int height, RecipeType<?>[] types) {
        return withTesla(original, x, y, width, height, List.of(types));
    }

    @Unique private static IGuiContainerHandler<?> withTesla(IGuiContainerHandler<?> original,
            int x, int y, int width, int height, List<RecipeType<?>> types) {
        if (!types.contains(TeslaCoilCategory.TYPE)) return original;
        var merged = new ArrayList<>(types);
        merged.add(SimulationJeiPlugin.TESLA);
        return new IGuiContainerHandler<AbstractContainerScreen<?>>() {
            @Override public Collection<IGuiClickableArea> getGuiClickableAreas(AbstractContainerScreen<?> screen,
                    double mouseX, double mouseY) {
                return List.of(IGuiClickableArea.createBasic(x, y, width, height,
                        merged.toArray(RecipeType<?>[]::new)));
            }
        };
    }
}
