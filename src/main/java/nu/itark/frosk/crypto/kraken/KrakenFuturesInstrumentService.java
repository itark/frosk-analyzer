package nu.itark.frosk.crypto.kraken;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Order-size precision per Kraken Futures instrument, and the sizing rule built
 * on it.
 *
 * <p>{@code PF_*} perpetuals are {@code flexible_futures}: one contract is ONE
 * UNIT OF THE BASE ASSET (1 BTC for PF_XBTUSD), traded in fractions down to
 * {@code 10^-contractValueTradePrecision} — 0.0001 BTC, 0.001 ETH, whole coins
 * for most small caps, and multiples of 10 for PF_PENGUUSD (precision −1). The
 * "1 contract = 1 USD" rule belongs to the inverse FI_/PI_ contracts, which this
 * system does not trade; applying it to PF_XBTUSD would turn a 250 USD position
 * into 250 BTC.
 *
 * <p>Precision comes from the public {@code /instruments} endpoint and is cached.
 * An instrument whose precision is unknown is never sized — the caller refuses
 * the order rather than guess.
 */
@Service
@Profile("kraken-futures")
@RequiredArgsConstructor
@Slf4j
public class KrakenFuturesInstrumentService {

    /** A missing symbol triggers a refetch at most this often. */
    private static final Duration REFRESH_ON_MISS_INTERVAL = Duration.ofMinutes(10);

    /**
     * Upper bound on the one-step bump: when even the smallest tradeable size
     * costs more than this multiple of the budget, the order is refused. On the
     * configured whitelist a step is at most ~9 USD, so this never binds there;
     * it exists for instruments outside it (an expensive coin traded in whole
     * units could otherwise turn a 25 USD budget into a four-figure order).
     */
    static final BigDecimal MAX_STEP_OVER_BUDGET = BigDecimal.valueOf(2);

    private final KrakenFuturesHttpClient httpClient;

    private final Map<String, Integer> precisionBySymbol = new ConcurrentHashMap<>();
    private final Map<String, BigDecimal> tickSizeBySymbol = new ConcurrentHashMap<>();
    private volatile Instant lastFetch = Instant.EPOCH;

    /** Result of sizing: the order size, or null with the reason it was refused. */
    public record Sizing(BigDecimal size, String refusal) {
        static Sizing ok(BigDecimal size) { return new Sizing(size, null); }
        static Sizing refused(String reason) { return new Sizing(null, reason); }
        public boolean isOk() { return size != null; }
    }

    /**
     * Order size for a {@code budgetUsd} position at {@code markPrice}: the base
     * quantity rounded DOWN to the instrument's precision so the order never
     * exceeds the risk-sized budget; if that rounds to zero, one minimum step
     * (bounded by {@link #MAX_STEP_OVER_BUDGET}).
     */
    public Sizing sizeEntry(String symbol, BigDecimal budgetUsd, BigDecimal markPrice) {
        Integer precision = precision(symbol);
        if (precision == null) {
            return Sizing.refused("Okänd order-precision för " + symbol + " (saknas i /instruments)");
        }
        if (budgetUsd == null || budgetUsd.signum() <= 0 || markPrice == null || markPrice.signum() <= 0) {
            return Sizing.refused("Ogiltig budget/mark price: budget=" + budgetUsd + " mark=" + markPrice);
        }
        BigDecimal size = budgetUsd.divide(markPrice, precision, RoundingMode.DOWN);
        if (size.signum() > 0) {
            return Sizing.ok(size);
        }
        BigDecimal step = BigDecimal.ONE.scaleByPowerOfTen(-precision);
        BigDecimal stepNotional = step.multiply(markPrice);
        if (stepNotional.compareTo(budgetUsd.multiply(MAX_STEP_OVER_BUDGET)) > 0) {
            return Sizing.refused("Minsta orderstorlek " + step.toPlainString() + " " + symbol + " kostar "
                    + stepNotional.setScale(2, RoundingMode.HALF_UP) + " USD — mer än "
                    + MAX_STEP_OVER_BUDGET + "× budgeten " + budgetUsd);
        }
        log.info("KrakenFuturesInstrumentService: {} budget {} USD < one step — using minimum size {} (~{} USD)",
                symbol, budgetUsd, step.toPlainString(), stepNotional.setScale(2, RoundingMode.HALF_UP));
        return Sizing.ok(step);
    }

    /**
     * An exit quantity in the form Kraken accepts. The quantity is the entry's
     * recorded FILL size, which Kraken produced and is therefore already on the
     * instrument's grid, so normally this only strips trailing zeros. If it is
     * somehow off-grid it is rounded DOWN (a reduce-only close never overshoots)
     * and logged, since any remainder then stays open on Kraken.
     */
    public BigDecimal exitSize(String symbol, BigDecimal filledQuantity) {
        Integer precision = precision(symbol);
        BigDecimal qty = filledQuantity.stripTrailingZeros();
        if (precision == null) return qty;
        BigDecimal aligned = qty.setScale(precision, RoundingMode.DOWN);
        if (aligned.compareTo(qty) != 0) {
            log.warn("KrakenFuturesInstrumentService: exit size {} for {} is off the {}-decimal grid — sending {}; "
                    + "remainder {} stays open on Kraken", qty.toPlainString(), symbol, precision,
                    aligned.toPlainString(), qty.subtract(aligned).toPlainString());
        }
        return aligned.stripTrailingZeros();
    }

    /**
     * A trigger price snapped to the instrument's {@code tickSize} — Kraken rejects
     * prices off the tick grid. Rounded in the conservative direction: DOWN for a
     * long's stop (which sits below entry) and UP for a short's, so snapping can
     * only make the stop trigger slightly earlier, never later.
     *
     * @return the aligned price, or null when the tick size is unknown — the caller
     *         must then skip the stop rather than send a price Kraken will reject
     */
    public BigDecimal alignStopPrice(String symbol, BigDecimal price, boolean isLong) {
        BigDecimal tick = tickSize(symbol);
        if (tick == null || tick.signum() <= 0 || price == null || price.signum() <= 0) return null;
        BigDecimal ticks = price.divide(tick, 0, isLong ? RoundingMode.FLOOR : RoundingMode.CEILING);
        BigDecimal aligned = ticks.multiply(tick).stripTrailingZeros();
        return aligned.signum() > 0 ? aligned : null;
    }

    /** {@code tickSize} for {@code symbol}, or null when Kraken doesn't list it. */
    BigDecimal tickSize(String symbol) {
        String key = symbol.toUpperCase();
        BigDecimal t = tickSizeBySymbol.get(key);
        if (t == null && Duration.between(lastFetch, Instant.now()).compareTo(REFRESH_ON_MISS_INTERVAL) > 0) {
            refresh();
            t = tickSizeBySymbol.get(key);
        }
        return t;
    }

    /** {@code contractValueTradePrecision} for {@code symbol}, or null when Kraken doesn't list it. */
    Integer precision(String symbol) {
        String key = symbol.toUpperCase();
        Integer p = precisionBySymbol.get(key);
        if (p == null && Duration.between(lastFetch, Instant.now()).compareTo(REFRESH_ON_MISS_INTERVAL) > 0) {
            refresh();
            p = precisionBySymbol.get(key);
        }
        return p;
    }

    private synchronized void refresh() {
        lastFetch = Instant.now();
        JsonNode resp = httpClient.getPublic("/instruments", JsonNode.class);
        if (resp == null || !resp.path("instruments").isArray()) {
            log.warn("KrakenFuturesInstrumentService: /instruments unavailable — order sizing refused until it answers");
            return;
        }
        int n = 0;
        for (JsonNode i : resp.path("instruments")) {
            JsonNode p = i.path("contractValueTradePrecision");
            String symbol = i.path("symbol").asText(null);
            if (symbol != null && p.isNumber()) {
                precisionBySymbol.put(symbol.toUpperCase(), p.asInt());
                n++;
            }
            JsonNode tick = i.path("tickSize");
            if (symbol != null && tick.isNumber()) {
                tickSizeBySymbol.put(symbol.toUpperCase(), new BigDecimal(tick.asText()));
            }
        }
        log.info("KrakenFuturesInstrumentService: loaded order precision for {} instruments", n);
    }
}
