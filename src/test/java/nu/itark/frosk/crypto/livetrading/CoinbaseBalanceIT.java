package nu.itark.frosk.crypto.livetrading;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.FroskApplication;
import nu.itark.frosk.FroskStartupApplicationListener;
import nu.itark.frosk.crypto.coinbase.advanced.Coinbase;
import nu.itark.frosk.crypto.coinbase.service.CoinbaseOrderClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test that hits the live Coinbase Advanced Trade API and verifies
 * that account balances can be fetched with the configured credentials.
 *
 * <p>Run manually:
 * <pre>
 *   mvn test -Dtest=CoinbaseBalanceIT -pl . -am
 * </pre>
 *
 * <p>Uses the real crypto Spring context (application-crypto.properties) with
 * actual JWT credentials from application.properties. No orders are placed.
 */
@SpringBootTest(classes = {FroskApplication.class})
@ActiveProfiles("crypto")
@TestPropertySource(properties = {
        "frosk.adddatasetandsecurities=false",
        "frosk.addsecuritypricesfromcoinbase=false",
        "frosk.runallstrategies=false",
        "frosk.buildportfolio=false",
        "frosk.updatehedgeindex=false",
        "scheduler.crypto.cron=-",
        "scheduler.crypto.intraday.cron=-"
})
@Slf4j
class CoinbaseBalanceIT {

    @Autowired
    private CoinbaseOrderClient coinbaseOrderClient;

    @Autowired
    private Coinbase coinbase;

    @MockBean
    private FroskStartupApplicationListener applicationStartup;

    @Test
    void getAllBalances_shouldReturnNonNullMapWithNonNegativeEur() {
        Map<String, BigDecimal> balances = coinbaseOrderClient.getAllBalances();

        assertNotNull(balances, "Balances map must not be null");

        log.info("=== Coinbase Account Balances ===");
        if (balances.isEmpty()) {
            log.info("  (no non-zero balances found)");
        } else {
            balances.entrySet().stream()
                    .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                    .forEach(e -> log.info("  {}: {}", e.getKey(), e.getValue().toPlainString()));
        }

        BigDecimal eurBalance = balances.getOrDefault("EUR", BigDecimal.ZERO);
        log.info("EUR balance (from getAllBalances): {}", eurBalance.toPlainString());

        assertTrue(eurBalance.compareTo(BigDecimal.ZERO) >= 0,
                "EUR balance must be >= 0, got: " + eurBalance);
    }

    @Test
    void getEurBalance_shouldReturnNonNegativeValue() {
        BigDecimal eurBalance = coinbaseOrderClient.getEurBalance();

        assertNotNull(eurBalance, "EUR balance must not be null");
        log.info("EUR balance (from getEurBalance): {}", eurBalance.toPlainString());

        assertTrue(eurBalance.compareTo(BigDecimal.ZERO) >= 0,
                "EUR balance must be >= 0, got: " + eurBalance);
    }

    /** Diagnostic: logs the raw JSON string from /accounts to verify response shape. */
    @Test
    void rawAccountsResponse_shouldBeNonEmpty() {
        String raw = coinbase.get("/accounts", new ParameterizedTypeReference<String>() {});
        log.info("=== Raw /accounts response ===");
        log.info("{}", raw);
        assertNotNull(raw, "Raw accounts response must not be null");
        assertFalse(raw.isBlank(), "Raw accounts response must not be blank");
    }

    /** Check which permissions the configured API key has (view / trade / transfer). */
    @Test
    void keyPermissions_shouldShowGrantedScopes() {
        String raw = coinbase.get("/key_permissions", new ParameterizedTypeReference<String>() {});
        log.info("=== API Key Permissions ===");
        log.info("{}", raw);
        assertNotNull(raw, "key_permissions response must not be null");
        assertFalse(raw.isBlank(), "key_permissions response must not be blank");
    }
}
