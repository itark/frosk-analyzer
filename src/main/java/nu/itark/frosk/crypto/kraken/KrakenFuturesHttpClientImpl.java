package nu.itark.frosk.crypto.kraken;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.crypto.kraken.security.KrakenFuturesSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.stream.Collectors;

/**
 * RestTemplate-based implementation of {@link KrakenFuturesHttpClient}.
 *
 * <p>Kraken Futures differs from Coinbase in two important ways:
 * <ul>
 *   <li>Authentication uses HMAC-SHA-512, not JWT</li>
 *   <li>POST bodies are {@code application/x-www-form-urlencoded}, not JSON</li>
 * </ul>
 *
 * <p>The nonce is included in both the form body (for POST) and as a header
 * for GET requests, and is always the same value used to compute Authent.
 */
@Component
@Profile("kraken-futures")
@Slf4j
public class KrakenFuturesHttpClientImpl implements KrakenFuturesHttpClient {

    /** Kraken Futures REST API v3 base URL. */
    @Value("${kraken.futures.api.baseUrl:https://futures.kraken.com/derivatives/api/v3}")
    private String baseUrl;

    @Autowired
    private KrakenFuturesSignature signature;

    @Autowired
    private ObjectMapper objectMapper;

    private final RestTemplate restTemplate = new RestTemplate();

    // ── KrakenFuturesHttpClient ──────────────────────────────────────────

    @Override
    public <T> T get(String endpointPath, Class<T> responseType) {
        String nonce = signature.generateNonce();
        String authent = signature.computeAuthent("", nonce, signingPath(endpointPath));

        HttpHeaders headers = buildAuthHeaders(nonce, authent);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<T> response = restTemplate.exchange(
                    baseUrl + endpointPath, HttpMethod.GET, entity, responseType);
            return response.getBody();
        } catch (HttpClientErrorException ex) {
            log.error("KrakenFuturesHttpClient GET {}: {} — {}", endpointPath,
                    ex.getStatusCode(), ex.getResponseBodyAsString());
            return null;
        } catch (Exception ex) {
            log.error("KrakenFuturesHttpClient GET {} failed", endpointPath, ex);
            return null;
        }
    }

    @Override
    public <T> T getPublic(String endpointPath, Class<T> responseType) {
        try {
            return restTemplate.getForObject(baseUrl + endpointPath, responseType);
        } catch (Exception ex) {
            log.error("KrakenFuturesHttpClient public GET {} failed", endpointPath, ex);
            return null;
        }
    }

    @Override
    public <T> T post(String endpointPath, MultiValueMap<String, String> formParams, Class<T> responseType) {
        String nonce = signature.generateNonce();

        // Build the postData string: URL-encoded form body as it will appear in the request
        String postData = formParams.entrySet().stream()
                .flatMap(e -> e.getValue().stream()
                        .map(v -> encode(e.getKey()) + "=" + encode(v)))
                .collect(Collectors.joining("&"));

        String authent = signature.computeAuthent(postData, nonce, signingPath(endpointPath));

        HttpHeaders headers = buildAuthHeaders(nonce, authent);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // Add nonce to form body (Kraken recommends including it)
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>(formParams);
        body.add("nonce", nonce);

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<T> response = restTemplate.exchange(
                    baseUrl + endpointPath, HttpMethod.POST, entity, responseType);
            return response.getBody();
        } catch (HttpClientErrorException ex) {
            log.error("KrakenFuturesHttpClient POST {}: {} — {}", endpointPath,
                    ex.getStatusCode(), ex.getResponseBodyAsString());
            return null;
        } catch (Exception ex) {
            log.error("KrakenFuturesHttpClient POST {} failed", endpointPath, ex);
            return null;
        }
    }

    // ── helpers ──────────────────────────────────────────────────────────

    /**
     * The path Kraken expects inside the {@code Authent} HMAC: the request path
     * with the {@code /derivatives} prefix removed, e.g. {@code /api/v3/accounts}
     * for {@code https://futures.kraken.com/derivatives/api/v3/accounts}.
     *
     * <p>Signing the bare endpoint ({@code /accounts}) instead produces an
     * {@code Authent} Kraken rejects with 401 on every private endpoint. Derived
     * from {@link #baseUrl} rather than hard-coded so a {@code baseUrl} override
     * without the {@code /derivatives} segment still signs correctly.
     */
    private String signingPath(String endpointPath) {
        String basePath;
        try {
            basePath = java.net.URI.create(baseUrl).getPath();
        } catch (IllegalArgumentException ex) {
            basePath = "";
        }
        if (basePath == null) basePath = "";
        if (basePath.startsWith("/derivatives")) {
            basePath = basePath.substring("/derivatives".length());
        }
        if (basePath.endsWith("/")) {
            basePath = basePath.substring(0, basePath.length() - 1);
        }
        return basePath + endpointPath;
    }

    private HttpHeaders buildAuthHeaders(String nonce, String authent) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("APIKey", signature.getApiKey());
        headers.set("Authent", authent);
        headers.set("Nonce", nonce);
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
        return headers;
    }

    /** URL-encodes a single form parameter component. */
    private String encode(String value) {
        try {
            return java.net.URLEncoder.encode(value, "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            return value;
        }
    }
}
