package dev.overloadsim.data;

import java.util.*;
import dev.overloadsim.ModContent;
import dev.overloadsim.api.*;
import dev.overloadsim.machine.*;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

public final class SimulationResolvers {
    public record Production(ResourceLocation id,SimulationRecipe.Production config,Optional<ResolvedMineral> mineral){
        public List<ItemStack> roll(ServerLevel level,BlockPos pos,CrystalData data,String machine){
            return mineral.isPresent()?MobLoot.normalize(SimulationMineralLoot.roll(level,pos,mineral.get(),level.random)):MobLoot.roll(level,pos,data,config,machine);
        }
    }
    public static Optional<Production> production(ServerLevel level,CrystalData crystal){
        MineralSimulationData.ensure(level);
        if(crystal.entityType().isPresent()&&!MobSimulationData.resolve(crystal.entityType().get()).enabled())return Optional.empty();
        var recipes=SimulationData.recipes(level,SimulationRecipe.Kind.PRODUCTION).stream().filter(r->r.value().data().profile().equals(crystal.profile())).toList();
        if(!recipes.isEmpty())return SimulationData.select(recipes).map(r->new Production(r.id(),r.value().data().production(),Optional.empty()));
        var mineral=MineralSimulationData.resolveProfile(level,crystal.profile());
        if(mineral.valid())return Optional.of(new Production(crystal.profile(),SimulationRecipe.Production.DEFAULT,mineral.mineral()));
        var crop=CropSimulationData.resolveProfile(level,crystal.profile());
        if(!crop.valid())return Optional.empty();
        var config=new SimulationRecipe.Production(200,1000,1,List.of(),Optional.of(CropSimulationData.PROVIDER),false,Optional.empty());
        return Optional.of(new Production(crystal.profile(),config,Optional.empty()));
    }
    private SimulationResolvers(){}
}
