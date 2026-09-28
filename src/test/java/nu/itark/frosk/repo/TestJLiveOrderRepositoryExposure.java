package nu.itark.frosk.repo;

import nu.itark.frosk.model.LiveOrder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Bug 5, against a real (in-memory H2) database: the native
 * {@code sumOpenExposureEur} query must count short entries and positions whose
 * state is uncertain. A mocked repository cannot test this — the fix is in the SQL.
 */
@DataJpaTest
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
public class TestJLiveOrderRepositoryExposure {

    /**
     * Own minimal configuration: FroskApplication declares @Bean methods that need
     * a Coinbase client, which a JPA slice cannot (and should not) provide.
     */
    @SpringBootConfiguration
    @EntityScan(basePackageClasses = LiveOrder.class)
    @EnableJpaRepositories(basePackageClasses = LiveOrderRepository.class,
            includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = LiveOrderRepository.class))
    static class JpaSliceConfig {}

    @Autowired
    private LiveOrderRepository repo;

    private void order(String side, String status, String amount) {
        LiveOrder o = new LiveOrder();
        o.setTicker("PF_XBTUSD");
        o.setStrategyName("S");
        o.setSide(side);
        o.setStatus(status);
        o.setEurAmount(new BigDecimal(amount));
        repo.save(o);
    }

    @Test
    void exposure_includesShortsAndUncertainPositions_excludesClosedAndExits() {
        order("BUY",  "FILLED",     "100");   // counted
        order("SHRT", "FILLED",     "200");   // counted — was missing before the fix
        order("SHRT", "PENDING",    "40");    // counted — placed, fill unconfirmed
        order("BUY",  "CLOSING",    "30");    // counted — exit in flight
        order("SHRT", "UNRESOLVED", "20");    // counted — may be open on Kraken
        order("SHRT", "CLOSED",     "999");   // not counted
        order("BUY",  "FAILED",     "999");   // not counted
        order("COVR", "FILLED",     "999");   // exit rows never count

        assertEquals(0, new BigDecimal("390").compareTo(repo.sumOpenExposureEur()));
    }

    @Test
    void exposure_isZeroWhenNothingOpen() {
        assertEquals(0, BigDecimal.ZERO.compareTo(repo.sumOpenExposureEur()));
    }
}
