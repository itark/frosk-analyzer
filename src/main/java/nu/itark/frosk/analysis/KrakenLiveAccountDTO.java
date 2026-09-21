package nu.itark.frosk.analysis;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * Snapshot of the REAL Kraken Futures account behind
 * {@code GET /kraken/live-account} (kraken-futures process, port 8082).
 *
 * <p>This is the live counterpart to {@link CryptoPaperAccountDTO}: the dashboard
 * "Kraken Konto" page shows the two side by side, so an obviously-wrong paper
 * simulation is visible against what the exchange actually reports.
 *
 * <p>Every field is best-effort. The Kraken REST API is reached over the network
 * with credentials that may be missing on a given machine, so the endpoint never
 * fails the request: {@link #available} is {@code false} and {@link #error}
 * carries the reason, with the three lists empty.
 */
@Data
@Builder
public class KrakenLiveAccountDTO {

    /** True when at least the {@code /accounts} call came back with data. */
    private boolean available;

    /** Human-readable reason when {@link #available} is false; null otherwise. */
    private String error;

    /** When this snapshot was taken, {@code yyyy-MM-dd HH:mm} in server-local time. */
    private String fetchedAt;

    /** One entry per Kraken account (cash, flex/multi-collateral, per-contract margin accounts). */
    private List<Account> accounts;

    private List<Position> openPositions;

    private List<Order> openOrders;

    /** Fills from the last 7 days, newest first. */
    private List<Fill> recentFills;

    // ── nested value types ───────────────────────────────────────────────

    /**
     * A single Kraken account. Kraken's {@code /accounts} response keys the
     * accounts by name ("cash", "flex", "fi_xbtusd", …) and the available fields
     * differ per account type, so anything absent stays null rather than 0 — a
     * missing value and a zero value mean different things on a margin account.
     */
    @Data
    @Builder
    public static class Account {
        /** Map key from Kraken, e.g. {@code flex}. */
        private String name;
        /** Kraken's own type string, e.g. {@code multiCollateralMarginAccount}. */
        private String type;
        /** Total account value in USD. */
        private BigDecimal balanceValue;
        /** Balance + unrealized PnL of open positions. */
        private BigDecimal portfolioValue;
        private BigDecimal collateralValue;
        /** Portfolio value less initial margin — what is left to open with. */
        private BigDecimal availableMargin;
        private BigDecimal marginEquity;
        private BigDecimal initialMargin;
        private BigDecimal maintenanceMargin;
        /** Unrealized PnL on open positions. */
        private BigDecimal pnl;
        private BigDecimal unrealizedFunding;
        private BigDecimal totalUnrealized;
    }

    @Data
    @Builder
    public static class Position {
        private String symbol;
        /** {@code long} or {@code short}, as Kraken reports it. */
        private String side;
        /** Position size in contracts. */
        private BigDecimal size;
        /** Average fill price of the position. */
        private BigDecimal entryPrice;
        /** Current mark price from the public ticker feed; null when unavailable. */
        private BigDecimal markPrice;
        /** {@code size × markPrice} — exposure in USD; null when mark price is unavailable. */
        private BigDecimal notionalUsd;
        /** {@code (mark − entry) × size}, negated for shorts; null when mark price is unavailable. */
        private BigDecimal unrealizedPnlUsd;
        /** Unrealized PnL as a percentage of the position's entry notional. */
        private BigDecimal unrealizedPnlPct;
        /** Funding accrued but not yet settled, as reported by Kraken. */
        private BigDecimal unrealizedFunding;
        /** When the position was opened, {@code yyyy-MM-dd HH:mm}. */
        private String openedAt;
    }

    @Data
    @Builder
    public static class Order {
        private String orderId;
        private String symbol;
        private String side;
        /** {@code lmt}, {@code stp}, {@code take_profit}, … */
        private String orderType;
        private BigDecimal limitPrice;
        private BigDecimal stopPrice;
        private BigDecimal size;
        private BigDecimal filledSize;
        private BigDecimal unfilledSize;
        private boolean reduceOnly;
        private String status;
        /** When Kraken received the order, {@code yyyy-MM-dd HH:mm}. */
        private String receivedAt;
    }

    @Data
    @Builder
    public static class Fill {
        private String fillId;
        private String orderId;
        private String symbol;
        private String side;
        private BigDecimal size;
        private BigDecimal price;
        /** {@code taker}, {@code maker}, {@code liquidation}, … */
        private String fillType;
        /** When the fill happened, {@code yyyy-MM-dd HH:mm}. */
        private String filledAt;
    }
}
