package nu.itark.frosk.crypto.livetrading;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import nu.itark.frosk.crypto.kraken.KrakenFuturesHttpClient;
import nu.itark.frosk.crypto.kraken.KrakenFuturesOrderClient;
import nu.itark.frosk.repo.LiveOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Bug 4 end to end: LiveTradingGate wired to the REAL KrakenFuturesOrderClient
 * (only HTTP mocked), so the safety check runs on the balance parsed from
 * Kraken's keyed /accounts object. Before the fix the balance was always 0 and
 * canTrade refused everything — including orders that should pass.
 */
public class TestJLiveTradingGateKrakenBalance {

    private KrakenFuturesHttpClient http;
    private LiveOrderRepository repo;
    private LiveTradingGate gate;

    @BeforeEach
    void setUp() {
        http = mock(KrakenFuturesHttpClient.class);
        repo = mock(LiveOrderRepository.class);
        when(repo.sumEurLossSince(any())).thenReturn(BigDecimal.ZERO);

        KrakenFuturesOrderClient client = new KrakenFuturesOrderClient();
        ReflectionTestUtils.setField(client, "httpClient", http);

        gate = new LiveTradingGate();
        ReflectionTestUtils.setField(gate, "brokerOrderClient", client);
        ReflectionTestUtils.setField(gate, "liveOrderRepository", repo);
        ReflectionTestUtils.setField(gate, "enabled", true);
        ReflectionTestUtils.setField(gate, "positionPctOfEquity", new BigDecimal("0.05"));
        ReflectionTestUtils.setField(gate, "minPositionEur", new BigDecimal("25"));
        ReflectionTestUtils.setField(gate, "maxPositionEur", new BigDecimal("500"));
        ReflectionTestUtils.setField(gate, "maxDailyLossEur", new BigDecimal("2000"));
        ReflectionTestUtils.setField(gate, "maxTotalExposurePct", new BigDecimal("0.5"));
    }

    private void givenFlexAvailableMargin(String margin) throws Exception {
        JsonNode accounts = new ObjectMapper().readTree("""
                {"result":"success","accounts":{
                  "cash":{"type":"cashAccount","balances":{"xbt":0}},
                  "fi_xbtusd":{"type":"marginAccount","balanceValue":0},
                  "flex":{"type":"multiCollateralMarginAccount","availableMargin":%s}}}""".formatted(margin));
        when(http.get(eq("/accounts"), eq(JsonNode.class))).thenReturn(accounts);
    }

    @Test
    void fundedAccount_allowsAnOrderWithinLimits() throws Exception {
        givenFlexAvailableMargin("1000");
        when(repo.sumOpenExposureEur()).thenReturn(BigDecimal.ZERO);

        assertEquals(0, new BigDecimal("1000").compareTo(gate.computeEquity()));
        assertTrue(gate.canTrade("PF_XBTUSD", new BigDecimal("250")));
    }

    @Test
    void orderLargerThanAvailableMargin_isRefused() throws Exception {
        givenFlexAvailableMargin("100");
        when(repo.sumOpenExposureEur()).thenReturn(BigDecimal.ZERO);

        // 50 % of equity 100 = 50 exposure cap → a 60 order already trips the cap.
        assertFalse(gate.canTrade("PF_XBTUSD", new BigDecimal("60")));
    }

    @Test
    void openShortExposure_countsTowardsTheCap() throws Exception {
        // Equity = 1000 margin + 800 open exposure (shorts now included) = 1800; cap = 900.
        givenFlexAvailableMargin("1000");
        when(repo.sumOpenExposureEur()).thenReturn(new BigDecimal("800"));

        assertFalse(gate.canTrade("PF_XBTUSD", new BigDecimal("250")), "800 open + 250 new > 900 cap");
    }

    @Test
    void krakenAuthError_failsSafe() throws Exception {
        when(http.get(eq("/accounts"), eq(JsonNode.class)))
                .thenReturn(new ObjectMapper().readTree("{\"result\":\"error\",\"error\":\"authenticationError\"}"));
        when(repo.sumOpenExposureEur()).thenReturn(BigDecimal.ZERO);

        assertFalse(gate.canTrade("PF_XBTUSD", new BigDecimal("25")));
    }
}
