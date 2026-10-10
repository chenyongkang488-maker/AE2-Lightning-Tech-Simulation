package dev.overloadsim.integration.jei;

import com.moakiee.ae2lt.integration.jei.LargeStackJeiItemRenderer;
import com.moakiee.ae2lt.integration.jei.LightningJeiIngredients;
import com.moakiee.ae2lt.me.key.LightningKey;
import com.moakiee.ae2lt.registry.ModBlocks;
import com.moakiee.ae2lt.registry.ModItems;
import dev.overloadsim.ModContent;
import dev.overloadsim.client.HighEnergyWorkstationScreen;
import dev.overloadsim.client.LaserScreen;
import dev.overloadsim.config.MachineSettings;
import dev.overloadsim.workstation.WorkstationContent;
import dev.overloadsim.workstation.WorkstationRecipes;
import java.util.Arrays;
import java.util.List;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

/** Optional JEI entry point; no common registration path references JEI classes. */
@JeiPlugin
public final class SimulationJeiPlugin implements IModPlugin {
    private static final RecipeType<WorkstationRecipes.Entry> WORKSTATION = RecipeType.create(
            "overload_sim", "high_energy_workstation", WorkstationRecipes.Entry.class);
    public static final RecipeType<TeslaRecipe> TESLA = RecipeType.create(
            "overload_sim", "transcendent_tesla", TeslaRecipe.class);
    public record TeslaRecipe() {}

    @Override public ResourceLocation getPluginUid() { return ModContent.id("machines"); }

    @Override public void registerCategories(IRecipeCategoryRegistration registration) {
        var gui = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(new WorkstationCategory(gui), new TranscendentTeslaCategory(gui));
    }

    @Override public void registerRecipes(IRecipeRegistration registration) {
        var level = Minecraft.getInstance().level;
        if (level != null) registration.addRecipes(WORKSTATION, WorkstationRecipes.recipes(level.getRecipeManager()));
        registration.addRecipes(TESLA, List.of(new TeslaRecipe()));
    }

    @Override public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(WorkstationContent.CONTROLLER_ITEM.get()), WORKSTATION);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.TESLA_COIL.get()), TESLA);
    }

    @Override public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addRecipeClickArea(HighEnergyWorkstationScreen.class, 91, 90, 18, 32, WORKSTATION);
    }

    private abstract static class MachineCategory<T> implements IRecipeCategory<T> {
        private final RecipeType<T> type;
        private final Component title;
        private final IDrawable icon;
        MachineCategory(IGuiHelper gui, RecipeType<T> type, Component title, ItemStack icon) {
            this.type = type; this.title = title;
            this.icon = gui.createDrawableIngredient(VanillaTypes.ITEM_STACK, icon);
        }
        @Override public RecipeType<T> getRecipeType() { return type; }
        @Override public Component getTitle() { return title; }
        @Override public IDrawable getIcon() { return icon; }
        @Override public int getWidth() { return 220; }
        @Override public int getHeight() { return 116; }
        void line(GuiGraphics graphics, Component text, int y) {
            var font = Minecraft.getInstance().font;
            graphics.drawString(font, text, (getWidth() - font.width(text)) / 2, y, 0x413F54, false);
        }
        void arrow(GuiGraphics graphics) {
            graphics.fill(90, 29, 121, 33, 0xFFD981AC);
            graphics.fill(117, 25, 121, 37, 0xFFD981AC);
            graphics.fill(121, 27, 125, 35, 0xFFD981AC);
        }
    }

    private static final class WorkstationCategory extends MachineCategory<WorkstationRecipes.Entry> {
        WorkstationCategory(IGuiHelper gui) {
            super(gui, WORKSTATION, Component.translatable("block.overload_sim.high_energy_workstation"),
                    new ItemStack(WorkstationContent.CONTROLLER_ITEM.get()));
        }
        @Override public void setRecipe(IRecipeLayoutBuilder builder, WorkstationRecipes.Entry entry, IFocusGroup focuses) {
            var recipe = entry.recipe();
            for (int i = 0; i < recipe.itemInputs().size(); i++) {
                var input = recipe.itemInputs().get(i);
                builder.addSlot(RecipeIngredientRole.INPUT, 22 + i % 3 * 18, 6 + i / 3 * 18)
                        .setStandardSlotBackground()
                        .addItemStacks(Arrays.stream(input.ingredient().getItems()).map(s -> s.copyWithCount(input.count())).toList())
                        .setCustomRenderer(VanillaTypes.ITEM_STACK, LargeStackJeiItemRenderer.INSTANCE);
            }
            for (int i = 0; i < recipe.itemResults().size(); i++)
                builder.addSlot(RecipeIngredientRole.OUTPUT, 142 + i % 3 * 18, 6 + i / 3 * 18)
                        .setStandardSlotBackground().addItemStack(recipe.itemResults().get(i).copy())
                        .setCustomRenderer(VanillaTypes.ITEM_STACK, LargeStackJeiItemRenderer.INSTANCE);
            fluid(builder, RecipeIngredientRole.INPUT, 3, recipe.fluidInput(), WorkstationRecipes.inputFluidAmount(recipe));
            fluid(builder, RecipeIngredientRole.OUTPUT, 201, recipe.fluidResult(), recipe.fluidResult().getAmount());
        }
        private static void fluid(IRecipeLayoutBuilder builder, RecipeIngredientRole role, int x, FluidStack stack, int amount) {
            if (!stack.isEmpty()) builder.addSlot(role, x, 6)
                    .addFluidStack(stack.getFluid(), amount, stack.getComponentsPatch())
                    .setFluidRenderer(Math.max(1, amount), true, 14, 52);
        }
        @Override public void draw(WorkstationRecipes.Entry entry, IRecipeSlotsView slots, GuiGraphics graphics, double mx, double my) {
            arrow(graphics);
            line(graphics, Component.translatable("jei.overload_sim.workstation.laser",
                    LaserScreen.compact(MachineSettings.longValue(MachineSettings.WORKSTATION_LASER_THRESHOLD))), 69);
            line(graphics, Component.translatable("jei.overload_sim.workstation.cycle",
                    MachineSettings.intValue(MachineSettings.WORKSTATION_CYCLE_TICKS),
                    MachineSettings.intValue(MachineSettings.WORKSTATION_MAX_PARALLEL)), 82);
            line(graphics, Component.translatable("jei.overload_sim.workstation.materials_only"), 95);
        }
    }

    private static final class TranscendentTeslaCategory extends MachineCategory<TeslaRecipe> {
        TranscendentTeslaCategory(IGuiHelper gui) {
            super(gui, TESLA, Component.translatable("jei.overload_sim.tesla.title"), new ItemStack(ModBlocks.TESLA_COIL.get()));
        }
        @Override public void setRecipe(IRecipeLayoutBuilder builder, TeslaRecipe recipe, IFocusGroup focuses) {
            builder.addSlot(RecipeIngredientRole.INPUT, 42, 23).setStandardSlotBackground()
                    .addItemStack(new ItemStack(ModContent.TRANSCENDENT.get()));
            builder.addSlot(RecipeIngredientRole.CATALYST, 69, 23).setStandardSlotBackground()
                    .addItemStack(new ItemStack(ModItems.LIGHTNING_COLLAPSE_MATRIX.get()))
                    .addRichTooltipCallback((slot, tooltip) -> tooltip.add(Component.translatable("jei.overload_sim.tesla.matrix")));
            builder.addSlot(RecipeIngredientRole.OUTPUT, 153, 23).setStandardSlotBackground()
                    .addIngredient(LightningJeiIngredients.TYPE, LightningKey.EXTREME_HIGH_VOLTAGE)
                    .addRichTooltipCallback((slot, tooltip) -> tooltip.add(Component.translatable("jei.overload_sim.tesla.output",
                            MachineSettings.longValue(MachineSettings.TESLA_CRYSTAL_EHV))));
        }
        @Override public void draw(TeslaRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mx, double my) {
            arrow(graphics);
            line(graphics, Component.translatable("jei.overload_sim.tesla.output",
                    MachineSettings.longValue(MachineSettings.TESLA_CRYSTAL_EHV)), 54);
            line(graphics, Component.translatable("jei.overload_sim.tesla.energy",
                    LaserScreen.compact(MachineSettings.longValue(MachineSettings.TESLA_CRYSTAL_FE))), 69);
            line(graphics, Component.translatable("jei.overload_sim.tesla.cycle"), 82);
            line(graphics, Component.translatable("jei.overload_sim.tesla.matrix"), 95);
        }
    }
}
