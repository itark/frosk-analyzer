package nu.itark.frosk.service;

import nu.itark.frosk.coinbase.BaseIntegrationTest;
import nu.itark.frosk.model.IntradaySignal;
import nu.itark.frosk.model.Portfolio;
import nu.itark.frosk.repo.IntradaySignalRepository;
import nu.itark.frosk.repo.PortfolioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The intraday portfolio must accumulate the day's realized round trips
 * (net of the 2×0.03% fee) — an open-positions-only snapshot reads 0.0000
 * essentially always, because intraday round trips close within hours.
 */
public class TestJIntradayPortfolioPnl extends BaseIntegrationTest {

    private static final ZoneId STOCKHOLM = ZoneId.of("Europe/Stockholm");

    @Autowired
    PortfolioService portfolioService;

    @Autowired
    IntradaySignalRepository intradaySignalRepository;

    @Autowired
    PortfolioRepository portfolioRepository;

    @Test
    public void realizedRoundTripsNetOfFeesFlowIntoIntradayPortfolio() {
        List<IntradaySignal> created = new ArrayList<>();
        Portfolio snapshot = null;
        long t0 = ZonedDateTime.now(STOCKHOLM).withHour(9).withMinute(30).toEpochSecond();

        try {
            // One winning round trip today: 100 → 102 = +2% gross
            created.add(intradaySignalRepository.save(new IntradaySignal(
                    "OpeningRangeBreakoutIntradayStrategy", "TEST-PNL.ST", t0, "BUY", new BigDecimal("100"))));
            created.add(intradaySignalRepository.save(new IntradaySignal(
                    "OpeningRangeBreakoutIntradayStrategy", "TEST-PNL.ST", t0 + 900, "SELL", new BigDecimal("102"))));
            // One losing round trip today: 200 → 199 = -0.5% gross
            created.add(intradaySignalRepository.save(new IntradaySignal(
                    "GapReversalIntradayStrategy", "TEST-PNL.ST", t0 + 1800, "BUY", new BigDecimal("200"))));
            created.add(intradaySignalRepository.save(new IntradaySignal(
                    "GapReversalIntradayStrategy", "TEST-PNL.ST", t0 + 2700, "SELL", new BigDecimal("199"))));

            snapshot = portfolioService.buildIntraday();

            // Net = (2% - 0.06%) + (-0.5% - 0.06%) = 1.94 - 0.56 = 1.38
            assertEquals(2, snapshot.getClosedTradeCount());
            assertEquals(0, snapshot.getRealizedPnlPercent().compareTo(new BigDecimal("1.3800")),
                    "realized=" + snapshot.getRealizedPnlPercent());
            // Total = realized + unrealized open sum; with no open positions they are equal
            assertTrue(snapshot.getTotalPnlPercent().compareTo(snapshot.getRealizedPnlPercent()) >= 0
                            || snapshot.getOpenPositionCount() > 0,
                    "total=" + snapshot.getTotalPnlPercent());
        } finally {
            intradaySignalRepository.deleteAll(created);
            if (snapshot != null) {
                portfolioRepository.deleteById(snapshot.getId());
            }
        }
    }
}
