package nu.itark.frosk.crypto.coinbase.security;

import org.apache.commons.codec.digest.HmacUtils;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.management.RuntimeErrorException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Created by robevansuk on 17/03/2017.
 */
public class Signature {

    private final String secretKey;

    public Signature(final String secretKey) {
        this.secretKey = secretKey;
    }
    //https://docs.cloud.coinbase.com/advanced-trade-api/docs/rest-api-auth#step-3-create-signature
    public String generate(String requestPath, String method, String body, String timestamp) {
        String prehash = timestamp + method.toUpperCase() + requestPath + body;
        return new HmacUtils("HmacSHA256", secretKey).hmacHex(prehash);
    }

}
