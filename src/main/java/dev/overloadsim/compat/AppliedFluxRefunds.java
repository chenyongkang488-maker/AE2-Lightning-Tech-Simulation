package dev.overloadsim.compat;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

/** Refunds discovered after a machine's dropped item has already been serialized. */
public final class AppliedFluxRefunds extends SavedData {
    private static final String SAVE_ID = "overload_sim_network_fe_refunds";
    private static final String ITEM_KEY = "overload_sim:network_fe_refund";
    private static final Factory<AppliedFluxRefunds> FACTORY = new Factory<>(AppliedFluxRefunds::new, AppliedFluxRefunds::load);
    private final Map<UUID, Long> pending = new HashMap<>();

    public record ItemRefund(UUID identity, long credit) {}

    private static AppliedFluxRefunds get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(FACTORY, SAVE_ID);
    }

    public static void deposit(Level level, UUID identity, long amount) {
        if (amount <= 0) return;
        if (!(level instanceof ServerLevel server)) throw new IllegalStateException("FE refund escrow requires a server level");
        var refunds = get(server);
        refunds.pending.merge(identity, amount, Math::addExact);
        refunds.setDirty();
    }

    /** Removing the entry before returning prevents another restored copy from receiving the same FE. */
    public static long claim(Level level, UUID identity) {
        if (!(level instanceof ServerLevel server)) return 0;
        var refunds = get(server);
        var amount = refunds.pending.remove(identity);
        if (amount == null) return 0;
        refunds.setDirty();
        return amount;
    }

    public static void writeItem(DataComponentMap.Builder builder, UUID identity, long credit) {
        var custom = builder.build().getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        var refund = new CompoundTag(); refund.putUUID("Identity", identity); refund.putLong("Credit", credit);
        custom.put(ITEM_KEY, refund);
        builder.set(DataComponents.CUSTOM_DATA, CustomData.of(custom));
    }

    public static ItemRefund readItem(DataComponentMap components) {
        var custom = components.get(DataComponents.CUSTOM_DATA);
        if (custom == null) return null;
        var refund = custom.copyTag().getCompound(ITEM_KEY);
        return refund.hasUUID("Identity") ? new ItemRefund(refund.getUUID("Identity"), Math.max(0, refund.getLong("Credit"))) : null;
    }

    private static AppliedFluxRefunds load(CompoundTag tag, HolderLookup.Provider registries) {
        var refunds = new AppliedFluxRefunds();
        for (var value : tag.getList("Pending", Tag.TAG_COMPOUND)) {
            var entry = (CompoundTag) value;
            if (entry.hasUUID("Identity") && entry.getLong("Credit") > 0) refunds.pending.put(entry.getUUID("Identity"), entry.getLong("Credit"));
        }
        return refunds;
    }

    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        var entries = new ListTag();
        pending.forEach((identity, amount) -> {
            var entry = new CompoundTag(); entry.putUUID("Identity", identity); entry.putLong("Credit", amount); entries.add(entry);
        });
        tag.put("Pending", entries);
        return tag;
    }
}
