package nu.itark.frosk.crypto.kraken;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.broker.BrokerOrderClient;
import nu.itark.frosk.broker.ProtectiveStopClient;
import nu.itark.frosk.crypto.livetrading.OrderResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

/**
 * Implements {@link BrokerOrderClient} against the Kraken Futures REST API v3.
 *
 * <p>Active only when the {@code kraken-futures} Spring profile is set.
 *
 * <h3>Sizing</h3>
 * {@code PF_*} perpetuals are sized in units of the base asset (1 contract =
 * 1 BTC on PF_XBTUSD) at a per-instrument precision — see
 * {@link KrakenFuturesInstrumentService}. The budget (named {@code eurAmount}
 * by the shared interface; USD here, since the Kraken account is
 * USD-denominated) is divided by the current mark price and rounded DOWN to
 * that precision.
 *
 * <h3>Fills</h3>
 * A placed order is only reported {@code FILLED} with the size and average
 * price Kraken actually executed, taken from the {@code EXECUTION} events in
 * the {@code /sendorder} response, or failing that from {@code /fills}. If
 * neither shows a fill yet the order is reported {@code UNCONFIRMED}, and
 * {@link KrakenFuturesLiveOrderReconciler} resolves it later — an order is never
 * recorded as filled without a real quantity, because that quantity is what
 * the exit closes.
 *
 * <h3>Short selling</h3>
 * A short entry is a plain {@code side=sell} on the perpetual. Closing orders
 * carry {@code reduceOnly=true} so they can never flip or enlarge a position.
 *
 * @see <a href="https://docs.kraken.com/api/docs/futures-api/trading/send-order">Kraken Futures send order</a>
 */
@Service
@Profile("kraken-futures")
@Slf4j
public class KrakenFuturesOrderClient implements BrokerOrderClient, ProtectiveStopClient {

    /** Status for a placed order whose fill Kraken has not confirmed yet — see class doc. */
    public static final String STATUS_UNCONFIRMED = "UNCONFIRMED";

    private static final String SEND_ORDER_PATH     = "/sendorder";
    private static final String OPEN_POSITIONS_PATH = "/openpositions";
    private static final String ACCOUNTS_PATH       = "/accounts";
    private static final String FILLS_PATH          = "/fills";
    private static final String TICKER_PATH         = "/tickers/";

    @Autowired
    private KrakenFuturesHttpClient httpClient;

    @Autowired
    private KrakenFuturesInstrumentService instrumentService;

    /** How many times {@code /fills} is checked when the send response carries no execution. */
    @Value("${kraken.futures.fill.poll.attempts:3}")
    private int fillPollAttempts = 3;

    @Value("${kraken.futures.fill.poll.delay.ms:1000}")
    private long fillPollDelayMs = 1000;

    /** Price a protective stop triggers on: {@code mark}, {@code last} or {@code spot}. */
    @Value("${kraken.futures.protective.stop.trigger:mark}")
    private String triggerSignal = "mark";

    // ── BrokerOrderClient ────────────────────────────────────────────────

    @Override
    public OrderResponse placeLongEntry(String symbol, BigDecimal budgetUsd) {
        return placeEntry(symbol, "buy", budgetUsd);
    }

    @Override
    public OrderResponse placeShortEntry(String symbol, BigDecimal budgetUsd) {
        return placeEntry(symbol, "sell", budgetUsd);
    }

    @Override
    public OrderResponse placeLongExit(String symbol, BigDecimal quantity) {
        return placeExit(symbol, "sell", quantity);
    }

    @Override
    public OrderResponse placeShortExit(String symbol, BigDecimal quantity) {
        return placeExit(symbol, "buy", quantity);
    }

    /**
     * Available margin of the flex (multi-collateral) account — the collateral
     * that backs PF_* perpetuals. {@code /accounts} is an OBJECT keyed by
     * account name ({@code {"accounts": {"cash": {...}, "flex": {...}}}}), not
     * an array; parsing it as a list used to fail silently and return 0, which
     * made {@code LiveTradingGate} refuse every entry.
     */
    @Override
    public BigDecimal getAvailableBalance() {
        try {
            JsonNode resp = httpClient.get(ACCOUNTS_PATH, JsonNode.class);
            if (resp == null || !"success".equals(resp.path("result").asText())) {
                log.warn("KrakenFuturesOrderClient: /accounts unavailable ({}) — balance treated as 0",
                        resp != null ? resp.path("error").asText("?") : "no response");
                return BigDecimal.ZERO;
            }
            JsonNode flex = resp.path("accounts").path("flex");
            BigDecimal available = decimal(flex, "availableMargin");
            if (available == null) {
                log.warn("KrakenFuturesOrderClient: no flex.availableMargin in /accounts — balance treated as 0");
                return BigDecimal.ZERO;
            }
            return available;
        } catch (Exception e) {
            log.error("KrakenFuturesOrderClient: getAvailableBalance failed", e);
            return BigDecimal.ZERO;
        }
    }

    @Override
    public boolean supportsShort() {
        return true;
    }

    // ── public extra API ─────────────────────────────────────────────────

    /** Currently open positions, for reconciliation. Empty on any failure. */
    public List<OpenPosition> getOpenPositions() {
        try {
            OpenPositionsResponse resp = httpClient.get(OPEN_POSITIONS_PATH, OpenPositionsResponse.class);
            if (resp == null || resp.openPositions == null) return List.of();
            return resp.openPositions;
        } catch (Exception e) {
            log.error("KrakenFuturesOrderClient: getOpenPositions failed", e);
            return List.of();
        }
    }

    /**
     * Executed size and average price for {@code orderId} from {@code /fills}
     * (Kraken's most recent fills), or null when no fill for it is listed —
     * either not filled (yet) or the API is unreachable.
     */
    public Fill findFill(String orderId) {
        if (orderId == null) return null;
        JsonNode resp = httpClient.get(FILLS_PATH, JsonNode.class);
        if (resp == null || !resp.path("fills").isArray()) return null;
        FillAccumulator acc = new FillAccumulator();
        for (JsonNode f : resp.path("fills")) {
            if (orderId.equals(f.path("order_id").asText(null))) {
                acc.add(decimal(f, "price"), decimal(f, "size"));
            }
        }
        return acc.result();
    }

    /** What actually executed: total base-asset size and the size-weighted average price. */
    public record Fill(BigDecimal size, BigDecimal averagePrice) {}

    /**
     * Places a resting reduce-only STOP order that protects an open position.
     *
     * <p>This is the exchange-side safety net. The strategy's own stop is evaluated
     * on 15-minute bar closes, so a move that blows through the level inside one bar
     * is only exited at that bar's close — on 2026-09-23 that turned four 2.7 %
     * stops into 4–7 % losses. A resting stop triggers intrabar, without waiting for
     * the runner.
     *
     * <p>{@code reduceOnly} means it can only ever close, never open or flip.
     * The trigger price must sit on the instrument's tick grid — use
     * {@link KrakenFuturesInstrumentService#alignStopPrice}.
     *
     * @param closingSide "sell" to protect a long, "buy" to protect a short
     * @return the order id in {@code orderId}, status FILLED/UNCONFIRMED is not
     *         expected here — a resting stop comes back {@code placed}
     */
    @Override
    public OrderResponse placeProtectiveStop(String symbol, boolean isLong, BigDecimal size,
                                             BigDecimal entryPrice, BigDecimal stopPct) {
        // Long stops sit below entry and close with a sell; short stops above, closing with a buy.
        String closingSide = isLong ? "sell" : "buy";
        BigDecimal factor = isLong
                ? BigDecimal.ONE.subtract(stopPct.movePointLeft(2))
                : BigDecimal.ONE.add(stopPct.movePointLeft(2));
        BigDecimal stopPrice = instrumentService.alignStopPrice(symbol, entryPrice.multiply(factor), isLong);

        OrderResponse pre = new OrderResponse();
        pre.setProductId(symbol);
        pre.setSide(closingSide.toUpperCase());
        if (stopPrice == null) {
            pre.setStatus("FAILED");
            pre.setErrorMessage("Unknown tick size for " + symbol + " — cannot place a protective stop");
            log.error("KrakenFuturesOrderClient: no tick size for {} — position left WITHOUT an exchange stop", symbol);
            return pre;
        }

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("orderType", "stp");
        params.add("symbol", symbol);
        params.add("side", closingSide);
        params.add("size", size.toPlainString());
        params.add("stopPrice", stopPrice.toPlainString());
        // Trigger on the mark price: it is the price Kraken liquidates against and is
        // far harder to wick than last-trade on a thin book.
        params.add("triggerSignal", triggerSignal);
        params.add("reduceOnly", "true");
        params.add("cliOrdId", "frosk-stop-" + UUID.randomUUID());

        OrderResponse r = new OrderResponse();
        r.setProductId(symbol);
        r.setSide(closingSide.toUpperCase());
        try {
            JsonNode resp = httpClient.post(SEND_ORDER_PATH, params, JsonNode.class);
            if (resp == null) {
                r.setStatus("FAILED");
                r.setErrorMessage("No response from Kraken /sendorder (stop)");
                return r;
            }
            JsonNode sendStatus = resp.path("sendStatus");
            String status = sendStatus.path("status").asText(null);
            r.setOrderId(sendStatus.path("order_id").asText(null));
            r.setClientOrderId(sendStatus.path("cliOrdId").asText(params.getFirst("cliOrdId")));
            if ("placed".equals(status) && r.getOrderId() != null) {
                r.setStatus("PLACED");
                log.info("KrakenFuturesOrderClient: protective STOP placed for {} — {} {} @ stop {} (orderId={})",
                        symbol, closingSide, size.toPlainString(), stopPrice.toPlainString(), r.getOrderId());
            } else {
                r.setStatus("FAILED");
                r.setErrorMessage(status != null ? status : resp.path("error").asText("unknown"));
            }
            return r;
        } catch (Exception e) {
            log.error("KrakenFuturesOrderClient: protective stop for {} failed", symbol, e);
            r.setStatus("FAILED");
            r.setErrorMessage("Exception: " + e.getMessage());
            return r;
        }
    }

    /**
     * Cancels a resting order. Used to retire the protective stop when the position
     * is closed by the strategy's own exit, so no orphan stop is left behind.
     *
     * @return true when Kraken reports it cancelled or already gone (filled/notFound):
     *         in all three cases no resting stop remains
     */
    @Override
    public boolean cancelOrder(String orderId) {
        if (orderId == null) return true;
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("order_id", orderId);
        try {
            JsonNode resp = httpClient.post("/cancelorder", params, JsonNode.class);
            String status = resp == null ? null : resp.path("cancelStatus").path("status").asText(null);
            boolean gone = "cancelled".equals(status) || "filled".equals(status) || "notFound".equals(status);
            if (!gone) {
                log.error("KrakenFuturesOrderClient: could not cancel stop {} — status '{}'. A resting stop may "
                        + "still be live on Kraken; check the exchange.", orderId, status);
            }
            return gone;
        } catch (Exception e) {
            log.error("KrakenFuturesOrderClient: cancel of stop {} failed — it may still be live on Kraken",
                    orderId, e);
            return false;
        }
    }

    // ── private: order placement ─────────────────────────────────────────

    private OrderResponse placeEntry(String symbol, String side, BigDecimal budgetUsd) {
        String signalSide = "buy".equals(side) ? "BUY" : "SELL";
        BigDecimal markPrice = getMarkPrice(symbol);
        if (markPrice == null || markPrice.signum() <= 0) {
            return failedResponse(symbol, signalSide, "Could not retrieve mark price for " + symbol);
        }
        KrakenFuturesInstrumentService.Sizing sizing = instrumentService.sizeEntry(symbol, budgetUsd, markPrice);
        if (!sizing.isOk()) {
            return failedResponse(symbol, signalSide, sizing.refusal());
        }
        log.info("KrakenFuturesOrderClient: {} ENTRY {} — budget {} USD @ mark {} → size {} (~{} USD)",
                side.toUpperCase(), symbol, budgetUsd, markPrice, sizing.size().toPlainString(),
                sizing.size().multiply(markPrice).setScale(2, RoundingMode.HALF_UP));
        return sendOrder(symbol, side, sizing.size(), false);
    }

    private OrderResponse placeExit(String symbol, String side, BigDecimal filledQuantity) {
        String signalSide = "buy".equals(side) ? "BUY" : "SELL";
        if (filledQuantity == null || filledQuantity.signum() <= 0) {
            return failedResponse(symbol, signalSide, "No filled quantity to close");
        }
        BigDecimal size = instrumentService.exitSize(symbol, filledQuantity);
        if (size.signum() <= 0) {
            return failedResponse(symbol, signalSide, "Exit size rounds to 0 for " + filledQuantity.toPlainString());
        }
        log.info("KrakenFuturesOrderClient: {} EXIT {} — size {} (reduceOnly)", side.toUpperCase(), symbol, size.toPlainString());
        return sendOrder(symbol, side, size, true);
    }

    /**
     * Sends a market order and determines what executed.
     *
     * <p>Any EXECUTION in the response means money moved, so the order is
     * FILLED with those amounts regardless of the status string. Otherwise a
     * status other than {@code placed} is a rejection (FAILED). A {@code placed}
     * order with no execution in the response is looked up in {@code /fills};
     * still nothing → {@link #STATUS_UNCONFIRMED}.
     */
    OrderResponse sendOrder(String symbol, String side, BigDecimal size, boolean reduceOnly) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("orderType", "mkt");
        params.add("symbol", symbol);
        params.add("side", side);
        params.add("size", size.toPlainString());
        params.add("cliOrdId", "frosk-" + UUID.randomUUID());
        if (reduceOnly) {
            params.add("reduceOnly", "true");
        }

        OrderResponse r = new OrderResponse();
        r.setProductId(symbol);
        r.setSide("buy".equals(side) ? "BUY" : "SELL");

        JsonNode resp;
        try {
            resp = httpClient.post(SEND_ORDER_PATH, params, JsonNode.class);
        } catch (Exception e) {
            // The request may still have reached Kraken. There is no order id to
            // reconcile against, so this is logged loudly for a manual check.
            log.error("KrakenFuturesOrderClient: sendOrder({} {} {}) threw — order state UNKNOWN, check Kraken "
                    + "(cliOrdId={})", side, size.toPlainString(), symbol, params.getFirst("cliOrdId"), e);
            r.setStatus("FAILED");
            r.setErrorMessage("Exception: " + e.getMessage());
            return r;
        }
        if (resp == null) {
            log.error("KrakenFuturesOrderClient: sendOrder({} {} {}) got no response — order state UNKNOWN, check "
                    + "Kraken (cliOrdId={})", side, size.toPlainString(), symbol, params.getFirst("cliOrdId"));
            r.setStatus("FAILED");
            r.setErrorMessage("No response from Kraken /sendorder");
            return r;
        }

        JsonNode sendStatus = resp.path("sendStatus");
        String status = sendStatus.path("status").asText(null);
        r.setOrderId(sendStatus.path("order_id").asText(null));
        r.setClientOrderId(sendStatus.path("cliOrdId").asText(params.getFirst("cliOrdId")));

        FillAccumulator acc = new FillAccumulator();
        for (JsonNode ev : sendStatus.path("orderEvents")) {
            if ("EXECUTION".equals(ev.path("type").asText())) {
                acc.add(decimal(ev, "price"), decimal(ev, "amount"));
            }
        }
        Fill fill = acc.result();

        if (fill == null && !"placed".equals(status)) {
            r.setStatus("FAILED");
            r.setErrorMessage(status != null ? status : resp.path("error").asText("unknown"));
            return r;
        }
        if (fill == null) {
            fill = pollFills(r.getOrderId());
        }
        if (fill == null) {
            log.warn("KrakenFuturesOrderClient: order {} ({} {} {}) placed but no fill confirmed yet — UNCONFIRMED, "
                    + "the reconciler will resolve it", r.getOrderId(), side, size.toPlainString(), symbol);
            r.setStatus(STATUS_UNCONFIRMED);
            return r;
        }

        if (fill.size().compareTo(size) < 0) {
            log.warn("KrakenFuturesOrderClient: order {} PARTIALLY filled — {} of {} {}",
                    r.getOrderId(), fill.size().toPlainString(), size.toPlainString(), symbol);
        }
        r.setStatus("FILLED");
        r.setFilledSize(fill.size());
        r.setAverageFilledPrice(fill.averagePrice());
        return r;
    }

    private Fill pollFills(String orderId) {
        for (int attempt = 1; attempt <= fillPollAttempts; attempt++) {
            sleep(fillPollDelayMs);
            Fill fill = findFill(orderId);
            if (fill != null) {
                log.info("KrakenFuturesOrderClient: fill for {} found in /fills on attempt {}", orderId, attempt);
                return fill;
            }
        }
        return null;
    }

    /**
     * Current mark price from {@code /tickers/{symbol}}. That endpoint returns
     * one {@code ticker} OBJECT — not the {@code tickers} array of the bulk
     * endpoint — which is why the old list-typed parse always yielded null.
     *
     * <p>Public (no signature needed) — also called by {@code
     * KrakenFuturesPaperTradingService.checkProtectiveStops()} so the paper stop
     * polls the identical price reference ({@code mark}) the live resting stop
     * triggers on, not a separately-sourced price.
     */
    public BigDecimal getMarkPrice(String symbol) {
        try {
            JsonNode resp = httpClient.getPublic(TICKER_PATH + symbol, JsonNode.class);
            if (resp == null) return null;
            JsonNode ticker = resp.path("ticker");
            BigDecimal mark = decimal(ticker, "markPrice");
            return mark != null ? mark : decimal(ticker, "last");
        } catch (Exception e) {
            log.warn("KrakenFuturesOrderClient: could not fetch mark price for {}", symbol, e);
            return null;
        }
    }

    private OrderResponse failedResponse(String symbol, String side, String message) {
        log.warn("KrakenFuturesOrderClient: {} {} refused before sending — {}", side, symbol, message);
        OrderResponse r = new OrderResponse();
        r.setProductId(symbol);
        r.setSide(side);
        r.setStatus("FAILED");
        r.setErrorMessage(message);
        return r;
    }

    private static BigDecimal decimal(JsonNode node, String field) {
        JsonNode v = node.path(field);
        if (v.isMissingNode() || v.isNull() || !v.isValueNode()) return null;
        try {
            return new BigDecimal(v.asText());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void sleep(long ms) {
        if (ms <= 0) return;
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** Sums partial executions into one size and a size-weighted average price. */
    private static final class FillAccumulator {
        private BigDecimal size = BigDecimal.ZERO;
        private BigDecimal notional = BigDecimal.ZERO;

        void add(BigDecimal price, BigDecimal amount) {
            if (price == null || amount == null || amount.signum() <= 0) return;
            size = size.add(amount);
            notional = notional.add(price.multiply(amount));
        }

        Fill result() {
            if (size.signum() <= 0) return null;
            return new Fill(size, notional.divide(size, 8, RoundingMode.HALF_UP));
        }
    }

    // ── response POJOs ───────────────────────────────────────────────────

    @Data @NoArgsConstructor
    public static class OpenPositionsResponse {
        private String result;
        @JsonProperty("openPositions") private List<OpenPosition> openPositions;
    }

    @Data @NoArgsConstructor
    public static class OpenPosition {
        private String symbol;
        private String side;
        private BigDecimal size;
        @JsonProperty("price") private BigDecimal entryPrice;
        @JsonProperty("unrealizedFunding") private BigDecimal unrealizedFunding;
    }
}
