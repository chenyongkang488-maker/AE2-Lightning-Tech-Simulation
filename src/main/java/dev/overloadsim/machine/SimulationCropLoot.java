package dev.overloadsim.machine;

import java.util.List;
import dev.overloadsim.api.SimulationExtensions;
import dev.overloadsim.data.CropSimulationData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

/** Dispatch through BlockState.getDrops so Java overrides and loot modifiers are respected. */
public final class SimulationCropLoot {
    public static List<ItemStack> roll(SimulationExtensions.OutputContextV2 context){
        var crop=CropSimulationData.resolveProfile(context.level(),context.crystal().profile());
        if(!crop.valid())throw new IllegalStateException(crop.error());var state=crop.crop().orElseThrow().harvest();
        var params=new LootParams.Builder(context.level()).withParameter(LootContextParams.BLOCK_STATE,state).withParameter(LootContextParams.ORIGIN,context.position().getCenter()).withParameter(LootContextParams.TOOL,ItemStack.EMPTY).withLuck(0);
        return state.getDrops(params);
    }
    private SimulationCropLoot(){}
}
