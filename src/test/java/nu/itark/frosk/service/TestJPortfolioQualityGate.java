package nu.itark.frosk.service;

import nu.itark.frosk.coinbase.BaseIntegrationTest;
import nu.itark.frosk.model.FeaturedStrategy;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The win-rate gate compares against FeaturedStrategy.profitableTradesRatio,
 * which is stored in percent (e.g. 26.08). The threshold property must be on
 * the same scale — a 0.35 threshold made the gate a no-op for years.
 */
public class TestJPortfolioQualityGate extends BaseIntegrationTest {

    @Autowired
    PortfolioService portfolioService;

    @Test
    public void winRateGateUsesPercentScale() {
        // 26% win rate (the historical ShortTermMomentum average) must be excluded
        assertFalse(passesGate(fs("2.0", "26.08")));
        // 45% win rate passes
        assertTrue(passesGate(fs("2.0", "45.00")));
        // SQN below 1.0 excluded regardless of win rate
        assertFalse(passesGate(fs("0.5", "60.00")));
        // No track record excluded
        assertFalse(passesGate(fs(null, "60.00")));
        assertFalse(passesGate(fs("2.0", null)));
    }

    private boolean passesGate(FeaturedStrategy fs) {
        // Unwrap the @Transactional proxy — the @Value fields live on the target bean
        PortfolioService target = AopTestUtils.getUltimateTargetObject(portfolioService);
        return Boolean.TRUE.equals(ReflectionTestUtils.invokeMethod(target, "passesQualityGate", fs));
    }

    private FeaturedStrategy fs(String sqn, String winRatePercent) {
        FeaturedStrategy fs = new FeaturedStrategy();
        fs.setName("TestStrategy");
        fs.setSecurityName("TEST.ST");
        fs.setSqn(sqn != null ? new BigDecimal(sqn) : null);
        fs.setProfitableTradesRatio(winRatePercent != null ? new BigDecimal(winRatePercent) : null);
        return fs;
    }
}
