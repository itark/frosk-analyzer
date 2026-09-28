package nu.itark.frosk.controller;

import nu.itark.frosk.broker.BrokerOrderClient;
import nu.itark.frosk.crypto.livetrading.LiveTradingGate;
import nu.itark.frosk.repo.LiveOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Kill switch: disable always works, enable needs confirmation and a clean order book. */
public class TestJKrakenLiveTradingController {

    private LiveTradingGate gate;
    private LiveOrderRepository repo;
    private KrakenLiveTradingController controller;
    private final MockHttpServletRequest request = new MockHttpServletRequest();

    @BeforeEach
    void setUp() {
        repo = mock(LiveOrderRepository.class);
        BrokerOrderClient broker = mock(BrokerOrderClient.class);
        when(broker.getAvailableBalance()).thenReturn(new BigDecimal("1000"));
        when(repo.sumOpenExposureEur()).thenReturn(BigDecimal.ZERO);

        gate = new LiveTradingGate(); // real gate: the test checks the switch actually flips
        ReflectionTestUtils.setField(gate, "liveOrderRepository", repo);
        ReflectionTestUtils.setField(gate, "brokerOrderClient", broker);

        controller = new KrakenLiveTradingController(gate, broker, repo);
    }

    private static KrakenLiveTradingController.EnableRequest confirm(String phrase) {
        return new KrakenLiveTradingController.EnableRequest(phrase);
    }

    @Test
    void enable_withConfirmation_turnsTradingOn() {
        ResponseEntity<Map<String, Object>> r = controller.enable(confirm("ENABLE_LIVE_TRADING"), request);

        assertEquals(200, r.getStatusCode().value());
        assertTrue(gate.isEnabled());
        assertEquals(true, r.getBody().get("enabled"));
    }

    @Test
    void enable_withoutOrWithWrongConfirmation_isRefused_andStaysOff() {
        assertEquals(400, controller.enable(null, request).getStatusCode().value());
        assertEquals(400, controller.enable(confirm("yes"), request).getStatusCode().value());
        assertFalse(gate.isEnabled());
    }

    @Test
    void enable_isRefused_whileAnyOrderIsUnresolved() {
        when(repo.countByStatus("UNRESOLVED")).thenReturn(1L);

        ResponseEntity<Map<String, Object>> r = controller.enable(confirm("ENABLE_LIVE_TRADING"), request);

        assertEquals(409, r.getStatusCode().value());
        assertFalse(gate.isEnabled());
        assertTrue(r.getBody().get("error").toString().contains("UNRESOLVED"));
    }

    @Test
    void disable_alwaysWorks_evenWithUnresolvedOrders() {
        gate.setEnabled(true);
        when(repo.countByStatus("UNRESOLVED")).thenReturn(3L);

        Map<String, Object> body = controller.disable(request);

        assertFalse(gate.isEnabled());
        assertEquals(false, body.get("enabled"));
    }

    @Test
    void status_reportsSwitchAndRiskFigures() {
        when(repo.countByStatus("PENDING")).thenReturn(2L);

        Map<String, Object> s = controller.status();

        assertEquals(false, s.get("enabled"));
        assertEquals(new BigDecimal("1000"), s.get("availableMarginUsd"));
        assertEquals(2L, s.get("pendingOrders"));
        assertEquals(0L, s.get("unresolvedOrders"));
    }
}
