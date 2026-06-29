package nu.itark.frosk.service;

import nu.itark.frosk.coinbase.BaseIntegrationTest;
import nu.itark.frosk.model.IntradayBar;
import nu.itark.frosk.model.Security;
import nu.itark.frosk.repo.IntradayBarRepository;
import nu.itark.frosk.repo.SecurityRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.ta4j.core.BarSeries;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Network test (Coinbase Advanced market data — read only): verifies the
 * crypto intraday pipeline fetches 15m candles, persists them and builds a
 * BarSeries. The test DB has no COINBASE securities, so it seeds one and
 * cleans up after itself. Run manually:
 *
 * <pre>mvn test -Dmaven.test.skip=false -Dtest=TestJCryptoIntradaySync -Dcrypto.intraday.products=BTC-EUR</pre>
 */
public class TestJCryptoIntradaySync extends BaseIntegrationTest {

    @Autowired
    CryptoIntradayDataService cryptoIntradayDataService;

    @Autowired
    SecurityRepository securityRepository;

    @Autowired
    IntradayBarRepository intradayBarRepository;

    @Test
    public void syncsFifteenMinuteCandlesAndBuildsSeries() {
        Security btc = securityRepository.findByName("BTC-EUR");
        boolean seeded = false;
        if (btc == null) {
            Security seed = new Security("BTC-EUR", "Bitcoin EUR (test seed)", "COINBASE", "EUR");
            seed.setActive(true);
            btc = securityRepository.save(seed);
            seeded = true;
        }

        try {
            Map<Security, BarSeries> seriesMap = cryptoIntradayDataService.syncAndBuildAllSeries();

            long cutoff = Instant.now().minus(Duration.ofDays(2)).getEpochSecond();
            List<IntradayBar> bars = intradayBarRepository
                    .findBySecurityIdAndIntervalCodeAndBarTimestampGreaterThanOrderByBarTimestampAsc(
                            btc.getId(), "15m", cutoff);
            assertFalse(bars.isEmpty(), "No 15m bars persisted for BTC-EUR");

            BarSeries series = seriesMap.entrySet().stream()
                    .filter(e -> "BTC-EUR".equals(e.getKey().getName()))
                    .map(Map.Entry::getValue)
                    .findFirst().orElse(null);
            assertNotNull(series, "No BarSeries built for BTC-EUR");
            assertTrue(series.getBarCount() > 0);
            // Bars are complete 15m candles with sane OHLC
            IntradayBar last = bars.get(bars.size() - 1);
            assertTrue(last.getHigh().compareTo(last.getLow()) >= 0);
            System.out.println("BTC-EUR: " + bars.size() + " bars, last close=" + last.getClose());
        } finally {
            List<IntradayBar> created = intradayBarRepository
                    .findBySecurityIdAndIntervalCodeAndBarTimestampGreaterThanOrderByBarTimestampAsc(
                            btc.getId(), "15m", 0L);
            intradayBarRepository.deleteAll(created);
            if (seeded) {
                securityRepository.delete(btc);
            }
        }
    }
}
