package dev.overloadsim.compat;
import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.IActionSource;
import appeng.blockentity.grid.AENetworkedBlockEntity;
/** Public host I/O when available; refunds deliberately retain the inactive-grid storage path. */
public final class LightningNetwork {
    private LightningNetwork(){}
    public static long extract(AENetworkedBlockEntity be,long amount,boolean simulate){return UpstreamApi.get().extract(be,false,amount,simulate,IActionSource.ofMachine((IActionHost)be));}
    public static long refund(AENetworkedBlockEntity be,long amount,boolean legacyEhv){if(amount<=0)return 0;var grid=be.getMainNode().getGrid();return grid==null?0:grid.getStorageService().getInventory().insert(UpstreamApi.get().key(legacyEhv),amount,Actionable.MODULATE,IActionSource.ofMachine((IActionHost)be));}
}
