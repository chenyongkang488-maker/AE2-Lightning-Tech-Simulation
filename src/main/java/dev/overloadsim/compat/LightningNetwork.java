package dev.overloadsim.compat;
import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.IActionSource;
import appeng.blockentity.grid.AENetworkedBlockEntity;
import com.moakiee.ae2lt.me.key.LightningKey;
/** Version-pinned bridge: the public API does not yet expose an arbitrary grid's lightning key. */
public final class LightningNetwork {
    private LightningNetwork(){}
    public static long extract(AENetworkedBlockEntity be,long amount,boolean simulate){var grid=be.getMainNode().getGrid();if(grid==null||!be.getMainNode().isActive())return 0;return grid.getStorageService().getInventory().extract(LightningKey.EXTREME_HIGH_VOLTAGE,amount,simulate?Actionable.SIMULATE:Actionable.MODULATE,IActionSource.ofMachine((IActionHost)be));}
    public static long refund(AENetworkedBlockEntity be,long amount){var grid=be.getMainNode().getGrid();return grid==null?0:grid.getStorageService().getInventory().insert(LightningKey.EXTREME_HIGH_VOLTAGE,amount,Actionable.MODULATE,IActionSource.ofMachine((IActionHost)be));}
}
