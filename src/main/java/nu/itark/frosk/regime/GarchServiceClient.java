package nu.itark.frosk.regime;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;

/**
 * Thin HTTP client for the frosk-garch-service Python microservice.
 *
 * <p>Uses its own {@link RestTemplate} instance (not the shared bean in
 * {@code FroskApplication}) so this client's short timeout doesn't affect
 * other callers — same pattern as {@code ProductService.getPublicCandles}.
 *
 * <p>Never throws: any failure (connection refused, timeout, non-2xx,
 * malformed body) is logged and surfaced as an empty {@link Optional} so
 * {@link nu.itark.frosk.service.RegimeForecastService} can fail closed.
 */
@Component
@Slf4j
public class GarchServiceClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public GarchServiceClient(@Value("${regime.garch.service.url:http://localhost:8000}") String baseUrl,
                               @Value("${regime.garch.service.timeout.ms:2000}") int timeoutMs) {
        this.baseUrl = baseUrl;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeoutMs);
        requestFactory.setReadTimeout(timeoutMs);
        this.restTemplate = new RestTemplate(requestFactory);
    }

    public Optional<VolatilityRegimeResponse> fetchVolatilityRegime(String ticker, List<Double> returns) {
        try {
            VolatilityRegimeRequest request = new VolatilityRegimeRequest(ticker, returns);
            VolatilityRegimeResponse response = restTemplate.postForObject(
                    baseUrl + "/volatility-regime", request, VolatilityRegimeResponse.class);
            return Optional.ofNullable(response);
        } catch (RestClientException e) {
            log.warn("GarchServiceClient: call to {} failed for {} — {}", baseUrl, ticker, e.toString());
            return Optional.empty();
        } catch (Exception e) {
            log.warn("GarchServiceClient: unexpected error calling {} for {} — {}", baseUrl, ticker, e.toString());
            return Optional.empty();
        }
    }
}
