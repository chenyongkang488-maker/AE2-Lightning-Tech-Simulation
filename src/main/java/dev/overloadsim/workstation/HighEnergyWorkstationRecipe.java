package dev.overloadsim.workstation;

import com.moakiee.ae2lt.machine.overloadfactory.recipe.OverloadProcessingRecipe;
import com.moakiee.ae2lt.machine.overloadfactory.recipe.OverloadProcessingRecipeInput;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

/** Independent datapack type using the established counted ingredient/fluid schema. */
public record HighEnergyWorkstationRecipe(OverloadProcessingRecipe delegate) implements Recipe<OverloadProcessingRecipeInput> {
    @Override public boolean matches(OverloadProcessingRecipeInput input,Level level){return delegate.matches(input,level);}
    @Override public ItemStack assemble(OverloadProcessingRecipeInput input,HolderLookup.Provider registries){return delegate.assemble(input,registries);}
    @Override public boolean canCraftInDimensions(int width,int height){return true;}
    @Override public ItemStack getResultItem(HolderLookup.Provider registries){return delegate.getResultItem(registries);}
    @Override public net.minecraft.core.NonNullList<Ingredient> getIngredients(){return delegate.getIngredients();}
    @Override public boolean isIncomplete(){return delegate.isIncomplete();}
    @Override public RecipeType<?> getType(){return WorkstationContent.RECIPE_TYPE.get();}
    @Override public RecipeSerializer<?> getSerializer(){return WorkstationContent.RECIPE_SERIALIZER.get();}
    public static final class Serializer implements RecipeSerializer<HighEnergyWorkstationRecipe>{
        private final OverloadProcessingRecipe.Serializer original=new OverloadProcessingRecipe.Serializer();
        @Override public MapCodec<HighEnergyWorkstationRecipe> codec(){return original.codec().xmap(HighEnergyWorkstationRecipe::new,HighEnergyWorkstationRecipe::delegate);}
        @Override public StreamCodec<RegistryFriendlyByteBuf,HighEnergyWorkstationRecipe> streamCodec(){return original.streamCodec().map(HighEnergyWorkstationRecipe::new,HighEnergyWorkstationRecipe::delegate);}
    }
}
