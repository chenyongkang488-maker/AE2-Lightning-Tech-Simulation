package dev.overloadsim.multiblock;
import java.util.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.world.item.ItemStack;
/** Persisted fixed outcomes; all output stack prototypes have count one. */
public final class SimulationBatch {
    public record Output(ItemStack prototype,long count){
        public Output{if(prototype.isEmpty()||count<1||count>1_000_000_000L)throw new IllegalArgumentException("output");prototype=prototype.copyWithCount(1);}
    }
    public final List<Output> outputs;public final Map<Integer,ItemStack> inputs;
    public final int duration;public int remaining;
    public final MultiblockRules.Costs cost;public final boolean overload,smelting;public boolean paid;
    public final String policy;
    public SimulationBatch(List<Output> outputs,Map<Integer,ItemStack> inputs,int duration,MultiblockRules.Costs cost,boolean overload,boolean smelting,String policy){
        this.outputs=List.copyOf(outputs);this.inputs=Map.copyOf(inputs);this.duration=duration;remaining=duration;this.cost=cost;this.overload=overload;this.smelting=smelting;this.policy=policy;
    }
    public boolean fits(BulkOutputBuffer buffer){var copy=buffer.copy();for(var output:outputs)if(copy.insert(output.prototype,output.count,false)!=output.count)return false;return true;}
    public boolean flush(BulkOutputBuffer buffer){if(!fits(buffer))return false;for(var output:outputs)buffer.insert(output.prototype,output.count,false);return true;}
    public CompoundTag save(HolderLookup.Provider r){
        var tag=new CompoundTag();var out=new ListTag();for(var o:outputs){var t=new CompoundTag();t.put("Item",o.prototype.save(r));t.putLong("Count",o.count);out.add(t);}tag.put("Outputs",out);
        var in=new ListTag();inputs.forEach((slot,item)->{var t=new CompoundTag();t.putInt("Slot",slot);t.put("Item",item.save(r));in.add(t);});tag.put("Inputs",in);
        tag.putInt("Duration",duration);tag.putInt("Remaining",remaining);tag.putLong("Fe",cost.fe());tag.putLong("Hv",cost.hv());tag.putLong("Ehv",cost.ehv());tag.putBoolean("Paid",paid);tag.putBoolean("Overload",overload);tag.putBoolean("Smelting",smelting);tag.putString("Policy",policy);return tag;
    }
    public static SimulationBatch load(CompoundTag tag,HolderLookup.Provider r){
        var outputs=new ArrayList<Output>();for(var value:tag.getList("Outputs",Tag.TAG_COMPOUND)){var t=(CompoundTag)value;outputs.add(new Output(ItemStack.parseOptional(r,t.getCompound("Item")),t.getLong("Count")));}
        var inputs=new HashMap<Integer,ItemStack>();for(var value:tag.getList("Inputs",Tag.TAG_COMPOUND)){var t=(CompoundTag)value;int slot=t.getInt("Slot");if(slot<0||slot>=49)throw new IllegalArgumentException("batch input");inputs.put(slot,ItemStack.parseOptional(r,t.getCompound("Item")));}
        var job=new SimulationBatch(outputs,inputs,Math.clamp(tag.getInt("Duration"),1,1_000_000),new MultiblockRules.Costs(Math.max(0,tag.getLong("Fe")),Math.max(0,tag.getLong("Hv")),Math.max(0,tag.getLong("Ehv"))),tag.getBoolean("Overload"),tag.getBoolean("Smelting"),tag.getString("Policy"));
        job.paid=tag.getBoolean("Paid");job.remaining=Math.clamp(tag.getInt("Remaining"),0,job.duration);return job;
    }
}
