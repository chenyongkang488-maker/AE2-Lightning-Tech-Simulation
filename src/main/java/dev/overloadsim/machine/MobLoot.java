package dev.overloadsim.machine;
import java.util.*;
import dev.overloadsim.api.*;
import dev.overloadsim.data.SimulationRecipe;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.minecraft.world.phys.Vec3;

public final class MobLoot {
    private MobLoot(){}
    public static List<ItemStack> roll(ServerLevel level,BlockPos pos,CrystalData data,SimulationRecipe.Production production){
        var result=new ArrayList<ItemStack>();
        if(production.provider().isPresent()){
            result.addAll(provider(level,pos,data,production.provider().get(),level.random,"simulation",data.profile().toString()));
        }else if(production.entityLoot()){
            var resolution=dev.overloadsim.data.MobSimulationData.resolve(data.entityType().orElseThrow());
            if(!resolution.enabled())throw new IllegalStateException(resolution.error());
            result.addAll(SimulationEntityLoot.roll(level,pos,data,resolution.rule().orElseThrow()));
        }else{
            for(var output:production.outputs())if(level.random.nextDouble()<output.chance())result.add(new ItemStack(BuiltInRegistries.ITEM.getOptional(output.item()).orElseThrow(),output.count()));
        }
        return normalize(result);
    }
    public static List<ItemStack> provider(ServerLevel level,BlockPos pos,CrystalData data,net.minecraft.resources.ResourceLocation id,net.minecraft.util.RandomSource random,String machine,String rule){
        var v2=SimulationExtensions.outputV2(id);if(v2!=null)return v2.generate(new SimulationExtensions.OutputContextV2(level,pos,data,random,machine,rule));
        var legacy=SimulationExtensions.output(id);if(legacy==null)throw new IllegalStateException("Unknown output provider "+id);return legacy.generate(new SimulationExtensions.OutputContext(level,pos,data));
    }
    public static List<ItemStack> normalize(List<ItemStack> result){
        if(result.size()>64)throw new IllegalStateException("Output provider exceeded 64 stacks per operation");
        if(result.stream().anyMatch(s->s.getCount()>4096))throw new IllegalStateException("Output stack exceeded 4096 items");
        var normalized=new ArrayList<ItemStack>();long total=0;
        for(var stack:result){
            if(stack.isEmpty())continue;total+=stack.getCount();
            if(total>4096)throw new IllegalStateException("Output exceeded 4096 items per operation");
            int limit=Math.min(64,stack.getMaxStackSize());
            for(int left=stack.getCount();left>0;){int count=Math.min(left,limit);normalized.add(stack.copyWithCount(count));left-=count;if(normalized.size()>256)throw new IllegalStateException("Output exceeded 256 normalized stacks per operation");}
        }
        return List.copyOf(normalized);
    }
}
