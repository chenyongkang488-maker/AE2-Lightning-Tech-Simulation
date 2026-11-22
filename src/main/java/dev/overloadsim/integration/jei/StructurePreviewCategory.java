package dev.overloadsim.integration.jei;

import dev.overloadsim.construction.StructurePreviewCatalog;
import dev.overloadsim.laser.LaserContent;
import dev.overloadsim.laser.LaserKind;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Building information, not a crafting or processing recipe. Indexed by each actual controller. */
public final class StructurePreviewCategory implements IRecipeCategory<StructurePreviewCatalog.Layout> {
    public static final int WIDTH=286,HEIGHT=144;
    public static final RecipeType<StructurePreviewCatalog.Layout> TYPE=RecipeType.create("overload_sim","multiblock_structure",StructurePreviewCatalog.Layout.class);
    private final IDrawable icon;
    public StructurePreviewCategory(IGuiHelper gui){icon=gui.createDrawableIngredient(VanillaTypes.ITEM_STACK,new ItemStack(LaserContent.machine(LaserKind.DEFLECTOR)));}
    @Override public RecipeType<StructurePreviewCatalog.Layout> getRecipeType(){return TYPE;}
    @Override public Component getTitle(){return Component.translatable("jei.overload_sim.structure.title");}
    @Override public IDrawable getIcon(){return icon;}
    @Override public int getWidth(){return WIDTH;}
    @Override public int getHeight(){return HEIGHT;}
    @Override public ResourceLocation getRegistryName(StructurePreviewCatalog.Layout recipe){return recipe.id();}
    @Override public void setRecipe(IRecipeLayoutBuilder builder,StructurePreviewCatalog.Layout recipe,IFocusGroup focuses){
        // Input rows include the controller, so U filters to its own structure; the output also makes R discoverable.
        builder.addSlot(RecipeIngredientRole.OUTPUT,268,1).addItemStack(new ItemStack(recipe.controller()));
        var materials=recipe.materials();
        for(int i=0;i<materials.size();i++){
            var material=materials.get(i);
            builder.addSlot(RecipeIngredientRole.INPUT,194,32+i*22).setStandardSlotBackground()
                    .addItemStack(new ItemStack(material.block(),material.count()))
                    .addRichTooltipCallback((slot,tooltip)->tooltip.add(Component.translatable("jei.overload_sim.structure.required",material.count())));
        }
    }
    @Override public void createRecipeExtras(IRecipeExtrasBuilder builder,StructurePreviewCatalog.Layout recipe,IFocusGroup focuses){
        var widget=new StructurePreviewWidget(recipe);
        builder.addWidget(widget);builder.addInputHandler(widget);
    }
    @Override public void draw(StructurePreviewCatalog.Layout recipe,IRecipeSlotsView slots,GuiGraphics graphics,double mx,double my){
        var font=Minecraft.getInstance().font;
        graphics.drawString(font,font.plainSubstrByWidth(recipe.controller().getName().getString(),254),4,3,0x413F54,false);
        graphics.drawString(font,Component.translatable("jei.overload_sim.structure.materials"),192,23,0x413F54,false);
        var materials=recipe.materials();
        for(int i=0;i<materials.size();i++){
            var m=materials.get(i);
            graphics.drawString(font,font.plainSubstrByWidth(m.block().getName().getString(),70),214,32+i*22,0x413F54,false);
            graphics.drawString(font,Component.literal("×"+m.count()),214,43+i*22,0x69667D,false);
        }
        graphics.drawString(font,Component.translatable("jei.overload_sim.structure.total",materials.stream().mapToInt(StructurePreviewCatalog.Material::count).sum()),192,122,0x413F54,false);
        graphics.drawString(font,Component.translatable("jei.overload_sim.structure.info"),192,135,0x856285,false);
    }
    @Override public void getTooltip(mezz.jei.api.gui.builder.ITooltipBuilder tooltip,StructurePreviewCatalog.Layout recipe,IRecipeSlotsView slots,double mx,double my){
        if(mx<190||my<132||my>=144)return;
        var lines=new java.util.ArrayList<Component>();
        lines.add(Component.translatable("jei.overload_sim.structure.not_crafting"));
        lines.add(Component.translatable("jei.overload_sim.structure.count_hint"));
        if(recipe.kind()!=null)lines.add(Component.translatable("jei.overload_sim.structure.optional_network"));
        if(recipe.kind()==LaserKind.RECEIVER)lines.add(Component.translatable("jei.overload_sim.structure.optional_ports"));
        if(recipe.ring())lines.add(Component.translatable("jei.overload_sim.structure.placement_hint"));
        tooltip.addAll(lines);
    }
}
