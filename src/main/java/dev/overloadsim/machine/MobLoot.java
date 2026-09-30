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
            var provider=SimulationExtensions.output(production.provider().get());if(provider==null)throw new IllegalStateException("Unknown output provider "+production.provider().get());
            result.addAll(provider.generate(new SimulationExtensions.OutputContext(level,pos,data)));
        }else if(production.entityLoot()){
            var id=data.entityType().orElseThrow();var type=BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElseThrow();
            var entity=type.create(level);if(!(entity instanceof Mob mob))throw new IllegalStateException("Not a Mob "+id);
            mob.setPos(Vec3.atCenterOf(pos));
            // Do not finalizeSpawn, add to the world, copy equipment, or supply a player/looting context.
            var params=new LootParams.Builder(level).withParameter(LootContextParams.THIS_ENTITY,mob).withParameter(LootContextParams.ORIGIN,Vec3.atCenterOf(pos)).withParameter(LootContextParams.DAMAGE_SOURCE,level.damageSources().generic()).withLuck(0).create(LootContextParamSets.ENTITY);
            result.addAll(level.getServer().reloadableRegistries().getLootTable(type.getDefaultLootTable()).getRandomItems(params));
        }else{
            for(var output:production.outputs())if(level.random.nextDouble()<output.chance())result.add(new ItemStack(BuiltInRegistries.ITEM.getOptional(output.item()).orElseThrow(),output.count()));
        }
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
