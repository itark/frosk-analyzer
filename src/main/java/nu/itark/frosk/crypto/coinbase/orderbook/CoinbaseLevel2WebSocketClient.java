package nu.itark.frosk.crypto.coinbase.orderbook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.crypto.coinbase.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Maintains a persistent connection to Coinbase's Advanced Trade WebSocket
 * API, subscribed to the {@code level2} channel for the products locked in
 * {@code ~/itark/PREREG_ofi_1m.md} §3 (BTC-EUR, ETH-EUR — not the full
 * {@code crypto.intraday.products} list; do not widen this without a new
 * pre-registration).
 *
 * <p>This is new infrastructure, not a revival of the old
 * {@code nu.itark.frosk.coinbase.exchange.api.websocketfeed} package — that
 * one targets the defunct {@code ws-feed.gdax.com} endpoint and was never
 * wired up (see the investigation recorded in this project's history around
 * 2026-08-13). This class speaks the current Advanced Trade WS protocol
 * ({@code wss://advanced-trade-ws.coinbase.com}, JWT auth, {@code l2_data}
 * envelope) and reuses only {@link JwtUtil}'s key-loading/signing.
 *
 * <p>Every level2 event is fed into a per-product {@link OrderBookState},
 * which does the actual OFI accounting; this class is transport plumbing —
 * connect, subscribe, parse the envelope, dispatch, reconnect. See
 * {@code OrderBookCaptureService} for the once-a-minute persistence step.
 */
@Service
@Profile("crypto")
@Slf4j
public class CoinbaseLevel2WebSocketClient {

    private static final URI WS_URI = URI.create("wss://advanced-trade-ws.coinbase.com");

    /**
     * Coinbase validates the JWT at subscribe time; whether an established
     * connection needs re-authentication before the 120s token expiry is not
     * documented clearly enough to assume either way (see PREREG_ofi_1m.md
     * §10 — verify against raw feed output, don't assume from docs alone).
     * Resubscribing this often is cheap and removes the uncertainty rather
     * than risk a connection that silently stops accepting new subscriptions
     * without ever closing.
     */
    private static final Duration RESUBSCRIBE_INTERVAL = Duration.ofSeconds(60);

    /** No message at all (not even a heartbeat) for this long means the connection is dead, not quiet. */
    private static final Duration STALE_TIMEOUT = Duration.ofSeconds(30);

    private static final Duration RECONNECT_DELAY = Duration.ofSeconds(5);

    @Value("${coinbase.l2.capture.enabled:false}")
    private boolean enabled;

    @Value("${coinbase.l2.capture.products:BTC-EUR,ETH-EUR}")
    private String productsRaw;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    private List<String> products;
    private final Map<String, OrderBookState> stateByProduct = new ConcurrentHashMap<>();
    private final Map<String, StringBuilder> frameBuffers = new ConcurrentHashMap<>();

    private volatile WebSocket webSocket;
    private volatile Instant lastMessageAt = Instant.now();
    private volatile boolean shuttingDown = false;
    private final AtomicInteger reconnectAttempts = new AtomicInteger(0);

    private ScheduledExecutorService scheduler;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @PostConstruct
    private void start() {
        if (!enabled) {
            log.info("CoinbaseLevel2WebSocketClient: disabled (coinbase.l2.capture.enabled=false)");
            return;
        }
        products = Arrays.stream(productsRaw.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
        for (String p : products) {
            stateByProduct.put(p, new OrderBookState());
        }
        log.info("CoinbaseLevel2WebSocketClient: starting for products {}", products);

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "coinbase-l2-ws-scheduler");
            t.setDaemon(true);
            return t;
        });
        connect();
        scheduler.scheduleAtFixedRate(this::resubscribeSafely,
                RESUBSCRIBE_INTERVAL.toSeconds(), RESUBSCRIBE_INTERVAL.toSeconds(), TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(this::checkStaleness, 10, 10, TimeUnit.SECONDS);
    }

    @PreDestroy
    private void stop() {
        shuttingDown = true;
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
        WebSocket ws = webSocket;
        if (ws != null) {
            ws.sendClose(WebSocket.NORMAL_CLOSURE, "shutdown");
        }
    }

    /** Read-only view for {@code OrderBookCaptureService} — null if the product isn't tracked. */
    public OrderBookState getState(String productId) {
        return stateByProduct.get(productId);
    }

    public List<String> getTrackedProducts() {
        return products == null ? List.of() : products;
    }

    public boolean isConnected() {
        return webSocket != null && !webSocket.isOutputClosed() && !webSocket.isInputClosed();
    }

    // ── Connection lifecycle ─────────────────────────────────────────────

    private synchronized void connect() {
        if (shuttingDown) return;
        httpClient.newWebSocketBuilder()
                .buildAsync(WS_URI, new L2Listener())
                .thenAccept(ws -> {
                    webSocket = ws;
                    reconnectAttempts.set(0);
                    lastMessageAt = Instant.now();
                    log.info("CoinbaseLevel2WebSocketClient: connected");
                    subscribe(ws);
                })
                .exceptionally(ex -> {
                    log.warn("CoinbaseLevel2WebSocketClient: connect failed — {}", ex.toString());
                    scheduleReconnect();
                    return null;
                });
    }

    private void scheduleReconnect() {
        if (shuttingDown || scheduler == null) return;
        int attempt = reconnectAttempts.incrementAndGet();
        // Simple capped backoff: 5s, 10s, 20s, ... up to 2 minutes.
        long delaySeconds = Math.min(RECONNECT_DELAY.toSeconds() * (1L << Math.min(attempt - 1, 4)), 120);
        log.warn("CoinbaseLevel2WebSocketClient: reconnecting in {}s (attempt {})", delaySeconds, attempt);
        scheduler.schedule(this::connect, delaySeconds, TimeUnit.SECONDS);
    }

    private void checkStaleness() {
        if (shuttingDown || webSocket == null) return;
        if (Duration.between(lastMessageAt, Instant.now()).compareTo(STALE_TIMEOUT) > 0) {
            log.warn("CoinbaseLevel2WebSocketClient: no message in {}s — forcing reconnect", STALE_TIMEOUT.toSeconds());
            WebSocket ws = webSocket;
            webSocket = null;
            if (ws != null) {
                ws.abort();
            }
            connect();
        }
    }

    private void resubscribeSafely() {
        WebSocket ws = webSocket;
        if (!shuttingDown && ws != null) {
            subscribe(ws);
        }
    }

    private void subscribe(WebSocket ws) {
        try {
            String jwt = jwtUtil.getSignedJWTForWebsocket();
            sendSubscribe(ws, "level2", jwt);
            sendSubscribe(ws, "heartbeats", jwt);
        } catch (Exception e) {
            log.warn("CoinbaseLevel2WebSocketClient: subscribe failed — {}", e.toString());
        }
    }

    private void sendSubscribe(WebSocket ws, String channel, String jwt) throws Exception {
        Map<String, Object> msg = Map.of(
                "type", "subscribe",
                "product_ids", products,
                "channel", channel,
                "jwt", jwt
        );
        ws.sendText(objectMapper.writeValueAsString(msg), true);
    }

    // ── Message handling ─────────────────────────────────────────────────

    private void handleMessage(String json) {
        lastMessageAt = Instant.now();
        JsonNode root;
        try {
            root = objectMapper.readTree(json);
        } catch (Exception e) {
            log.warn("CoinbaseLevel2WebSocketClient: unparseable message — {}", e.toString());
            return;
        }
        String channel = root.path("channel").asText();
        if ("error".equals(channel) || root.has("message") && root.path("type").asText("").equals("error")) {
            log.warn("CoinbaseLevel2WebSocketClient: server error message — {}", json);
            return;
        }
        if (!"l2_data".equals(channel)) {
            return; // subscriptions ack / heartbeats — nothing to do beyond the lastMessageAt bump above
        }
        for (JsonNode event : root.path("events")) {
            String productId = event.path("product_id").asText();
            OrderBookState state = stateByProduct.get(productId);
            if (state == null) continue; // not one of our tracked products

            List<OrderBookState.Level2Update> updates = new ArrayList<>();
            for (JsonNode u : event.path("updates")) {
                try {
                    updates.add(new OrderBookState.Level2Update(
                            u.path("side").asText(),
                            new BigDecimal(u.path("price_level").asText()),
                            new BigDecimal(u.path("new_quantity").asText())
                    ));
                } catch (NumberFormatException nfe) {
                    log.warn("CoinbaseLevel2WebSocketClient: bad update entry for {} — {}", productId, u);
                }
            }

            if ("snapshot".equals(event.path("type").asText())) {
                state.applySnapshot(updates);
            } else {
                for (OrderBookState.Level2Update u : updates) {
                    state.applyUpdate(u.side(), u.priceLevel(), u.newQuantity());
                }
            }
        }
    }

    private class L2Listener implements WebSocket.Listener {

        @Override
        public void onOpen(WebSocket webSocket) {
            webSocket.request(1);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            String key = String.valueOf(System.identityHashCode(webSocket));
            StringBuilder buf = frameBuffers.computeIfAbsent(key, k -> new StringBuilder());
            buf.append(data);
            webSocket.request(1);
            if (last) {
                String full = buf.toString();
                frameBuffers.remove(key);
                handleMessage(full);
            }
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            log.warn("CoinbaseLevel2WebSocketClient: closed — {} {}", statusCode, reason);
            CoinbaseLevel2WebSocketClient.this.webSocket = null;
            scheduleReconnect();
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            log.warn("CoinbaseLevel2WebSocketClient: error — {}", error.toString());
            CoinbaseLevel2WebSocketClient.this.webSocket = null;
            scheduleReconnect();
        }
    }
}
