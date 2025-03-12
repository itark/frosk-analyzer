package nu.itark.frosk.dataset;

import lombok.extern.slf4j.Slf4j;
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

    public void get() {
        WebClient client = WebClient.builder()
                .baseUrl("https://yahoo-finance15.p.rapidapi.com/api/v1/markets/stock/quotes?ticker=AAPL%2CMSFT%2C%5ESPX%2C%5ENYA%2CGAZP.ME%2CSIBN.ME%2CGEECEE.NS")
                .defaultHeader("x-rapidapi-key", "f3c9579c6cmsh5c0657bd7a299b8p18b5a9jsn1cde3a01eef6")
                .defaultHeader("x-rapidapi-host", "yahoo-finance15.p.rapidapi.com")
                .build();


        final Mono<ResponseEntity<String>> entity = client.get().retrieve().toEntity(String.class);

        log.info("entity :{}", entity);

    }

    public String get2() {
        WebClient webClient = WebClient.create();

        webClient.get()
                .uri("https://yahoo-finance15.p.rapidapi.com/api/v1/markets/stock/quotes?ticker=AAPL%2CMSFT%2C%5ESPX%2C%5ENYA%2CGAZP.ME%2CSIBN.ME%2CGEECEE.NS")
                .header("x-rapidapi-key", "f3c9579c6cmsh5c0657bd7a299b8p18b5a9jsn1cde3a01eef6")
                .header("x-rapidapi-host", "yahoo-finance15.p.rapidapi.com")
                .exchange()
                .doOnTerminate(() -> System.out.println("Request completed")) // Optional: Prints after request completion
                .doOnNext(response -> {
                    // Log status and headers
                    System.out.println("Status Code: " + response.statusCode());
                //    response.headers().forEach((key, value) -> System.out.println(key + ": " + value));
                })
                .flatMap(response -> {
                    if (response.statusCode().is2xxSuccessful()) {
                        // Extract the body only if the response is successful
                        return response.bodyToMono(String.class);
                    } else {
                        // Handle non-2xx responses
                        return Mono.error(new RuntimeException("Failed to fetch data"));
                    }
                })
                .subscribe(responseBody -> {
                    // Print the response body
                    System.out.println("Response Body: " + responseBody);
                }, error -> {
                    // Print any error
                    System.err.println("Error: " + error.getMessage());
                });

        // To block the main thread (if required, usually for testing/demo purposes)
        try {
            Thread.sleep(5000); // Adjust this to give enough time for the request to complete
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return null;




    }

    public void get3() throws IOException, InterruptedException {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://yahoo-finance15.p.rapidapi.com/api/v1/markets/stock/history?symbol=AAPL&interval=5m&diffandsplits=false"))
                .header("x-rapidapi-key", "f3c9579c6cmsh5c0657bd7a299b8p18b5a9jsn1cde3a01eef6")
                .header("x-rapidapi-host", "yahoo-finance15.p.rapidapi.com")
                .method("GET", HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println(response.body());


    }
}
