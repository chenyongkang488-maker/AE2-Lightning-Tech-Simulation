package dev.overloadsim.multiblock;
import java.util.*;
import dev.overloadsim.*;
import dev.overloadsim.api.*;
import dev.overloadsim.data.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
public final class SimulationBatchPlanner {
    public static SimulationBatch plan(SimulationControllerBlockEntity host,int start){
        if(!(host.getLevel() instanceof ServerLevel server)||host.structure()==null)return null;
        var s=host.structure();var policy=MultiblockData.policy();var per=policy.cost(1,s);
        long available=per.fe()==0?49:host.availableFe()/per.fe();
        if(per.hv()>0)available=Math.min(available,host.bridge().extract(false,Long.MAX_VALUE,true)/per.hv());
        if(per.ehv()>0)available=Math.min(available,host.bridge().extract(true,Long.MAX_VALUE,true)/per.ehv());
        if(available<1)return null;
        var reserve=host.outputs().copy();var out=new ArrayList<SimulationBatch.Output>();var inputs=new HashMap<Integer,ItemStack>();int nextSlot=start;
        for(int i=0;i<s.capacity()&&inputs.size()<available;i++){
            int slot=(start+i)%s.capacity();var item=host.crystals().getStackInSlot(slot);var data=CrystalDataAccess.read(item);
            if(!item.is(ModContent.PERFECT.get())||data.isEmpty()||SimulationData.profile(server,data.get().profile()).isEmpty())continue;
            var chosen=SimulationResolvers.production(server,data.get());if(chosen.isEmpty())continue;
            var fixed=host.fixedRoll(slot,item,()->chosen.get().roll(server,host.getBlockPos(),data.get(),"multiblock"));
            var outputs=new ArrayList<SimulationBatch.Output>();
            for(var stack:fixed){
                var prototype=stack.copyWithCount(1);long count=stack.getCount();
                if(s.smelting()){
                    var explicit=chosen.get().mineral().flatMap(m->m.smelting()).filter(m->BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(m.input()));
                    var smelt=explicit.map(m->new MultiblockData.Smelt(m.result(),m.count())).orElseGet(()->MultiblockData.smelt(server,stack));if(smelt!=null){prototype=new ItemStack(BuiltInRegistries.ITEM.getOptional(smelt.result()).orElseThrow());count=Math.multiplyExact(count,smelt.count());}
                }
                outputs.add(new SimulationBatch.Output(prototype,Math.multiplyExact(count,policy.multiplier(s))));
            }
            var next=reserve.copy();boolean fits=true;
            for(var value:outputs)if(next.insert(value.prototype(),value.count(),false)!=value.count()){fits=false;break;}
            if(!fits)continue;reserve=next;out.addAll(outputs);inputs.put(slot,item.copy());nextSlot=(slot+1)%s.capacity();
        }
        if(inputs.isEmpty())return null;
        return new SimulationBatch(out,inputs,policy.duration(s),policy.cost(inputs.size(),s),s.overload(),s.smelting(),policy.signature(s),nextSlot);
    }
}
