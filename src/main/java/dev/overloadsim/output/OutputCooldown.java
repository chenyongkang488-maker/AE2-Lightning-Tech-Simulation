// SPDX-License-Identifier: LGPL-3.0-only
package dev.overloadsim.output;

/** Adaptive output cadence, based on AE2LT CooldownTracker (Moakiee, LGPL-3.0-only). */
public final class OutputCooldown {
    private long nextTick=Long.MIN_VALUE,lastSuccess=Long.MIN_VALUE;
    private int idleDelay=1,previousGap=1,latestGap=1,predictedGap=1;
    private boolean failedSinceSuccess;
    public boolean isDue(long now){return now>=nextTick||now<lastSuccess;}
    public long nextTick(){return nextTick;}
    public void complete(long now,boolean success,boolean fast) {
        if(success) {
            long elapsed=lastSuccess==Long.MIN_VALUE?0:now-lastSuccess;
            int gap=failedSinceSuccess&&elapsed>0&&elapsed<100?(int)elapsed:1;
            predictedGap=gap==1?1:Math.max(Math.min(previousGap,latestGap),Math.min(Math.max(previousGap,latestGap),gap));
            previousGap=latestGap;latestGap=gap;lastSuccess=now;failedSinceSuccess=false;idleDelay=1;nextTick=now+1;
        }else{
            failedSinceSuccess=true;
            long elapsed=lastSuccess==Long.MIN_VALUE?100:now-lastSuccess;
            if(elapsed>=0&&elapsed<100)nextTick=now+Math.max(1,predictedGap-(int)elapsed);
            else{int maximum=fast?20:80;idleDelay=Math.min(maximum,idleDelay+Math.max(1,maximum/10));nextTick=now+idleDelay;}
        }
    }
    public void reset(){nextTick=lastSuccess=Long.MIN_VALUE;idleDelay=previousGap=latestGap=predictedGap=1;failedSinceSuccess=false;}
}
