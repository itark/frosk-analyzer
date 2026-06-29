package nu.itark.frosk.service;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.model.Security;
import nu.itark.frosk.model.SecurityPrice;
import nu.itark.frosk.repo.SecurityPriceRepository;
import nu.itark.frosk.repo.SecurityRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * Crypto market regime: risk-on while BTC trades above its N-day SMA.
 *
 * <p>The equity HedgeIndex is built from equity/FX/rates indicators and does
 * not describe the crypto market. The crypto equivalent is deliberately
 * simple: altcoin breakouts fail and stretched prices keep stretching while
 * BTC is in a downtrend, so no long entries are taken below the BTC trend line.
 *
 * <p>Computed from the daily BTC closes in {@code security_price} (synced
 * nightly by the crypto process) and cached day-floored — the same lookup
 * semantics as {@link HedgeIndexService}, so it works for 15m bar timestamps
 * and {@code ZonedDateTime.now()} alike.
 */
@Service
@Slf4j
public class CryptoRegimeService {

    private static final ZoneId UTC = ZoneId.of("UTC");

    @Value("${crypto.regime.product:BTC-EUR}")
    private String regimeProduct;

    @Value("${crypto.regime.sma.period:20}")
    private int smaPeriod;

    private final SecurityRepository securityRepository;
    private final SecurityPriceRepository securityPriceRepository;

    private volatile NavigableMap<Long, Boolean> riskOnCache = null;

    public CryptoRegimeService(SecurityRepository securityRepository,
                               SecurityPriceRepository securityPriceRepository) {
        this.securityRepository = securityRepository;
        this.securityPriceRepository = securityPriceRepository;
    }

    /** True when BTC closed above its SMA on the most recent day at or before {@code at}. */
    public boolean isRiskOn(ZonedDateTime at) {
        if (riskOnCache == null) {
            synchronized (this) {
                if (riskOnCache == null) {
                    buildCache();
                }
            }
        }
        Map.Entry<Long, Boolean> entry = riskOnCache.floorEntry(startOfDayKey(at));
        // No data at all → fail closed (no entries) rather than risk-on
        return entry != null && entry.getValue();
    }

    /** Clears the cache (call after the nightly crypto price sync). */
    public synchronized void clearCache() {
        riskOnCache = null;
    }

    private void buildCache() {
        TreeMap<Long, Boolean> cache = new TreeMap<>();
        Security btc = securityRepository.findByName(regimeProduct);
        if (btc == null) {
            log.warn("CryptoRegimeService: regime product '{}' not found — regime defaults to risk-off", regimeProduct);
            riskOnCache = cache;
            return;
        }
        List<SecurityPrice> prices = securityPriceRepository.findBySecurityIdOrderByTimestamp(btc.getId());
        double sum = 0;
        for (int i = 0; i < prices.size(); i++) {
            double close = prices.get(i).getClose().doubleValue();
            sum += close;
            if (i >= smaPeriod) {
                sum -= prices.get(i - smaPeriod).getClose().doubleValue();
            }
            if (i >= smaPeriod - 1) {
                double sma = sum / smaPeriod;
                long dayKey = startOfDayKey(ZonedDateTime.ofInstant(
                        prices.get(i).getTimestamp().toInstant(), UTC));
                cache.put(dayKey, close > sma);
            }
        }
        riskOnCache = cache;
        log.info("CryptoRegimeService: cache built — {} days, current regime: {}",
                cache.size(), cache.isEmpty() ? "unknown" : (cache.lastEntry().getValue() ? "RISK-ON" : "RISK-OFF"));
    }

    private static long startOfDayKey(ZonedDateTime t) {
        return t.withZoneSameInstant(UTC).toLocalDate().atStartOfDay(UTC).toInstant().toEpochMilli();
    }
}
