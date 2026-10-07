package dev.overloadsim.laser;

import appeng.api.config.Actionable;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IGridConnection;
import appeng.api.networking.security.IActionSource;
import com.moakiee.ae2lt.me.key.LightningKey;
import dev.overloadsim.api.LaserExtensions;
import dev.overloadsim.compat.AppliedFluxEnergy;
import dev.overloadsim.compat.GridConnections;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/** Bounded AE transactions. Applied Flux is discovered without linking optional classes. */
public final class LaserGridBridge {
    private final LaserMachineBlockEntity host;
    private IGridConnection connection;
    private BlockPos port;
    private boolean ownedConnection;
    private int feTransfersInProgress;

    public LaserGridBridge(LaserMachineBlockEntity host) { this.host = host; }

    public static boolean hasAppliedFlux() { return AppliedFluxEnergy.available(); }
    boolean feTransferInProgress() { return feTransfersInProgress > 0; }

    public void refresh() {
        if (host.kind() == LaserKind.CORE) { disconnect(); return; }
        var structure = host.structure();
        var expected = structure == null ? null : structure.networkInterface();
        if (expected == null || !host.loaded()) { disconnect(); return; }
        if (!(host.getLevel().getBlockEntity(expected) instanceof com.moakiee.ae2lt.blockentity.OverloadedInterfaceBlockEntity network)) { disconnect(); return; }
        var a = host.getMainNode().getNode(); var b = network.getMainNode().getNode();
        if (a == null || b == null) { disconnect(); return; }
        if (connection != null && (!expected.equals(port) || !a.getConnections().contains(connection) || connection.getOtherSide(a) != b)) disconnect();
        if (connection == null) {
            connection = a.getConnections().stream().filter(candidate -> candidate.getOtherSide(a) == b).findFirst().orElse(null);
            ownedConnection = connection == null;
            if (connection == null) connection = GridHelper.createConnection(a, b);
            port = expected;
        }
    }

    public void disconnect() {
        var previous = connection;
        boolean owned = ownedConnection;
        connection = null; port = null; ownedConnection = false;
        GridConnections.destroyOwned(previous, owned);
    }

    public long extractExtreme(long amount, boolean simulate) {
        if (amount <= 0 || host.kind() == LaserKind.CORE) return 0;
        var grid = host.getMainNode().getGrid();
        if (grid == null || !host.getMainNode().isActive()) return 0;
        long extracted = grid.getStorageService().getInventory().extract(LightningKey.EXTREME_HIGH_VOLTAGE, amount,
                simulate ? Actionable.SIMULATE : Actionable.MODULATE, IActionSource.ofMachine(host));
        return Math.clamp(extracted, 0L, amount);
    }

    public long insertExtreme(long amount) {
        if (amount <= 0 || host.kind() == LaserKind.CORE) return 0;
        var grid = host.getMainNode().getGrid();
        if (grid == null) return 0;
        return Math.clamp(grid.getStorageService().getInventory().insert(LightningKey.EXTREME_HIGH_VOLTAGE, amount,
                Actionable.MODULATE, IActionSource.ofMachine(host)), 0L, amount);
    }

    public void charge() {
        host.claimNetworkFeRefund();
        if (host.networkFeCredit() > 0) {
            long accepted = host.energy().insert(host.networkFeCredit(), false);
            if (accepted > 0) host.setNetworkFeCredit(host.networkFeCredit() - accepted);
        }
        if (host.kind() == LaserKind.CORE || host.kind() == LaserKind.RECEIVER || !host.formed()) return;
        var grid = host.getMainNode().getGrid();
        if (host.isRemoved() || grid == null || !host.getMainNode().isActive()) return;
        long space = host.energy().capacity() - host.energy().stored();
        if (space == 0) return;
        long offeredSpace = Math.max(0, space - (host.aeInputFraction > 0 ? 1 : 0));
        feTransfersInProgress++;
        try {
            var transfer = AppliedFluxEnergy.extractInto(grid, IActionSource.ofMachine(host), offeredSpace,
                    amount -> host.isRemoved() || !host.formed() ? 0 : host.energy().insert(amount, false));
            host.retainNetworkFeRefund(transfer.credit());
            if (transfer.accepted() > 0) host.saveChanges();
        } finally { feTransfersInProgress--; }
    }

    public void export() {
        if (host.kind() != LaserKind.RECEIVER || !host.formed() || host.energy().stored() == 0 || !(host.getLevel() instanceof ServerLevel level)) return;
        // Extension sinks are evaluated first and can accept exact long quantities.
        for (var sink : LaserExtensions.energySinks().stream().limit(32).toList()) {
            long offered = wholeExportable();
            if (offered == 0) break;
            long accepted = sink.insert(level, host.getBlockPos(), offered, false);
            if (accepted < 0 || accepted > offered) throw new IllegalStateException("energy sink returned an invalid accepted amount");
            if (accepted > 0) { host.energy().extract(accepted, false); host.saveChanges(); }
        }
        var grid = host.getMainNode().getGrid();
        if (grid == null || !host.getMainNode().isActive()) return;
        long accepted = AppliedFluxEnergy.insert(grid, IActionSource.ofMachine(host), wholeExportable());
        if (accepted > 0) { host.energy().extract(accepted, false); host.saveChanges(); }
    }

    /** A partly settled FE belongs to the AE fractional ledger until its final fraction is accepted. */
    private long wholeExportable() { return Math.max(0, host.energy().stored() - (host.aeOutputFraction > 0 ? 1 : 0)); }
}
