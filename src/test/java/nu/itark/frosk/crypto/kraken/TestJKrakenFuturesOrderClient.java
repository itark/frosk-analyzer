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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Bugs 1–3 plus the balance parse, against mocked Kraken responses. No real
 * request is made.
 */
public class TestJKrakenFuturesOrderClient {

    private static final ObjectMapper JSON = new ObjectMapper();

    private KrakenFuturesHttpClient http;
    private KrakenFuturesOrderClient client;

    @BeforeEach
    void setUp() throws Exception {
        http = mock(KrakenFuturesHttpClient.class);
        when(http.getPublic(eq("/instruments"), eq(JsonNode.class))).thenReturn(json("""
                {"result":"success","instruments":[{"symbol":"PF_XBTUSD","contractValueTradePrecision":4}]}"""));
        // Single-symbol ticker: ONE "ticker" object (the old parse expected a "tickers" list).
        when(http.getPublic(eq("/tickers/PF_XBTUSD"), eq(JsonNode.class))).thenReturn(json("""
                {"result":"success","serverTime":"2026-09-21T12:00:00Z",
                 "ticker":{"symbol":"PF_XBTUSD","markPrice":85693.71,"last":85690}}"""));

        client = new KrakenFuturesOrderClient();
        ReflectionTestUtils.setField(client, "httpClient", http);
        ReflectionTestUtils.setField(client, "instrumentService", new KrakenFuturesInstrumentService(http));
        ReflectionTestUtils.setField(client, "fillPollAttempts", 2);
        ReflectionTestUtils.setField(client, "fillPollDelayMs", 0L);
    }

    private static JsonNode json(String s) throws Exception {
        return JSON.readTree(s);
    }

    @SuppressWarnings("unchecked")
    private MultiValueMap<String, String> sentParams() {
        ArgumentCaptor<MultiValueMap<String, String>> captor = ArgumentCaptor.forClass(MultiValueMap.class);
        verify(http).post(eq("/sendorder"), captor.capture(), eq(JsonNode.class));
        return captor.getValue();
    }

    // ── Bug 1 ─────────────────────────────────────────────────────────────

    @Test
    void markPrice_isReadFromSingleTickerObject() {
        assertEquals(new BigDecimal("85693.71"), client.getMarkPrice("PF_XBTUSD"));
    }

    // ── Bug 2 + Bug 3 (fill in the send response) ─────────────────────────

    @Test
    void entry_sendsFractionalBtcSize_andRecordsExecutedFill() throws Exception {
        when(http.post(eq("/sendorder"), any(), eq(JsonNode.class))).thenReturn(json("""
                {"result":"success","sendStatus":{"order_id":"ord-1","status":"placed","orderEvents":[
                  {"type":"EXECUTION","price":85700.0,"amount":0.0020},
                  {"type":"EXECUTION","price":85710.0,"amount":0.0009}]}}"""));

        OrderResponse r = client.placeShortEntry("PF_XBTUSD", new BigDecimal("250"));

        MultiValueMap<String, String> p = sentParams();
        assertEquals("0.0029", p.getFirst("size"));
        assertEquals("sell", p.getFirst("side"));
        assertNull(p.getFirst("reduceOnly"));

        assertEquals("FILLED", r.getStatus());
        assertEquals("ord-1", r.getOrderId());
        assertEquals(0, new BigDecimal("0.0029").compareTo(r.getFilledSize()));
        // size-weighted: (85700×0.0020 + 85710×0.0009) / 0.0029 = 85703.10…
        assertEquals(0, new BigDecimal("85703.10344828").compareTo(r.getAverageFilledPrice()));
    }

    // ── Bug 3 (fill found via /fills polling) ─────────────────────────────

    @Test
    void placedWithoutExecution_isResolvedFromFills() throws Exception {
        when(http.post(eq("/sendorder"), any(), eq(JsonNode.class))).thenReturn(json("""
                {"result":"success","sendStatus":{"order_id":"ord-2","status":"placed","orderEvents":[]}}"""));
        when(http.get(eq("/fills"), eq(JsonNode.class))).thenReturn(json("""
                {"result":"success","fills":[
                  {"order_id":"someone-else","price":1,"size":1},
                  {"order_id":"ord-2","price":85650.5,"size":0.0029}]}"""));

        OrderResponse r = client.placeLongEntry("PF_XBTUSD", new BigDecimal("250"));

        assertEquals("FILLED", r.getStatus());
        assertEquals(0, new BigDecimal("0.0029").compareTo(r.getFilledSize()));
        assertEquals(0, new BigDecimal("85650.5").compareTo(r.getAverageFilledPrice()));
    }

    @Test
    void placedButNoFillAnywhere_isUnconfirmed_neverFilledWithoutQuantity() throws Exception {
        when(http.post(eq("/sendorder"), any(), eq(JsonNode.class))).thenReturn(json("""
                {"result":"success","sendStatus":{"order_id":"ord-3","status":"placed"}}"""));
        when(http.get(eq("/fills"), eq(JsonNode.class))).thenReturn(json("""
                {"result":"success","fills":[]}"""));

        OrderResponse r = client.placeLongEntry("PF_XBTUSD", new BigDecimal("250"));

        assertEquals(KrakenFuturesOrderClient.STATUS_UNCONFIRMED, r.getStatus());
        assertEquals("ord-3", r.getOrderId());
        assertNull(r.getFilledSize());
    }

    @Test
    void rejectedWithoutExecution_isFailed() throws Exception {
        when(http.post(eq("/sendorder"), any(), eq(JsonNode.class))).thenReturn(json("""
                {"result":"success","sendStatus":{"status":"insufficientAvailableFunds"}}"""));

        OrderResponse r = client.placeLongEntry("PF_XBTUSD", new BigDecimal("250"));

        assertEquals("FAILED", r.getStatus());
        assertEquals("insufficientAvailableFunds", r.getErrorMessage());
        verify(http, never()).get(eq("/fills"), any());
    }

    @Test
    void anyExecution_isFilled_evenWithANonPlacedStatus() throws Exception {
        // Money moved — this must never be booked as FAILED.
        when(http.post(eq("/sendorder"), any(), eq(JsonNode.class))).thenReturn(json("""
                {"result":"success","sendStatus":{"order_id":"ord-4","status":"partiallyFilled","orderEvents":[
                  {"type":"EXECUTION","price":85700,"amount":0.0010}]}}"""));

        OrderResponse r = client.placeLongEntry("PF_XBTUSD", new BigDecimal("250"));

        assertEquals("FILLED", r.getStatus());
        assertEquals(0, new BigDecimal("0.0010").compareTo(r.getFilledSize()));
    }

    // ── exit ──────────────────────────────────────────────────────────────

    @Test
    void exit_closesExactFilledSize_reduceOnly() throws Exception {
        when(http.post(eq("/sendorder"), any(), eq(JsonNode.class))).thenReturn(json("""
                {"result":"success","sendStatus":{"order_id":"ord-5","status":"placed","orderEvents":[
                  {"type":"EXECUTION","price":85000,"amount":0.0029}]}}"""));

        OrderResponse r = client.placeShortExit("PF_XBTUSD", new BigDecimal("0.00290000"));

        MultiValueMap<String, String> p = sentParams();
        assertEquals("0.0029", p.getFirst("size"));   // previously rounded to 0 — position never closed
        assertEquals("buy", p.getFirst("side"));
        assertEquals("true", p.getFirst("reduceOnly"));
        assertEquals("FILLED", r.getStatus());
    }

    @Test
    void entry_refusedWithoutSending_whenMarkPriceMissing() throws Exception {
        when(http.getPublic(eq("/tickers/PF_XBTUSD"), eq(JsonNode.class))).thenReturn(json("""
                {"result":"error","error":"unavailable"}"""));

        OrderResponse r = client.placeLongEntry("PF_XBTUSD", new BigDecimal("250"));

        assertEquals("FAILED", r.getStatus());
        verify(http, never()).post(any(), any(), any());
    }

    // ── balance ───────────────────────────────────────────────────────────

    @Test
    void availableBalance_readsFlexAccountFromKeyedObject() throws Exception {
        when(http.get(eq("/accounts"), eq(JsonNode.class))).thenReturn(json("""
                {"result":"success","accounts":{
                  "cash":{"type":"cashAccount","balances":{"xbt":0}},
                  "flex":{"type":"multiCollateralMarginAccount","availableMargin":1234.56,"portfolioValue":1300}}}"""));

        assertEquals(new BigDecimal("1234.56"), client.getAvailableBalance());
    }

    @Test
    void availableBalance_isZero_onAuthError() throws Exception {
        when(http.get(eq("/accounts"), eq(JsonNode.class))).thenReturn(json("""
                {"result":"error","error":"authenticationError"}"""));

        assertTrue(client.getAvailableBalance().signum() == 0);
    }
}
