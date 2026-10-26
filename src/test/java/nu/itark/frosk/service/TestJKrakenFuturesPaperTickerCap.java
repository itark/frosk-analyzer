package nu.itark.frosk.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import nu.itark.frosk.crypto.kraken.KrakenFuturesHttpClient;
import nu.itark.frosk.crypto.kraken.KrakenFuturesInstrumentService;
import nu.itark.frosk.model.KrakenFuturesPaperAccount;
import nu.itark.frosk.model.KrakenFuturesPaperOrder;
import nu.itark.frosk.repo.KrakenFuturesPaperAccountRepository;
import nu.itark.frosk.repo.KrakenFuturesPaperOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Per-symbol exposure cap ({@code kraken.futures.paper.max.ticker.exposure.pct}) in
 * {@link KrakenFuturesPaperTradingService} — the Kraken mirror of
 * {@code TestJCryptoPaperTradingServiceTickerCap} and {@code TestJLiveTradingGateTickerCap}.
 *
 * <p>Collateral 2000 USD (available + open), base size 5% = 100 USD, cap 10% = 200 USD.
 * Mark price 10 on a 2-decimal instrument, so the opened notional equals the budget.
 */
public class TestJKrakenFuturesPaperTickerCap {

    private static final String SYMBOL = "PF_TAOUSD";
    private static final String STRATEGY = "CryptoVWAPReversionIntradayStrategy";

    private KrakenFuturesPaperTradingService service;
    private KrakenFuturesPaperOrderRepository orderRepo;
    private KrakenFuturesPaperAccount account;

    @BeforeEach
    void setUp() throws Exception {
        KrakenFuturesHttpClient http = mock(KrakenFuturesHttpClient.class);
        when(http.getPublic(eq("/instruments"), eq(JsonNode.class))).thenReturn(new ObjectMapper().readTree("""
                {"result":"success","instruments":[
                  {"symbol":"PF_TAOUSD","contractValueTradePrecision":2}]}"""));

        orderRepo = mock(KrakenFuturesPaperOrderRepository.class);
        when(orderRepo.findByStatusOrderByCreatedAtDesc(any())).thenReturn(List.of());

        account = new KrakenFuturesPaperAccount();
        account.setInitCollateralUsd(new BigDecimal("2000"));
        KrakenFuturesPaperAccountRepository accountRepo = mock(KrakenFuturesPaperAccountRepository.class);
        when(accountRepo.findAll()).thenReturn(List.of(account));

        RiskManagementService risk = mock(RiskManagementService.class);
        when(risk.checkEntry(any(), any())).thenReturn(new RiskCheckResult(true, null));

        service = new KrakenFuturesPaperTradingService();
        ReflectionTestUtils.setField(service, "orderRepository", orderRepo);
        ReflectionTestUtils.setField(service, "accountRepository", accountRepo);
        ReflectionTestUtils.setField(service, "riskManagementService", risk);
        ReflectionTestUtils.setField(service, "instrumentService", new KrakenFuturesInstrumentService(http));
        ReflectionTestUtils.setField(service, "positionPctOfCollateral", new BigDecimal("0.05"));
        ReflectionTestUtils.setField(service, "minPositionUsd", new BigDecimal("25"));
        ReflectionTestUtils.setField(service, "maxPositionUsd", new BigDecimal("1000"));
        ReflectionTestUtils.setField(service, "maxTotalExposurePct", new BigDecimal("0.5"));
        ReflectionTestUtils.setField(service, "maxTickerExposurePct", new BigDecimal("0.10"));
        ReflectionTestUtils.setField(service, "takerFeeFraction", new BigDecimal("0.0005"));
    }

    /** {@code open} USD already deployed in {@link #SYMBOL}; collateral stays 2000 in total. */
    private void givenOpenInSymbol(String open) {
        BigDecimal openUsd = new BigDecimal(open);
        account.setCollateralUsd(new BigDecimal("2000").subtract(openUsd));
        when(orderRepo.sumOpenExposureUsd()).thenReturn(openUsd);
        when(orderRepo.sumOpenExposureUsdBySymbol(SYMBOL)).thenReturn(openUsd);
    }

    private BigDecimal openedUsd() {
        ArgumentCaptor<KrakenFuturesPaperOrder> captor = ArgumentCaptor.forClass(KrakenFuturesPaperOrder.class);
        verify(orderRepo).save(captor.capture());
        return captor.getValue().getUsdAmount();
    }

    @Test
    void fullSizeWhenTheSymbolHasNoOpenExposure() {
        givenOpenInSymbol("0");
        service.dispatchLong(STRATEGY, SYMBOL, new BigDecimal("10"), null);
        assertEquals(0, new BigDecimal("100").compareTo(openedUsd()));
    }

    @Test
    void scaledDownToTheRoomLeftUnderTheCap() {
        givenOpenInSymbol("150");
        service.dispatchLong(STRATEGY, SYMBOL, new BigDecimal("10"), null);
        assertEquals(0, new BigDecimal("50").compareTo(openedUsd()));
    }

    @Test
    void shortsShareTheSameSymbolCap() {
        givenOpenInSymbol("150");
        service.dispatchShort(STRATEGY, SYMBOL, new BigDecimal("10"), null);
        assertEquals(0, new BigDecimal("50").compareTo(openedUsd()));
    }

    @Test
    void skippedWhenTheRoomIsBelowTheMinimumPosition() {
        givenOpenInSymbol("180");
        service.dispatchLong(STRATEGY, SYMBOL, new BigDecimal("10"), null);
        verify(orderRepo, never()).save(any());
    }

    @Test
    void disabledCapNeverQueriesSymbolExposure() {
        ReflectionTestUtils.setField(service, "maxTickerExposurePct", null);
        account.setCollateralUsd(new BigDecimal("2000"));
        when(orderRepo.sumOpenExposureUsd()).thenReturn(BigDecimal.ZERO);
        service.dispatchLong(STRATEGY, SYMBOL, new BigDecimal("10"), null);
        assertEquals(0, new BigDecimal("100").compareTo(openedUsd()));
        verify(orderRepo, never()).sumOpenExposureUsdBySymbol(any());
    }
}
