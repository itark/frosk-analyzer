package nu.itark.frosk.dataset;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.newsdriven.NewsItem;
import nu.itark.frosk.rapidapi.yhfinance.model.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

@Component
@Slf4j
public class YahooFinanceDirectClient {

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36";

    @Value("${yahoo.finance.direct.base-url:https://query1.finance.yahoo.com}")
    private String baseUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ReentrantLock crumbLock = new ReentrantLock();
    private volatile String crumb;
    private volatile String sessionCookie;

    private WebClient webClient() {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("User-Agent", USER_AGENT)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(20 * 1024 * 1024))
                .build();
    }

    private void ensureCrumb() {
        if (crumb != null && sessionCookie != null) return;
        crumbLock.lock();
        try {
            if (crumb != null && sessionCookie != null) return;
            refreshCrumb();
        } finally {
            crumbLock.unlock();
        }
    }

    private void refreshCrumb() {
        try {
            ClientResponse response = WebClient.builder()
                    .defaultHeader("User-Agent", USER_AGENT)
                    .codecs(c -> c.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
                    .build()
                    .get()
                    .uri("https://fc.yahoo.com/")
                    .exchangeToMono(r -> reactor.core.publisher.Mono.just(r))
                    .block();

            if (response != null) {
                List<String> cookies = new ArrayList<>();
                response.cookies().forEach((name, responseCookies) ->
                        responseCookies.forEach(c -> cookies.add(name + "=" + c.getValue())));
                sessionCookie = String.join("; ", cookies);
                response.releaseBody().block();
            }

            if (sessionCookie == null || sessionCookie.isBlank()) {
                log.warn("Yahoo crumb: no cookies returned from fc.yahoo.com");
                return;
            }

            String crumbResponse = WebClient.builder()
                    .defaultHeader("User-Agent", USER_AGENT)
                    .defaultHeader("Cookie", sessionCookie)
                    .build()
                    .get()
                    .uri("https://query2.finance.yahoo.com/v1/test/getcrumb")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (crumbResponse != null && !crumbResponse.isBlank()) {
                crumb = crumbResponse.trim();
                log.info("Yahoo crumb acquired successfully");
            } else {
                log.warn("Yahoo crumb: empty response from getcrumb");
            }
        } catch (Exception e) {
            log.warn("Yahoo crumb acquisition failed: {}", e.getMessage());
        }
    }

    private void invalidateCrumb() {
        crumbLock.lock();
        try {
            crumb = null;
            sessionCookie = null;
        } finally {
            crumbLock.unlock();
        }
    }

    // ── Chart / History ────────────────────────────────────────────────

    public Map<String, StockHistoryDTO.StockData> getIntradayBars(String symbol, String interval, String range) {
        return getHistory(symbol, interval, range);
    }

    public Map<String, StockHistoryDTO.StockData> getHistory(String symbol, Interval interval) {
        String range = switch (interval) {
            case FIVE_MINUTES, FIFTEEN_MINUTES, THIRTY_MINUTES -> "5d";
            case ONE_HOUR -> "5d";
            case ONE_DAY -> "10y";
            case ONE_WEEK, ONE_MONTH, THREE_MONTHS -> "max";
        };
        return getHistory(symbol, interval.getValue(), range);
    }

    private Map<String, StockHistoryDTO.StockData> getHistory(String symbol, String interval, String range) {
        try {
            YahooChartResponse response = webClient().get()
                    .uri("/v8/finance/chart/{symbol}?interval={interval}&range={range}",
                            symbol, interval, range)
                    .retrieve()
                    .bodyToMono(YahooChartResponse.class)
                    .block();

            if (response == null || response.getChart() == null
                    || response.getChart().getResult() == null
                    || response.getChart().getResult().isEmpty()) {
                log.debug("YahooFinanceDirectClient: empty chart response for {}", symbol);
                return Collections.emptyMap();
            }

            return toStockDataMap(response.getChart().getResult().get(0));
        } catch (WebClientResponseException e) {
            log.warn("YahooFinanceDirectClient: HTTP {} for chart {}: {}",
                    e.getStatusCode().value(), symbol, e.getMessage());
            return Collections.emptyMap();
        } catch (Exception e) {
            log.warn("YahooFinanceDirectClient: chart failed for {}: {}", symbol, e.getMessage());
            return Collections.emptyMap();
        }
    }

    // ── Quotes ─────────────────────────────────────────────────────────

    public List<QuotesDTO.Quote> getQuotesReal(String symbol, String type) {
        try {
            String json = webClient().get()
                    .uri("/v6/finance/quote?symbols={symbol}", symbol)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (json == null) return Collections.emptyList();
            JsonNode root = objectMapper.readTree(json);
            JsonNode results = root.path("quoteResponse").path("result");
            if (results.isMissingNode() || !results.isArray()) return Collections.emptyList();

            List<QuotesDTO.Quote> quotes = new ArrayList<>();
            for (JsonNode node : results) {
                QuotesDTO.Quote q = objectMapper.treeToValue(node, QuotesDTO.Quote.class);
                quotes.add(q);
            }
            return quotes;
        } catch (Exception e) {
            log.warn("YahooFinanceDirectClient: quotes failed for {}: {}", symbol, e.getMessage());
            return Collections.emptyList();
        }
    }

    // ── Tickers ────────────────────────────────────────────────────────

    public List<TickersDTO.Stock> getTickers(int page, String type) {
        try {
            String json = webClient().get()
                    .uri("/v1/finance/screener/predefined/saved?scrIds={type}&count=250&start={start}",
                            type, page * 250)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (json == null) return Collections.emptyList();
            JsonNode root = objectMapper.readTree(json);
            JsonNode quotes = root.path("finance").path("result").path(0).path("quotes");
            if (quotes.isMissingNode() || !quotes.isArray()) return Collections.emptyList();

            List<TickersDTO.Stock> stocks = new ArrayList<>();
            for (JsonNode node : quotes) {
                TickersDTO.Stock s = objectMapper.treeToValue(node, TickersDTO.Stock.class);
                stocks.add(s);
            }
            return stocks;
        } catch (Exception e) {
            log.warn("YahooFinanceDirectClient: tickers failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    // ── Module: statistics ──────────────────────────────────────────────

    public StatisticsBody getModuleStatistics(String symbol) throws JsonProcessingException {
        String json = fetchQuoteSummaryModule(symbol, "defaultKeyStatistics,financialData");
        if (json == null) return null;
        try {
            JsonNode module = extractQuoteSummaryResult(json);
            if (module == null) return null;
            JsonNode stats = module.path("defaultKeyStatistics");
            if (stats.isMissingNode()) return null;
            return objectMapper.treeToValue(stats, StatisticsBody.class);
        } catch (Exception e) {
            log.warn("YahooFinanceDirectClient: statistics parse failed for {}: {}", symbol, e.getMessage());
            return null;
        }
    }

    // ── Module: income-statement ─────────────────────────────────────────

    public Body getModuleIncomeStatement(String symbol) throws JsonProcessingException {
        String json = fetchQuoteSummaryModule(symbol, "incomeStatementHistory,incomeStatementHistoryQuarterly");
        if (json == null) return null;
        try {
            JsonNode module = extractQuoteSummaryResult(json);
            if (module == null) return null;
            return objectMapper.treeToValue(module, Body.class);
        } catch (Exception e) {
            log.warn("YahooFinanceDirectClient: income-statement parse failed for {}: {}", symbol, e.getMessage());
            return null;
        }
    }

    // ── Module: recommendation-trend ─────────────────────────────────────

    public RecommendationBody getModuleRecommendationTrend(String symbol) throws JsonProcessingException {
        String json = fetchQuoteSummaryModule(symbol, "recommendationTrend");
        if (json == null) return null;
        try {
            JsonNode module = extractQuoteSummaryResult(json);
            if (module == null) return null;
            JsonNode trend = module.path("recommendationTrend");
            if (trend.isMissingNode()) return null;
            return objectMapper.treeToValue(trend, RecommendationBody.class);
        } catch (Exception e) {
            log.warn("YahooFinanceDirectClient: recommendation parse failed for {}: {}", symbol, e.getMessage());
            return null;
        }
    }

    // ── Module: asset-profile ────────────────────────────────────────────

    public AssetProfileBody getModuleAssetProfile(String symbol) throws JsonProcessingException {
        String json = fetchQuoteSummaryModule(symbol, "assetProfile");
        if (json == null) return null;
        try {
            JsonNode module = extractQuoteSummaryResult(json);
            if (module == null) return null;
            JsonNode profile = module.path("assetProfile");
            if (profile.isMissingNode()) return null;
            return objectMapper.treeToValue(profile, AssetProfileBody.class);
        } catch (Exception e) {
            log.warn("YahooFinanceDirectClient: asset-profile parse failed for {}: {}", symbol, e.getMessage());
            return null;
        }
    }

    /**
     * Extracts the first result object from the v10 quoteSummary response:
     * { "quoteSummary": { "result": [ { ...module data... } ] } }
     */
    private JsonNode extractQuoteSummaryResult(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode results = root.path("quoteSummary").path("result");
            if (results.isArray() && !results.isEmpty()) {
                return results.get(0);
            }
        } catch (Exception e) {
            log.warn("YahooFinanceDirectClient: failed to parse quoteSummary envelope: {}", e.getMessage());
        }
        return null;
    }

    // ── Module: raw JSON ─────────────────────────────────────────────────

    public String getModuleRaw(String symbol, String module) {
        return fetchQuoteSummaryModule(symbol, module);
    }

    // ── Search / News ────────────────────────────────────────────────────

    public void search(String query) {
        try {
            String json = webClient().get()
                    .uri("/v1/finance/search?q={query}&newsCount=0&listsCount=0", query)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            log.info("YahooFinanceDirectClient search: {}", json);
        } catch (Exception e) {
            log.warn("YahooFinanceDirectClient: search failed for {}: {}", query, e.getMessage());
        }
    }

    /**
     * Fetches the latest news headlines for a ticker from the Yahoo Finance search endpoint.
     *
     * @param ticker Yahoo Finance ticker symbol (e.g. "VOLV-B.ST")
     * @return list of news items, newest first; empty list on error or no results
     */
    public List<NewsItem> getNews(String ticker) {
        try {
            String json = webClient().get()
                    .uri("/v1/finance/search?q={ticker}&newsCount=10&listsCount=0", ticker)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (json == null) return Collections.emptyList();

            JsonNode root = objectMapper.readTree(json);
            JsonNode newsArray = root.path("news");
            if (newsArray.isMissingNode() || !newsArray.isArray()) return Collections.emptyList();

            List<NewsItem> result = new ArrayList<>();
            for (JsonNode node : newsArray) {
                String title = node.path("title").asText(null);
                if (title == null || title.isBlank()) continue;
                String publisher = node.path("publisher").asText("");
                String link = node.path("link").asText("");
                long publishTime = node.path("providerPublishTime").asLong(0);
                Instant publishedAt = publishTime > 0 ? Instant.ofEpochSecond(publishTime) : Instant.EPOCH;
                result.add(new NewsItem(title, publisher, link, publishedAt, ticker));
            }
            return result;
        } catch (Exception e) {
            log.warn("YahooFinanceDirectClient: getNews failed for {}: {}", ticker, e.getMessage());
            return Collections.emptyList();
        }
    }

    // ── Internal: quoteSummary fetcher ────────────────────────────────────

    private String fetchQuoteSummaryModule(String symbol, String modules) {
        return fetchQuoteSummaryModule(symbol, modules, true);
    }

    private String fetchQuoteSummaryModule(String symbol, String modules, boolean retryOnAuth) {
        ensureCrumb();
        try {
            WebClient.RequestHeadersSpec<?> request = webClient().get()
                    .uri("/v10/finance/quoteSummary/{symbol}?modules={modules}&crumb={crumb}",
                            symbol, modules, crumb != null ? crumb : "");
            if (sessionCookie != null) {
                request = request.header("Cookie", sessionCookie);
            }
            String json = request
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (json == null || json.contains("\"error\":{\"code\":\"Not Found\"")) {
                log.debug("YahooFinanceDirectClient: no data for {} module {}", symbol, modules);
                return null;
            }
            return json;
        } catch (WebClientResponseException e) {
            if (e.getStatusCode().value() == 401 && retryOnAuth) {
                log.info("Yahoo 401 for {} — refreshing crumb and retrying", symbol);
                invalidateCrumb();
                return fetchQuoteSummaryModule(symbol, modules, false);
            }
            log.warn("YahooFinanceDirectClient: HTTP {} for {} module {}: {}",
                    e.getStatusCode().value(), symbol, modules, e.getMessage());
            return null;
        } catch (Exception e) {
            log.warn("YahooFinanceDirectClient: quoteSummary failed for {} module {}: {}",
                    symbol, modules, e.getMessage());
            return null;
        }
    }

    // ── Chart response DTOs ──────────────────────────────────────────────

    private Map<String, StockHistoryDTO.StockData> toStockDataMap(ChartResult result) {
        List<Long> timestamps = result.getTimestamp();
        if (timestamps == null || timestamps.isEmpty()) {
            return Collections.emptyMap();
        }

        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");
        QuoteBlock quote = result.getIndicators().getQuote().get(0);
        Map<String, StockHistoryDTO.StockData> map = new LinkedHashMap<>();

        for (int i = 0; i < timestamps.size(); i++) {
            Double open = safeGet(quote.getOpen(), i);
            Double high = safeGet(quote.getHigh(), i);
            Double low = safeGet(quote.getLow(), i);
            Double close = safeGet(quote.getClose(), i);
            Long volume = safeGetLong(quote.getVolume(), i);

            if (open == null || close == null) continue;

            StockHistoryDTO.StockData sd = new StockHistoryDTO.StockData();
            sd.setDateUtc(timestamps.get(i));
            sd.setDate(fmt.format(new Date(timestamps.get(i) * 1000L)));
            sd.setOpen(open);
            sd.setHigh(high != null ? high : open);
            sd.setLow(low != null ? low : open);
            sd.setClose(close);
            sd.setVolume(volume != null ? volume : 0L);
            sd.setAdjclose(close);

            map.put(String.valueOf(timestamps.get(i)), sd);
        }

        return map;
    }

    private Double safeGet(List<Double> list, int index) {
        if (list == null || index >= list.size()) return null;
        return list.get(index);
    }

    private Long safeGetLong(List<Long> list, int index) {
        if (list == null || index >= list.size()) return null;
        return list.get(index);
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class YahooChartResponse {
        private Chart chart;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Chart {
        private List<ChartResult> result;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ChartResult {
        private List<Long> timestamp;
        private Indicators indicators;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Indicators {
        private List<QuoteBlock> quote;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class QuoteBlock {
        private List<Double> open;
        private List<Double> high;
        private List<Double> low;
        private List<Double> close;
        private List<Long> volume;
    }

    public enum Interval {
        FIVE_MINUTES("5m"),
        FIFTEEN_MINUTES("15m"),
        THIRTY_MINUTES("30m"),
        ONE_HOUR("1h"),
        ONE_DAY("1d"),
        ONE_WEEK("1wk"),
        ONE_MONTH("1mo"),
        THREE_MONTHS("3mo");

        private final String value;

        Interval(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static Interval fromValue(String value) {
            for (Interval interval : values()) {
                if (interval.value.equals(value)) {
                    return interval;
                }
            }
            throw new IllegalArgumentException("Invalid interval: " + value);
        }
    }
}
