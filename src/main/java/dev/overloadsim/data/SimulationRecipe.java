package dev.overloadsim.data;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overloadsim.ModContent;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public record SimulationRecipe(Kind kind, Data data) implements Recipe<RecipeInput> {
    private static final Codec<Long> COST = Codec.LONG.validate(value -> value >= 0 && value <= 1_000_000_000L ? DataResult.success(value) : DataResult.error(() -> "Cost must be between 0 and 1000000000"));
    public enum Kind { BINDING, MOB_BINDING, CULTIVATION, PRODUCTION }
    public record Selector(String id, Map<String,String> properties) {
        public static final Codec<Selector> CODEC=RecordCodecBuilder.create(i->i.group(Codec.STRING.fieldOf("id").forGetter(Selector::id),Codec.unboundedMap(Codec.STRING,Codec.STRING).optionalFieldOf("properties",Map.of()).forGetter(Selector::properties)).apply(i,Selector::new));
        public boolean matches(BlockState state) {
            boolean match=id.startsWith("#") ? state.is(TagKey.create(Registries.BLOCK,ResourceLocation.parse(id.substring(1)))) : BuiltInRegistries.BLOCK.getKey(state.getBlock()).equals(ResourceLocation.parse(id));
            if(!match)return false;
            return properties.entrySet().stream().allMatch(e->{var p=state.getBlock().getStateDefinition().getProperty(e.getKey());return p!=null && state.getValue(p).toString().equals(e.getValue());});
        }
    }
    public record WorldRule(String mode, Optional<Selector> material, Optional<Selector> soil, Optional<ResourceLocation> condition) {
        public static final WorldRule DEFAULT=new WorldRule("mineral",Optional.empty(),Optional.empty(),Optional.empty());
        public static final Codec<WorldRule> CODEC=RecordCodecBuilder.create(i->i.group(Codec.STRING.optionalFieldOf("mode","mineral").forGetter(WorldRule::mode),Selector.CODEC.optionalFieldOf("material").forGetter(WorldRule::material),Selector.CODEC.optionalFieldOf("soil").forGetter(WorldRule::soil),ResourceLocation.CODEC.optionalFieldOf("condition").forGetter(WorldRule::condition)).apply(i,WorldRule::new));
    }
    public record MobRule(String entity, double radius, double probability, boolean disabled, Optional<ResourceLocation> condition) {
        public static final MobRule DEFAULT=new MobRule("*",5,.1,false,Optional.empty());
        public static final Codec<MobRule> CODEC=RecordCodecBuilder.create(i->i.group(Codec.STRING.optionalFieldOf("entity","*").forGetter(MobRule::entity),Codec.doubleRange(0,32).optionalFieldOf("radius",5d).forGetter(MobRule::radius),Codec.doubleRange(0,1).optionalFieldOf("probability",.1).forGetter(MobRule::probability),Codec.BOOL.optionalFieldOf("disabled",false).forGetter(MobRule::disabled),ResourceLocation.CODEC.optionalFieldOf("condition").forGetter(MobRule::condition)).apply(i,MobRule::new));
    }
    public record Cultivation(int required, int increment) {
        public static final Cultivation DEFAULT=new Cultivation(10,1);
        public static final Codec<Cultivation> CODEC=RecordCodecBuilder.create(i->i.group(Codec.intRange(1,1_000_000).optionalFieldOf("required",10).forGetter(Cultivation::required),Codec.intRange(1,1_000_000).optionalFieldOf("increment",1).forGetter(Cultivation::increment)).apply(i,Cultivation::new));
    }
    public record Output(ResourceLocation item, int count, double chance) {
        public static final Codec<Output> CODEC=RecordCodecBuilder.create(i->i.group(ResourceLocation.CODEC.fieldOf("item").forGetter(Output::item),Codec.intRange(1,4096).optionalFieldOf("count",1).forGetter(Output::count),Codec.doubleRange(0,1).optionalFieldOf("chance",1d).forGetter(Output::chance)).apply(i,Output::new));
    }
    public record Production(int ticks,long fe,long lightning,List<Output> outputs,Optional<ResourceLocation> provider,boolean entityLoot,Optional<Output> input) {
        public static final Production DEFAULT=new Production(200,1000,1,List.of(),Optional.empty(),false,Optional.empty());
        public static final Codec<Production> CODEC=RecordCodecBuilder.create(i->i.group(Codec.intRange(1,1_000_000).optionalFieldOf("ticks",200).forGetter(Production::ticks),COST.optionalFieldOf("fe",1000L).forGetter(Production::fe),COST.optionalFieldOf("lightning",1L).forGetter(Production::lightning),Output.CODEC.listOf(0,64).optionalFieldOf("outputs",List.of()).forGetter(Production::outputs),ResourceLocation.CODEC.optionalFieldOf("provider").forGetter(Production::provider),Codec.BOOL.optionalFieldOf("entity_loot",false).forGetter(Production::entityLoot),Output.CODEC.optionalFieldOf("input").forGetter(Production::input)).apply(i,Production::new));
    }
    public record Data(ResourceLocation profile,int priority,boolean allowArtificial,WorldRule world,MobRule mob,Cultivation cultivation,Production production) {
        public static final MapCodec<Data> CODEC=RecordCodecBuilder.mapCodec(i->i.group(ResourceLocation.CODEC.fieldOf("profile").forGetter(Data::profile),Codec.INT.optionalFieldOf("priority",0).forGetter(Data::priority),Codec.BOOL.optionalFieldOf("allow_artificial",true).forGetter(Data::allowArtificial),WorldRule.CODEC.optionalFieldOf("world",WorldRule.DEFAULT).forGetter(Data::world),MobRule.CODEC.optionalFieldOf("mob",MobRule.DEFAULT).forGetter(Data::mob),Cultivation.CODEC.optionalFieldOf("cultivation",Cultivation.DEFAULT).forGetter(Data::cultivation),Production.CODEC.optionalFieldOf("production",Production.DEFAULT).forGetter(Data::production)).apply(i,Data::new));
    }
    public static class Serializer implements RecipeSerializer<SimulationRecipe> {
        private final MapCodec<SimulationRecipe> codec;
        private final StreamCodec<RegistryFriendlyByteBuf,SimulationRecipe> stream;
        public Serializer(Kind kind) { codec=Data.CODEC.xmap(d->new SimulationRecipe(kind,d),SimulationRecipe::data);stream=ByteBufCodecs.fromCodecWithRegistries(codec.codec()); }
        public MapCodec<SimulationRecipe> codec(){return codec;}
        public StreamCodec<RegistryFriendlyByteBuf,SimulationRecipe> streamCodec(){return stream;}
    }
    @Override public boolean matches(RecipeInput input,Level level){return false;}
    @Override public ItemStack assemble(RecipeInput input,HolderLookup.Provider p){return ItemStack.EMPTY;}
    @Override public boolean canCraftInDimensions(int w,int h){return false;}
    @Override public ItemStack getResultItem(HolderLookup.Provider p){return ItemStack.EMPTY;}
    @Override public boolean isSpecial(){return true;}
    @Override public RecipeSerializer<?> getSerializer(){return ModContent.SERIALIZERS.get(kind).get();}
    @Override public RecipeType<?> getType(){return ModContent.RECIPE_TYPES.get(kind).get();}
}
