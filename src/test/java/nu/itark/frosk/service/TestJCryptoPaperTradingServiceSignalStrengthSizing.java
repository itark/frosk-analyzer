package nu.itark.frosk.service;

import nu.itark.frosk.model.CryptoPaperAccount;
import nu.itark.frosk.model.CryptoPaperOrder;
import nu.itark.frosk.repo.CryptoPaperAccountRepository;
import nu.itark.frosk.repo.CryptoPaperOrderRepository;
import nu.itark.frosk.strategies.SignalStrength;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/**
 * Pure unit test (Mockito, no Spring context) for the signal-strength position
 * multiplier: BASE=1x, ELEVATED=1.5x, STRONG=2x of the base 5%-of-equity size,
 * always clamped to [minPositionEur, maxPositionEur].
 */
@ExtendWith(MockitoExtension.class)
public class TestJCryptoPaperTradingServiceSignalStrengthSizing {

    @Mock
    private CryptoPaperAccountRepository accountRepository;

    @Mock
    private CryptoPaperOrderRepository orderRepository;

    private CryptoPaperTradingService service;
    private CryptoPaperAccount account;

    @BeforeEach
    void setUp() {
        service = new CryptoPaperTradingService();
        ReflectionTestUtils.setField(service, "accountRepository", accountRepository);
        ReflectionTestUtils.setField(service, "orderRepository", orderRepository);
        ReflectionTestUtils.setField(service, "positionPctOfEquity", new BigDecimal("0.05"));
        ReflectionTestUtils.setField(service, "minPositionEur", new BigDecimal("25"));
        ReflectionTestUtils.setField(service, "maxPositionEur", new BigDecimal("500"));
        ReflectionTestUtils.setField(service, "maxTotalExposurePct", new BigDecimal("0.5"));
        ReflectionTestUtils.setField(service, "takerFeeFraction", new BigDecimal("0.006"));
        ReflectionTestUtils.setField(service, "elevatedMultiplier", new BigDecimal("1.5"));
        ReflectionTestUtils.setField(service, "strongMultiplier", new BigDecimal("2.0"));

        // Equity 4000 EUR, no open exposure: base 5% = 200 EUR, well under the 500 cap
        // for every tier, so the multiplier's effect is directly observable.
        account = new CryptoPaperAccount();
        account.setCashEur(new BigDecimal("4000"));
        account.setRealizedPnlEur(BigDecimal.ZERO);
        when(accountRepository.findAll()).thenReturn(List.of(account));
        when(orderRepository.sumOpenExposureEur()).thenReturn(BigDecimal.ZERO);
    }

    @Test
    void baseStrengthUsesOneTimesMultiplier() {
        BigDecimal eurAmount = buyAndCaptureAmount(SignalStrength.BASE);
        assertEquals(0, new BigDecimal("200.0000").compareTo(eurAmount));
    }

    @Test
    void nullStrengthIsTreatedAsBase() {
        BigDecimal eurAmount = buyAndCaptureAmount(null);
        assertEquals(0, new BigDecimal("200.0000").compareTo(eurAmount));
    }

    @Test
    void elevatedStrengthScalesByOnePointFive() {
        BigDecimal eurAmount = buyAndCaptureAmount(SignalStrength.ELEVATED);
        assertEquals(0, new BigDecimal("300.0000").compareTo(eurAmount));
    }

    @Test
    void strongStrengthScalesByTwo() {
        BigDecimal eurAmount = buyAndCaptureAmount(SignalStrength.STRONG);
        assertEquals(0, new BigDecimal("400.0000").compareTo(eurAmount));
    }

    @Test
    void strongStrengthNeverExceedsMaxPositionEur() {
        // Equity high enough that base*2 alone would be 1000 EUR, well past the 500 cap.
        account.setCashEur(new BigDecimal("10000"));
        BigDecimal eurAmount = buyAndCaptureAmount(SignalStrength.STRONG);
        assertEquals(0, new BigDecimal("500").compareTo(eurAmount));
    }

    private BigDecimal buyAndCaptureAmount(SignalStrength strength) {
        ArgumentCaptor<CryptoPaperOrder> captor = ArgumentCaptor.forClass(CryptoPaperOrder.class);
        service.dispatchBuy("CryptoEMACrossLongIntradayStrategy", "BTC-EUR", new BigDecimal("50000"), strength);
        verify(orderRepository, atLeastOnce()).save(captor.capture());
        return captor.getValue().getEurAmount();
    }
}
