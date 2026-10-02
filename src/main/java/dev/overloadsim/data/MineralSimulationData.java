package dev.overloadsim.data;

import java.util.*;
import com.google.gson.*;
import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overloadsim.*;
import dev.overloadsim.api.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.*;
import net.minecraft.tags.TagKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;

/** Conservative tag discovery with explicit JSON/Java overrides and stable material IDs. */
public final class MineralSimulationData extends SimpleJsonResourceReloadListener {
    public record Rule(ResourceLocation material,Optional<ResourceLocation> profile,SimulationRecipe.Selector binding,int priority,boolean disabled,Optional<ResourceLocation> item,int min,int max,Optional<ResourceLocation> ore,Optional<ResolvedMineral.Smelting> smelting,boolean sameBlock){
        private static final Codec<ResolvedMineral.Smelting> SMELT=RecordCodecBuilder.create(i->i.group(ResourceLocation.CODEC.fieldOf("input").forGetter(ResolvedMineral.Smelting::input),ResourceLocation.CODEC.fieldOf("result").forGetter(ResolvedMineral.Smelting::result),Codec.intRange(1,64).optionalFieldOf("count",2).forGetter(ResolvedMineral.Smelting::count)).apply(i,ResolvedMineral.Smelting::new));
        public static final Codec<Rule> CODEC=RecordCodecBuilder.<Rule>create(i->i.group(ResourceLocation.CODEC.fieldOf("material").forGetter(Rule::material),ResourceLocation.CODEC.optionalFieldOf("profile").forGetter(Rule::profile),SimulationRecipe.Selector.CODEC.fieldOf("binding").forGetter(Rule::binding),Codec.INT.optionalFieldOf("priority",0).forGetter(Rule::priority),Codec.BOOL.optionalFieldOf("disabled",false).forGetter(Rule::disabled),ResourceLocation.CODEC.optionalFieldOf("item").forGetter(Rule::item),Codec.intRange(1,4096).optionalFieldOf("min",1).forGetter(Rule::min),Codec.intRange(1,4096).optionalFieldOf("max",1).forGetter(Rule::max),ResourceLocation.CODEC.optionalFieldOf("block_loot").forGetter(Rule::ore),SMELT.optionalFieldOf("smelting").forGetter(Rule::smelting),Codec.BOOL.optionalFieldOf("same_block",false).forGetter(Rule::sameBlock)).apply(i,Rule::new)).validate(r->r.max>=r.min&&(r.disabled||r.item.isPresent()||r.ore.isPresent())?DataResult.success(r):DataResult.error(()->"A mineral needs an output and a valid min/max range"));
        public ResolvedMineral resolved(){return new ResolvedMineral(profile.orElseGet(()->autoId(material)),material,binding.id(),item,min,max,ore,smelting,sameBlock);}
    }
    public record Resolution(Optional<ResolvedMineral> mineral,String error){public boolean valid(){return mineral.isPresent()&&error.isEmpty();}}
    private static volatile Map<ResourceLocation,Rule> rules=Map.of();
    private static Map<ResourceLocation,Resolution> materials=Map.of();
    private static Map<Block,List<ResourceLocation>> bindings=Map.of();
    private static Map<ResourceLocation,Resolution> profiles=Map.of();
    private static ServerLevel indexedLevel;private static long indexedRevision=-1;
    public MineralSimulationData(){super(new Gson(),"simulation_mineral");}
    @Override protected void apply(Map<ResourceLocation,JsonElement> json,ResourceManager manager,ProfilerFiller profiler){var next=new HashMap<ResourceLocation,Rule>();json.forEach((id,j)->next.put(id,Rule.CODEC.parse(JsonOps.INSTANCE,j).getOrThrow()));rules=Map.copyOf(next);SimulationData.invalidate();}
    public static Map<ResourceLocation,Rule> rules(){return rules;}
    public static ResourceLocation autoId(ResourceLocation material){return ModContent.id("auto/mineral/"+material.getNamespace()+"/"+material.getPath());}
    private static Resolution bad(String error){return new Resolution(Optional.empty(),error);}
    private static Resolution ok(ResolvedMineral value){return new Resolution(Optional.of(value),"");}
    private static List<Item> items(ResourceLocation tag){return BuiltInRegistries.ITEM.getTag(TagKey.create(Registries.ITEM,tag)).map(t->t.stream().map(Holder::value).filter(i->i!=Items.AIR).distinct().toList()).orElse(List.of());}
    private static List<Block> blocks(ResourceLocation tag){return BuiltInRegistries.BLOCK.getTag(TagKey.create(Registries.BLOCK,tag)).map(t->t.stream().map(Holder::value).filter(b->b!=Blocks.AIR).distinct().toList()).orElse(List.of());}
    public static void ensure(ServerLevel level){
        if(indexedLevel==level&&indexedRevision==SimulationData.revision())return;
        var byMaterial=new HashMap<ResourceLocation,Resolution>();var byBlock=new HashMap<Block,List<ResourceLocation>>();var byProfile=new HashMap<ResourceLocation,Resolution>();
        var tags=BuiltInRegistries.BLOCK.getTagNames().map(TagKey::location).filter(t->t.getNamespace().equals("c")&&t.getPath().startsWith("storage_blocks/")).sorted().toList();
        for(var tag:tags){
            String name=tag.getPath().substring("storage_blocks/".length());boolean raw=name.startsWith("raw_");String material=raw?name.substring(4):name;
            var key=ResourceLocation.fromNamespaceAndPath("c",material);var bind=blocks(tag);if(bind.isEmpty())continue;
            // Ore/dust discovery is only for materials without raw storage, e.g. redstone.
            // Mek metals expose both branches; they share one stable material profile.
            if(!raw&&!blocks(ResourceLocation.parse("c:storage_blocks/raw_"+material)).isEmpty())continue;
            var resources=raw?items(ResourceLocation.parse("c:raw_materials/"+material)):new ArrayList<Item>();
            var ores=blocks(ResourceLocation.parse("c:ores/"+material));
            if(!raw){resources.addAll(items(ResourceLocation.parse("c:gems/"+material)));resources.addAll(items(ResourceLocation.parse("c:dusts/"+material)));resources=resources.stream().distinct().toList();if(resources.isEmpty()||ores.isEmpty())continue;}
            Resolution result;
            if(resources.size()!=1)result=bad("ambiguous_material_items:"+key);
            else if(raw)result=ok(new ResolvedMineral(autoId(key),key,"#"+tag,Optional.of(BuiltInRegistries.ITEM.getKey(resources.getFirst())),1,1,Optional.empty(),Optional.empty(),false));
            else{
                var signatures=ores.stream().map(o->lootSignature(level,o)).distinct().toList();
                if(signatures.size()!=1||signatures.getFirst().isEmpty())result=bad("ambiguous_ore_loot:"+key);
                else result=ok(new ResolvedMineral(autoId(key),key,"#"+tag,Optional.of(BuiltInRegistries.ITEM.getKey(resources.getFirst())),1,1,Optional.of(BuiltInRegistries.BLOCK.getKey(ores.stream().sorted(Comparator.comparing(b->BuiltInRegistries.BLOCK.getKey(b).toString())).findFirst().orElseThrow())),Optional.empty(),false));
            }
            if(byMaterial.containsKey(key)&&!byMaterial.get(key).equals(result))result=bad("ambiguous_material_binding:"+key);
            byMaterial.put(key,result);for(var b:bind)byBlock.computeIfAbsent(b,k->new ArrayList<>()).add(key);
        }
        byMaterial.forEach((key,r)->byProfile.put(autoId(key),r));
        // Multiple explicit profiles at the highest priority are also a visible conflict.
        var ruleProfiles=new HashMap<ResourceLocation,List<Rule>>();rules.values().forEach(r->ruleProfiles.computeIfAbsent(r.profile().orElseGet(()->autoId(r.material())),k->new ArrayList<>()).add(r));
        ruleProfiles.forEach((id,list)->{list.sort(Comparator.comparingInt(Rule::priority).reversed());var first=list.getFirst();byProfile.put(id,list.size()>1&&list.get(1).priority()==first.priority()?bad("ambiguous_mineral_profile"):first.disabled()?bad("disabled_mineral"):ok(first.resolved()));});
        if(SimulationExtensions.hasMineralResolvers())for(var block:BuiltInRegistries.BLOCK)for(var state:block.getStateDefinition().getPossibleStates())for(var m:SimulationExtensions.minerals(level,state)){
            if(ruleProfiles.containsKey(m.profile()))continue;var result=ok(m);var previous=byProfile.get(m.profile());byProfile.put(m.profile(),previous==null||previous.equals(result)?result:bad("ambiguous_java_profile"));
        }
        materials=Map.copyOf(byMaterial);bindings=Map.copyOf(byBlock);profiles=Map.copyOf(byProfile);indexedLevel=level;indexedRevision=SimulationData.revision();
    }
    /** Require equivalent full tables after normalizing only the Silk Touch ore block identity. */
    private static String lootSignature(ServerLevel level,Block block){
        var id=block.getLootTable().location();var resource=level.getServer().getResourceManager().getResource(ResourceLocation.fromNamespaceAndPath(id.getNamespace(),"loot_table/"+id.getPath()+".json"));if(resource.isEmpty())return "";
        try(var reader=resource.get().openAsReader()){var json=JsonParser.parseReader(reader).getAsJsonObject();json.remove("random_sequence");return json.toString().replace('"'+BuiltInRegistries.BLOCK.getKey(block).toString()+'"',"\"$ORE\"");}catch(Exception e){return "";}
    }
    public static Resolution resolveBinding(ServerLevel level,BlockState state){
        ensure(level);if(state.is(TagKey.create(Registries.BLOCK,ModContent.id("simulation_mineral_blacklist"))))return bad("blacklisted_mineral");
        var matches=rules.values().stream().filter(r->r.binding().matches(state)).sorted(Comparator.comparingInt(Rule::priority).reversed()).toList();
        if(!matches.isEmpty()){var first=matches.getFirst();return matches.size()>1&&matches.get(1).priority()==first.priority()?bad("ambiguous_mineral_rule"):first.disabled()?bad("disabled_mineral"):ok(first.resolved());}
        var javaMatches=SimulationExtensions.minerals(level,state);if(javaMatches.size()>1)return bad("ambiguous_java_mineral");if(javaMatches.size()==1){var m=javaMatches.getFirst();var old=profiles.get(m.profile());if(old!=null&&!old.equals(ok(m)))return bad("ambiguous_java_profile");var copy=new HashMap<>(profiles);copy.put(m.profile(),ok(m));profiles=Map.copyOf(copy);return ok(m);}
        var ids=bindings.getOrDefault(state.getBlock(),List.of()).stream().distinct().toList();if(ids.size()!=1)return bad(ids.isEmpty()?"no_mineral_tags":"ambiguous_material_tags");return materials.get(ids.getFirst());
    }
    public static Resolution resolveProfile(ServerLevel level,ResourceLocation profile){ensure(level);return profiles.getOrDefault(profile,bad("missing_mineral_profile"));}
    public static Map<ResourceLocation,Resolution> audit(ServerLevel level){ensure(level);return profiles;}
    public static Optional<SimulationProfileView> view(ResourceLocation id){
        var resolved=profiles.get(id);if(resolved!=null&&resolved.valid()){var m=resolved.mineral().orElseThrow();return Optional.of(new SimulationProfileView(id,"mineral",m.material().toString(),m.item().orElseGet(()->m.ore().orElseThrow())));}
        if(!id.getNamespace().equals(OverloadSimulation.ID)||!id.getPath().startsWith("auto/mineral/"))return Optional.empty();
        var key=id.getPath().substring("auto/mineral/".length());int slash=key.indexOf('/');if(slash<1)return Optional.empty();String ns=key.substring(0,slash),material=key.substring(slash+1);
        for(var prefix:List.of("raw_materials/","gems/","dusts/")){var values=items(ResourceLocation.fromNamespaceAndPath(ns,prefix+material));if(values.size()==1)return Optional.of(new SimulationProfileView(id,"mineral",ns+":"+material,BuiltInRegistries.ITEM.getKey(values.getFirst())));}return Optional.empty();
    }
}
