package nu.itark.frosk.controller;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.analysis.CoinbaseLiveAccountDTO;
import nu.itark.frosk.crypto.coinbase.advanced.Coinbase;
import org.springframework.context.annotation.Profile;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Read-only view of the REAL Coinbase (Advanced Trade) account, for the
 * dashboard's "Coinbase Konto" page (crypto process, port 8081).
 *
 * <p>Places no orders and changes nothing on the exchange — a paginated
 * authenticated GET {@code /accounts}, one authenticated GET
 * {@code /orders/historical/batch?order_status=OPEN}, one authenticated GET
 * {@code /orders/historical/fills}, plus one authenticated {@code /products/{id}}
 * call per non-EUR currency held to mark it to EUR. Authentication is
 * delegated to the existing {@link Coinbase} bean (JWT-signed), so the API key
 * configured for the {@code crypto} profile is the only configuration.
 *
 * <p>Responses are parsed as {@link JsonNode} rather than into fixed POJOs
 * deliberately, mirroring {@code KrakenAccountController}: a field that moves
 * or disappears degrades to a null cell in the dashboard instead of a
 * deserialization failure that blanks the whole page.
 *
 * <p>Never returns a 4xx/5xx. A missing credential or a Coinbase outage
 * surfaces as {@code available=false} plus an {@code error} string, which is
 * what the card renders.
 */
@RestController
@Profile("crypto")
@RequiredArgsConstructor
@Slf4j
public class CoinbaseAccountController {

    private static final String ACCOUNTS_PATH = "/accounts";
    private static final String ORDERS_PATH   = "/orders/historical/batch";
    private static final String FILLS_PATH    = "/orders/historical/fills";
    private static final String PRODUCTS_PATH = "/products";

    /** How far back the "recent fills" feed reaches. */
    private static final int FILLS_WINDOW_DAYS = 7;

    /** Pagination safety cap — mirrors CoinbaseOrderClient's own cap. */
    private static final int MAX_PAGES = 20;

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final Coinbase coinbase;

    /**
     * @Example GET http://localhost:8081/coinbase/live-account
     */
    @GetMapping("/coinbase/live-account")
    public CoinbaseLiveAccountDTO getLiveAccount() {
        log.info("GET /coinbase/live-account");

        List<JsonNode> accountPages = safeGetAllPages(ACCOUNTS_PATH, "accounts");
        if (accountPages == null) {
            return unavailable("Inget svar från Coinbase /accounts — kontrollera API-nyckel och nätverk");
        }

        List<CoinbaseLiveAccountDTO.Holding> holdings = parseHoldings(accountPages);
        BigDecimal totalValueEur = holdings.stream()
                .map(CoinbaseLiveAccountDTO.Holding::getValueEur)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<JsonNode> orderPages = safeGetAllPages(ORDERS_PATH + "?order_status=OPEN", "orders");
        List<JsonNode> fillPages = safeGetAllPages(FILLS_PATH, "fills");

        return CoinbaseLiveAccountDTO.builder()
                .available(true)
                .fetchedAt(LocalDateTime.now().format(TS))
                .totalValueEur(totalValueEur)
                .holdings(holdings)
                .openOrders(parseOrders(orderPages))
                .recentFills(parseFills(fillPages))
                .build();
    }

    // ── Coinbase calls ───────────────────────────────────────────────────

    /**
     * Follows {@code has_next}/{@code cursor} pagination (same contract as
     * {@code CoinbaseOrderClient.getAllBalances}), returning every page's array
     * under {@code arrayField} as one flat list of its elements. Null — not an
     * empty list — means the very first call failed, which the caller treats as
     * "Coinbase unreachable"; a later page failing just stops pagination early
     * with whatever was already fetched.
     */
    private List<JsonNode> safeGetAllPages(String path, String arrayField) {
        List<JsonNode> out = new ArrayList<>();
        String endpoint = path;
        int pages = 0;
        boolean first = true;
        do {
            JsonNode page;
            try {
                page = coinbase.get(endpoint, new ParameterizedTypeReference<JsonNode>() {});
            } catch (Exception e) {
                log.warn("Coinbase {} failed — {}", endpoint, e.toString());
                page = null;
            }
            if (page == null) {
                return first ? null : out;
            }
            first = false;
            if (page.path(arrayField).isArray()) {
                for (JsonNode item : page.path(arrayField)) {
                    out.add(item);
                }
            }
            if (page.path("has_next").asBoolean(false) && text(page, "cursor") != null) {
                endpoint = path + (path.contains("?") ? "&" : "?") + "cursor=" + text(page, "cursor");
            } else {
                break;
            }
        } while (++pages < MAX_PAGES);
        return out;
    }

    /** Price lookup for one unit of {@code currency} in EUR; null on any failure. */
    private BigDecimal fetchEurPrice(String currency) {
        try {
            JsonNode product = coinbase.get(PRODUCTS_PATH + "/" + currency + "-EUR", new ParameterizedTypeReference<JsonNode>() {});
            return product != null ? dec(product, "price") : null;
        } catch (Exception e) {
            log.debug("Coinbase {}-EUR price lookup failed — {}", currency, e.toString());
            return null;
        }
    }

    // ── parsing ──────────────────────────────────────────────────────────

    private List<CoinbaseLiveAccountDTO.Holding> parseHoldings(List<JsonNode> accounts) {
        List<CoinbaseLiveAccountDTO.Holding> out = new ArrayList<>();
        for (JsonNode a : accounts) {
            BigDecimal available = dec(a.path("available_balance"), "value");
            BigDecimal hold = dec(a.path("hold"), "value");
            // Only non-zero balances — a crypto account typically holds dozens of
            // zero-balance currency rows that would otherwise swamp the table.
            boolean nonZero = (available != null && available.compareTo(BigDecimal.ZERO) != 0)
                    || (hold != null && hold.compareTo(BigDecimal.ZERO) != 0);
            if (!nonZero) continue;

            String currency = text(a, "currency");
            BigDecimal eurPrice = null;
            BigDecimal valueEur = null;
            if ("EUR".equalsIgnoreCase(currency)) {
                valueEur = available;
            } else if (currency != null) {
                eurPrice = fetchEurPrice(currency);
                if (eurPrice != null && available != null) {
                    valueEur = available.multiply(eurPrice).setScale(2, RoundingMode.HALF_UP);
                }
            }

            out.add(CoinbaseLiveAccountDTO.Holding.builder()
                    .currency(currency)
                    .availableBalance(available)
                    .hold(hold)
                    .type(text(a, "type"))
                    .eurPrice(eurPrice)
                    .valueEur(valueEur)
                    .build());
        }
        // Largest EUR value first; holdings with no resolvable EUR value sort last.
        out.sort(Comparator.comparing(
                (CoinbaseLiveAccountDTO.Holding h) -> h.getValueEur() != null ? h.getValueEur() : BigDecimal.valueOf(-1),
                Comparator.reverseOrder()));
        return out;
    }

    private List<CoinbaseLiveAccountDTO.Order> parseOrders(List<JsonNode> orders) {
        List<CoinbaseLiveAccountDTO.Order> out = new ArrayList<>();
        if (orders == null) return out;
        for (JsonNode o : orders) {
            out.add(CoinbaseLiveAccountDTO.Order.builder()
                    .orderId(text(o, "order_id"))
                    .productId(text(o, "product_id"))
                    .side(text(o, "side"))
                    .orderType(text(o, "order_type"))
                    .status(text(o, "status"))
                    .limitPrice(dec(o, "limit_price"))
                    .averageFilledPrice(dec(o, "average_filled_price"))
                    .filledSize(dec(o, "filled_size"))
                    .filledValue(dec(o, "filled_value"))
                    .createdAt(timestamp(o, "created_time"))
                    .build());
        }
        return out;
    }

    private List<CoinbaseLiveAccountDTO.Fill> parseFills(List<JsonNode> fills) {
        List<CoinbaseLiveAccountDTO.Fill> out = new ArrayList<>();
        if (fills == null) return out;

        Instant cutoff = Instant.now().minusSeconds(FILLS_WINDOW_DAYS * 24L * 3600L);

        for (JsonNode f : fills) {
            Instant tradeTime = instant(f, "trade_time");
            if (tradeTime != null && tradeTime.isBefore(cutoff)) continue;

            out.add(CoinbaseLiveAccountDTO.Fill.builder()
                    .tradeId(text(f, "trade_id"))
                    .orderId(text(f, "order_id"))
                    .productId(text(f, "product_id"))
                    .side(text(f, "side"))
                    .size(dec(f, "size"))
                    .price(dec(f, "price"))
                    .commission(dec(f, "commission"))
                    .liquidityIndicator(text(f, "liquidity_indicator"))
                    .tradeTime(timestamp(f, "trade_time"))
                    .build());
        }
        // Newest first — the dashboard feed reads top-down. Fills with an
        // unparsable timestamp sort last rather than being dropped.
        out.sort(Comparator.comparing(CoinbaseLiveAccountDTO.Fill::getTradeTime,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return out;
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private CoinbaseLiveAccountDTO unavailable(String error) {
        return CoinbaseLiveAccountDTO.builder()
                .available(false)
                .error(error)
                .fetchedAt(LocalDateTime.now().format(TS))
                .holdings(List.of())
                .openOrders(List.of())
                .recentFills(List.of())
                .build();
    }

    private static String text(JsonNode node, String field) {
        JsonNode v = node.path(field);
        return v.isMissingNode() || v.isNull() || !v.isValueNode() ? null : v.asText();
    }

    private static BigDecimal dec(JsonNode node, String field) {
        JsonNode v = node.path(field);
        if (v.isMissingNode() || v.isNull() || !v.isValueNode()) return null;
        try {
            return new BigDecimal(v.asText());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** ISO-8601 instant from Coinbase, or null when absent/unparsable. */
    private static Instant instant(JsonNode node, String field) {
        String raw = text(node, field);
        if (raw == null) return null;
        try {
            return Instant.parse(raw);
        } catch (Exception e) {
            return null;
        }
    }

    private static String timestamp(JsonNode node, String field) {
        Instant i = instant(node, field);
        return i == null ? null : LocalDateTime.ofInstant(i, ZoneId.systemDefault()).format(TS);
    }
}
