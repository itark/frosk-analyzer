package nu.itark.frosk.service;

import nu.itark.frosk.analysis.IntradayOpenPositionDTO;
import nu.itark.frosk.analysis.IntradayPnlDTO;
import nu.itark.frosk.analysis.IntradayTodaySignalDTO;
import nu.itark.frosk.coinbase.BaseIntegrationTest;
import nu.itark.frosk.controller.DataController;
import nu.itark.frosk.model.IntradaySignal;
import nu.itark.frosk.repo.IntradaySignalRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CryptoLiquiditySweepIntradayStrategy is accumulating forward data for a
 * pre-registered test (~/itark/PREREG_liquidity_sweep_15m.md). Owner decision
 * (2026-08-07): show its numbers rather than hide them, but flag every row so
 * the dashboard can badge it as unvalidated — an explicit, informed choice to
 * accept the peeking risk the pre-registration warns against, in exchange for
 * visibility. A strategy NOT on the pending list must never be flagged, or the
 * badge stops meaning anything. Uses synthetic signals, not the real
 * strategy's data, so running this test does not itself peek.
 */
public class TestJPreRegistrationPendingStrategiesFlagged extends BaseIntegrationTest {

    private static final ZoneId STOCKHOLM = ZoneId.of("Europe/Stockholm");
    private static final String PENDING_STRATEGY = "CryptoLiquiditySweepIntradayStrategy";
    private static final String VALIDATED_STRATEGY = "CryptoVWAPReversionIntradayStrategy";
    private static final String TICKER = "TEST-SWEEP-EUR";

    @Autowired
    DataController dataController;

    @Autowired
    IntradaySignalRepository intradaySignalRepository;

    @Test
    public void pendingStrategyIsShownButFlaggedOnAllThreeTraderFacingViews() {
        List<IntradaySignal> created = new ArrayList<>();
        long t0 = ZonedDateTime.now(STOCKHOLM).minusMinutes(30).toEpochSecond();

        try {
            // Pending strategy: closed round trip + a still-open entry.
            created.add(intradaySignalRepository.save(new IntradaySignal(
                    PENDING_STRATEGY, TICKER, t0, "BUY", new BigDecimal("100.00"))));
            created.add(intradaySignalRepository.save(new IntradaySignal(
                    PENDING_STRATEGY, TICKER, t0 + 900, "SELL", new BigDecimal("101.00"))));
            created.add(intradaySignalRepository.save(new IntradaySignal(
                    PENDING_STRATEGY, TICKER, t0 + 1800, "BUY", new BigDecimal("102.00"))));
            // Validated strategy on the same ticker, for contrast — must never be flagged.
            created.add(intradaySignalRepository.save(new IntradaySignal(
                    VALIDATED_STRATEGY, TICKER, t0, "BUY", new BigDecimal("50.00"))));
            created.add(intradaySignalRepository.save(new IntradaySignal(
                    VALIDATED_STRATEGY, TICKER, t0 + 900, "SELL", new BigDecimal("51.00"))));

            List<IntradayPnlDTO> pnl = dataController.getIntradayPnl(TICKER);
            IntradayPnlDTO pendingPnl = pnl.stream()
                    .filter(p -> PENDING_STRATEGY.equals(p.getStrategyName())).findFirst().orElse(null);
            IntradayPnlDTO validatedPnl = pnl.stream()
                    .filter(p -> VALIDATED_STRATEGY.equals(p.getStrategyName())).findFirst().orElse(null);
            assertTrue(pendingPnl != null, "/intraday/pnl should still show the pending strategy's numbers");
            assertTrue(pendingPnl.isPreRegistrationPending(), "pending strategy must be flagged in /intraday/pnl");
            assertTrue(validatedPnl != null, "/intraday/pnl should show the validated strategy's numbers");
            assertFalse(validatedPnl.isPreRegistrationPending(), "validated strategy must NOT be flagged in /intraday/pnl");

            List<IntradayTodaySignalDTO> todaySignals = dataController.getIntradayTodaySignals();
            List<IntradayTodaySignalDTO> pendingSignals = todaySignals.stream()
                    .filter(s -> PENDING_STRATEGY.equals(s.getStrategyName())).toList();
            assertFalse(pendingSignals.isEmpty(), "/intradayTodaySignals should still show the pending strategy's signals");
            assertTrue(pendingSignals.stream().allMatch(IntradayTodaySignalDTO::isPreRegistrationPending),
                    "every pending-strategy signal must be flagged in /intradayTodaySignals");

            List<IntradayOpenPositionDTO> openPositions = dataController.getIntradayOpenPositions();
            IntradayOpenPositionDTO pendingOpen = openPositions.stream()
                    .filter(p -> PENDING_STRATEGY.equals(p.getStrategyName())).findFirst().orElse(null);
            assertTrue(pendingOpen != null, "/intradayOpenPositions should still show the pending strategy's open position");
            assertTrue(pendingOpen.isPreRegistrationPending(), "pending strategy must be flagged in /intradayOpenPositions");
        } finally {
            intradaySignalRepository.deleteAll(created);
        }
    }
}
