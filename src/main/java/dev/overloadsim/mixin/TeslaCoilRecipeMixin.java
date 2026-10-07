package dev.overloadsim.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.moakiee.ae2lt.blockentity.TeslaCoilBlockEntity;
import com.moakiee.ae2lt.machine.teslacoil.TeslaCoilInventory;
import com.moakiee.ae2lt.machine.teslacoil.TeslaCoilMode;
import com.moakiee.ae2lt.me.key.LightningKey;
import com.moakiee.ae2lt.registry.ModItems;
import dev.overloadsim.ModContent;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Specializes one native locked batch; upstream owns charging, ticking, status and reset. */
@Mixin(value = TeslaCoilBlockEntity.class, remap = false)
public abstract class TeslaCoilRecipeMixin {
    @Unique private static final long overloadSim$TOTAL_FE = 500_000L;
    @Unique private static final long overloadSim$OUTPUT_EHV = 64L;
    @Unique private static final String overloadSim$RECIPE_TAG = "OverloadSimTranscendentBatch";
    @Unique private static final String overloadSim$OUTPUT_TAG = "OverloadSimTranscendentDelivered";
    @Unique private static final String overloadSim$RESERVED_TAG = "OverloadSimTranscendentReserved";

    @Shadow @Final private TeslaCoilInventory inventory;
    @Shadow private TeslaCoilMode lockedMode;
    @Unique private boolean overloadSim$transcendentBatch;
    /** Nonzero output means the batch already owns its consumed crystal, even on insert-only storage. */
    @Unique private long overloadSim$deliveredOutput;
    @Unique private ItemStack overloadSim$reservedCrystal = ItemStack.EMPTY;
    @Unique private boolean overloadSim$committing;

    @Shadow private long simulateInsert(LightningKey key, long amount) { throw new AssertionError(); }
    @Shadow private long insert(LightningKey key, long amount) { throw new AssertionError(); }
    @Shadow private long extract(LightningKey key, long amount) { throw new AssertionError(); }

    @Unique
    private boolean overloadSim$isCrystal(ItemStack stack) {
        return !stack.isEmpty() && stack.is(ModContent.TRANSCENDENT.get());
    }

    @Unique
    private boolean overloadSim$hasMatrix() {
        var stack = inventory.getStackInSlot(TeslaCoilInventory.SLOT_MATRIX);
        return !stack.isEmpty() && stack.is(ModItems.LIGHTNING_COLLAPSE_MATRIX.get());
    }

    @Unique
    private boolean overloadSim$isCrystalRecipe() {
        // A locked native dust batch must never change recipes when its input is replaced.
        return lockedMode != null ? overloadSim$transcendentBatch
                : overloadSim$isCrystal(inventory.getStackInSlot(TeslaCoilInventory.SLOT_DUST));
    }

    @Unique
    private boolean overloadSim$hasCrystalPrerequisites() {
        return overloadSim$hasMatrix() && (overloadSim$deliveredOutput > 0L
                || overloadSim$isCrystal(overloadSim$reservedCrystal)
                || overloadSim$isCrystal(inventory.getStackInSlot(TeslaCoilInventory.SLOT_DUST)));
    }

    @Unique
    private boolean overloadSim$isLive() {
        var host = (TeslaCoilBlockEntity) (Object) this;
        var level = host.getLevel();
        return level != null && !level.isClientSide() && !host.isRemoved()
                && level.getBlockEntity(host.getBlockPos()) == host;
    }

    @Inject(method = "lockSelectedMode", at = @At("HEAD"))
    private void overloadSim$rememberRecipe(CallbackInfoReturnable<Boolean> cir) {
        if (lockedMode == null) {
            // Set before upstream assigns lockedMode and asks for this batch's size.
            overloadSim$transcendentBatch = overloadSim$isCrystal(inventory.getStackInSlot(TeslaCoilInventory.SLOT_DUST));
            overloadSim$deliveredOutput = 0L;
            overloadSim$reservedCrystal = ItemStack.EMPTY;
        }
    }

    @Inject(method = "lockSelectedMode", at = @At("RETURN"))
    private void overloadSim$discardFailedLock(CallbackInfoReturnable<Boolean> cir) {
        if (lockedMode == null) {
            overloadSim$transcendentBatch = false;
            overloadSim$deliveredOutput = 0L;
            overloadSim$reservedCrystal = ItemStack.EMPTY;
        }
    }

    @Inject(method = "hasLocalResourcesForMinimumOperation", at = @At("HEAD"), cancellable = true)
    private void overloadSim$minimumCrystalResources(CallbackInfoReturnable<Boolean> cir) {
        if (overloadSim$isCrystal(inventory.getStackInSlot(TeslaCoilInventory.SLOT_DUST))) {
            cir.setReturnValue(overloadSim$hasMatrix());
        }
    }

    @Inject(method = "hasLocalPrerequisites", at = @At("HEAD"), cancellable = true)
    private void overloadSim$localCrystalPrerequisites(TeslaCoilMode mode, long batchSize,
            CallbackInfoReturnable<Boolean> cir) {
        if (overloadSim$isCrystalRecipe()) {
            cir.setReturnValue(batchSize == 1L && overloadSim$hasCrystalPrerequisites());
        } else if (mode == TeslaCoilMode.HIGH_VOLTAGE
                && overloadSim$isCrystal(inventory.getStackInSlot(TeslaCoilInventory.SLOT_DUST))) {
            // Upstream hasRequiredDust checks count alone, so widened input acceptance needs this guard.
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "canCommitAgainstNetwork", at = @At("HEAD"), cancellable = true)
    private void overloadSim$crystalOutputSpace(TeslaCoilMode mode, long batchSize,
            CallbackInfoReturnable<Boolean> cir) {
        if (overloadSim$isCrystalRecipe()) {
            long remaining = overloadSim$OUTPUT_EHV - overloadSim$deliveredOutput;
            boolean accepted = batchSize == 1L && overloadSim$isLive()
                    && (remaining == 0L || simulateInsert(LightningKey.EXTREME_HIGH_VOLTAGE, remaining) >= remaining);
            cir.setReturnValue(accepted && overloadSim$isLive());
        }
    }

    @Inject(method = "getBatchSizeForMode", at = @At("HEAD"), cancellable = true)
    private void overloadSim$oneCrystalBatch(TeslaCoilMode mode, CallbackInfoReturnable<Long> cir) {
        if (overloadSim$isCrystalRecipe()) {
            boolean accepted = overloadSim$isLive() && overloadSim$hasCrystalPrerequisites()
                    && simulateInsert(LightningKey.EXTREME_HIGH_VOLTAGE, overloadSim$OUTPUT_EHV) >= overloadSim$OUTPUT_EHV
                    && overloadSim$isLive();
            cir.setReturnValue(accepted ? 1L : 0L);
        }
    }

    @Inject(method = "getTotalEnergyFor", at = @At("HEAD"), cancellable = true)
    private void overloadSim$crystalEnergy(TeslaCoilMode mode, long batchSize, CallbackInfoReturnable<Long> cir) {
        if (mode != null && batchSize > 0L && overloadSim$isCrystalRecipe()) {
            cir.setReturnValue(overloadSim$TOTAL_FE);
        }
    }

    @Inject(method = {"commitHighVoltage", "commitExtremeHighVoltage"}, at = @At("HEAD"), cancellable = true)
    private void overloadSim$commitCrystalRecipe(CallbackInfoReturnable<Boolean> cir) {
        if (lockedMode != null && overloadSim$transcendentBatch) {
            cir.setReturnValue(overloadSim$commitCrystal());
        }
    }

    @Unique
    private boolean overloadSim$commitCrystal() {
        var host = (TeslaCoilBlockEntity) (Object) this;
        if (!overloadSim$isLive() || !host.isReadyToCommit() || !overloadSim$hasCrystalPrerequisites()) return false;
        long remaining = overloadSim$OUTPUT_EHV - overloadSim$deliveredOutput;
        if (remaining == 0L) return true;

        if (overloadSim$deliveredOutput == 0L && overloadSim$reservedCrystal.isEmpty()) {
            var consumed = inventory.extractItem(TeslaCoilInventory.SLOT_DUST, 1, false);
            if (!overloadSim$isCrystal(consumed) || consumed.getCount() != 1) {
                return false;
            }
            overloadSim$reservedCrystal = consumed;
        }
        if (!overloadSim$isLive()) return false;

        long inserted = insert(LightningKey.EXTREME_HIGH_VOLTAGE, remaining);
        overloadSim$deliveredOutput += inserted;
        if (!overloadSim$isLive()) return false;
        if (inserted == remaining) return true;

        // Reverse a short actual write when possible. Insert-only storage may deny this extraction;
        // retain its accepted output as a paid, persisted debt and deliver only the remainder on retry.
        long reversed = inserted > 0L ? extract(LightningKey.EXTREME_HIGH_VOLTAGE, inserted) : 0L;
        overloadSim$deliveredOutput -= reversed;
        if (!overloadSim$isLive()) return false;
        if (overloadSim$deliveredOutput == 0L && !overloadSim$reservedCrystal.isEmpty()) {
            overloadSim$reservedCrystal = inventory.insertItem(TeslaCoilInventory.SLOT_DUST,
                    overloadSim$reservedCrystal, false);
        } else if (overloadSim$deliveredOutput > 0L) {
            overloadSim$reservedCrystal = ItemStack.EMPTY;
        }
        if (overloadSim$isLive()) {
            host.saveChanges();
            host.markForClientUpdate();
            host.getLogic().onStateChanged();
        }
        return false;
    }

    @WrapMethod(method = "commitLockedMode", remap = false)
    private boolean overloadSim$guardCommit(Operation<Boolean> original) {
        if (!overloadSim$transcendentBatch || lockedMode == null) return original.call();
        if (overloadSim$committing || !overloadSim$isLive()) return false;
        var host = (TeslaCoilBlockEntity) (Object) this;
        var level = host.getLevel();
        overloadSim$committing = true;
        try {
            boolean committed = original.call();
            if (committed) {
                overloadSim$transcendentBatch = false;
                overloadSim$deliveredOutput = 0L;
                overloadSim$reservedCrystal = ItemStack.EMPTY;
            }
            return committed;
        } finally {
            overloadSim$committing = false;
            // Native destruction during a storage callback suppresses the in-flight reservation's
            // drop until accepted output is known. Return it only if no irreversible output exists.
            if (!overloadSim$isLive() && !overloadSim$reservedCrystal.isEmpty()) {
                if (overloadSim$deliveredOutput == 0L && level != null && !level.isClientSide()) {
                    Block.popResource(level, host.getBlockPos(), overloadSim$reservedCrystal.copy());
                }
                overloadSim$reservedCrystal = ItemStack.EMPTY;
            }
        }
    }

    @Inject(method = "addAdditionalDrops", at = @At("TAIL"))
    private void overloadSim$dropUndeliveredReservation(Level level, BlockPos pos, List<ItemStack> drops,
            CallbackInfo ci) {
        if (!overloadSim$committing && overloadSim$deliveredOutput == 0L && !overloadSim$reservedCrystal.isEmpty()) {
            drops.add(overloadSim$reservedCrystal.copy());
        }
    }

    @Inject(method = "clearContent", at = @At("TAIL"))
    private void overloadSim$clearUndeliveredReservation(CallbackInfo ci) {
        if (!overloadSim$committing) overloadSim$reservedCrystal = ItemStack.EMPTY;
    }

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void overloadSim$saveRecipe(CompoundTag data, HolderLookup.Provider registries, CallbackInfo ci) {
        if (lockedMode != null && overloadSim$transcendentBatch) {
            data.putBoolean(overloadSim$RECIPE_TAG, true);
            data.putLong(overloadSim$OUTPUT_TAG, overloadSim$deliveredOutput);
            if (!overloadSim$reservedCrystal.isEmpty()) {
                data.put(overloadSim$RESERVED_TAG, overloadSim$reservedCrystal.save(registries));
            } else {
                data.remove(overloadSim$RESERVED_TAG);
            }
        } else {
            data.remove(overloadSim$RECIPE_TAG);
            data.remove(overloadSim$OUTPUT_TAG);
            data.remove(overloadSim$RESERVED_TAG);
        }
    }

    @Inject(method = "loadTag", at = @At("HEAD"))
    private void overloadSim$loadRecipe(CompoundTag data, HolderLookup.Provider registries, CallbackInfo ci) {
        // Upstream clamps consumed FE against getLockedTotalEnergy during load, before a TAIL hook could run.
        overloadSim$transcendentBatch = data.contains("LockedMode") && data.getBoolean(overloadSim$RECIPE_TAG);
        overloadSim$deliveredOutput = overloadSim$transcendentBatch
                ? Math.clamp(data.getLong(overloadSim$OUTPUT_TAG), 0L, overloadSim$OUTPUT_EHV) : 0L;
        overloadSim$reservedCrystal = overloadSim$transcendentBatch && overloadSim$deliveredOutput == 0L
                && data.contains(overloadSim$RESERVED_TAG)
                ? ItemStack.parse(registries, data.getCompound(overloadSim$RESERVED_TAG)).orElse(ItemStack.EMPTY)
                : ItemStack.EMPTY;
        if (!overloadSim$isCrystal(overloadSim$reservedCrystal) || overloadSim$reservedCrystal.getCount() != 1) {
            overloadSim$reservedCrystal = ItemStack.EMPTY;
        }
    }
}
