package dev.overloadsim.core;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class WorkstationLaserLeaseTest {
    @Test void weakBeamsNeverAddAndCannotSuppressAValidStrongBeam(){
        var lease=new WorkstationLaserLease();lease.accept(600_000_000L,100,100);lease.accept(600_000_000L,100,100);assertEquals(600_000_000L,lease.power(100));
        lease.accept(1_000_000_001L,101,101);lease.accept(100,102,102);assertEquals(1_000_000_001L,lease.power(102));
    }
    @Test void receiptsExpireAfter22TicksAndStaleOrFutureReceiptsCannotRenewThem(){
        var lease=new WorkstationLaserLease();assertTrue(lease.accept(2_000_000_000L,100,100));assertEquals(2_000_000_000L,lease.power(121));assertEquals(0,lease.power(122));
        assertFalse(lease.accept(2_000_000_000L,100,122));assertFalse(lease.accept(2_000_000_000L,123,122));assertEquals(0,lease.power(122));
    }
    @Test void independentSourceRemovalImmediatelyRecomputesStrongestBeam(){
        var lease=new WorkstationLaserLease();assertTrue(lease.acceptSource("strong",2_000_000_000L,100,100));assertTrue(lease.acceptSource("weak",600_000_000L,100,100));
        assertEquals(2_000_000_000L,lease.power(100));lease.releaseSource("strong");assertEquals(600_000_000L,lease.power(100));lease.releaseSource("weak");assertEquals(0,lease.power(100));
    }
    @Test void sourceIdentityAndLatestPulseCannotReviveRemovedOrWeakenedSources(){
        var lease=new WorkstationLaserLease();lease.acceptSource("old-identity",2_000_000_000L,100,100);lease.acceptSource("new-identity",1_500_000_000L,101,101);lease.releaseSource("old-identity");assertEquals(1_500_000_000L,lease.power(101));
        lease.acceptSource("new-identity",300_000_000L,102,102);assertEquals(300_000_000L,lease.power(102));
        assertFalse(lease.acceptSource("new-identity",2_000_000_000L,80,102));assertFalse(lease.acceptSource("new-identity",2_000_000_000L,103,102));assertEquals(300_000_000L,lease.power(102));
        lease.releaseSource("new-identity");assertEquals(0,lease.power(102));
    }
}
