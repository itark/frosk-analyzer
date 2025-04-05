package nu.itark.frosk.crypto.coinbase.advanced;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.handler.logging.LogLevel;
import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.crypto.coinbase.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.transport.logging.AdvancedByteBufFormat;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static java.util.Collections.emptyList;

/**
 * This class acts as a central point for providing user configuration and making GET/POST/PUT requests as well as
 * getting responses as Lists of objects rather than arrays.
 *
 *
 * https://docs.cloud.coinbase.com/advanced-trade-api/docs/rest-api-overview
 *
 *
 */
@Component
@Slf4j
public class CoinbaseImpl implements Coinbase {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${exchange.api.baseUrl}")
    String baseUrl;

    @Value("${exchange.api.baseEndpoint}")
    String baseEndpoint;

    HttpClient httpClient = HttpClient.create()
            .wiretap(this.getClass().getCanonicalName(), LogLevel.DEBUG, AdvancedByteBufFormat.TEXTUAL);

    WebClient webClient = WebClient.builder()
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .codecs(configurer -> configurer
                    .defaultCodecs()
                    .maxInMemorySize(16 * 1024 * 1024)) //Consuming size
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .build();

    @Override
    public <T> T get(String resourcePath, ParameterizedTypeReference<T> responseType) {
        try {
            final HttpHeaders httpHeaders = securityHeaders(resourcePath,
                    "GET",
                    "").getHeaders();
            Mono<T> resource = webClient.get()
                    .uri(getBaseUrl() + resourcePath)
                    .headers(h -> {
                        h.addAll(httpHeaders);
                    })
                    .retrieve()
                    .bodyToMono(responseType);
            return resource.block();
        } catch (HttpClientErrorException ex) {
            log.error("GET request Failed for url:{}, resourcePath:{}, metod:{}", getBaseUrl(), resourcePath, "GET", ex);
        }
        return null;
    }

    @Override
    public <T> List<T> getAsList(String resourcePath, ParameterizedTypeReference<T[]> responseType) {
       T[] result = get(resourcePath, responseType);

       return result == null ? emptyList() : Arrays.asList(result);
    }

    @Override
    public <T> T pagedGet(String resourcePath,
                          ParameterizedTypeReference<T> responseType,
                          String beforeOrAfter,
                          Integer pageNumber,
                          Integer limit) {
        resourcePath += "?" + beforeOrAfter + "=" + pageNumber + "&limit=" + limit;
        return get(resourcePath, responseType);
    }

    @Override
    public <T> List<T> pagedGetAsList(String resourcePath,
                          ParameterizedTypeReference<T[]> responseType,
                          String beforeOrAfter,
                          Integer pageNumber,
                          Integer limit) {
        T[] result = pagedGet(resourcePath, responseType, beforeOrAfter, pageNumber, limit );
        return result == null ? emptyList() : Arrays.asList(result);
    }

    @Override
    public <T> T delete(String resourcePath, ParameterizedTypeReference<T> responseType) {
        try {
            ResponseEntity<T> response = restTemplate.exchange(getBaseUrl() + resourcePath,
                HttpMethod.DELETE,
                securityHeaders(resourcePath, "DELETE", ""),
                responseType);
            return response.getBody();
        } catch (HttpClientErrorException ex) {
            log.error("DELETE request Failed for '" + resourcePath + "': " + ex.getResponseBodyAsString());
        }
        return null;
    }

    @Override
    public <T, R> T post(String resourcePath,  ParameterizedTypeReference<T> responseType, R jsonObj) {
        String jsonBody = toJson(jsonObj);

        try {
            ResponseEntity<T> response = restTemplate.exchange(getBaseUrl() + resourcePath,
                    HttpMethod.POST,
                    securityHeaders(resourcePath, "POST", jsonBody),
                    responseType);
            return response.getBody();
        } catch (HttpClientErrorException ex) {
            log.error("POST request Failed for '" + resourcePath + "': " + ex.getResponseBodyAsString());
        }
        return null;
    }

    @Override
    public String getBaseUrl() {
        return baseUrl;
    }

    public HttpEntity<String> securityHeaders(String endpoint, String method, String jsonBody) {
        HttpHeaders headers = new HttpHeaders();
        String sJWT;
        String resource = endpoint.replace(getBaseUrl(), ""); //For curl
        String fullEndpoint = baseEndpoint + endpoint;
        String noQueryParamsFullEndPoint = fullEndpoint.split("\\?")[0];
        try {
            sJWT = jwtUtil.getSignedJWT(noQueryParamsFullEndPoint);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        headers.add("accept", "application/json");
        headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + sJWT);

        curlRequest(method, jsonBody, headers, resource);

        return new HttpEntity<>(jsonBody, headers);
    }

    /**
     * Purely here for logging an equivalent curl request for debugging
     * note that the signature is time-sensitive and has a time to live of about 1 minute after which the request
     * is no longer valid.
     */
    private void curlRequest(String method, String jsonBody, HttpHeaders headers, String resource) {
        String curlTest = "curl ";
        for (String key : headers.keySet()){
            curlTest +=  "-H '" + key + ":" + headers.get(key).get(0) + "' ";
        }
        if (jsonBody!=null && !jsonBody.equals(""))
            curlTest += "-d '" + jsonBody + "' ";

        curlTest += "-X " + method + " " + getBaseUrl() + resource;
        log.debug(curlTest);
    }

    private String toJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error("Unable to serialize", e);
            throw new RuntimeException("Unable to serialize");
        }
    }
}
