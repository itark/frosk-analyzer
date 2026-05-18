package nu.itark.frosk.dataset;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import nu.itark.frosk.rapidapi.yhfinance.model.StockHistoryDTO;

@Component
@Slf4j
public class YahooFinanceDirectClient {

    @Value("${yahoo.finance.direct.base-url:https://query1.finance.yahoo.com}")
    private String baseUrl;

    private WebClient webClient() {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("User-Agent", "Mozilla/5.0")
                .build();
    }

    public Map<String, StockHistoryDTO.StockData> getIntradayBars(String symbol, String interval, String range) {
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
                log.debug("YahooFinanceDirectClient: empty response for {}", symbol);
                return Collections.emptyMap();
            }

            return toStockDataMap(response.getChart().getResult().get(0));
        } catch (WebClientResponseException e) {
            log.warn("YahooFinanceDirectClient: HTTP {} for {}: {}",
                    e.getStatusCode().value(), symbol, e.getMessage());
            return Collections.emptyMap();
        } catch (Exception e) {
            log.warn("YahooFinanceDirectClient: failed for {}: {}", symbol, e.getMessage());
            return Collections.emptyMap();
        }
    }

    private Map<String, StockHistoryDTO.StockData> toStockDataMap(ChartResult result) {
        List<Long> timestamps = result.getTimestamp();
        if (timestamps == null || timestamps.isEmpty()) {
            return Collections.emptyMap();
        }

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
}
