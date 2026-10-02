package dev.overloadsim.data;

import java.util.*;
import com.google.gson.*;
import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.*;
import net.minecraft.tags.TagKey;
import net.minecraft.util.profiling.ProfilerFiller;

/** Reloadable entity rules. Equal highest priorities are errors, never load-order tie breaks. */
public final class MobSimulationData extends SimpleJsonResourceReloadListener {
    public record Output(ResourceLocation item,int min,int max,double chance){
        public static final Codec<Output> CODEC=RecordCodecBuilder.<Output>create(i->i.group(ResourceLocation.CODEC.fieldOf("item").forGetter(Output::item),Codec.intRange(1,4096).optionalFieldOf("min",1).forGetter(Output::min),Codec.intRange(1,4096).optionalFieldOf("max",1).forGetter(Output::max),Codec.doubleRange(0,1).optionalFieldOf("chance",1d).forGetter(Output::chance)).apply(i,Output::new)).validate(o->o.max>=o.min?DataResult.success(o):DataResult.error(()->"max is less than min"));
    }
    public record Rule(String entity,int priority,boolean disabled,List<Output> outputs,Optional<ResourceLocation> lootTable,Optional<ResourceLocation> provider,String context,Optional<ResourceLocation> template){
        public static final Rule DEFAULT=new Rule("*",Integer.MIN_VALUE,false,List.of(),Optional.empty(),Optional.empty(),"player",Optional.empty());
        public static final Codec<Rule> CODEC=RecordCodecBuilder.<Rule>create(i->i.group(Codec.STRING.optionalFieldOf("entity","*").forGetter(Rule::entity),Codec.INT.optionalFieldOf("priority",0).forGetter(Rule::priority),Codec.BOOL.optionalFieldOf("disabled",false).forGetter(Rule::disabled),Output.CODEC.listOf(0,64).optionalFieldOf("outputs",List.of()).forGetter(Rule::outputs),ResourceLocation.CODEC.optionalFieldOf("loot_table").forGetter(Rule::lootTable),ResourceLocation.CODEC.optionalFieldOf("provider").forGetter(Rule::provider),Codec.STRING.optionalFieldOf("context","player").forGetter(Rule::context),ResourceLocation.CODEC.optionalFieldOf("template").forGetter(Rule::template)).apply(i,Rule::new)).validate(r->{
            if(!Set.of("player","environment").contains(r.context))return DataResult.error(()->"context must be player or environment");
            int modes=(r.outputs.isEmpty()?0:1)+(r.lootTable.isPresent()?1:0)+(r.provider.isPresent()?1:0);if(modes>1)return DataResult.error(()->"choose outputs, loot_table or provider");
            if(!r.entity.equals("*"))ResourceLocation.parse(r.entity.replaceFirst("^#",""));return DataResult.success(r);
        });
        public boolean matches(ResourceLocation id){return entity.equals("*")||(entity.startsWith("#")?BuiltInRegistries.ENTITY_TYPE.getOptional(id).map(t->t.is(TagKey.create(Registries.ENTITY_TYPE,ResourceLocation.parse(entity.substring(1))))).orElse(false):id.equals(ResourceLocation.parse(entity)));}
    }
    public record Resolution(Optional<Rule> rule,Optional<ResourceLocation> id,String error){public boolean enabled(){return error.isEmpty()&&rule.isPresent()&&!rule.get().disabled();}}
    private static volatile Map<ResourceLocation,Rule> rules=Map.of();
    public MobSimulationData(){super(new Gson(),"simulation_mob");}
    @Override protected void apply(Map<ResourceLocation,JsonElement> json,ResourceManager manager,ProfilerFiller profiler){
        var next=new HashMap<ResourceLocation,Rule>();json.forEach((id,j)->next.put(id,Rule.CODEC.parse(JsonOps.INSTANCE,j).getOrThrow()));rules=Map.copyOf(next);SimulationData.invalidate();
    }
    public static Map<ResourceLocation,Rule> rules(){return rules;}
    public static Resolution resolve(ResourceLocation entity){return resolve(entity,rules);}
    public static Resolution resolve(ResourceLocation entity,Map<ResourceLocation,Rule> source){
        var type=BuiltInRegistries.ENTITY_TYPE.getOptional(entity);if(type.isEmpty())return new Resolution(Optional.empty(),Optional.empty(),"missing_entity");
        if(type.get().is(TagKey.create(Registries.ENTITY_TYPE,ResourceLocation.fromNamespaceAndPath("overload_sim","simulation_mob_blacklist"))))return new Resolution(Optional.empty(),Optional.empty(),"blacklisted_entity");
        var matches=source.entrySet().stream().filter(e->e.getValue().matches(entity)).sorted(Comparator.<Map.Entry<ResourceLocation,Rule>>comparingInt(e->e.getValue().priority()).reversed().thenComparing(e->e.getKey().toString())).toList();
        if(matches.isEmpty())return new Resolution(Optional.of(Rule.DEFAULT),Optional.empty(),"");
        if(matches.size()>1&&matches.get(0).getValue().priority()==matches.get(1).getValue().priority())return new Resolution(Optional.empty(),Optional.empty(),"ambiguous_mob_rule");
        var first=matches.getFirst();return new Resolution(Optional.of(first.getValue()),Optional.of(first.getKey()),first.getValue().disabled()?"disabled_entity":"");
    }
}
