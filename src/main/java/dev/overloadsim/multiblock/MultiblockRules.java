package dev.overloadsim.multiblock;

/** Pure default rules; the multiblock has its own policy, independent of single-block recipes. */
public final class MultiblockRules {
    private MultiblockRules() {}
    public record Costs(long fe, long hv, long ehv) {}
    public static int duration(int t1, int t2, int t3, boolean overload) {
        long reduction = Math.min(104, Math.max(0L,t1)*2 + Math.max(0L,t2)*4 + Math.max(0L,t3)*8);
        return (int)((180-reduction+(overload?1:0))/(overload?2:1));
    }
    public static long multiplier(int fortune) { return 1L << Math.clamp(fortune,0,10); }
    public static Costs costs(int crystals, boolean overload, boolean smelting) {
        if(crystals<0 || crystals>49) throw new IllegalArgumentException("crystal count");
        return new Costs(crystals*1000L,crystals*(smelting?3L:1L),overload?crystals:0);
    }
}
