package dev.overloadsim.laser;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.config.PowerUnit;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IGridConnection;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import com.moakiee.ae2lt.me.key.LightningKey;
import dev.overloadsim.api.LaserExtensions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/** Bounded AE transactions. Applied Flux is discovered without linking optional classes. */
public final class LaserGridBridge {
    private static final int AE_FE_LIMIT = 10_000;
    private static final AEKey FLUX_FE = optionalFluxKey();
    private final LaserMachineBlockEntity host;
    private IGridConnection connection;
    private BlockPos port;
    private boolean ownedConnection;

    public LaserGridBridge(LaserMachineBlockEntity host) { this.host = host; }

    private static AEKey optionalFluxKey() {
        try {
            Class<?> type = Class.forName("com.glodblock.github.appflux.common.me.key.type.EnergyType");
            Object fe = type.getField("FE").get(null);
            Object key = Class.forName("com.glodblock.github.appflux.common.me.key.FluxKey").getMethod("of", type).invoke(null, fe);
            return key instanceof AEKey aeKey ? aeKey : null;
        } catch (ReflectiveOperationException | LinkageError absentOrIncompatible) {
            return null;
        }
    }

    public static boolean hasAppliedFlux() { return FLUX_FE != null; }

    public void refresh() {
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
        if (connection != null && ownedConnection) connection.destroy();
        connection = null; port = null; ownedConnection = false;
    }

    public long extractExtreme(long amount, boolean simulate) {
        if (amount <= 0) return 0;
        var grid = host.getMainNode().getGrid();
        if (grid == null || !host.getMainNode().isActive()) return 0;
        long extracted = grid.getStorageService().getInventory().extract(LightningKey.EXTREME_HIGH_VOLTAGE, amount,
                simulate ? Actionable.SIMULATE : Actionable.MODULATE, IActionSource.ofMachine(host));
        return Math.clamp(extracted, 0L, amount);
    }

    public long insertExtreme(long amount) {
        if (amount <= 0) return 0;
        var grid = host.getMainNode().getGrid();
        if (grid == null) return 0;
        return Math.clamp(grid.getStorageService().getInventory().insert(LightningKey.EXTREME_HIGH_VOLTAGE, amount,
                Actionable.MODULATE, IActionSource.ofMachine(host)), 0L, amount);
    }

    public void charge() {
        if (host.kind() == LaserKind.RECEIVER || !host.formed()) return;
        var grid = host.getMainNode().getGrid();
        if (grid == null || !host.getMainNode().isActive()) return;
        long space = host.energy().capacity() - host.energy().stored();
        if (space == 0) return;
        if (FLUX_FE != null) {
            long offeredSpace = Math.max(0, space - (host.aeInputFraction > 0 ? 1 : 0));
            long drawn = Math.clamp(grid.getStorageService().getInventory().extract(FLUX_FE, offeredSpace, Actionable.MODULATE, IActionSource.ofMachine(host)), 0L, offeredSpace);
            if (drawn > 0) { host.energy().insert(drawn, false); host.saveChanges(); }
            space -= drawn;
        }
        if (space == 0) return;
        double requestedFe = Math.max(0, Math.min(AE_FE_LIMIT, space) - host.aeInputFraction);
        double requestedAe = PowerUnit.FE.convertTo(PowerUnit.AE, requestedFe);
        double drawn = grid.getEnergyService().extractAEPower(requestedAe, Actionable.MODULATE, PowerMultiplier.ONE);
        if (!Double.isFinite(drawn) || drawn <= 0) return;
        double available = host.aeInputFraction + Math.clamp(PowerUnit.AE.convertTo(PowerUnit.FE, drawn), 0d, requestedFe);
        long whole = Math.min(space, (long) Math.floor(available));
        host.energy().insert(whole, false);
        host.aeInputFraction = Math.clamp(available - whole, 0d, Math.nextDown(1d));
        host.saveChanges();
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
        if (FLUX_FE != null) {
            long offered = wholeExportable();
            long accepted = Math.clamp(grid.getStorageService().getInventory().insert(FLUX_FE, offered, Actionable.MODULATE, IActionSource.ofMachine(host)), 0L, offered);
            if (accepted > 0) { host.energy().extract(accepted, false); host.saveChanges(); }
        }
        if (host.energy().stored() == 0) return;
        // injectPower returns EXCESS AE, verified against AE2 19.2.17. Carry fractions across calls and saves.
        double offeredFe = Math.max(0, Math.min(AE_FE_LIMIT, host.energy().stored()) - host.aeOutputFraction);
        double offeredAe = PowerUnit.FE.convertTo(PowerUnit.AE, offeredFe);
        double excess = grid.getEnergyService().injectPower(offeredAe, Actionable.MODULATE);
        if (!Double.isFinite(excess)) return;
        double acceptedFe = Math.clamp(PowerUnit.AE.convertTo(PowerUnit.FE, offeredAe - Math.clamp(excess, 0d, offeredAe)), 0d, offeredFe);
        if (acceptedFe == 0) return;
        double settled = host.aeOutputFraction + acceptedFe;
        long debit = Math.min(host.energy().stored(), (long) Math.floor(settled));
        host.energy().extract(debit, false);
        host.aeOutputFraction = Math.clamp(settled - debit, 0d, Math.nextDown(1d));
        host.saveChanges();
    }

    /** A partly settled FE belongs to the AE fractional ledger until its final fraction is accepted. */
    private long wholeExportable() { return Math.max(0, host.energy().stored() - (host.aeOutputFraction > 0 ? 1 : 0)); }
}
