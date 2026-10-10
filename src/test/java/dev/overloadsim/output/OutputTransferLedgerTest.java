package dev.overloadsim.output;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class OutputTransferLedgerTest {
    @Test void targetsShareOneSlotBudgetAndRotateFirstTarget() {
        var ledger = new OutputTransferLedger<String>();
        var accepted = new long[2];
        var targets = List.<OutputTransferLedger.Target<String>>of(
                (key, amount, simulate) -> { if (!simulate) accepted[0] += amount; return amount; },
                (key, amount, simulate) -> { if (!simulate) accepted[1] += amount; return amount; });
        var network = new Network(100);
        assertEquals(7, ledger.export(0, "iron", 7, targets, network));
        assertArrayEquals(new long[]{7, 0}, accepted);
        assertEquals(7, ledger.export(0, "iron", 7, targets, network));
        assertArrayEquals(new long[]{7, 7}, accepted);
        assertEquals(86, network.stock);
    }
    @Test void refusedAfterSimulationRefundsAndRetainsUnrefundedOwnership() {
        var ledger = new OutputTransferLedger<String>();
        var network = new Network(10);
        network.refundLimit = 2;
        var refused = List.<OutputTransferLedger.Target<String>>of((key, amount, simulate) -> simulate ? amount : 0);
        assertEquals(0, ledger.export(0, "iron", 7, refused, network));
        assertEquals(Map.of("iron", 5L), ledger.pendingRefunds());
        assertEquals(5, network.stock);
        var restored = new OutputTransferLedger<String>();
        restored.restoreRefunds(ledger.pendingRefunds());
        network.refundLimit = Long.MAX_VALUE;
        restored.refund(network);
        assertTrue(restored.pendingRefunds().isEmpty());
        assertEquals(10, network.stock);
    }
    @Test void refusedTargetLeavesBudgetForNextWithoutExtractingTarget() {
        var ledger = new OutputTransferLedger<String>();
        var network = new Network(20);
        var received = new long[1];
        assertEquals(9, ledger.export(3, "water", 9, List.of(
            (key, amount, simulate) -> 0L,
            (key, amount, simulate) -> { if (!simulate) received[0] += amount; return amount; }), network));
        assertEquals(9, received[0]);
        assertEquals(11, network.stock);
    }
    @Test void callbackExceptionPersistsExtractedStack() {
        var ledger = new OutputTransferLedger<String>();
        var network = new Network(20); network.refundLimit = 0;
        assertEquals(0, ledger.export(0, "iron", 6, List.of((key, amount, simulate) -> {
            if (simulate) return amount; throw new IllegalStateException("target vanished");
        }), network));
        assertEquals(Map.of("iron", 6L), ledger.pendingRefunds());
    }
    private static final class Network implements OutputTransferLedger.Network<String> {
        long stock, refundLimit = Long.MAX_VALUE;
        Network(long stock) { this.stock = stock; }
        public long extract(String key, long amount) { long extracted = Math.min(stock, amount); stock -= extracted; return extracted; }
        public long refund(String key, long amount) { long inserted = Math.min(amount, refundLimit); stock += inserted; return inserted; }
    }
}
