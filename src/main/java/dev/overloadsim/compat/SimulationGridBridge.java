package dev.overloadsim.compat;
import appeng.api.config.*;
import appeng.api.networking.*;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.networking.GridHelper;
import appeng.blockentity.grid.AENetworkedBlockEntity;
import com.moakiee.ae2lt.me.key.LightningKey;
import dev.overloadsim.multiblock.SimulationControllerBlockEntity;
import net.minecraft.core.BlockPos;
public final class SimulationGridBridge implements SimulationLightningPayment.Storage {
    private final SimulationControllerBlockEntity host;
    private IGridConnection connection;private BlockPos port;private boolean ownedConnection;
    public SimulationGridBridge(SimulationControllerBlockEntity host){this.host=host;}
    public void refresh(){
        var structure=host.structure();var expected=structure==null?null:structure.networkInterface();
        if(expected==null||!host.loaded()){disconnect();return;}
        var be=host.getLevel().getBlockEntity(expected);
        if(!(be instanceof com.moakiee.ae2lt.blockentity.OverloadedInterfaceBlockEntity network)){disconnect();return;}
        var a=host.getMainNode().getNode();var b=network.getMainNode().getNode();
        if(a==null||b==null){disconnect();return;}
        if(connection!=null&&(!expected.equals(port)||!a.getConnections().contains(connection)||connection.getOtherSide(a)!=b))disconnect();
        if(connection==null){
            connection=a.getConnections().stream().filter(c->c.getOtherSide(a)==b).findFirst().orElse(null);
            ownedConnection=connection==null;
            if(connection==null)connection=GridHelper.createConnection(a,b);port=expected;
        }
    }
    public void disconnect(){if(connection!=null){if(ownedConnection)connection.destroy();connection=null;}ownedConnection=false;port=null;}
    @Override public long extract(boolean extreme,long amount,boolean simulate){
        if(amount<=0)return 0;
        var grid=host.getMainNode().getGrid();if(grid==null||!host.getMainNode().isActive())return 0;
        return grid.getStorageService().getInventory().extract(extreme?LightningKey.EXTREME_HIGH_VOLTAGE:LightningKey.HIGH_VOLTAGE,amount,simulate?Actionable.SIMULATE:Actionable.MODULATE,IActionSource.ofMachine(host));
    }
    @Override public long insert(boolean extreme,long amount){
        if(amount<=0)return 0;var grid=host.getMainNode().getGrid();if(grid==null)return 0;
        return grid.getStorageService().getInventory().insert(extreme?LightningKey.EXTREME_HIGH_VOLTAGE:LightningKey.HIGH_VOLTAGE,amount,Actionable.MODULATE,IActionSource.ofMachine(host));
    }
    public void export(){
        refresh();
        var structure=host.structure();var grid=host.getMainNode().getGrid();
        if(structure==null||structure.networkInterface()==null||connection==null||!host.loaded()||grid==null||!host.getMainNode().isActive())return;
        var buffer=host.outputs();boolean moved=false;
        for(int slot=0;slot<buffer.SLOTS;slot++)if(buffer.count(slot)>0){
            long accepted=grid.getStorageService().getInventory().insert(AEItemKey.of(buffer.prototype(slot)),buffer.count(slot),Actionable.MODULATE,IActionSource.ofMachine(host));
            if(accepted>0){buffer.debit(slot,accepted);moved=true;}
        }if(moved)host.saveChanges();
    }
}
