package dev.overloadsim.data;

import java.util.*;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import dev.overloadsim.ModContent;
import dev.overloadsim.api.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/** Generated, reload-safe crop profiles. Discovery never harvests or changes the world. */
public final class CropSimulationData {
    public static final ResourceLocation PROVIDER=ModContent.id("crop_harvest");
    public static final TagKey<Block> BLACKLIST=TagKey.create(Registries.BLOCK,ModContent.id("simulation_crop_blacklist"));
    public static final TagKey<Block> CROPS=TagKey.create(Registries.BLOCK,ModContent.id("simulation_crops"));
    public static final TagKey<Block> FARMLANDS=TagKey.create(Registries.BLOCK,ModContent.id("simulation_farmlands"));
    private static final TagKey<Block> COMMON_FARMLANDS=TagKey.create(Registries.BLOCK,ResourceLocation.parse("c:farmlands"));
    private static final TagKey<Item> SEEDS=TagKey.create(Registries.ITEM,ResourceLocation.parse("c:seeds"));
    private static Map<Block,ResourceLocation> icons=Map.of();
    private static Set<Block> seeded=Set.of();
    private static Map<Block,Block> adultAliases=Map.of();
    private static Set<Block> ambiguousAdults=Set.of();
    private static long indexedRevision=-1;
    public record Template(ResourceLocation source,BlockState harvest,ResourceLocation icon){
        public ResourceLocation profile(){return autoId(source);}
    }
    public record Resolution(Optional<Template> crop,String error){public boolean valid(){return crop.isPresent()&&error.isEmpty();}}
    private static Resolution bad(String error){return new Resolution(Optional.empty(),error);}
    private static Resolution ok(Template crop){return new Resolution(Optional.of(crop),"");}
    public static ResourceLocation autoId(ResourceLocation source){return ModContent.id("auto/crop/"+source.getNamespace()+"/"+source.getPath());}
    private static void ensureIcons(){
        if(indexedRevision==SimulationData.revision())return;
        var next=new HashMap<Block,ResourceLocation>();var candidates=new HashSet<Block>();
        var items=BuiltInRegistries.ITEM.stream().filter(i->i instanceof BlockItem).sorted(Comparator.comparing(i->BuiltInRegistries.ITEM.getKey(i).toString())).toList();
        for(var item:items){var block=((BlockItem)item).getBlock();next.putIfAbsent(block,BuiltInRegistries.ITEM.getKey(item));if(item.builtInRegistryHolder().is(SEEDS)){candidates.add(block);next.put(block,BuiltInRegistries.ITEM.getKey(item));}}
        var adults=new HashMap<Block,Block>();var ambiguous=new HashSet<Block>();
        for(var block:BuiltInRegistries.BLOCK)if(block instanceof CropBlock crop){var adult=crop.getStateForAge(crop.getMaxAge()).getBlock();if(adult!=block){var previous=adults.putIfAbsent(adult,block);if(previous!=null&&previous!=block)ambiguous.add(adult);}}
        icons=Map.copyOf(next);seeded=Set.copyOf(candidates);adultAliases=Map.copyOf(adults);ambiguousAdults=Set.copyOf(ambiguous);indexedRevision=SimulationData.revision();
    }
    public static Resolution resolve(ServerLevel level,BlockState state){
        ensureIcons();var block=state.getBlock();
        if(state.is(BLACKLIST))return bad("blacklisted_crop");
        if(ambiguousAdults.contains(block))return bad("ambiguous_adult_crop");
        if(adultAliases.containsKey(block)){block=adultAliases.get(block);state=block.defaultBlockState();if(state.is(BLACKLIST))return bad("blacklisted_crop");}
        // Saved profiles identify a block, so adapters must see the same state at binding and production.
        var custom=SimulationExtensions.cropMaturity(level,block.defaultBlockState());
        if(custom.size()>1)return bad("ambiguous_crop_maturity");
        if(custom.isEmpty()&&!(block instanceof BushBlock)&&!state.is(BlockTags.CROPS)&&!state.is(CROPS)&&!seeded.contains(block))return bad("not_crop");
        if(state.hasBlockEntity())return bad("crop_block_entity_requires_explicit_adapter");
        var source=BuiltInRegistries.BLOCK.getKey(block);BlockState harvest=block.defaultBlockState();
        if(!custom.isEmpty())harvest=custom.getFirst();
        else if(block instanceof CropBlock crop)harvest=crop.getStateForAge(crop.getMaxAge());
        else if(block instanceof StemBlock stem){
            var json=StemBlock.CODEC.codec().encodeStart(JsonOps.INSTANCE,stem).getOrThrow().getAsJsonObject();
            var fruit=fruit(json);if(fruit.isEmpty())return bad("missing_crop_fruit");harvest=fruit.get();
        }else if(block instanceof AttachedStemBlock stem){
            var json=AttachedStemBlock.CODEC.codec().encodeStart(JsonOps.INSTANCE,stem).getOrThrow().getAsJsonObject();
            var fruit=fruit(json);if(fruit.isEmpty())return bad("missing_crop_fruit");harvest=fruit.get();source=ResourceLocation.parse(json.get("stem").getAsString());
        }else for(var property:harvest.getProperties())if(property instanceof IntegerProperty growth&&Set.of("age","growth","stage","maturity").contains(property.getName()))harvest=harvest.setValue(growth,Collections.max(growth.getPossibleValues()));
        if(harvest.isAir()||harvest.hasBlockEntity())return bad("invalid_crop_harvest_state");
        var icon=icons.getOrDefault(BuiltInRegistries.BLOCK.get(source),BuiltInRegistries.ITEM.getKey(Items.WHEAT_SEEDS));
        return ok(new Template(source,harvest,icon));
    }
    private static Optional<BlockState> fruit(JsonObject json){return BuiltInRegistries.BLOCK.getOptional(ResourceLocation.parse(json.get("fruit").getAsString())).map(Block::defaultBlockState);}
    public static boolean supports(ServerLevel level,BlockPos ground,BlockState plant){
        var soil=level.getBlockState(ground);
        var decision=soil.canSustainPlant(level,ground,Direction.UP,plant);
        if(decision.isFalse())return false;
        if(decision.isTrue())return true;
        return (soil.getBlock() instanceof FarmBlock||soil.is(FARMLANDS)||soil.is(COMMON_FARMLANDS))&&plant.canSurvive(level,ground.above());
    }
    public static Resolution resolveProfile(ServerLevel level,ResourceLocation id){
        var source=source(id);if(source.isEmpty())return bad("missing_crop_profile");
        var block=BuiltInRegistries.BLOCK.getOptional(source.get());if(block.isEmpty())return bad("missing_crop_block");
        var result=resolve(level,block.get().defaultBlockState());
        return result.valid()&&!result.crop().orElseThrow().profile().equals(id)?bad("inconsistent_crop_profile"):result;
    }
    private static Optional<ResourceLocation> source(ResourceLocation id){
        if(!id.getNamespace().equals("overload_sim")||!id.getPath().startsWith("auto/crop/"))return Optional.empty();
        var path=id.getPath().substring("auto/crop/".length());int split=path.indexOf('/');if(split<1||split==path.length()-1)return Optional.empty();
        return Optional.of(ResourceLocation.fromNamespaceAndPath(path.substring(0,split),path.substring(split+1)));
    }
    public static Optional<SimulationProfileView> view(ResourceLocation id){
        ensureIcons();return source(id).flatMap(source->BuiltInRegistries.BLOCK.getOptional(source).map(block->new SimulationProfileView(id,"crop",source.toString(),icons.getOrDefault(block,BuiltInRegistries.ITEM.getKey(Items.WHEAT_SEEDS)))));
    }
    private CropSimulationData(){}
}
