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
 * Thin HTTP client for the frosk-prophet-service Python microservice.
 *
 * <p>Own {@link RestTemplate} instance with a short timeout, same pattern as
 * {@link nu.itark.frosk.regime.GarchServiceClient} — never affects other
 * callers' timeouts.
 *
 * <p>Never throws: any failure (connection refused, timeout, non-2xx,
 * malformed body) is logged and surfaced as an empty {@link Optional} so
 * {@link nu.itark.frosk.service.DailyPnlForecastService} can fail closed.
 */
@Component
@Slf4j
public class ProphetServiceClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public ProphetServiceClient(@Value("${forecast.prophet.service.url:http://localhost:8001}") String baseUrl,
                                 @Value("${forecast.prophet.service.timeout.ms:5000}") int timeoutMs) {
        this.baseUrl = baseUrl;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeoutMs);
        requestFactory.setReadTimeout(timeoutMs);
        this.restTemplate = new RestTemplate(requestFactory);
    }

    public Optional<ProphetForecastResponse> fetchForecast(String strategyName, List<DailyPnlPoint> dailyPnl, int horizonDays) {
        try {
            DailyPnlForecastRequest request = new DailyPnlForecastRequest(strategyName, dailyPnl, horizonDays);
            ProphetForecastResponse response = restTemplate.postForObject(
                    baseUrl + "/daily-pnl-forecast", request, ProphetForecastResponse.class);
            return Optional.ofNullable(response);
        } catch (RestClientException e) {
            log.warn("ProphetServiceClient: call to {} failed for {} — {}", baseUrl, strategyName, e.toString());
            return Optional.empty();
        } catch (Exception e) {
            log.warn("ProphetServiceClient: unexpected error calling {} for {} — {}", baseUrl, strategyName, e.toString());
            return Optional.empty();
        }
    }
}
