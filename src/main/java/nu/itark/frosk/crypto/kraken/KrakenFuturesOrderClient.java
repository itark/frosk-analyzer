package nu.itark.frosk.crypto.kraken;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.broker.BrokerOrderClient;
import nu.itark.frosk.crypto.livetrading.OrderResponse;
import org.springframework.beans.factory.annotation.Autowired;
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
 * <h3>Contract sizing</h3>
 * Kraken Futures orders are placed in integer contracts. The contract value
 * for PF_XBTUSD is 1 USD per contract, for PF_ETHUSD it is 0.01 ETH per
 * contract, etc. This implementation uses a simplified approach: the EUR
 * notional is divided by the current mark price (obtained from the ticker
 * endpoint) and rounded to the nearest whole contract. This gives an
 * approximation that is accurate enough for intraday sizing where EUR/USD
 * parity is close.
 *
 * <h3>Short selling</h3>
 * A short entry on Kraken Futures is a regular {@code side=sell} on a
 * perpetual futures contract — no borrow mechanics. Closing requires
 * {@code reduceOnly=true} to avoid accidentally flipping the position.
 *
 * @see <a href="https://docs.kraken.com/api/docs/futures-api/trading/order-management/">
 *      Kraken Futures Order Management</a>
 */
@Service
@Profile("kraken-futures")
@Slf4j
public class KrakenFuturesOrderClient implements BrokerOrderClient {

    private static final String SEND_ORDER_PATH   = "/sendorder";
    private static final String OPEN_POSITIONS_PATH = "/openpositions";
    private static final String ACCOUNTS_PATH     = "/accounts";
    private static final String TICKER_PATH       = "/tickers/";

    @Autowired
    private KrakenFuturesHttpClient httpClient;

    // ── BrokerOrderClient ────────────────────────────────────────────────

    @Override
    public OrderResponse placeLongEntry(String symbol, BigDecimal eurAmount) {
        BigDecimal markPrice = getMarkPrice(symbol);
        if (markPrice == null || markPrice.compareTo(BigDecimal.ZERO) == 0) {
            return failedResponse(symbol, "BUY", "Could not retrieve mark price for contract sizing");
        }
        long contracts = eurAmount.divide(markPrice, 0, RoundingMode.HALF_UP).longValue();
        if (contracts <= 0) {
            return failedResponse(symbol, "BUY", "Computed contract count is 0 — eurAmount too small");
        }
        log.info("KrakenFuturesOrderClient: LONG ENTRY {} — {}EUR @ {} markPrice = {} contracts",
                symbol, eurAmount, markPrice, contracts);
        return sendOrder(symbol, "buy", contracts, false);
    }

    @Override
    public OrderResponse placeLongExit(String symbol, BigDecimal quantity) {
        long contracts = quantity.setScale(0, RoundingMode.HALF_UP).longValue();
        log.info("KrakenFuturesOrderClient: LONG EXIT {} — {} contracts (reduceOnly)", symbol, contracts);
        return sendOrder(symbol, "sell", contracts, true);
    }

    @Override
    public OrderResponse placeShortEntry(String symbol, BigDecimal eurAmount) {
        BigDecimal markPrice = getMarkPrice(symbol);
        if (markPrice == null || markPrice.compareTo(BigDecimal.ZERO) == 0) {
            return failedResponse(symbol, "SELL", "Could not retrieve mark price for contract sizing");
        }
        long contracts = eurAmount.divide(markPrice, 0, RoundingMode.HALF_UP).longValue();
        if (contracts <= 0) {
            return failedResponse(symbol, "SELL", "Computed contract count is 0 — eurAmount too small");
        }
        log.info("KrakenFuturesOrderClient: SHORT ENTRY {} — {}EUR @ {} markPrice = {} contracts",
                symbol, eurAmount, markPrice, contracts);
        return sendOrder(symbol, "sell", contracts, false);
    }

    @Override
    public OrderResponse placeShortExit(String symbol, BigDecimal quantity) {
        long contracts = quantity.setScale(0, RoundingMode.HALF_UP).longValue();
        log.info("KrakenFuturesOrderClient: SHORT EXIT {} — {} contracts (reduceOnly)", symbol, contracts);
        return sendOrder(symbol, "buy", contracts, true);
    }

    @Override
    public BigDecimal getAvailableBalance() {
        try {
            AccountsResponse resp = httpClient.get(ACCOUNTS_PATH, AccountsResponse.class);
            if (resp == null || resp.accounts == null) return BigDecimal.ZERO;
            // Use the flexible (multi-collateral) account's available funds
            for (Account acc : resp.accounts) {
                if ("flex".equalsIgnoreCase(acc.type) || "cash".equalsIgnoreCase(acc.type)) {
                    if (acc.availableFunds != null) {
                        return acc.availableFunds;
                    }
                }
            }
            // Fallback: return the first non-null available balance
            return resp.accounts.stream()
                    .filter(a -> a.availableFunds != null)
                    .map(a -> a.availableFunds)
                    .findFirst()
                    .orElse(BigDecimal.ZERO);
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

    /**
     * Returns all currently open positions.
     * Useful for reconciliation and paper-trading verification.
     */
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

    // ── private helpers ──────────────────────────────────────────────────

    /**
     * Sends a market order to Kraken Futures.
     *
     * @param symbol      e.g. "PF_XBTUSD"
     * @param side        "buy" or "sell"
     * @param contracts   number of contracts
     * @param reduceOnly  true for closing orders (prevents position flip)
     */
    private OrderResponse sendOrder(String symbol, String side, long contracts, boolean reduceOnly) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("orderType", "mkt");   // market order
        params.add("symbol", symbol);
        params.add("side", side);
        params.add("size", String.valueOf(contracts));
        params.add("cliOrdId", "frosk-" + UUID.randomUUID());
        if (reduceOnly) {
            params.add("reduceOnly", "true");
        }

        try {
            SendOrderResponse resp = httpClient.post(SEND_ORDER_PATH, params, SendOrderResponse.class);
            return mapSendOrderResponse(resp, symbol, side.toUpperCase());
        } catch (Exception e) {
            log.error("KrakenFuturesOrderClient: sendOrder({} {} {}) failed", side, contracts, symbol, e);
            return failedResponse(symbol, side.toUpperCase(), e.getMessage());
        }
    }

    /** Fetches the current mark price from the ticker endpoint. */
    private BigDecimal getMarkPrice(String symbol) {
        try {
            TickerResponse resp = httpClient.getPublic(TICKER_PATH + symbol, TickerResponse.class);
            if (resp == null || resp.tickers == null || resp.tickers.isEmpty()) return null;
            return resp.tickers.get(0).markPrice;
        } catch (Exception e) {
            log.warn("KrakenFuturesOrderClient: could not fetch mark price for {}", symbol, e);
            return null;
        }
    }

    private OrderResponse mapSendOrderResponse(SendOrderResponse raw, String symbol, String side) {
        OrderResponse r = new OrderResponse();
        r.setProductId(symbol);
        r.setSide(side);
        if (raw == null) {
            r.setStatus("FAILED");
            r.setErrorMessage("Null response from Kraken API");
            return r;
        }
        if ("placed".equals(raw.sendStatus != null ? raw.sendStatus.status : null)) {
            r.setOrderId(raw.sendStatus.orderId);
            r.setClientOrderId(raw.sendStatus.cliOrdId);
            r.setStatus("FILLED");  // market orders fill immediately
        } else {
            r.setStatus("FAILED");
            r.setErrorMessage(raw.sendStatus != null ? raw.sendStatus.status : "unknown");
        }
        return r;
    }

    private OrderResponse failedResponse(String symbol, String side, String message) {
        OrderResponse r = new OrderResponse();
        r.setProductId(symbol);
        r.setSide(side);
        r.setStatus("FAILED");
        r.setErrorMessage(message);
        return r;
    }

    // ── response POJOs ───────────────────────────────────────────────────

    @Data @NoArgsConstructor
    public static class SendOrderResponse {
        private String result;
        @JsonProperty("sendStatus") private SendStatus sendStatus;

        @Data @NoArgsConstructor
        public static class SendStatus {
            private String status;
            @JsonProperty("order_id") private String orderId;
            @JsonProperty("cliOrdId") private String cliOrdId;
            @JsonProperty("receivedTime") private String receivedTime;
        }
    }

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
        @JsonProperty("markPrice") private BigDecimal markPrice;
        @JsonProperty("unrealizedFunding") private BigDecimal unrealizedFunding;
        @JsonProperty("pnl") private BigDecimal pnl;
    }

    @Data @NoArgsConstructor
    public static class AccountsResponse {
        private String result;
        @JsonProperty("accounts") private List<Account> accounts;
    }

    @Data @NoArgsConstructor
    public static class Account {
        @JsonProperty("type") private String type;
        @JsonProperty("availableFunds") private BigDecimal availableFunds;
        @JsonProperty("balanceValue") private BigDecimal balanceValue;
        @JsonProperty("portfolioValue") private BigDecimal portfolioValue;
        @JsonProperty("marginRequirements") private BigDecimal marginRequirements;
    }

    @Data @NoArgsConstructor
    public static class TickerResponse {
        private String result;
        private List<Ticker> tickers;

        @Data @NoArgsConstructor
        public static class Ticker {
            private String symbol;
            @JsonProperty("markPrice") private BigDecimal markPrice;
            @JsonProperty("bid") private BigDecimal bid;
            @JsonProperty("ask") private BigDecimal ask;
            @JsonProperty("last") private BigDecimal last;
        }
    }
}
