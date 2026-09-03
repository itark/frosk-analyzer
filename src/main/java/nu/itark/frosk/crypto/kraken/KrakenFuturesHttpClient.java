package nu.itark.frosk.crypto.kraken;

import org.springframework.util.MultiValueMap;

/**
 * HTTP client abstraction for the Kraken Futures REST API.
 *
 * <p>Base URL: {@code https://futures.kraken.com/derivatives/api/v3}
 *
 * <p>GET endpoints use query parameters; POST endpoints use form-encoded body
 * (unlike Coinbase which uses JSON). Private endpoints require the
 * {@code APIKey} + {@code Authent} headers computed by
 * {@link nu.itark.frosk.crypto.kraken.security.KrakenFuturesSignature}.
 */
public interface KrakenFuturesHttpClient {

    /**
     * Authenticated GET to a private endpoint.
     *
     * @param endpointPath path relative to the API base, e.g. {@code /openpositions}
     * @param responseType Jackson target class
     */
    <T> T get(String endpointPath, Class<T> responseType);

    /**
     * Unauthenticated GET to a public endpoint.
     */
    <T> T getPublic(String endpointPath, Class<T> responseType);

    /**
     * Authenticated POST with form-encoded body to a private endpoint.
     *
     * @param endpointPath path relative to the API base, e.g. {@code /sendorder}
     * @param formParams   form parameters (application/x-www-form-urlencoded)
     * @param responseType Jackson target class
     */
    <T> T post(String endpointPath, MultiValueMap<String, String> formParams, Class<T> responseType);
}
