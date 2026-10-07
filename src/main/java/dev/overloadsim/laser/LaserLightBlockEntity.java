package dev.overloadsim.laser;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Shared short leases survive saves and expire even if their owners' chunks stay unloaded. */
public final class LaserLightBlockEntity extends BlockEntity {
    private static final int MAX_OWNERS = 32;
    static final int LEASE_TICKS = 60;
    private record Lease(BlockPos source, long until) {}
    private final Map<UUID, Lease> owners = new LinkedHashMap<>();

    public LaserLightBlockEntity(BlockPos pos, BlockState state) { super(LaserContent.LIGHT_ENTITY.get(), pos, state); }

    public boolean claim(LaserMachineBlockEntity host) {
        if (level == null || level.isClientSide || level != host.getLevel() || host.isRemoved()) return false;
        if (!owners.containsKey(host.identity()) && owners.size() >= MAX_OWNERS) return false;
        long now = level.getGameTime();
        var lease = owners.get(host.identity());
        if (lease == null || !lease.source().equals(host.getBlockPos()) || lease.until() - now <= LEASE_TICKS / 2) {
            owners.put(host.identity(), new Lease(host.getBlockPos().immutable(), now + LEASE_TICKS));
            setChanged();
        }
        return true;
    }

    public void release(LaserMachineBlockEntity host) {
        var lease = owners.get(host.identity());
        if (lease != null && lease.source().equals(host.getBlockPos())) {
            owners.remove(host.identity());
            setChanged();
            removeIfUnclaimed();
        }
    }

    @Override public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            long maximum = level.getGameTime() + LEASE_TICKS;
            owners.replaceAll((id, lease) -> new Lease(lease.source(), Math.min(lease.until(), maximum)));
            level.scheduleTick(worldPosition, getBlockState().getBlock(), 20);
        }
    }

    public void recover() {
        if (level == null || level.isClientSide || level.getBlockEntity(worldPosition) != this) return;
        long now = level.getGameTime();
        boolean changed = owners.entrySet().removeIf(entry -> {
            var lease = entry.getValue();
            if (lease.until() <= now) return true;
            if (!level.hasChunkAt(lease.source())) return false;
            return !(level.getBlockEntity(lease.source()) instanceof LaserMachineBlockEntity host)
                    || host.isRemoved() || !entry.getKey().equals(host.identity());
        });
        if (changed) setChanged();
        if (!removeIfUnclaimed()) level.scheduleTick(worldPosition, getBlockState().getBlock(), 20);
    }

    private boolean removeIfUnclaimed() {
        if (!owners.isEmpty() || level == null || level.isClientSide) return false;
        if (level.hasChunkAt(worldPosition) && level.getBlockEntity(worldPosition) == this && getBlockState().is(LaserContent.LIGHT.get()))
            level.setBlockAndUpdate(worldPosition, Blocks.AIR.defaultBlockState());
        return true;
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        var leases = new ListTag();
        owners.forEach((id, lease) -> {
            var saved = new CompoundTag();
            saved.putUUID("Id", id); saved.putLong("Source", lease.source().asLong()); saved.putLong("Until", lease.until());
            leases.add(saved);
        });
        tag.put("Owners", leases);
    }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        owners.clear();
        var leases = tag.getList("Owners", Tag.TAG_COMPOUND);
        for (int index = 0; index < Math.min(MAX_OWNERS, leases.size()); index++) {
            var saved = leases.getCompound(index);
            if (saved.hasUUID("Id")) owners.put(saved.getUUID("Id"), new Lease(BlockPos.of(saved.getLong("Source")), saved.getLong("Until")));
        }
    }
}
