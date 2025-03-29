package nu.itark.frosk.dataset;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.rapidapi.yhfinance.model.QuotesDTO;
import nu.itark.frosk.rapidapi.yhfinance.model.StockHistoryDTO;
import nu.itark.frosk.rapidapi.yhfinance.model.TickersDTO;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
* https://rapidapi.com/sparior/api/yahoo-finance15/playground/apiendpoint_9d15a1cc-d1be-4a69-9622-1f59ab68183c
 */
@Component
@Slf4j
public class RapidApiManager {


    private WebClient webClient(String baseUrl) {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("x-rapidapi-key", "f3c9579c6cmsh5c0657bd7a299b8p18b5a9jsn1cde3a01eef6")
                .defaultHeader("x-rapidapi-host", "yahoo-finance15.p.rapidapi.com")
                .build();
    }

    /**
     * https://rapidapi.com/sparior/api/yahoo-finance15/playground/apiendpoint_3202b1b9-9938-4e63-9ae2-de6791d96c3b
     * @param symbol
     * @param type
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    public List<QuotesDTO.Quote> getQuotesReal(String symbol, String type) throws IOException, InterruptedException {
        String baseUrl = "https://yahoo-finance15.p.rapidapi.com/api/v1/markets/stock/quotes";
        String uri = "?ticker="+symbol+"&type="+type;
        QuotesDTO response = webClient(baseUrl).get()
                .uri(uri)
                .retrieve()
                .bodyToMono(QuotesDTO.class)
                .block();
        return response.getBody();
    }

    /**
     * https://rapidapi.com/sparior/api/yahoo-finance15/playground/apiendpoint_aec6b4d4-eb59-47a9-82bf-f162aa41c166
     * @param page
     * @param type
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    public List<TickersDTO.Stock> getTickers(int page, String type) throws IOException, InterruptedException {
        String baseUrl = "https://yahoo-finance15.p.rapidapi.com/api/v2/markets/tickers";
        String uri = "?page="+page+"&type="+type;
        TickersDTO response = webClient(baseUrl).get()
                .uri(uri)
                .retrieve()
                .bodyToMono(TickersDTO.class)
                .block();
       return response.getBody();
    }

    /**
     * https://rapidapi.com/sparior/api/yahoo-finance15/playground/apiendpoint_b7dd3888-f254-4081-a4cb-178d5638136e
     *
     * @param symbol
     * @param interval
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    public Map<String, StockHistoryDTO.StockData> getHistory(String symbol, Interval interval) throws IOException, InterruptedException {
        String baseUrl = "https://yahoo-finance15.p.rapidapi.com/api/v1/markets/stock/history";
        String uri = "?symbol=+"+symbol+"+&interval=+"+interval.getValue()+"+&diffandsplits=false";
        StockHistoryDTO response = webClient(baseUrl).get()
                .uri(uri)
                .retrieve()
                .bodyToMono(StockHistoryDTO.class)
                .block();
        return response.getBody();
    }

    enum Interval {
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
