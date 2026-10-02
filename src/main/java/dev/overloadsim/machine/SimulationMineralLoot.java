package dev.overloadsim.machine;

import java.util.*;
import dev.overloadsim.api.ResolvedMineral;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.*;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;

public final class SimulationMineralLoot {
    public static List<ItemStack> roll(ServerLevel level,BlockPos pos,ResolvedMineral mineral,RandomSource random){
        if(mineral.ore().isEmpty())return List.of(new ItemStack(BuiltInRegistries.ITEM.getOptional(mineral.item().orElseThrow()).orElseThrow(),mineral.min()+random.nextInt(mineral.max()-mineral.min()+1)));
        var ore=BuiltInRegistries.BLOCK.getOptional(mineral.ore().get()).orElseThrow();var tool=new ItemStack(Items.NETHERITE_PICKAXE);
        var params=new LootParams.Builder(level).withParameter(LootContextParams.BLOCK_STATE,ore.defaultBlockState()).withParameter(LootContextParams.ORIGIN,pos.getCenter()).withParameter(LootContextParams.TOOL,tool).withLuck(0).create(LootContextParamSets.BLOCK);
        return level.getServer().reloadableRegistries().getLootTable(ore.getLootTable()).getRandomItems(params,random);
    }
    private SimulationMineralLoot(){}
}
