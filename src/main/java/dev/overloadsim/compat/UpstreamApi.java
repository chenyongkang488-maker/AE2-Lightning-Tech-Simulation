package dev.overloadsim.compat;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;

/** New API implementation is in a separately compiled, lazily linked source set. */
public interface UpstreamApi {
    void register();
    AEKey key(boolean extreme);
    long extract(IActionHost host,boolean extreme,long amount,boolean simulate,IActionSource source);

    static UpstreamApi get() { return Holder.INSTANCE; }
    static boolean modern() { return get().getClass().getName().equals("dev.overloadsim.compat.modern.ModernUpstreamApi"); }

    final class Holder {
        private static final UpstreamApi INSTANCE=create();
        private static UpstreamApi create() {
            var loader=UpstreamApi.class.getClassLoader();
            if(!UpstreamApiPresence.available(name->{
                try { Class.forName(name,false,loader); return true; }
                catch(ClassNotFoundException missing) { return false; }
            })) return new Legacy();
            try { return (UpstreamApi)Class.forName("dev.overloadsim.compat.modern.ModernUpstreamApi",true,loader).getConstructor().newInstance(); }
            catch(ReflectiveOperationException failure) { throw new IllegalStateException("AE2LT public API exists but addon bridge could not initialize",failure); }
        }
    }

    final class Legacy implements UpstreamApi {
        @Override public void register() {}
        @Override public AEKey key(boolean extreme) {
            return extreme?com.moakiee.ae2lt.me.key.LightningKey.EXTREME_HIGH_VOLTAGE:com.moakiee.ae2lt.me.key.LightningKey.HIGH_VOLTAGE;
        }
        @Override public long extract(IActionHost host,boolean extreme,long amount,boolean simulate,IActionSource source) {
            if(amount<=0) return 0;
            var node=host.getActionableNode();
            if(node==null||!node.isActive()) return 0;
            return node.getGrid().getStorageService().getInventory().extract(key(extreme),amount,
                    simulate?Actionable.SIMULATE:Actionable.MODULATE,source);
        }
    }
}
