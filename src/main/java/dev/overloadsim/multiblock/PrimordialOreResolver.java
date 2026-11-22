package dev.overloadsim.multiblock;

import java.util.*;
import dev.overloadsim.ModContent;
import dev.overloadsim.api.CrystalData;
import dev.overloadsim.data.MineralSimulationData;
import dev.overloadsim.data.SimulationData;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.*;

/** Ore identities, independent of normal loot tables and explicit production recipes. Server thread only. */
public final class PrimordialOreResolver {
    public record Resolution(Optional<ResourceLocation> block,String reason){
        public boolean converted(){return block.isPresent();}
        public boolean fallback(){return !converted()&&!reason.equals("not_mineral");}
        public ItemStack output(){return block.map(id->new ItemStack(BuiltInRegistries.BLOCK.get(id))).orElse(ItemStack.EMPTY);}
    }
    private static final Map<ResourceLocation,ResourceLocation> VANILLA=defaults();
    private static final Map<ResourceLocation, ResourceLocation> OPTIONAL_STONE_ORES = Map.of(
            MineralSimulationData.autoId(ResourceLocation.parse("c:fluorite")),
            ResourceLocation.parse("mekanism:fluorite_ore"),
            MineralSimulationData.autoId(ResourceLocation.parse("c:sulfur")),
            ResourceLocation.parse("alltheores:sulfur_ore"));
    private static final Map<ResourceLocation,Resolution> CACHE=new HashMap<>();
    private static String generation="";
    private static ServerLevel cacheLevel;
    private PrimordialOreResolver(){}
    private static Map<ResourceLocation,ResourceLocation> defaults(){
        var result=new HashMap<ResourceLocation,ResourceLocation>();
        for(var material:List.of("iron","copper","gold","coal","diamond","emerald","redstone","lapis"))result.put(ModContent.id(material),ResourceLocation.withDefaultNamespace(material+"_ore"));
        result.put(ModContent.id("quartz"),ResourceLocation.withDefaultNamespace("nether_quartz_ore"));
        result.put(ModContent.id("netherite"),ResourceLocation.withDefaultNamespace("ancient_debris"));return Map.copyOf(result);
    }
    public static void invalidate(){CACHE.clear();generation="";cacheLevel=null;}
    public static Resolution resolve(ServerLevel level,CrystalData crystal){
        if(crystal.entityType().isPresent()||SimulationData.profile(level,crystal.profile()).filter(p->p.kind().equals("mineral")).isEmpty())return new Resolution(Optional.empty(),"not_mineral");
        String current=MultiblockData.generation();if(cacheLevel!=level||!generation.equals(current)){CACHE.clear();generation=current;cacheLevel=level;}
        return CACHE.computeIfAbsent(crystal.profile(),profile->resolveMineral(level,profile));
    }
    private static Resolution resolveMineral(ServerLevel level,ResourceLocation profile){
        var explicit=MultiblockData.primordialOverride(profile);if(explicit.isPresent())return target(explicit.get(),"invalid_mapping");
        var vanilla=VANILLA.get(profile);if(vanilla!=null)return target(vanilla,"invalid_mapping");
        var mineral=MineralSimulationData.resolveProfile(level,profile).mineral();if(mineral.isEmpty())return missing("missing_ore");
        var tag=TagKey.create(Registries.BLOCK,ResourceLocation.parse("c:ores/"+mineral.get().material().getPath()));
        var preferred = OPTIONAL_STONE_ORES.get(profile);
        if (preferred != null && BuiltInRegistries.BLOCK.getOptional(preferred)
                .filter(block -> block.defaultBlockState().is(tag)).isPresent()) {
            var result = target(preferred, "invalid_mapping");
            if (result.converted()) return result;
        }
        var candidates=BuiltInRegistries.BLOCK.getTag(tag).stream().flatMap(set->set.stream())
                .map(holder->holder.value()).filter(block->!block.defaultBlockState().isAir()&&block.asItem()!=Items.AIR)
                .map(BuiltInRegistries.BLOCK::getKey).distinct().toList();
        return candidates.size()==1?target(candidates.getFirst(),"invalid_mapping"):missing(candidates.isEmpty()?"missing_ore":"ambiguous_ore");
    }
    private static Resolution target(ResourceLocation id,String error){
        var block=BuiltInRegistries.BLOCK.getOptional(id);
        return block.isPresent()&&!block.get().defaultBlockState().isAir()&&block.get().asItem()!=Items.AIR?new Resolution(Optional.of(id),""):missing(error);
    }
    private static Resolution missing(String reason){return new Resolution(Optional.empty(),reason);}
}
