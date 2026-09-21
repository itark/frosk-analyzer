package nu.itark.frosk.crypto.kraken.security;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * Computes Kraken Futures REST API authentication headers.
 *
 * <p>Authentication algorithm per Kraken documentation:
 * <ol>
 *   <li>Concatenate: {@code postData + nonce + endpointPath}</li>
 *   <li>Hash step 1 with SHA-256</li>
 *   <li>Base64-decode the {@code api_secret}</li>
 *   <li>HMAC-SHA-512 the SHA-256 result using the decoded secret</li>
 *   <li>Base64-encode the HMAC result → this is {@code Authent}</li>
 * </ol>
 *
 * <p>Headers required on private endpoints:
 * <pre>
 *   APIKey:  &lt;api_key&gt;
 *   Authent: &lt;computed authent&gt;
 *   Nonce:   &lt;millis since epoch&gt;   (optional but recommended)
 * </pre>
 *
 * @see <a href="https://support.kraken.com/articles/360022635592">Kraken auth docs</a>
 */
@Component
@Profile("kraken-futures")
@Slf4j
public class KrakenFuturesSignature {

    private static final String HMAC_SHA_512 = "HmacSHA512";

    /** Prefix of the placeholder value shipped in application-kraken-futures.properties. */
    private static final String PLACEHOLDER = "CHANGE_ME";

    @Value("${kraken.futures.api.key}")
    private String apiKey;

    @Value("${kraken.futures.api.secret}")
    private String apiSecret;

    /**
     * Per-request logging of the exact signed material. Off by default: this is
     * a debugging aid, not something to leave on in a running process. Even when
     * on it never logs the secret — only the key prefix, the signed string and
     * the resulting Authent, which is what is needed to compare against Kraken's
     * reference implementation.
     */
    @Value("${kraken.futures.api.debug.auth:false}")
    private boolean debugAuth;

    public String getApiKey() {
        return apiKey;
    }

    /**
     * Startup sanity check on the credentials, logged without exposing them.
     *
     * <p>Exists because every credential failure mode below reaches Kraken as the
     * same opaque {@code authenticationError} response, so the distinction has to
     * be made here instead:
     * <ul>
     *   <li>the properties never resolved and the bean holds the {@code CHANGE_ME}
     *       placeholder (credentials file missing, or a property-precedence
     *       problem between it and application-kraken-futures.properties)</li>
     *   <li>the secret is not valid base64, so the HMAC key is garbage</li>
     *   <li>the credentials resolved and are well-formed — in which case an
     *       {@code authenticationError} is Kraken rejecting the key itself
     *       (revoked, expired, IP-restricted, or a Spot key rather than a
     *       Futures key), not a bug in this class</li>
     * </ul>
     */
    @PostConstruct
    void logCredentialStatus() {
        if (apiKey == null || apiKey.startsWith(PLACEHOLDER)) {
            log.warn("KrakenFuturesSignature: API key is unset/placeholder — authenticated Kraken calls will fail. "
                    + "Put a real key in ~/.frosk/kraken-credentials.properties or set KRAKEN_FUTURES_API_KEY.");
            return;
        }
        if (apiSecret == null || apiSecret.startsWith(PLACEHOLDER)) {
            log.warn("KrakenFuturesSignature: API secret is unset/placeholder — authenticated Kraken calls will fail.");
            return;
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(apiSecret);
            log.info("KrakenFuturesSignature: credentials loaded — key {}…{} ({} chars), secret decodes to {} bytes",
                    apiKey.substring(0, Math.min(6, apiKey.length())),
                    apiKey.substring(Math.max(0, apiKey.length() - 4)),
                    apiKey.length(), decoded.length);
            if (decoded.length != 64) {
                log.warn("KrakenFuturesSignature: secret decodes to {} bytes; a Kraken Futures secret is normally 64. "
                        + "Check that the value was copied whole, and that it is a FUTURES key from futures.kraken.com "
                        + "(a Spot key from kraken.com does not authenticate against the derivatives API).", decoded.length);
            }
        } catch (IllegalArgumentException e) {
            log.error("KrakenFuturesSignature: API secret is not valid base64 — every Authent will be computed with a "
                    + "garbage HMAC key and Kraken will answer authenticationError.");
        }
    }

    /**
     * Generates the {@code Authent} header value.
     *
     * @param postData     request body as URL-encoded string (empty string for GET)
     * @param nonce        nonce string (typically System.currentTimeMillis() as string)
     * @param endpointPath path portion of the URL with the {@code /derivatives}
     *                     prefix stripped, e.g. {@code /api/v3/sendorder} — the
     *                     same normalisation Kraken's own reference client does.
     *                     {@link nu.itark.frosk.crypto.kraken.KrakenFuturesHttpClientImpl}
     *                     derives it from the configured base URL; callers here
     *                     must not pass the bare endpoint ({@code /sendorder}).
     */
    public String computeAuthent(String postData, String nonce, String endpointPath) {
        try {
            // Step 1: SHA-256(postData + nonce + endpointPath)
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            sha256.update((postData + nonce + endpointPath).getBytes(StandardCharsets.UTF_8));
            byte[] sha256Digest = sha256.digest();

            // Step 2: Base64-decode the api_secret
            byte[] secretBytes = Base64.getDecoder().decode(apiSecret);

            // Step 3: HMAC-SHA-512 with the decoded secret over the SHA-256 digest
            Mac mac = Mac.getInstance(HMAC_SHA_512);
            mac.init(new SecretKeySpec(secretBytes, HMAC_SHA_512));
            byte[] hmacResult = mac.doFinal(sha256Digest);

            // Step 4: Base64-encode the HMAC result
            String authent = Base64.getEncoder().encodeToString(hmacResult).trim();

            if (debugAuth) {
                // Enough to replay the computation against Kraken's own reference
                // client byte for byte. The secret is never part of this.
                log.info("KrakenFuturesSignature DEBUG: postData='{}' nonce='{}' endpointPath='{}' "
                                + "-> sha256={} authent={} APIKey={}…",
                        postData, nonce, endpointPath,
                        Base64.getEncoder().encodeToString(sha256Digest),
                        authent,
                        apiKey == null ? "null" : apiKey.substring(0, Math.min(6, apiKey.length())));
            }
            return authent;

        } catch (Exception e) {
            log.error("KrakenFuturesSignature: failed to compute Authent", e);
            throw new RuntimeException("Failed to compute Kraken Futures Authent", e);
        }
    }

    /** Generates a nonce: current time in milliseconds as string. */
    public String generateNonce() {
        return String.valueOf(System.currentTimeMillis());
    }
}
