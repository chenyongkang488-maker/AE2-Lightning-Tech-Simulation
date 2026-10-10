package dev.overloadsim.output;

import java.util.*;

/** Owns extracted resources until accepted by a destination or returned to ME. */
public final class OutputTransferLedger<K> {
    @FunctionalInterface public interface Target<K> { long insert(K key, long amount, boolean simulate); }
    public interface Network<K> { long extract(K key, long amount); long refund(K key, long amount); }
    private final Map<K,Long> pending = new LinkedHashMap<>();
    private final Map<Integer,Integer> cursors = new HashMap<>();
    private final Runnable changed;
    private boolean busy;
    public OutputTransferLedger() { this(() -> {}); }
    public OutputTransferLedger(Runnable changed) { this.changed = Objects.requireNonNull(changed); }

    public long export(int slot, K key, long budget, List<Target<K>> targets, Network<K> network) {
        if (busy || key == null || budget <= 0 || targets.isEmpty()) return 0;
        busy = true;
        try {
            refundInternal(network);
            if (pending.containsKey(key)) return 0;
            int first = Math.floorMod(cursors.getOrDefault(slot, 0), targets.size());
            cursors.put(slot, (first + 1) % targets.size()); changed.run();
            long moved = 0;
            for (int offset=0; offset<targets.size() && moved<budget; offset++) {
                var target = targets.get((first+offset)%targets.size());
                long accepted;
                try { accepted = bounded(target.insert(key, budget-moved, true), budget-moved); }
                catch (RuntimeException refused) { continue; }
                if (accepted == 0) continue;
                long extracted = bounded(network.extract(key, accepted), accepted);
                if (extracted == 0) break;
                // Persist ownership before entering any external target callback.
                pending.merge(key, extracted, OutputTransferLedger::saturatingAdd); changed.run();
                long inserted;
                try { inserted = bounded(target.insert(key, extracted, false), extracted); }
                catch (RuntimeException refused) { inserted = 0; }
                takePending(key, inserted);
                moved += inserted;
                refundInternal(network);
                // Do not extract another batch while an earlier batch remains privately owned.
                if (pending.containsKey(key)) break;
            }
            return moved;
        } finally { busy=false; }
    }
    public void refund(Network<K> network) {
        if (busy) return;
        busy=true;
        try { refundInternal(network); } finally { busy=false; }
    }
    private void refundInternal(Network<K> network) {
        for (var entry : new ArrayList<>(pending.entrySet())) {
            long returned;
            try { returned=bounded(network.refund(entry.getKey(),entry.getValue()),entry.getValue()); }
            catch (RuntimeException refused) { continue; }
            takePending(entry.getKey(),returned);
        }
    }
    private void takePending(K key,long amount) {
        if (amount<=0) return;
        long left=pending.getOrDefault(key,0L)-amount;
        if (left<=0) pending.remove(key); else pending.put(key,left);
        changed.run();
    }
    public Map<K,Long> pendingRefunds() { return Map.copyOf(pending); }
    public Map<Integer,Integer> cursors() { return Map.copyOf(cursors); }
    public void restoreCursors(Map<Integer,Integer> saved) { cursors.clear(); cursors.putAll(saved); }
    public void restoreRefunds(Map<K,Long> saved) {
        pending.clear(); saved.forEach((key,amount)->{if(key!=null&&amount!=null&&amount>0)pending.merge(key,amount,OutputTransferLedger::saturatingAdd);});
    }
    public void clearRefunds() { pending.clear(); changed.run(); }
    public static long bounded(long actual,long requested) { return Math.max(0,Math.min(actual,requested)); }
    private static long saturatingAdd(long a,long b) { return a>Long.MAX_VALUE-b?Long.MAX_VALUE:a+b; }
}
