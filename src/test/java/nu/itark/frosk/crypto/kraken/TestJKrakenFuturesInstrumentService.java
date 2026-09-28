package nu.itark.frosk.crypto.kraken;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Bug 2: PF_* sizing in base-asset units at each instrument's precision.
 * Precisions and prices mirror the live /instruments and /tickers values
 * observed 2026-09-21.
 */
public class TestJKrakenFuturesInstrumentService {

    private KrakenFuturesInstrumentService service;

    @BeforeEach
    void setUp() throws Exception {
        KrakenFuturesHttpClient http = mock(KrakenFuturesHttpClient.class);
        JsonNode instruments = new ObjectMapper().readTree("""
                {"result":"success","instruments":[
                  {"symbol":"PF_XBTUSD","type":"flexible_futures","contractValueTradePrecision":4},
                  {"symbol":"PF_ETHUSD","type":"flexible_futures","contractValueTradePrecision":3},
                  {"symbol":"PF_PENGUUSD","type":"flexible_futures","contractValueTradePrecision":-1},
                  {"symbol":"PF_PRICEYUSD","type":"flexible_futures","contractValueTradePrecision":0}
                ]}""");
        when(http.getPublic(eq("/instruments"), eq(JsonNode.class))).thenReturn(instruments);
        service = new KrakenFuturesInstrumentService(http);
    }

    private static BigDecimal bd(String v) { return new BigDecimal(v); }

    @Test
    void btc_250usd_isFractionalBtc_notZero_andNot250Btc() {
        KrakenFuturesInstrumentService.Sizing s = service.sizeEntry("PF_XBTUSD", bd("250"), bd("85693.71"));

        assertTrue(s.isOk());
        assertEquals(bd("0.0029"), s.size()); // 0.002917… rounded DOWN to 4 dp ≈ 248.51 USD
        assertTrue(s.size().multiply(bd("85693.71")).compareTo(bd("250")) <= 0, "must not exceed budget");
    }

    @Test
    void eth_roundsDownToThreeDecimals() {
        KrakenFuturesInstrumentService.Sizing s = service.sizeEntry("PF_ETHUSD", bd("250"), bd("2753.52"));

        assertEquals(bd("0.090"), s.size()); // 0.09079… → 0.090
    }

    @Test
    void negativePrecision_sizesInMultiplesOfTen() {
        KrakenFuturesInstrumentService.Sizing s = service.sizeEntry("PF_PENGUUSD", bd("250"), bd("0.0086"));

        assertEquals("29060", s.size().toPlainString()); // 29069.7… → 29060
    }

    @Test
    void budgetBelowOneStep_usesOneMinimumStep() {
        KrakenFuturesInstrumentService.Sizing s = service.sizeEntry("PF_XBTUSD", bd("5"), bd("85693.71"));

        assertEquals(bd("0.0001"), s.size()); // ≈ 8.57 USD, the smallest tradeable BTC order
    }

    @Test
    void oneStepFarAboveBudget_isRefused() {
        // Whole-unit instrument at 1000 USD with a 25 USD budget: one step is 40× the budget.
        KrakenFuturesInstrumentService.Sizing s = service.sizeEntry("PF_PRICEYUSD", bd("25"), bd("1000"));

        assertFalse(s.isOk());
        assertNull(s.size());
    }

    @Test
    void unknownInstrument_isRefused_neverGuessed() {
        KrakenFuturesInstrumentService.Sizing s = service.sizeEntry("PF_NOPEUSD", bd("250"), bd("10"));

        assertFalse(s.isOk());
        assertTrue(s.refusal().contains("PF_NOPEUSD"));
    }

    @Test
    void exitSize_keepsAnOnGridFillExactly() {
        // As stored in live_order.filled_quantity (scale 8).
        assertEquals("0.0029", service.exitSize("PF_XBTUSD", bd("0.00290000")).toPlainString());
        assertEquals("29060", service.exitSize("PF_PENGUUSD", bd("29060.00000000")).toPlainString());
    }

    @Test
    void exitSize_offGrid_roundsDown_neverOvershoots() {
        assertEquals("0.0029", service.exitSize("PF_XBTUSD", bd("0.00299")).toPlainString());
    }
}
