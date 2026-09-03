package nu.itark.frosk.crypto.kraken.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
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

    @Value("${kraken.futures.api.key}")
    private String apiKey;

    @Value("${kraken.futures.api.secret}")
    private String apiSecret;

    public String getApiKey() {
        return apiKey;
    }

    /**
     * Generates the {@code Authent} header value.
     *
     * @param postData     request body as URL-encoded string (empty string for GET)
     * @param nonce        nonce string (typically System.currentTimeMillis() as string)
     * @param endpointPath path portion of the URL, e.g. {@code /derivatives/api/v3/sendorder}
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
            return Base64.getEncoder().encodeToString(hmacResult).trim();

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
