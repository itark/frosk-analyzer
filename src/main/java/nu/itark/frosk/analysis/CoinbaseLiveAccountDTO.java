package nu.itark.frosk.analysis;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * Snapshot of the REAL Coinbase (Advanced Trade) account behind
 * {@code GET /coinbase/live-account} (crypto process, port 8081).
 *
 * <p>This is the Coinbase counterpart to {@link KrakenLiveAccountDTO}: the
 * dashboard "Coinbase Konto" page shows this next to the paper-trading
 * simulation so an obviously-wrong paper simulation is visible against what
 * the exchange actually reports. Coinbase spot has no leveraged positions, so
 * where Kraken has {@code openPositions} this has {@link #holdings} — the
 * non-zero balance in every currency the account holds.
 *
 * <p>Every field is best-effort. The Coinbase REST API is reached over the
 * network with credentials that may be missing on a given machine, so the
 * endpoint never fails the request: {@link #available} is {@code false} and
 * {@link #error} carries the reason, with the three lists empty.
 */
@Data
@Builder
public class CoinbaseLiveAccountDTO {

    /** True when at least the {@code /accounts} call came back with data. */
    private boolean available;

    /** Human-readable reason when {@link #available} is false; null otherwise. */
    private String error;

    /** When this snapshot was taken, {@code yyyy-MM-dd HH:mm} in server-local time. */
    private String fetchedAt;

    /**
     * Sum of every holding's {@link Holding#getValueEur()}, skipping any holding
     * whose EUR value could not be resolved — so this is a floor, not an exact
     * total, whenever a currency without a direct {@code *-EUR} product is held.
     */
    private BigDecimal totalValueEur;

    /** One entry per non-zero-balance Coinbase account (one per currency). */
    private List<Holding> holdings;

    private List<Order> openOrders;

    /** Fills from the last 7 days, newest first. */
    private List<Fill> recentFills;

    // ── nested value types ───────────────────────────────────────────────

    /**
     * A single currency balance on the Coinbase account. Coinbase keys
     * {@code /accounts} one row per currency (EUR, BTC, ETH, …), unlike
     * Kraken's cash/flex/per-contract account split.
     */
    @Data
    @Builder
    public static class Holding {
        /** Currency symbol, e.g. {@code EUR}, {@code BTC}. */
        private String currency;
        private BigDecimal availableBalance;
        /** Amount held for pending orders/transfers, on top of {@link #availableBalance}. */
        private BigDecimal hold;
        /** Coinbase's account type, e.g. {@code CRYPTO}, {@code FIAT}. */
        private String type;
        /**
         * EUR price of one unit of {@link #currency}, from the public
         * {@code {currency}-EUR} product; null for EUR itself (price is 1 by
         * definition, not fetched) and for any currency without that product.
         */
        private BigDecimal eurPrice;
        /**
         * {@code availableBalance × eurPrice} ({@code availableBalance} itself
         * when the currency is EUR); null when {@link #eurPrice} is unavailable.
         */
        private BigDecimal valueEur;
    }

    @Data
    @Builder
    public static class Order {
        private String orderId;
        private String productId;
        /** {@code BUY} or {@code SELL}. */
        private String side;
        /** {@code MARKET}, {@code LIMIT}, {@code STOP}, … */
        private String orderType;
        /** Coinbase's order status string, e.g. {@code OPEN}, {@code PENDING}. */
        private String status;
        private BigDecimal limitPrice;
        private BigDecimal averageFilledPrice;
        private BigDecimal filledSize;
        private BigDecimal filledValue;
        /** When the order was created, {@code yyyy-MM-dd HH:mm}. */
        private String createdAt;
    }

    @Data
    @Builder
    public static class Fill {
        private String tradeId;
        private String orderId;
        private String productId;
        /** {@code BUY} or {@code SELL}. */
        private String side;
        private BigDecimal size;
        private BigDecimal price;
        private BigDecimal commission;
        /** {@code MAKER} or {@code TAKER}. */
        private String liquidityIndicator;
        /** When the fill happened, {@code yyyy-MM-dd HH:mm}. */
        private String tradeTime;
    }
}
