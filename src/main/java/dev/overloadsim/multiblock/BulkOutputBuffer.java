package dev.overloadsim.multiblock;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.world.item.ItemStack;

/** Legal count-one prototypes with quantities independent of Minecraft stack-size codecs. */
public final class BulkOutputBuffer {
    public static final int SLOTS=128, LIMIT=1024;
    private final ItemStack[] items=new ItemStack[SLOTS];
    private final int[] counts=new int[SLOTS];
    public BulkOutputBuffer(){java.util.Arrays.fill(items,ItemStack.EMPTY);}
    private void check(int slot){if(slot<0||slot>=SLOTS)throw new IllegalArgumentException("bulk slot");}
    public ItemStack prototype(int slot){check(slot);return items[slot].copy();}
    public int count(int slot){check(slot);return counts[slot];}
    public long insert(ItemStack item,long amount,boolean simulate){
        if(item.isEmpty()||amount<=0)return 0;
        long left=amount;
        for(int pass=0;pass<2;pass++)for(int i=0;i<SLOTS&&left>0;i++){
            if(pass==0?counts[i]>0&&ItemStack.isSameItemSameComponents(items[i],item):counts[i]==0){
                int moved=(int)Math.min(left,LIMIT-counts[i]);left-=moved;
                if(!simulate&&moved>0){items[i]=item.copyWithCount(1);counts[i]+=moved;}
            }
        }
        return amount-left;
    }
    public ItemStack extract(int slot,int amount,boolean simulate){
        check(slot);int moved=Math.min(Math.max(0,amount),Math.min(counts[slot],items[slot].getMaxStackSize()));
        if(moved==0)return ItemStack.EMPTY;
        ItemStack result=items[slot].copyWithCount(moved);
        if(!simulate){counts[slot]-=moved;if(counts[slot]==0)items[slot]=ItemStack.EMPTY;}
        return result;
    }
    public void debit(int slot,long amount){
        check(slot);if(amount<0||amount>counts[slot])throw new IllegalArgumentException("bulk debit");
        counts[slot]-=(int)amount;if(counts[slot]==0)items[slot]=ItemStack.EMPTY;
    }
    public BulkOutputBuffer copy(){var b=new BulkOutputBuffer();for(int i=0;i<SLOTS;i++){b.items[i]=items[i].copy();b.counts[i]=counts[i];}return b;}
    public CompoundTag save(HolderLookup.Provider registries){
        var tag=new CompoundTag();var entries=new ListTag();
        for(int i=0;i<SLOTS;i++)if(counts[i]>0){var entry=new CompoundTag();entry.putInt("Slot",i);entry.putInt("Quantity",counts[i]);entry.put("Item",items[i].save(registries));entries.add(entry);}
        tag.put("Entries",entries);return tag;
    }
    public void load(CompoundTag tag,HolderLookup.Provider registries){
        java.util.Arrays.fill(items,ItemStack.EMPTY);java.util.Arrays.fill(counts,0);
        for(var value:tag.getList("Entries",Tag.TAG_COMPOUND)){
            var entry=(CompoundTag)value;int slot=entry.getInt("Slot"),qty=entry.getInt("Quantity");
            if(slot<0||slot>=SLOTS||qty<1||qty>LIMIT)continue;
            var item=ItemStack.parseOptional(registries,entry.getCompound("Item"));
            if(!item.isEmpty()){items[slot]=item.copyWithCount(1);counts[slot]=qty;}
        }
    }
}
