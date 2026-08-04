package nu.itark.frosk.crypto.coinbase.model;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * Top-of-book snapshot from Advanced Trade {@code GET /product_book}.
 *
 * <p>Only the best bid and ask are needed: the spread they imply is a real,
 * unmodelled cost. Backtests and the paper account both fill at the 15m bar
 * close, but a market entry pays the ask and a market exit hits the bid, so a
 * round trip gives up roughly one full spread beyond the taker fee.
 */
@Data
public class ProductBook {

    private Book pricebook;

    @Data
    public static class Book {
        private String product_id;
        private List<Level> bids;
        private List<Level> asks;
    }

    @Data
    public static class Level {
        private BigDecimal price;
        private BigDecimal size;
    }

    /** Spread as a percentage of mid, or null when the book is empty/unusable. */
    public BigDecimal spreadPercent() {
        if (pricebook == null) return null;
        List<Level> bids = pricebook.getBids();
        List<Level> asks = pricebook.getAsks();
        if (bids == null || asks == null || bids.isEmpty() || asks.isEmpty()) return null;
        BigDecimal bid = bids.get(0).getPrice();
        BigDecimal ask = asks.get(0).getPrice();
        if (bid == null || ask == null || bid.signum() <= 0 || ask.signum() <= 0) return null;
        BigDecimal mid = bid.add(ask).divide(BigDecimal.valueOf(2), 12, java.math.RoundingMode.HALF_UP);
        if (mid.signum() <= 0) return null;
        return ask.subtract(bid)
                .divide(mid, 8, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    public BigDecimal bestBid() {
        if (pricebook == null || pricebook.getBids() == null || pricebook.getBids().isEmpty()) return null;
        return pricebook.getBids().get(0).getPrice();
    }

    public BigDecimal bestAsk() {
        if (pricebook == null || pricebook.getAsks() == null || pricebook.getAsks().isEmpty()) return null;
        return pricebook.getAsks().get(0).getPrice();
    }
}
