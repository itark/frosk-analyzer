package nu.itark.frosk.crypto.livetrading;

import nu.itark.frosk.broker.BrokerOrderClient;
import nu.itark.frosk.repo.LiveOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Live side of the per-ticker exposure cap — mirrors
 * {@code TestJCryptoPaperTradingServiceTickerCap} case for case so paper and live
 * stay in sync.
 *
 * <p>Equity 2000 EUR (1855 cash + 145 open), cap 10% = 200 EUR per ticker.
 */
public class TestJLiveTradingGateTickerCap {

    private static final String TICKER = "TRAC-USD";

    private LiveOrderRepository repo;
    private LiveTradingGate gate;

    @BeforeEach
    void setUp() {
        repo = mock(LiveOrderRepository.class);
        BrokerOrderClient broker = mock(BrokerOrderClient.class);
        when(broker.getAvailableBalance()).thenReturn(new BigDecimal("1855"));
        when(repo.sumOpenExposureEur()).thenReturn(new BigDecimal("145"));
        when(repo.sumEurLossSince(any())).thenReturn(BigDecimal.ZERO);

        gate = new LiveTradingGate();
        ReflectionTestUtils.setField(gate, "brokerOrderClient", broker);
        ReflectionTestUtils.setField(gate, "liveOrderRepository", repo);
        ReflectionTestUtils.setField(gate, "enabled", true);
        ReflectionTestUtils.setField(gate, "positionPctOfEquity", new BigDecimal("0.07"));
        ReflectionTestUtils.setField(gate, "minPositionEur", new BigDecimal("25"));
        ReflectionTestUtils.setField(gate, "maxPositionEur", new BigDecimal("500"));
        ReflectionTestUtils.setField(gate, "maxDailyLossEur", new BigDecimal("2000"));
        ReflectionTestUtils.setField(gate, "maxTotalExposurePct", new BigDecimal("0.5"));
        ReflectionTestUtils.setField(gate, "maxTickerExposurePct", new BigDecimal("0.10"));
    }

    @Test
    void fullSizeWhenTheTickerHasNoOpenExposure() {
        when(repo.sumOpenExposureEurByTicker(TICKER)).thenReturn(BigDecimal.ZERO);
        assertEquals(0, new BigDecimal("140").compareTo(gate.capToTickerExposure(TICKER, new BigDecimal("140"))));
    }

    @Test
    void scaledDownToTheRoomLeftUnderTheCap() {
        when(repo.sumOpenExposureEurByTicker(TICKER)).thenReturn(new BigDecimal("145"));
        assertEquals(0, new BigDecimal("55").compareTo(gate.capToTickerExposure(TICKER, new BigDecimal("140"))));
    }

    @Test
    void skippedWhenTheRoomIsBelowTheMinimumPosition() {
        when(repo.sumOpenExposureEurByTicker(TICKER)).thenReturn(new BigDecimal("180"));
        assertNull(gate.capToTickerExposure(TICKER, new BigDecimal("140")));
    }

    @Test
    void canTradeRefusesAnOrderThatWouldBreachTheTickerCap() {
        when(repo.sumOpenExposureEurByTicker(TICKER)).thenReturn(new BigDecimal("145"));
        assertTrue(gate.canTrade(TICKER, new BigDecimal("55")), "145 + 55 = 200 sits exactly on the cap");
        assertFalse(gate.canTrade(TICKER, new BigDecimal("56")), "145 + 56 > 200");
    }

    @Test
    void disabledCapNeverQueriesTickerExposure() {
        ReflectionTestUtils.setField(gate, "maxTickerExposurePct", null);
        assertEquals(0, new BigDecimal("140").compareTo(gate.capToTickerExposure(TICKER, new BigDecimal("140"))));
        assertTrue(gate.canTrade(TICKER, new BigDecimal("140")));
        verify(repo, never()).sumOpenExposureEurByTicker(any());
    }
}
