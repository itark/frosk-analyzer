package nu.itark.frosk.forecast;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;

/**
 * Thin HTTP client for the LSTM signal-filter microservice.
 *
 * <p>Own {@link RestTemplate} instance with a short timeout — same pattern
 * as {@link nu.itark.frosk.regime.GarchServiceClient} and {@link ProphetServiceClient}.
 *
 * <p>Never throws: any failure (connection refused, timeout, non-2xx,
 * malformed body) is logged and surfaced as an empty {@link Optional} so
 * {@link nu.itark.frosk.service.LstmSignalFilterService} can fail closed.
 */
@Component
@Slf4j
public class LstmServiceClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public LstmServiceClient(@Value("${forecast.lstm.service.url:http://localhost:8090}") String baseUrl,
                              @Value("${forecast.lstm.service.timeout.ms:3000}") int timeoutMs) {
        this.baseUrl = baseUrl;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeoutMs);
        requestFactory.setReadTimeout(timeoutMs);
        this.restTemplate = new RestTemplate(requestFactory);
    }

    public Optional<LstmSignalResponse> fetchSignal(String ticker, List<LstmBar> bars) {
        try {
            LstmSignalRequest request = new LstmSignalRequest(ticker, bars);
            LstmSignalResponse response = restTemplate.postForObject(
                    baseUrl + "/signal-filter", request, LstmSignalResponse.class);
            return Optional.ofNullable(response);
        } catch (RestClientException e) {
            log.warn("LstmServiceClient: call to {} failed for {} — {}", baseUrl, ticker, e.toString());
            return Optional.empty();
        } catch (Exception e) {
            log.warn("LstmServiceClient: unexpected error calling {} for {} — {}", baseUrl, ticker, e.toString());
            return Optional.empty();
        }
    }
}
