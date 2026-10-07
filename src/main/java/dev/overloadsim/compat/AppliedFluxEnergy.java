package dev.overloadsim.compat;

import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import java.util.function.LongUnaryOperator;

/** Integer FE storage transactions, with no linkage to optional Applied Flux classes. */
public final class AppliedFluxEnergy {
    private static final AEKey FE = discoverFeKey();
    public record Transfer(long accepted, long credit) {}
    private static final Transfer EMPTY = new Transfer(0, 0);

    private AppliedFluxEnergy() {}

    public static boolean available() { return FE != null; }

    private static AEKey discoverFeKey() {
        try {
            var type = Class.forName("com.glodblock.github.appflux.common.me.key.type.EnergyType");
            var key = Class.forName("com.glodblock.github.appflux.common.me.key.FluxKey")
                    .getMethod("of", type).invoke(null, type.getField("FE").get(null));
            return key instanceof AEKey aeKey ? aeKey : null;
        } catch (ReflectiveOperationException | LinkageError absentOrIncompatible) {
            return null;
        }
    }

    /** The caller must persist credit if a capacity change makes both the local buffer and refund reject FE. */
    public static Transfer extractInto(IGrid grid, IActionSource source, long amount, LongUnaryOperator receive) {
        if (FE == null || grid == null || amount <= 0) return EMPTY;
        // Keep the original inventory even if an extraction callback changes the host's grid.
        var inventory = grid.getStorageService().getInventory();
        long drawn = Math.clamp(inventory.extract(FE, amount, Actionable.MODULATE, source), 0L, amount);
        if (drawn == 0) return EMPTY;
        long accepted = Math.clamp(receive.applyAsLong(drawn), 0L, drawn);
        long remaining = drawn - accepted;
        long returned = remaining == 0 ? 0 : Math.clamp(inventory.insert(FE, remaining, Actionable.MODULATE, source), 0L, remaining);
        return new Transfer(accepted, remaining - returned);
    }

    public static long insert(IGrid grid, IActionSource source, long amount) {
        if (FE == null || grid == null || amount <= 0) return 0;
        return Math.clamp(grid.getStorageService().getInventory().insert(FE, amount, Actionable.MODULATE, source), 0L, amount);
    }
}
