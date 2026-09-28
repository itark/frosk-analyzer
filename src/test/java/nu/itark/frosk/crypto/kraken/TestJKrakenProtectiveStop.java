package nu.itark.frosk.crypto.kraken;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import nu.itark.frosk.crypto.livetrading.OrderResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.MultiValueMap;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The exchange-side stop order: trigger price, side, tick alignment and cancel.
 * Instrument data matches the live /instruments values (PF_XBTUSD tickSize 1,
 * PF_XTZUSD 0.0001).
 */
public class TestJKrakenProtectiveStop {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final BigDecimal STOP_PCT = new BigDecimal("2.7");

    private KrakenFuturesHttpClient http;
    private KrakenFuturesOrderClient client;

    @BeforeEach
    void setUp() throws Exception {
        http = mock(KrakenFuturesHttpClient.class);
        when(http.getPublic(eq("/instruments"), eq(JsonNode.class))).thenReturn(JSON.readTree("""
                {"result":"success","instruments":[
                  {"symbol":"PF_XBTUSD","contractValueTradePrecision":4,"tickSize":1},
                  {"symbol":"PF_XTZUSD","contractValueTradePrecision":0,"tickSize":0.0001},
                  {"symbol":"PF_NOTICKUSD","contractValueTradePrecision":0}]}"""));
        when(http.post(eq("/sendorder"), any(), eq(JsonNode.class))).thenReturn(JSON.readTree("""
                {"result":"success","sendStatus":{"order_id":"stop-1","status":"placed","orderEvents":[]}}"""));

        client = new KrakenFuturesOrderClient();
        ReflectionTestUtils.setField(client, "httpClient", http);
        ReflectionTestUtils.setField(client, "instrumentService", new KrakenFuturesInstrumentService(http));
        ReflectionTestUtils.setField(client, "triggerSignal", "mark");
    }

    @SuppressWarnings("unchecked")
    private MultiValueMap<String, String> sentParams() {
        ArgumentCaptor<MultiValueMap<String, String>> captor = ArgumentCaptor.forClass(MultiValueMap.class);
        verify(http).post(eq("/sendorder"), captor.capture(), eq(JsonNode.class));
        return captor.getValue();
    }

    @Test
    void longStop_sitsBelowEntry_andClosesWithASell() {
        OrderResponse r = client.placeProtectiveStop("PF_XBTUSD", true, new BigDecimal("0.0029"),
                new BigDecimal("85693.71"), STOP_PCT);

        MultiValueMap<String, String> p = sentParams();
        assertEquals("stp", p.getFirst("orderType"));
        assertEquals("sell", p.getFirst("side"));
        assertEquals("0.0029", p.getFirst("size"));
        assertEquals("true", p.getFirst("reduceOnly"));
        assertEquals("mark", p.getFirst("triggerSignal"));
        // 85693.71 × 0.973 = 83379.9... → floored to the 1.0 tick grid
        assertEquals("83379", p.getFirst("stopPrice"));
        assertEquals("PLACED", r.getStatus());
        assertEquals("stop-1", r.getOrderId());
    }

    @Test
    void shortStop_sitsAboveEntry_andClosesWithABuy() {
        client.placeProtectiveStop("PF_XBTUSD", false, new BigDecimal("0.0029"),
                new BigDecimal("85693.71"), STOP_PCT);

        MultiValueMap<String, String> p = sentParams();
        assertEquals("buy", p.getFirst("side"));
        // 85693.71 × 1.027 = 88007.4... → ceiled to the tick grid
        assertEquals("88008", p.getFirst("stopPrice"));
    }

    @Test
    void stopPriceIsSnappedToTheInstrumentsTickGrid() {
        client.placeProtectiveStop("PF_XTZUSD", true, new BigDecimal("724"),
                new BigDecimal("0.3450"), STOP_PCT);

        // 0.3450 × 0.973 = 0.3356850 → floored to 0.0001
        assertEquals("0.3356", sentParams().getFirst("stopPrice"));
    }

    @Test
    void unknownTickSize_refusesRatherThanSendingAPriceKrakenWouldReject() {
        OrderResponse r = client.placeProtectiveStop("PF_NOTICKUSD", true, BigDecimal.ONE,
                new BigDecimal("10"), STOP_PCT);

        assertEquals("FAILED", r.getStatus());
        assertNull(r.getOrderId());
        verify(http, never()).post(any(), any(), any());
    }

    @Test
    void rejectedStop_isReportedFailed() throws Exception {
        when(http.post(eq("/sendorder"), any(), eq(JsonNode.class))).thenReturn(JSON.readTree("""
                {"result":"success","sendStatus":{"status":"wouldNotReducePosition"}}"""));

        OrderResponse r = client.placeProtectiveStop("PF_XBTUSD", true, new BigDecimal("0.0029"),
                new BigDecimal("85693.71"), STOP_PCT);

        assertEquals("FAILED", r.getStatus());
        assertEquals("wouldNotReducePosition", r.getErrorMessage());
    }

    @Test
    void cancel_treatsCancelledFilledAndNotFoundAsGone() throws Exception {
        for (String status : new String[]{"cancelled", "filled", "notFound"}) {
            KrakenFuturesHttpClient h = mock(KrakenFuturesHttpClient.class);
            when(h.post(eq("/cancelorder"), any(), eq(JsonNode.class)))
                    .thenReturn(JSON.readTree("{\"cancelStatus\":{\"status\":\"" + status + "\"}}"));
            KrakenFuturesOrderClient c = new KrakenFuturesOrderClient();
            ReflectionTestUtils.setField(c, "httpClient", h);
            assertTrue(c.cancelOrder("stop-1"), status + " should count as no resting stop left");
        }
    }

    @Test
    void cancel_reportsFailureWhenKrakenDoesNotConfirm() throws Exception {
        when(http.post(eq("/cancelorder"), any(), eq(JsonNode.class)))
                .thenReturn(JSON.readTree("{\"cancelStatus\":{\"status\":\"error\"}}"));

        assertFalse(client.cancelOrder("stop-1"));
    }
}
