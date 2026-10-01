package dev.overloadsim.core;
import dev.overloadsim.compat.SimulationLightningPayment;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class SimulationPaymentTest {
    @Test void partialExtremePaymentRefundsExactSeparateKeys(){
        var storage=new SimulationLightningPayment.Storage(){
            public long extract(boolean extreme,long amount,boolean simulate){return simulate?amount:extreme?1:amount;}
            public long insert(boolean extreme,long amount){return extreme?0:amount-2;}
        };
        var result=SimulationLightningPayment.pay(storage,6,3);
        assertFalse(result.paid());assertEquals(2,result.refundHv());assertEquals(1,result.refundEhv());
    }
    @Test void insufficientExtremeDoesNotDebitHigh(){
        var storage=new SimulationLightningPayment.Storage(){
            public long extract(boolean extreme,long amount,boolean simulate){assertTrue(simulate);return extreme?0:amount;}
            public long insert(boolean extreme,long amount){fail("no refund necessary");return 0;}
        };assertFalse(SimulationLightningPayment.pay(storage,3,1).paid());
    }
}
