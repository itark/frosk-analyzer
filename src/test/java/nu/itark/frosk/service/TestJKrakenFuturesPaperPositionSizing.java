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
import java.math.RoundingMode;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Paper positions are sized like live orders: base-asset units at the
 * instrument's precision. Previously the size was rounded to whole units, so a
 * BTC or ETH paper position opened with 0 contracts and booked fees only.
 */
public class TestJKrakenFuturesPaperPositionSizing {

    private KrakenFuturesPaperTradingService service;
    private KrakenFuturesPaperOrderRepository orderRepo;
    private KrakenFuturesPaperAccount account;

    @BeforeEach
    void setUp() throws Exception {
        KrakenFuturesHttpClient http = mock(KrakenFuturesHttpClient.class);
        when(http.getPublic(eq("/instruments"), eq(JsonNode.class))).thenReturn(new ObjectMapper().readTree("""
                {"result":"success","instruments":[
                  {"symbol":"PF_XBTUSD","contractValueTradePrecision":4},
                  {"symbol":"PF_TAOUSD","contractValueTradePrecision":2},
                  {"symbol":"PF_XTZUSD","contractValueTradePrecision":0}]}"""));

        orderRepo = mock(KrakenFuturesPaperOrderRepository.class);
        when(orderRepo.sumOpenExposureUsd()).thenReturn(BigDecimal.ZERO);
        when(orderRepo.findByStatusOrderByCreatedAtDesc(any())).thenReturn(List.of());

        account = new KrakenFuturesPaperAccount();
        account.setInitCollateralUsd(new BigDecimal("5000"));
        account.setCollateralUsd(new BigDecimal("5000"));
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
        ReflectionTestUtils.setField(service, "takerFeeFraction", new BigDecimal("0.0005"));
    }

    /** 5 % of 5000 collateral = 250 USD per position. */
    private KrakenFuturesPaperOrder open(String symbol, String markPrice) {
        service.dispatchLong("CryptoVWAPReversionIntradayStrategy", symbol, new BigDecimal(markPrice), null);
        ArgumentCaptor<KrakenFuturesPaperOrder> captor = ArgumentCaptor.forClass(KrakenFuturesPaperOrder.class);
        verify(orderRepo).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void btcPosition_isFractional_notZero() {
        KrakenFuturesPaperOrder o = open("PF_XBTUSD", "85693.71");

        assertEquals(new BigDecimal("0.0029"), o.getContracts());
        assertTrue(o.getContracts().signum() > 0, "a BTC paper position used to round to 0 contracts");
    }

    @Test
    void notionalMatchesTheSizeActuallyOpened() {
        // The recorded usd_amount must be size × price, not the pre-rounding budget —
        // paper PnL is computed on the size, fees on the notional.
        KrakenFuturesPaperOrder o = open("PF_TAOUSD", "308.13");

        assertEquals(new BigDecimal("0.81"), o.getContracts());  // 250/308.13 = 0.8113 → 0.81
        assertEquals(0, o.getContracts().multiply(new BigDecimal("308.13"))
                .setScale(4, RoundingMode.HALF_UP).compareTo(o.getUsdAmount()));
        assertTrue(o.getUsdAmount().compareTo(new BigDecimal("250")) <= 0, "rounding down never oversizes");
    }

    @Test
    void wholeUnitInstrument_stillSizesInWholeUnits() {
        KrakenFuturesPaperOrder o = open("PF_XTZUSD", "0.3450");

        assertEquals(new BigDecimal("724"), o.getContracts()); // 724.6… → 724
    }

    @Test
    void collateralIsDeductedForTheSizeActuallyOpened() {
        KrakenFuturesPaperOrder o = open("PF_TAOUSD", "308.13");

        BigDecimal expectedFee = o.getUsdAmount().multiply(new BigDecimal("0.0005")).setScale(8, RoundingMode.HALF_UP);
        BigDecimal expected = new BigDecimal("5000").subtract(o.getUsdAmount()).subtract(expectedFee);
        assertEquals(0, expected.compareTo(account.getCollateralUsd()));
    }
}
