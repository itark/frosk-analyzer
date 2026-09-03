package nu.itark.frosk.crypto.kraken;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.crypto.coinbase.model.ProductBook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;

/**
 * Top-of-book snapshot for Kraken Futures perpetuals, used by
 * {@code CryptoIntradayStrategyRunner} to record the bid/ask spread at signal time.
 *
 * <p>The Coinbase {@code ProductService} cannot serve {@code PF_*} symbols — a
 * {@code /product_book?product_id=PF_ETHUSD} lookup 404s — so under the
 * {@code kraken-futures} profile this bean is injected in its place. It reads the
 * public {@code GET /derivatives/api/v3/tickers/{symbol}} endpoint (no auth) and
 * adapts the {@code bid}/{@code ask} fields into a {@link ProductBook} so the caller
 * stays data-source agnostic.
 *
 * <p>Best-effort by design: spread is diagnostic data and must never fail, delay or
 * block a trading signal. Every failure path returns {@code null}.
 */
@Service
@Profile("kraken-futures")
@Slf4j
public class KrakenFuturesTickerService {

    @Value("${kraken.futures.api.baseUrl:https://futures.kraken.com/derivatives/api/v3}")
    private String baseUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    /** Top-of-book for {@code symbol} as a {@link ProductBook}; {@code null} on any failure. */
    public ProductBook getProductBook(String symbol) {
        try {
            TickerResponse resp = restTemplate.getForObject(
                    baseUrl + "/tickers/" + symbol, TickerResponse.class);
            if (resp == null || resp.getTicker() == null) return null;
            BigDecimal bid = resp.getTicker().getBid();
            BigDecimal ask = resp.getTicker().getAsk();
            if (bid == null || ask == null) return null;
            return toBook(symbol, bid, ask);
        } catch (Exception e) {
            log.warn("KrakenFuturesTickerService: could not read ticker for {} — {}", symbol, e.toString());
            return null;
        }
    }

    private static ProductBook toBook(String symbol, BigDecimal bid, BigDecimal ask) {
        ProductBook.Book pricebook = new ProductBook.Book();
        pricebook.setProduct_id(symbol);
        pricebook.setBids(List.of(level(bid)));
        pricebook.setAsks(List.of(level(ask)));

        ProductBook book = new ProductBook();
        book.setPricebook(pricebook);
        return book;
    }

    private static ProductBook.Level level(BigDecimal price) {
        ProductBook.Level l = new ProductBook.Level();
        l.setPrice(price);
        return l;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class TickerResponse {
        private String result;
        private Ticker ticker;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class Ticker {
        private String symbol;
        private BigDecimal bid;
        private BigDecimal ask;
    }
}
