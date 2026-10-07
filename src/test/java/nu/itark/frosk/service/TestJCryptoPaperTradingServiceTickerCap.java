package nu.itark.frosk.service;

import nu.itark.frosk.model.CryptoPaperAccount;
import nu.itark.frosk.model.CryptoPaperOrder;
import nu.itark.frosk.repo.CryptoPaperAccountRepository;
import nu.itark.frosk.repo.CryptoPaperOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure unit test (Mockito, no Spring context) for the per-ticker exposure cap
 * ({@code crypto.paper.trading.max.ticker.exposure.pct}) in
 * {@link CryptoPaperTradingService#dispatchBuy}.
 *
 * <p>Equity 2000 EUR, no other open exposure: base size 7% = 140 EUR, cap 10% = 200 EUR.
 */
@ExtendWith(MockitoExtension.class)
public class TestJCryptoPaperTradingServiceTickerCap {

    private static final String TICKER = "TRAC-USD";

    @Mock
    private CryptoPaperAccountRepository accountRepository;

    @Mock
    private CryptoPaperOrderRepository orderRepository;

    private CryptoPaperTradingService service;

    @BeforeEach
    void setUp() {
        service = new CryptoPaperTradingService();
        ReflectionTestUtils.setField(service, "accountRepository", accountRepository);
        ReflectionTestUtils.setField(service, "orderRepository", orderRepository);
        ReflectionTestUtils.setField(service, "positionPctOfEquity", new BigDecimal("0.07"));
        ReflectionTestUtils.setField(service, "minPositionEur", new BigDecimal("25"));
        ReflectionTestUtils.setField(service, "maxPositionEur", new BigDecimal("500"));
        ReflectionTestUtils.setField(service, "maxTotalExposurePct", new BigDecimal("0.5"));
        ReflectionTestUtils.setField(service, "maxTickerExposurePct", new BigDecimal("0.10"));
        ReflectionTestUtils.setField(service, "takerFeeFraction", new BigDecimal("0.001"));

        CryptoPaperAccount account = new CryptoPaperAccount();
        account.setCashEur(new BigDecimal("2000"));
        account.setRealizedPnlEur(BigDecimal.ZERO);
        when(accountRepository.findAll()).thenReturn(List.of(account));
        when(orderRepository.sumOpenExposureEur()).thenReturn(BigDecimal.ZERO);
    }

    private BigDecimal buyAndCaptureAmount() {
        service.dispatchBuy("CryptoVWAPReversionIntradayStrategy", TICKER, new BigDecimal("0.41"), null);
        ArgumentCaptor<CryptoPaperOrder> captor = ArgumentCaptor.forClass(CryptoPaperOrder.class);
        verify(orderRepository).save(captor.capture());
        return captor.getValue().getEurAmount();
    }

    @Test
    void fullSizeWhenTheTickerHasNoOpenExposure() {
        when(orderRepository.sumOpenExposureEurByTicker(TICKER)).thenReturn(BigDecimal.ZERO);
        assertEquals(0, new BigDecimal("140").compareTo(buyAndCaptureAmount()));
    }

    @Test
    void scaledDownToTheRoomLeftUnderTheCap() {
        // 145 EUR already open in another strategy: 200 - 145 = 55 EUR of room
        when(orderRepository.sumOpenExposureEurByTicker(TICKER)).thenReturn(new BigDecimal("145"));
        assertEquals(0, new BigDecimal("55").compareTo(buyAndCaptureAmount()));
    }

    @Test
    void skippedWhenTheRoomIsBelowTheMinimumPosition() {
        // 180 EUR open: 20 EUR of room < 25 EUR minimum
        when(orderRepository.sumOpenExposureEurByTicker(TICKER)).thenReturn(new BigDecimal("180"));
        service.dispatchBuy("CryptoVWAPReversionIntradayStrategy", TICKER, new BigDecimal("0.41"), null);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void disabledCapNeverQueriesTickerExposure() {
        ReflectionTestUtils.setField(service, "maxTickerExposurePct", null);
        assertEquals(0, new BigDecimal("140").compareTo(buyAndCaptureAmount()));
        verify(orderRepository, never()).sumOpenExposureEurByTicker(any());
    }
}
