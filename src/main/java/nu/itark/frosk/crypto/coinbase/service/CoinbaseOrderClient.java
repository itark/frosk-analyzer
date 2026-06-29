package nu.itark.frosk.crypto.coinbase.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.crypto.coinbase.advanced.Coinbase;
import nu.itark.frosk.crypto.livetrading.OrderResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Thin wrapper around the Coinbase Advanced Trade v3 orders API.
 *
 * <p>Uses the existing {@link Coinbase} (CoinbaseImpl) for JWT-signed requests.
 * All order IDs use the prefix {@code frosk-} so they are identifiable in the
 * Coinbase console.
 */
@Service
@Profile("crypto")
@Slf4j
public class CoinbaseOrderClient {

    private static final String ORDERS_ENDPOINT   = "/orders";
    private static final String ACCOUNTS_ENDPOINT = "/accounts";

    @Autowired
    private Coinbase coinbase;

    // ── public API ───────────────────────────────────────────────────────

    /** Market BUY for {@code eurAmount} EUR (quote_size). */
    public OrderResponse placeBuyOrder(String productId, BigDecimal eurAmount) {
        CreateOrderRequest req = new CreateOrderRequest(
                "frosk-" + UUID.randomUUID(),
                productId,
                "BUY",
                new OrderConfiguration(new MarketIoc(eurAmount.toPlainString(), null))
        );
        log.info("CoinbaseOrderClient: BUY {} {} EUR — clientOrderId={}", productId, eurAmount, req.clientOrderId);
        return placeOrder(req);
    }

    /** Market SELL for {@code baseSize} coins (base_size). */
    public OrderResponse placeSellOrder(String productId, BigDecimal baseSize) {
        CreateOrderRequest req = new CreateOrderRequest(
                "frosk-" + UUID.randomUUID(),
                productId,
                "SELL",
                new OrderConfiguration(new MarketIoc(null, baseSize.toPlainString()))
        );
        log.info("CoinbaseOrderClient: SELL {} {} coins — clientOrderId={}", productId, baseSize, req.clientOrderId);
        return placeOrder(req);
    }

    /** Fetch the current status of an order. */
    public OrderResponse getOrder(String coinbaseOrderId) {
        try {
            GetOrderApiResponse raw = coinbase.get(
                    ORDERS_ENDPOINT + "/" + coinbaseOrderId,
                    new ParameterizedTypeReference<GetOrderApiResponse>() {}
            );
            if (raw == null || raw.order == null) {
                return failedResponse(coinbaseOrderId, null, null, "No response from API");
            }
            return mapOrderDetail(raw.order);
        } catch (Exception e) {
            log.error("CoinbaseOrderClient: getOrder({}) failed", coinbaseOrderId, e);
            return failedResponse(coinbaseOrderId, null, null, e.getMessage());
        }
    }

    /** Available EUR balance in the Coinbase account. */
    public BigDecimal getEurBalance() {
        return getBalance("EUR");
    }

    /** Available balance for a specific currency symbol (e.g. "BTC", "ETH"). Follows pagination. */
    public BigDecimal getBalance(String currency) {
        try {
            String endpoint = ACCOUNTS_ENDPOINT;
            int pages = 0;
            do {
                AccountsApiResponse raw = coinbase.get(endpoint, new ParameterizedTypeReference<AccountsApiResponse>() {});
                if (raw == null || raw.accounts == null) break;
                for (AccountEntry a : raw.accounts) {
                    if (currency.equals(a.currency) && a.availableBalance != null) {
                        return parseSafe(a.availableBalance.value);
                    }
                }
                if (Boolean.TRUE.equals(raw.hasNext) && raw.cursor != null && !raw.cursor.isBlank()) {
                    endpoint = ACCOUNTS_ENDPOINT + "?cursor=" + raw.cursor;
                } else {
                    break;
                }
            } while (++pages < 20);
        } catch (Exception e) {
            log.error("CoinbaseOrderClient: getBalance({}) failed", currency, e);
        }
        return BigDecimal.ZERO;
    }

    /** Returns available balances for all non-zero accounts, keyed by currency symbol. Follows pagination. */
    public Map<String, BigDecimal> getAllBalances() {
        Map<String, BigDecimal> result = new HashMap<>();
        try {
            String endpoint = ACCOUNTS_ENDPOINT;
            int pages = 0;
            do {
                AccountsApiResponse raw = coinbase.get(endpoint, new ParameterizedTypeReference<AccountsApiResponse>() {});
                if (raw == null || raw.accounts == null) break;
                for (AccountEntry a : raw.accounts) {
                    if (a.currency != null && a.availableBalance != null) {
                        BigDecimal value = parseSafe(a.availableBalance.value);
                        if (value.compareTo(BigDecimal.ZERO) > 0) {
                            result.put(a.currency, value);
                        }
                    }
                }
                if (Boolean.TRUE.equals(raw.hasNext) && raw.cursor != null && !raw.cursor.isBlank()) {
                    endpoint = ACCOUNTS_ENDPOINT + "?cursor=" + raw.cursor;
                } else {
                    break;
                }
            } while (++pages < 20); // safety cap
        } catch (Exception e) {
            log.error("CoinbaseOrderClient: getAllBalances() failed", e);
        }
        return result;
    }

    // ── private helpers ──────────────────────────────────────────────────

    private OrderResponse placeOrder(CreateOrderRequest req) {
        try {
            CreateOrderApiResponse raw = coinbase.post(
                    ORDERS_ENDPOINT,
                    new ParameterizedTypeReference<CreateOrderApiResponse>() {},
                    req
            );
            return mapCreateResponse(raw, req);
        } catch (Exception e) {
            log.error("CoinbaseOrderClient: placeOrder({}) failed", req.clientOrderId, e);
            return failedResponse(null, req.productId, req.side, e.getMessage());
        }
    }

    private OrderResponse mapCreateResponse(CreateOrderApiResponse raw, CreateOrderRequest req) {
        OrderResponse r = new OrderResponse();
        r.setClientOrderId(req.clientOrderId);
        r.setProductId(req.productId);
        r.setSide(req.side);
        if (raw == null) {
            r.setStatus("FAILED");
            r.setErrorMessage("Null response from API");
            return r;
        }
        if (raw.success && raw.successResponse != null) {
            r.setOrderId(raw.successResponse.orderId);
            r.setStatus("PENDING");
        } else {
            r.setStatus("FAILED");
            if (raw.errorResponse != null) {
                r.setErrorMessage(raw.errorResponse.message);
            }
        }
        return r;
    }

    private OrderResponse mapOrderDetail(OrderDetail d) {
        OrderResponse r = new OrderResponse();
        r.setOrderId(d.orderId);
        r.setProductId(d.productId);
        r.setSide(d.side);
        r.setStatus(d.status);
        r.setClientOrderId(d.clientOrderId);
        r.setFilledSize(parseSafeNullable(d.filledSize));
        r.setAverageFilledPrice(parseSafeNullable(d.averageFilledPrice));
        return r;
    }

    private OrderResponse failedResponse(String orderId, String productId, String side, String message) {
        OrderResponse r = new OrderResponse();
        r.setOrderId(orderId);
        r.setProductId(productId);
        r.setSide(side);
        r.setStatus("FAILED");
        r.setErrorMessage(message);
        return r;
    }

    private BigDecimal parseSafe(String value) {
        if (value == null || value.isBlank()) return BigDecimal.ZERO;
        try { return new BigDecimal(value); } catch (NumberFormatException e) { return BigDecimal.ZERO; }
    }

    private BigDecimal parseSafeNullable(String value) {
        if (value == null || value.isBlank()) return null;
        try { return new BigDecimal(value); } catch (NumberFormatException e) { return null; }
    }

    // ── request POJOs ────────────────────────────────────────────────────

    @Data
    @RequiredArgsConstructor
    static class CreateOrderRequest {
        @JsonProperty("client_order_id") private final String clientOrderId;
        @JsonProperty("product_id")      private final String productId;
                                         private final String side;
        @JsonProperty("order_configuration") private final OrderConfiguration orderConfiguration;
    }

    @Data
    @RequiredArgsConstructor
    static class OrderConfiguration {
        @JsonProperty("market_market_ioc") private final MarketIoc marketMarketIoc;
    }

    @Data
    @RequiredArgsConstructor
    static class MarketIoc {
        @JsonProperty("quote_size") private final String quoteSize; // EUR for BUY
        @JsonProperty("base_size")  private final String baseSize;  // coins for SELL
    }

    // ── response POJOs ───────────────────────────────────────────────────

    @Data @NoArgsConstructor
    static class CreateOrderApiResponse {
        private boolean success;
        @JsonProperty("order_id")        private String orderId;
        @JsonProperty("success_response") private SuccessResponse successResponse;
        @JsonProperty("error_response")   private ErrorResponse errorResponse;
    }

    @Data @NoArgsConstructor
    static class SuccessResponse {
        @JsonProperty("order_id")        private String orderId;
        @JsonProperty("product_id")      private String productId;
                                         private String side;
        @JsonProperty("client_order_id") private String clientOrderId;
    }

    @Data @NoArgsConstructor
    static class ErrorResponse {
        private String error;
        private String message;
        @JsonProperty("preview_failure_reason")   private String previewFailureReason;
        @JsonProperty("new_order_failure_reason") private String newOrderFailureReason;
    }

    @Data @NoArgsConstructor
    static class GetOrderApiResponse {
        private OrderDetail order;
    }

    @Data @NoArgsConstructor
    static class OrderDetail {
        @JsonProperty("order_id")              private String orderId;
        @JsonProperty("product_id")            private String productId;
                                               private String side;
        @JsonProperty("client_order_id")       private String clientOrderId;
                                               private String status;
        @JsonProperty("filled_size")           private String filledSize;
        @JsonProperty("average_filled_price")  private String averageFilledPrice;
    }

    @Data @NoArgsConstructor
    static class AccountsApiResponse {
        private List<AccountEntry> accounts;
        @JsonProperty("has_next") private Boolean hasNext;
        private String cursor;
    }

    @Data @NoArgsConstructor
    static class AccountEntry {
        private String currency;
        @JsonProperty("available_balance") private BalanceAmount availableBalance;
    }

    @Data @NoArgsConstructor
    static class BalanceAmount {
        private String value;
        private String currency;
    }
}
