package dev.overloadsim.core;

/** Strongest individual receipt in a bounded 22-tick window; strengths never add. */
public final class WorkstationLaserLease {
    private final long[] ticks=new long[22],strengths=new long[22];
    private record Receipt(long power,long tick) {}
    private final java.util.Map<Object,Receipt> sources=new java.util.HashMap<>();
    public WorkstationLaserLease(){clear();}
    public boolean accept(long power,long receiptTick,long now){
        if(power<0||receiptTick<0||receiptTick>now||now-receiptTick>=22)return false;
        int bucket=(int)(receiptTick%22);
        if(ticks[bucket]!=receiptTick){ticks[bucket]=receiptTick;strengths[bucket]=power;}else strengths[bucket]=Math.max(strengths[bucket],power);
        return true;
    }
    public long power(long now){long maximum=0;for(int i=0;i<22;i++)if(ticks[i]>=0&&ticks[i]<=now&&now-ticks[i]<22)maximum=Math.max(maximum,strengths[i]);sources.values().removeIf(receipt->receipt.tick()>now||now-receipt.tick()>=22);for(var receipt:sources.values())maximum=Math.max(maximum,receipt.power());return maximum;}
    public void clear(){java.util.Arrays.fill(ticks,-1);java.util.Arrays.fill(strengths,0);sources.clear();}
    public boolean acceptSource(Object source,long power,long receiptTick,long now){
        if(source==null||power<0||receiptTick<0||receiptTick>now||now-receiptTick>=22)return false;
        var previous=sources.get(source);if(previous!=null&&previous.tick()>receiptTick)return false;
        sources.put(source,new Receipt(previous!=null&&previous.tick()==receiptTick?Math.max(previous.power(),power):power,receiptTick));return true;
    }
    public void releaseSource(Object source){sources.remove(source);}
}
