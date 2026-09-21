package nu.itark.frosk.controller;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.analysis.KrakenLiveAccountDTO;
import nu.itark.frosk.crypto.kraken.KrakenFuturesHttpClient;
import org.springframework.context.annotation.Profile;
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Read-only view of the REAL Kraken Futures account, for the dashboard's
 * "Kraken Konto" page (kraken-futures process, port 8082).
 *
 * <p>Places no orders and changes nothing on the exchange — four authenticated
 * GETs ({@code /accounts}, {@code /openpositions}, {@code /openorders},
 * {@code /fills}) plus one public {@code /tickers} call to mark open positions
 * to market. Authentication is delegated to the existing
 * {@link KrakenFuturesHttpClient}, so the credentials in
 * {@code ~/.frosk/kraken-credentials.properties} are the only configuration.
 *
 * <p>Responses are parsed as {@link JsonNode} rather than into fixed POJOs
 * deliberately: Kraken keys {@code /accounts} by account name and the field set
 * differs per account type, so a field that moves or disappears degrades to a
 * null cell in the dashboard instead of a deserialization failure that blanks
 * the whole page.
 *
 * <p>Never returns a 4xx/5xx. A missing credential or a Kraken outage surfaces
 * as {@code available=false} plus an {@code error} string, which is what the
 * card renders.
 */
@RestController
@Profile("kraken-futures")
@RequiredArgsConstructor
@Slf4j
public class KrakenAccountController {

    private static final String ACCOUNTS_PATH       = "/accounts";
    private static final String OPEN_POSITIONS_PATH = "/openpositions";
    private static final String OPEN_ORDERS_PATH    = "/openorders";
    private static final String FILLS_PATH          = "/fills";
    private static final String TICKERS_PATH        = "/tickers";

    /** How far back the "recent fills" feed reaches. */
    private static final int FILLS_WINDOW_DAYS = 7;

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final KrakenFuturesHttpClient httpClient;

    /**
     * @Example GET http://localhost:8082/kraken/live-account
     */
    @GetMapping("/kraken/live-account")
    public KrakenLiveAccountDTO getLiveAccount() {
        log.info("GET /kraken/live-account");

        JsonNode accountsResponse;
        try {
            accountsResponse = httpClient.get(ACCOUNTS_PATH, JsonNode.class);
        } catch (Exception e) {
            log.error("GET /kraken/live-account: /accounts call failed", e);
            return unavailable("Kraken /accounts anropet misslyckades: " + e);
        }

        if (accountsResponse == null) {
            // KrakenFuturesHttpClient logs the HTTP status/body and returns null —
            // by far the most common cause is missing or invalid API credentials.
            return unavailable("Inget svar från Kraken /accounts — kontrollera API-nyckel och nätverk");
        }
        String apiError = errorOf(accountsResponse);
        if (apiError != null) {
            return unavailable(explain(apiError));
        }

        List<KrakenLiveAccountDTO.Account> accounts = parseAccounts(accountsResponse);
        Map<String, BigDecimal> markPrices = fetchMarkPrices();

        return KrakenLiveAccountDTO.builder()
                .available(true)
                .fetchedAt(LocalDateTime.now().format(TS))
                .accounts(accounts)
                .openPositions(parsePositions(safeGet(OPEN_POSITIONS_PATH), markPrices))
                .openOrders(parseOrders(safeGet(OPEN_ORDERS_PATH)))
                .recentFills(parseFills(safeGet(FILLS_PATH)))
                .build();
    }

    // ── Kraken calls ─────────────────────────────────────────────────────

    /** Authenticated GET that degrades to null instead of throwing. */
    private JsonNode safeGet(String path) {
        try {
            JsonNode node = httpClient.get(path, JsonNode.class);
            String apiError = node != null ? errorOf(node) : null;
            if (apiError != null) {
                log.warn("Kraken {} returned error: {}", path, apiError);
                return null;
            }
            return node;
        } catch (Exception e) {
            log.warn("Kraken {} failed — {}", path, e.toString());
            return null;
        }
    }

    /**
     * Mark prices for every Kraken perpetual, from the public bulk ticker
     * endpoint — one unauthenticated call, regardless of how many positions are
     * open. Empty map on any failure; positions then simply show no mark price.
     */
    private Map<String, BigDecimal> fetchMarkPrices() {
        Map<String, BigDecimal> prices = new HashMap<>();
        JsonNode response;
        try {
            response = httpClient.getPublic(TICKERS_PATH, JsonNode.class);
        } catch (Exception e) {
            log.warn("Kraken {} failed — {}", TICKERS_PATH, e.toString());
            return prices;
        }
        if (response == null || !response.path("tickers").isArray()) return prices;

        for (JsonNode ticker : response.path("tickers")) {
            String symbol = text(ticker, "symbol");
            if (symbol == null) continue;
            BigDecimal mark = dec(ticker, "markPrice");
            if (mark == null) mark = dec(ticker, "last");
            if (mark != null) prices.put(symbol, mark);
        }
        return prices;
    }

    // ── parsing ──────────────────────────────────────────────────────────

    /**
     * {@code /accounts} returns an object keyed by account name, not an array:
     * {@code {"accounts": {"cash": {...}, "flex": {...}}}}. Field names also
     * differ between the cash and multi-collateral shapes, hence the fallbacks.
     */
    private List<KrakenLiveAccountDTO.Account> parseAccounts(JsonNode response) {
        List<KrakenLiveAccountDTO.Account> out = new ArrayList<>();
        JsonNode accounts = response.path("accounts");
        if (!accounts.isObject()) return out;

        Iterator<Map.Entry<String, JsonNode>> it = accounts.fields();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> entry = it.next();
            JsonNode a = entry.getValue();
            if (!a.isObject()) continue;

            // Cash accounts nest the numbers one level down under "auxiliary";
            // flex/multi-collateral accounts carry them at the top level.
            JsonNode aux = a.path("auxiliary");
            JsonNode margin = a.path("marginRequirements");

            out.add(KrakenLiveAccountDTO.Account.builder()
                    .name(entry.getKey())
                    .type(text(a, "type"))
                    .balanceValue(firstNonNull(dec(a, "balanceValue"), dec(aux, "pv")))
                    .portfolioValue(firstNonNull(dec(a, "portfolioValue"), dec(aux, "pv")))
                    .collateralValue(dec(a, "collateralValue"))
                    .availableMargin(firstNonNull(dec(a, "availableMargin"), dec(a, "availableFunds"), dec(aux, "af")))
                    .marginEquity(firstNonNull(dec(a, "marginEquity"), dec(aux, "pv")))
                    .initialMargin(firstNonNull(dec(a, "initialMargin"), dec(margin, "im")))
                    .maintenanceMargin(firstNonNull(dec(a, "maintenanceMargin"), dec(margin, "mm")))
                    .pnl(firstNonNull(dec(a, "pnl"), dec(aux, "pnl")))
                    .unrealizedFunding(firstNonNull(dec(a, "unrealizedFunding"), dec(aux, "usd")))
                    .totalUnrealized(dec(a, "totalUnrealized"))
                    .build());
        }
        out.sort(Comparator.comparing(KrakenLiveAccountDTO.Account::getName));
        return out;
    }

    private List<KrakenLiveAccountDTO.Position> parsePositions(JsonNode response, Map<String, BigDecimal> markPrices) {
        List<KrakenLiveAccountDTO.Position> out = new ArrayList<>();
        if (response == null || !response.path("openPositions").isArray()) return out;

        for (JsonNode p : response.path("openPositions")) {
            String symbol = text(p, "symbol");
            String side = text(p, "side");
            BigDecimal size = dec(p, "size");
            BigDecimal entry = dec(p, "price");
            BigDecimal mark = symbol != null ? markPrices.get(symbol) : null;

            BigDecimal notional = null;
            BigDecimal pnl = null;
            BigDecimal pnlPct = null;
            if (mark != null && size != null) {
                notional = mark.multiply(size).setScale(2, RoundingMode.HALF_UP);
                if (entry != null) {
                    BigDecimal move = mark.subtract(entry);
                    if ("short".equalsIgnoreCase(side)) move = move.negate();
                    pnl = move.multiply(size).setScale(2, RoundingMode.HALF_UP);
                    BigDecimal entryNotional = entry.multiply(size);
                    if (entryNotional.compareTo(BigDecimal.ZERO) != 0) {
                        pnlPct = pnl.divide(entryNotional, 6, RoundingMode.HALF_UP)
                                .multiply(BigDecimal.valueOf(100))
                                .setScale(2, RoundingMode.HALF_UP);
                    }
                }
            }

            out.add(KrakenLiveAccountDTO.Position.builder()
                    .symbol(symbol)
                    .side(side)
                    .size(size)
                    .entryPrice(entry)
                    .markPrice(mark)
                    .notionalUsd(notional)
                    .unrealizedPnlUsd(pnl)
                    .unrealizedPnlPct(pnlPct)
                    .unrealizedFunding(dec(p, "unrealizedFunding"))
                    .openedAt(timestamp(p, "fillTime"))
                    .build());
        }
        return out;
    }

    private List<KrakenLiveAccountDTO.Order> parseOrders(JsonNode response) {
        List<KrakenLiveAccountDTO.Order> out = new ArrayList<>();
        if (response == null || !response.path("openOrders").isArray()) return out;

        for (JsonNode o : response.path("openOrders")) {
            BigDecimal filled = dec(o, "filledSize");
            BigDecimal unfilled = dec(o, "unfilledSize");
            BigDecimal size = firstNonNull(dec(o, "size"), dec(o, "quantity"));
            if (size == null && filled != null && unfilled != null) {
                size = filled.add(unfilled);
            }

            out.add(KrakenLiveAccountDTO.Order.builder()
                    .orderId(firstNonNullText(o, "order_id", "orderId"))
                    .symbol(text(o, "symbol"))
                    .side(text(o, "side"))
                    .orderType(firstNonNullText(o, "orderType", "type"))
                    .limitPrice(dec(o, "limitPrice"))
                    .stopPrice(dec(o, "stopPrice"))
                    .size(size)
                    .filledSize(filled)
                    .unfilledSize(unfilled)
                    .reduceOnly(o.path("reduceOnly").asBoolean(false))
                    .status(text(o, "status"))
                    .receivedAt(timestamp(o, "receivedTime"))
                    .build());
        }
        return out;
    }

    /**
     * Kraken's {@code /fills} returns the most recent fills (up to 100) with no
     * server-side date filter on the unparameterised call. Filtering to the
     * 7-day window here keeps the request unsigned-query-free — adding a
     * {@code lastFillTime} parameter would have to be folded into the HMAC
     * signature, which {@link KrakenFuturesHttpClient#get} does not do.
     */
    private List<KrakenLiveAccountDTO.Fill> parseFills(JsonNode response) {
        List<KrakenLiveAccountDTO.Fill> out = new ArrayList<>();
        if (response == null || !response.path("fills").isArray()) return out;

        Instant cutoff = Instant.now().minusSeconds(FILLS_WINDOW_DAYS * 24L * 3600L);

        for (JsonNode f : response.path("fills")) {
            Instant filled = instant(f, "fillTime");
            if (filled != null && filled.isBefore(cutoff)) continue;

            out.add(KrakenLiveAccountDTO.Fill.builder()
                    .fillId(firstNonNullText(f, "fill_id", "fillId"))
                    .orderId(firstNonNullText(f, "order_id", "orderId"))
                    .symbol(text(f, "symbol"))
                    .side(text(f, "side"))
                    .size(dec(f, "size"))
                    .price(dec(f, "price"))
                    .fillType(text(f, "fillType"))
                    .filledAt(timestamp(f, "fillTime"))
                    .build());
        }
        // Newest first — the dashboard feed reads top-down. Fills with an
        // unparsable timestamp sort last rather than being dropped.
        out.sort(Comparator.comparing(KrakenLiveAccountDTO.Fill::getFilledAt,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return out;
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private KrakenLiveAccountDTO unavailable(String error) {
        return KrakenLiveAccountDTO.builder()
                .available(false)
                .error(error)
                .fetchedAt(LocalDateTime.now().format(TS))
                .accounts(List.of())
                .openPositions(List.of())
                .openOrders(List.of())
                .recentFills(List.of())
                .build();
    }

    /**
     * Turns Kraken's terse error codes into something the dashboard reader can
     * act on. {@code authenticationError} in particular says nothing about which
     * of several causes applies, and it is returned with HTTP 200, so without
     * this it surfaces as an unexplained empty card.
     */
    private static String explain(String apiError) {
        if (apiError.toLowerCase().contains("authenticationerror")) {
            return "Kraken avvisade API-nyckeln (authenticationError). Signeringen följer Kraken-specen — det är "
                    + "nyckeln som inte godtas. Vanliga orsaker: nyckeln är återkallad/utgången, den är en Spot-nyckel "
                    + "från kraken.com i stället för en Futures-nyckel från futures.kraken.com, den saknar rättigheten "
                    + "att läsa konto/positioner, eller den är IP-låst till en annan adress. Skapa en ny read-only "
                    + "Futures-nyckel och lägg in den i ~/.frosk/kraken-credentials.properties.";
        }
        if (apiError.toLowerCase().contains("nonce")) {
            return "Kraken avvisade nonce (" + apiError + ") — serverklockan kan ligga fel, eller så har en annan "
                    + "process använt samma nyckel med ett högre nonce.";
        }
        return "Kraken svarade: " + apiError;
    }

    /** Kraken's application-level error, or null when the response says success. */
    private static String errorOf(JsonNode node) {
        String error = text(node, "error");
        if (error != null) return error;
        JsonNode errors = node.path("errors");
        if (errors.isArray() && !errors.isEmpty()) return errors.toString();
        String result = text(node, "result");
        if (result != null && !"success".equalsIgnoreCase(result)) return result;
        return null;
    }

    private static String text(JsonNode node, String field) {
        JsonNode v = node.path(field);
        return v.isMissingNode() || v.isNull() || !v.isValueNode() ? null : v.asText();
    }

    private static String firstNonNullText(JsonNode node, String... fields) {
        for (String field : fields) {
            String v = text(node, field);
            if (v != null) return v;
        }
        return null;
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

    @SafeVarargs
    private static <T> T firstNonNull(T... values) {
        for (T v : values) {
            if (v != null) return v;
        }
        return null;
    }

    /** ISO-8601 instant from Kraken, or null when absent/unparsable. */
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
