package dev.overloadsim.workstation;

import java.util.Arrays;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.world.item.ItemStack;

/** Count-one prototypes and separate bounded counts keep every codec-facing stack legal. */
public final class WorkstationBuffer {
    public static final int SLOTS=9,LIMIT=1_000_000;
    private final ItemStack[] items=new ItemStack[SLOTS];
    private final int[] counts=new int[SLOTS];
    private final Runnable changed;
    public WorkstationBuffer(){this(()->{});}
    public WorkstationBuffer(Runnable changed){this.changed=changed;Arrays.fill(items,ItemStack.EMPTY);}
    private void check(int slot){if(slot<0||slot>=SLOTS)throw new IllegalArgumentException("workstation slot");}
    public ItemStack prototype(int slot){check(slot);return items[slot].copy();}
    public int count(int slot){check(slot);return counts[slot];}
    public long insert(int slot,ItemStack item,long amount,boolean simulate){
        check(slot);if(item.isEmpty()||amount<=0||counts[slot]>0&&!ItemStack.isSameItemSameComponents(items[slot],item))return 0;
        int moved=(int)Math.min(amount,LIMIT-counts[slot]);
        if(!simulate&&moved>0){items[slot]=item.copyWithCount(1);counts[slot]+=moved;changed.run();}return moved;
    }
    public long insert(ItemStack item,long amount,boolean simulate){
        if(simulate)return copy().insert(item,amount,false);
        long left=amount;
        for(int pass=0;pass<2;pass++)for(int slot=0;slot<SLOTS&&left>0;slot++)
            if(pass==0?counts[slot]>0&&ItemStack.isSameItemSameComponents(items[slot],item):counts[slot]==0)left-=insert(slot,item,left,false);
        return amount-left;
    }
    public ItemStack extract(int slot,int amount,boolean simulate){
        check(slot);int moved=Math.min(Math.max(0,amount),Math.min(counts[slot],items[slot].getMaxStackSize()));
        if(moved==0)return ItemStack.EMPTY;var result=items[slot].copyWithCount(moved);if(!simulate)debit(slot,moved);return result;
    }
    public void debit(int slot,long amount){check(slot);if(amount<0||amount>counts[slot])throw new IllegalArgumentException("workstation debit");if(amount==0)return;counts[slot]-=(int)amount;if(counts[slot]==0)items[slot]=ItemStack.EMPTY;changed.run();}
    public WorkstationBuffer copy(){var copy=new WorkstationBuffer();for(int i=0;i<SLOTS;i++){copy.items[i]=items[i].copy();copy.counts[i]=counts[i];}return copy;}
    public void replace(WorkstationBuffer other){for(int i=0;i<SLOTS;i++){items[i]=other.items[i].copy();counts[i]=other.counts[i];}changed.run();}
    public boolean empty(){return Arrays.stream(counts).allMatch(count->count==0);}
    public CompoundTag save(HolderLookup.Provider registries){var tag=new CompoundTag();var entries=new ListTag();for(int i=0;i<SLOTS;i++)if(counts[i]>0){var entry=new CompoundTag();entry.putInt("Slot",i);entry.putInt("Quantity",counts[i]);entry.put("Item",items[i].save(registries));entries.add(entry);}tag.put("Entries",entries);return tag;}
    public void load(CompoundTag tag,HolderLookup.Provider registries){Arrays.fill(items,ItemStack.EMPTY);Arrays.fill(counts,0);for(var value:tag.getList("Entries",Tag.TAG_COMPOUND)){var entry=(CompoundTag)value;int slot=entry.getInt("Slot"),quantity=entry.getInt("Quantity");if(slot<0||slot>=SLOTS||quantity<1||quantity>LIMIT)continue;var item=ItemStack.parseOptional(registries,entry.getCompound("Item"));if(!item.isEmpty()){items[slot]=item.copyWithCount(1);counts[slot]=quantity;}}}
}
