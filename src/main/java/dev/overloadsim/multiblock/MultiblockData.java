package dev.overloadsim.multiblock;
import java.util.*;
import com.google.gson.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.*;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.Block;
public final class MultiblockData extends SimpleJsonResourceReloadListener {
    public record Policy(int ticks,int reductionCap,int fortuneCap,int t1,int t2,int t3,long fe,long hv,long overloadEhv,long smeltingHv){
        public int duration(SimulationStructure s){long reduction=Math.min(reductionCap,s.t1()*(long)t1+s.t2()*(long)t2+s.t3()*(long)t3);long time=Math.max(1,ticks-reduction);return (int)(s.overload()?(time+1)/2:time);}
        public long multiplier(SimulationStructure s){return 1L<<Math.min(s.fortune(),fortuneCap);}
        public MultiblockRules.Costs cost(int n,SimulationStructure s){return new MultiblockRules.Costs(Math.multiplyExact(n,fe),Math.multiplyExact(n,Math.addExact(hv,s.smelting()?smeltingHv:0)),Math.multiplyExact(n,s.overload()?overloadEhv:0));}
        public String signature(SimulationStructure s){return toString()+":"+s.t1()+":"+s.t2()+":"+s.t3()+":"+s.fortune()+":"+s.overload()+":"+s.smelting();}
    }
    public record Smelt(ResourceLocation result,int count){}
    private static final Policy DEFAULT=new Policy(180,104,10,2,4,8,1000,1,1,2);
    private static volatile Policy policy=DEFAULT;
    private static volatile Map<ResourceLocation,Smelt> smelts=defaults();
    private static volatile Map<ResourceLocation,SimulationPartBlock.Kind> aliases=Map.of();
    public MultiblockData(){super(new Gson(),"multiblock_simulation");}
    public static Policy policy(){return policy;}
    public static SimulationPartBlock.Kind module(Block block){return block instanceof SimulationPartBlock p?p.kind():aliases.get(BuiltInRegistries.BLOCK.getKey(block));}
    private static Map<ResourceLocation,Smelt> defaults(){var map=new HashMap<ResourceLocation,Smelt>();for(var metal:List.of("iron","gold","copper"))map.put(ResourceLocation.parse("minecraft:raw_"+metal),new Smelt(ResourceLocation.parse("minecraft:"+metal+"_ingot"),2));return Map.copyOf(map);}
    private static int number(JsonObject j,String key,int fallback,int max){int n=j.has(key)?j.get(key).getAsInt():fallback;if(n<0||n>max)throw new IllegalArgumentException("invalid "+key);return n;}
    @Override protected void apply(Map<ResourceLocation,JsonElement> files,ResourceManager manager,ProfilerFiller profiler){
        Policy next=DEFAULT;var mappings=new HashMap<>(defaults());var modules=new HashMap<ResourceLocation,SimulationPartBlock.Kind>();
        for(var entry:files.entrySet()){
            var j=entry.getValue().getAsJsonObject();
            if(j.has("policy")){
                var p=j.getAsJsonObject("policy");next=new Policy(Math.max(1,number(p,"ticks",180,1_000_000)),number(p,"reduction_cap",104,1_000_000),number(p,"fortune_cap",10,10),number(p,"t1",2,1_000_000),number(p,"t2",4,1_000_000),number(p,"t3",8,1_000_000),number(p,"fe",1000,1_000_000),number(p,"hv",1,1_000_000),number(p,"overload_ehv",1,1_000_000),number(p,"smelting_hv",2,1_000_000));
            }
            if(j.has("smelting"))for(var value:j.getAsJsonArray("smelting")){
                var s=value.getAsJsonObject();mappings.put(ResourceLocation.parse(s.get("input").getAsString()),new Smelt(ResourceLocation.parse(s.get("result").getAsString()),Math.max(1,number(s,"count",2,64))));
            }
            if(j.has("modules"))for(var value:j.getAsJsonArray("modules")){
                var m=value.getAsJsonObject();modules.put(ResourceLocation.parse(m.get("block").getAsString()),SimulationPartBlock.Kind.valueOf(m.get("kind").getAsString().toUpperCase(java.util.Locale.ROOT)));
            }
        }policy=next;smelts=Map.copyOf(mappings);aliases=Map.copyOf(modules);
    }
    public static Smelt smelt(ServerLevel level,ItemStack stack){
        var mapping=smelts.get(BuiltInRegistries.ITEM.getKey(stack.getItem()));if(mapping!=null)return mapping;
        var raw=net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,ResourceLocation.parse("c:raw_materials"));
        if(!stack.is(raw))return null;
        var recipe=level.getRecipeManager().getRecipeFor(RecipeType.SMELTING,new SingleRecipeInput(stack),level);
        if(recipe.isEmpty())return null;var result=recipe.get().value().getResultItem(level.registryAccess());
        var ingots=net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,ResourceLocation.parse("c:ingots"));
        return result.is(ingots)?new Smelt(BuiltInRegistries.ITEM.getKey(result.getItem()),2):null;
    }
}
