package dev.overloadsim.workstation;

import com.moakiee.ae2lt.machine.overloadfactory.recipe.OverloadProcessingRecipe;
import com.moakiee.ae2lt.registry.ModRecipeTypes;
import dev.overloadsim.core.WorkstationAllocation;
import java.lang.reflect.*;
import java.util.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.neoforged.neoforge.fluids.FluidStack;

/** Final post-script recipe objects; runtime views never get copied into RecipeManager. */
public final class WorkstationRecipes {
    public record Entry(ResourceLocation id,OverloadProcessingRecipe recipe) {}
    public record Batch(Entry entry,int parallel,int[] debit,WorkstationBuffer outputs,FluidStack fluidOutput,int fluidDebit) {}
    @FunctionalInterface public interface Source { List<Entry> recipes(RecipeManager manager); }
    private static final Map<ResourceLocation,Source> SOURCES=new TreeMap<>();
    private static final Method CATALOG=optionalMethod("com.moakiee.ae2lt.machine.overloadfactory.recipe.OverloadProcessingRecipeCatalog","recipes",RecipeManager.class);
    private static final Method FLUID_AMOUNT=optionalMethod(OverloadProcessingRecipe.class.getName(),"inputFluidAmount");
    private static RecipeManager cachedManager;
    private static Collection<RecipeHolder<?>> cachedSources;
    private static List<Entry> cached=List.of();
    private WorkstationRecipes() {}
    public static synchronized void registerSource(ResourceLocation id,Source source){if(SOURCES.putIfAbsent(Objects.requireNonNull(id),Objects.requireNonNull(source))!=null)throw new IllegalArgumentException("duplicate workstation source "+id);invalidate();}
    public static synchronized void invalidate(){cachedManager=null;cachedSources=null;cached=List.of();}
    private static Method optionalMethod(String type,String name,Class<?>... parameters){try{return Class.forName(type,false,WorkstationRecipes.class.getClassLoader()).getMethod(name,parameters);}catch(ClassNotFoundException|NoSuchMethodException absent){return null;}}
    @SuppressWarnings("unchecked")
    public static synchronized List<Entry> recipes(RecipeManager manager){
        if(manager==cachedManager&&manager.getRecipes()==cachedSources)return cached;
        List<RecipeHolder<OverloadProcessingRecipe>> inherited;
        if(CATALOG==null)inherited=manager.getAllRecipesFor(ModRecipeTypes.OVERLOAD_PROCESSING_TYPE.get());
        else try{inherited=(List<RecipeHolder<OverloadProcessingRecipe>>)CATALOG.invoke(null,manager);}catch(ReflectiveOperationException failure){throw new IllegalStateException("cannot read upstream processing catalog",failure);}
        var entries=new LinkedHashMap<ResourceLocation,Entry>();
        for(var holder:inherited)entries.put(holder.id(),new Entry(holder.id(),holder.value()));
        for(var holder:manager.getAllRecipesFor(WorkstationContent.RECIPE_TYPE.get()))entries.put(holder.id(),new Entry(holder.id(),holder.value().delegate()));
        for(var source:SOURCES.values())for(var entry:source.recipes(manager))if(entries.putIfAbsent(entry.id(),entry)!=null)throw new IllegalStateException("duplicate workstation recipe "+entry.id());
        cached=entries.values().stream().sorted(Comparator.<Entry>comparingInt(entry->entry.recipe().priority()).reversed()
                .thenComparing(Comparator.<Entry>comparingInt(entry->entry.recipe().itemInputs().size()).reversed())
                .thenComparing(Comparator.<Entry>comparingLong(entry->entry.recipe().itemInputs().stream().mapToLong(ingredient->ingredient.count()).sum()).reversed())
                .thenComparing(Entry::id)).toList();
        cachedManager=manager;cachedSources=manager.getRecipes();return cached;
    }
    public static Optional<Entry> find(RecipeManager manager,ResourceLocation id){return recipes(manager).stream().filter(entry->entry.id().equals(id)).findFirst();}
    public static int inputFluidAmount(OverloadProcessingRecipe recipe){if(FLUID_AMOUNT==null)return recipe.fluidInput().getAmount();try{return (int)FLUID_AMOUNT.invoke(recipe);}catch(ReflectiveOperationException failure){throw new IllegalStateException("cannot read recipe fluid demand",failure);}}
    public static Optional<Batch> select(RecipeManager manager,WorkstationBuffer inputs,WorkstationBuffer outputs,FluidStack inputFluid,FluidStack outputFluid){
        return select(manager,inputs,outputs,inputFluid,outputFluid,WorkstationSettings.capture());
    }
    static Optional<Batch> select(RecipeManager manager,WorkstationBuffer inputs,WorkstationBuffer outputs,FluidStack inputFluid,FluidStack outputFluid,WorkstationSettings settings){
        Batch best=null;
        for(var entry:recipes(manager)){
            var candidate=evaluate(entry,inputs,outputs,inputFluid,outputFluid,settings);
            if(candidate.isPresent()&&(best==null||candidate.get().parallel()>best.parallel()))best=candidate.get();
        }return Optional.ofNullable(best);
    }
    public static Optional<Batch> evaluate(Entry entry,WorkstationBuffer inputs,WorkstationBuffer outputs,FluidStack inputFluid,FluidStack outputFluid){
        return evaluate(entry,inputs,outputs,inputFluid,outputFluid,WorkstationSettings.capture());
    }
    static Optional<Batch> evaluate(Entry entry,WorkstationBuffer inputs,WorkstationBuffer outputs,FluidStack inputFluid,FluidStack outputFluid,WorkstationSettings settings){
        var recipe=entry.recipe();var requirements=recipe.itemInputs();int[] available=new int[9],demand=new int[requirements.size()];boolean[][] matches=new boolean[requirements.size()][9];
        boolean anyInput=false;for(int slot=0;slot<9;slot++){available[slot]=inputs.count(slot);anyInput|=available[slot]>0;}
        if(requirements.isEmpty()&&anyInput)return Optional.empty();
        for(int requirement=0;requirement<requirements.size();requirement++){var ingredient=requirements.get(requirement);demand[requirement]=ingredient.count();for(int slot=0;slot<9;slot++)if(available[slot]>0)matches[requirement][slot]=ingredient.ingredient().test(inputs.prototype(slot).copyWithCount(available[slot]));}
        int low=1,high=settings.maxParallel();Batch best=null;
        while(low<=high){int parallel=low+(high-low)/2;int[] debit=WorkstationAllocation.allocate(available,demand,matches,parallel);Batch candidate=debit==null?null:batch(entry,parallel,debit,outputs,inputFluid,outputFluid,settings);if(candidate==null)high=parallel-1;else{best=candidate;low=parallel+1;}}
        return Optional.ofNullable(best);
    }
    private static Batch batch(Entry entry,int parallel,int[] debit,WorkstationBuffer outputs,FluidStack inputFluid,FluidStack outputFluid,WorkstationSettings settings){
        var recipe=entry.recipe();if(!recipe.hasRequiredFluid(inputFluid,parallel))return null;
        long fluidCost=(long)inputFluidAmount(recipe)*parallel;if(fluidCost>Integer.MAX_VALUE)return null;
        var projected=outputs.copy(settings.slotCapacity());
        for(ItemStack item:recipe.itemResults()){long amount=(long)item.getCount()*parallel;if(projected.insert(item,amount,false)!=amount)return null;}
        var fluidResult=recipe.fluidResult();long fluidProduced=(long)fluidResult.getAmount()*parallel;
        if(fluidProduced>0&&(fluidProduced+outputFluid.getAmount()>settings.tankCapacity()||!outputFluid.isEmpty()&&!FluidStack.isSameFluidSameComponents(fluidResult,outputFluid)))return null;
        var projectedFluid=fluidProduced==0?outputFluid.copy():fluidResult.copyWithAmount((int)(fluidProduced+outputFluid.getAmount()));
        return new Batch(entry,parallel,debit,projected,projectedFluid,(int)fluidCost);
    }
}
