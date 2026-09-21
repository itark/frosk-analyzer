package nu.itark.frosk.service;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.model.Security;
import nu.itark.frosk.model.SecurityPrice;
import nu.itark.frosk.repo.SecurityPriceRepository;
import nu.itark.frosk.repo.SecurityRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.adx.ADXIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.num.DoubleNum;

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

    /** Trend-strength leg of the three-state {@link CryptoMarketRegime} — unused by {@link #isRiskOn}. */
    @Value("${crypto.regime.adx.period:14}")
    private int adxPeriod;

    /** ADX at/above this counts as trending; below is RANGING regardless of SMA side. */
    @Value("${crypto.regime.adx.threshold:25.0}")
    private double adxThreshold;

    private final SecurityRepository securityRepository;
    private final SecurityPriceRepository securityPriceRepository;

    private volatile NavigableMap<Long, Boolean> riskOnCache = null;
    private volatile NavigableMap<Long, CryptoMarketRegime> regimeCache = null;

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

    /**
     * Three-state regime (RANGING / TRENDING_UP / TRENDING_DOWN) on the most
     * recent day at or before {@code at}. Independent of {@link #isRiskOn} —
     * adds an ADX trend-strength leg on top of the same BTC/SMA direction leg.
     * RANGING is the fail-closed default when there is no data (grants neither
     * momentum direction).
     */
    public CryptoMarketRegime getRegime(ZonedDateTime at) {
        if (regimeCache == null) {
            synchronized (this) {
                if (regimeCache == null) {
                    buildCache();
                }
            }
        }
        Map.Entry<Long, CryptoMarketRegime> entry = regimeCache.floorEntry(startOfDayKey(at));
        return entry != null ? entry.getValue() : CryptoMarketRegime.RANGING;
    }

    /** Clears both caches (call after the nightly crypto price sync). */
    public synchronized void clearCache() {
        riskOnCache = null;
        regimeCache = null;
    }

    private void buildCache() {
        TreeMap<Long, Boolean> cache = new TreeMap<>();
        Security btc = securityRepository.findByName(regimeProduct);
        if (btc == null) {
            log.warn("CryptoRegimeService: regime product '{}' not found — regime defaults to risk-off", regimeProduct);
            riskOnCache = cache;
            regimeCache = new TreeMap<>();
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

        regimeCache = buildRegimeCache(prices);
    }

    /**
     * Builds the three-state cache via ta4j SMA/ADX indicators over the same
     * daily price list {@link #buildCache} already fetched — a separate
     * indicator stack from the hand-rolled rolling-sum SMA above, since the
     * two caches are independent features (this one adds the ADX leg).
     */
    private NavigableMap<Long, CryptoMarketRegime> buildRegimeCache(List<SecurityPrice> prices) {
        TreeMap<Long, CryptoMarketRegime> cache = new TreeMap<>();
        if (prices.isEmpty()) {
            return cache;
        }
        BarSeries series = new BaseBarSeriesBuilder().withName(regimeProduct).withNumTypeOf(DoubleNum.class).build();
        for (SecurityPrice p : prices) {
            series.addBar(ZonedDateTime.ofInstant(p.getTimestamp().toInstant(), UTC),
                    p.getOpen(), p.getHigh(), p.getLow(), p.getClose(),
                    p.getVolume() == null ? java.math.BigDecimal.ZERO : p.getVolume());
        }
        ClosePriceIndicator close = new ClosePriceIndicator(series);
        SMAIndicator sma = new SMAIndicator(close, smaPeriod);
        ADXIndicator adx = new ADXIndicator(series, adxPeriod);

        int warmup = Math.max(smaPeriod, adxPeriod) - 1;
        for (int i = warmup; i <= series.getEndIndex(); i++) {
            boolean trending = adx.getValue(i).doubleValue() >= adxThreshold;
            boolean above = close.getValue(i).doubleValue() > sma.getValue(i).doubleValue();
            CryptoMarketRegime regime = !trending
                    ? CryptoMarketRegime.RANGING
                    : (above ? CryptoMarketRegime.TRENDING_UP : CryptoMarketRegime.TRENDING_DOWN);
            long dayKey = startOfDayKey(series.getBar(i).getEndTime());
            cache.put(dayKey, regime);
        }
        log.info("CryptoRegimeService: regime cache built — {} days, current: {}",
                cache.size(), cache.isEmpty() ? "unknown" : cache.lastEntry().getValue());
        return cache;
    }

    private static long startOfDayKey(ZonedDateTime t) {
        return t.withZoneSameInstant(UTC).toLocalDate().atStartOfDay(UTC).toInstant().toEpochMilli();
    }
}
