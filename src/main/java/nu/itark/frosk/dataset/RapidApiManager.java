package nu.itark.frosk.dataset;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.rapidapi.yhfinance.model.YahooFinanceResponse;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collections;

/**
* https://rapidapi.com/sparior/api/yahoo-finance15/playground/apiendpoint_9d15a1cc-d1be-4a69-9622-1f59ab68183c
 */
@Component
@Slf4j
public class RapidApiManager {

    public void getQuotes() throws IOException, InterruptedException {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://yahoo-finance15.p.rapidapi.com/api/v1/markets/stock/quotes?ticker=AAPL%2CMSFT%2C%5ESPX%2C%5ENYA%2CGAZP.ME%2CSIBN.ME%2CGEECEE.NS"))
                .header("x-rapidapi-key", "f3c9579c6cmsh5c0657bd7a299b8p18b5a9jsn1cde3a01eef6")
                .header("x-rapidapi-host", "yahoo-finance15.p.rapidapi.com")
                .method("GET", HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println(response.body());


    }
    public void getHistory(String symbol, Interval interval) throws IOException, InterruptedException {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://yahoo-finance15.p.rapidapi.com/api/v1/markets/stock/history?symbol=+"+symbol+"+&interval=+"+interval.getValue()+"+&diffandsplits=false"))
                .header("x-rapidapi-key", "f3c9579c6cmsh5c0657bd7a299b8p18b5a9jsn1cde3a01eef6")
                .header("x-rapidapi-host", "yahoo-finance15.p.rapidapi.com")
                .method("GET", HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println(response.body());

    }

    public void getHistorySpring(String symbol, Interval interval) throws IOException, InterruptedException {
        String baseUrl = "https://yahoo-finance15.p.rapidapi.com/api/v1/markets/stock/history";
        String uri = "?symbol=+"+symbol+"+&interval=+"+interval.getValue()+"+&diffandsplits=false";

        WebClient webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("x-rapidapi-key", "f3c9579c6cmsh5c0657bd7a299b8p18b5a9jsn1cde3a01eef6")
                .defaultHeader("x-rapidapi-host", "yahoo-finance15.p.rapidapi.com")
                .build();

        YahooFinanceResponse response = webClient.get()
                .uri(uri)
                .retrieve()
              //  .bodyToMono(String.class)
                .bodyToMono(YahooFinanceResponse.class)
                .block();

        System.out.println(ReflectionToStringBuilder.toString(response, ToStringStyle.MULTI_LINE_STYLE,true));

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
