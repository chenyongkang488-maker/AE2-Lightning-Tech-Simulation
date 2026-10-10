package dev.overloadsim.output.tool;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/** Addon state shares CUSTOM_DATA without removing upstream or other addon entries. */
public record OutputSourceSelection(ResourceKey<Level> dimension, BlockPos pos, int sourceSlot) {
    public static final String TAG = "overload_sim:output_source";

    public OutputSourceSelection {
        if (dimension == null || pos == null || !OutputToolRules.validSourceSlot(sourceSlot))
            throw new IllegalArgumentException("Invalid output source");
        pos = pos.immutable();
    }

    @Nullable public static OutputSourceSelection read(ItemStack stack) {
        var root = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!root.contains(TAG, Tag.TAG_COMPOUND)) return null;
        var data = root.getCompound(TAG);
        if (!data.contains("dimension", Tag.TAG_STRING) || !data.contains("pos", Tag.TAG_LONG)
                || !data.contains("slot", Tag.TAG_INT) || !OutputToolRules.validSourceSlot(data.getInt("slot"))) return null;
        var id = ResourceLocation.tryParse(data.getString("dimension"));
        return id == null ? null : new OutputSourceSelection(ResourceKey.create(Registries.DIMENSION, id),
                BlockPos.of(data.getLong("pos")), data.getInt("slot"));
    }

    public void write(ItemStack stack) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, root -> {
            var data = new CompoundTag();
            data.putString("dimension", dimension.location().toString());
            data.putLong("pos", pos.asLong());
            data.putInt("slot", sourceSlot);
            root.put(TAG, data);
        });
    }

    public static boolean clear(ItemStack stack) {
        var root = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!root.contains(TAG)) return false;
        root.remove(TAG);
        if (root.isEmpty()) stack.remove(DataComponents.CUSTOM_DATA);
        else stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
        return true;
    }
}
