package nu.itark.frosk.crypto.coinbase.orderbook;

import nu.itark.frosk.model.OrderBookMinuteSnapshot;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure unit test (no Spring context, no network, no DB) for the gap-aware
 * rolling 15-minute OFI sum — the input to both the daily threshold
 * calculation and the live entry check in PREREG_ofi_1m.md §4-§5.
 */
public class TestJOrderFlowImbalanceStrategyRunner {

    private static final long MINUTE = 60L;
    private static final long BASE = 1_700_000_000L; // arbitrary aligned epoch-minute start

    private OrderBookMinuteSnapshot row(long minuteTimestamp, double ofi) {
        return new OrderBookMinuteSnapshot(1L, minuteTimestamp,
                new BigDecimal("100.00"), new BigDecimal("1.0"),
                new BigDecimal("100.10"), new BigDecimal("1.0"),
                BigDecimal.valueOf(ofi), 10);
    }

    private final OrderFlowImbalanceStrategyRunner runner = new OrderFlowImbalanceStrategyRunner();

    @Test
    void fewerThan15RowsProducesNoWindow() {
        List<OrderBookMinuteSnapshot> rows = new ArrayList<>();
        for (int i = 0; i < 14; i++) {
            rows.add(row(BASE + i * MINUTE, 1.0));
        }
        assertTrue(runner.rollingOfi15m(rows).isEmpty());
    }

    @Test
    void exactly15ConsecutiveRowsProducesOneWindowSummingAll() {
        List<OrderBookMinuteSnapshot> rows = new ArrayList<>();
        double expectedSum = 0;
        for (int i = 0; i < 15; i++) {
            double v = i + 1; // 1..15
            rows.add(row(BASE + i * MINUTE, v));
            expectedSum += v;
        }
        List<BigDecimal> result = runner.rollingOfi15m(rows);
        assertEquals(1, result.size());
        assertEquals(0, BigDecimal.valueOf(expectedSum).compareTo(result.get(0)));
    }

    @Test
    void twentyConsecutiveRowsProducesSixSlidingWindows() {
        List<OrderBookMinuteSnapshot> rows = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            rows.add(row(BASE + i * MINUTE, 1.0)); // every minute contributes exactly 1.0
        }
        List<BigDecimal> result = runner.rollingOfi15m(rows);
        assertEquals(6, result.size(), "20 - 15 + 1 = 6 full windows");
        for (BigDecimal sum : result) {
            assertEquals(0, BigDecimal.valueOf(15.0).compareTo(sum), "each window is 15 minutes of 1.0");
        }
    }

    @Test
    void gapResetsTheWindowInsteadOfSpanningIt() {
        List<OrderBookMinuteSnapshot> rows = new ArrayList<>();
        // 15 consecutive minutes...
        for (int i = 0; i < 15; i++) {
            rows.add(row(BASE + i * MINUTE, 1.0));
        }
        // ...then a gap (skip 5 minutes)...
        long afterGap = BASE + 15 * MINUTE + 5 * MINUTE;
        // ...then only 14 more consecutive minutes — one short of a full window post-gap.
        for (int i = 0; i < 14; i++) {
            rows.add(row(afterGap + i * MINUTE, 2.0));
        }
        List<BigDecimal> result = runner.rollingOfi15m(rows);
        // Exactly the one window completed before the gap; none after (only 14 post-gap rows).
        assertEquals(1, result.size());
        assertEquals(0, BigDecimal.valueOf(15.0).compareTo(result.get(0)));
    }

    @Test
    void oneMoreRowAfterAGapCompletesASecondWindow() {
        List<OrderBookMinuteSnapshot> rows = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            rows.add(row(BASE + i * MINUTE, 1.0));
        }
        long afterGap = BASE + 15 * MINUTE + 5 * MINUTE;
        for (int i = 0; i < 15; i++) {
            rows.add(row(afterGap + i * MINUTE, 2.0));
        }
        List<BigDecimal> result = runner.rollingOfi15m(rows);
        assertEquals(2, result.size());
        assertEquals(0, BigDecimal.valueOf(15.0).compareTo(result.get(0)), "pre-gap window: 15 x 1.0");
        assertEquals(0, BigDecimal.valueOf(30.0).compareTo(result.get(1)), "post-gap window: 15 x 2.0");
    }
}
