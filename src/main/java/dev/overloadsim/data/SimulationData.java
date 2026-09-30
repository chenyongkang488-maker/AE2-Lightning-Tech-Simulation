package dev.overloadsim.data;

import java.util.*;
import com.google.gson.*;
import dev.overloadsim.ModContent;
import dev.overloadsim.api.SimulationProfileView;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.*;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;

public class SimulationData extends SimpleJsonResourceReloadListener {
    private static volatile Map<ResourceLocation,SimulationProfileView> profiles=Map.of();
    private static RecipeManager indexedManager;
    private static final Map<SimulationRecipe.Kind,List<RecipeHolder<SimulationRecipe>>> INDEX=new EnumMap<>(SimulationRecipe.Kind.class);
    public SimulationData(){super(new Gson(),"simulation_profile");}
    @Override protected void apply(Map<ResourceLocation,JsonElement> json,ResourceManager manager,ProfilerFiller profiler){
        var next=new HashMap<ResourceLocation,SimulationProfileView>();
        json.forEach((id,value)->{var j=value.getAsJsonObject();var kind=j.get("kind").getAsString();if(!Set.of("mineral","crop","tree","mob").contains(kind))throw new IllegalArgumentException("invalid profile kind "+id);next.put(id,new SimulationProfileView(id,kind,j.get("name").getAsString(),ResourceLocation.parse(j.get("icon").getAsString())));});
        profiles=Map.copyOf(next); indexedManager=null; INDEX.clear();
    }
    public static Optional<SimulationProfileView> profile(ResourceLocation id){return Optional.ofNullable(profiles.get(id));}
    public static Collection<SimulationProfileView> profiles(){return profiles.values();}
    public static List<RecipeHolder<SimulationRecipe>> recipes(ServerLevel level,SimulationRecipe.Kind kind){
        var manager=level.getRecipeManager();
        if(manager!=indexedManager){INDEX.clear();for(var k:SimulationRecipe.Kind.values()){var entries=new ArrayList<>(manager.getAllRecipesFor(ModContent.RECIPE_TYPES.get(k).get()));entries.sort(Comparator.<RecipeHolder<SimulationRecipe>>comparingInt(h->h.value().data().priority()).reversed().thenComparing(h->h.id().toString())); INDEX.put(k,List.copyOf(entries));}indexedManager=manager;}
        return INDEX.get(kind);
    }
    /** Ambiguous highest-priority matches fail closed instead of relying on load order. */
    public static Optional<RecipeHolder<SimulationRecipe>> select(List<RecipeHolder<SimulationRecipe>> matches){
        if(matches.isEmpty())return Optional.empty();var first=matches.getFirst();
        if(matches.size()>1 && matches.get(1).value().data().priority()==first.value().data().priority())return Optional.empty();
        return Optional.of(first);
    }
}
