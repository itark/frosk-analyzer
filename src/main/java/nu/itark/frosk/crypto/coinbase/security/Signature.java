package nu.itark.frosk.crypto.coinbase.security;

import org.apache.commons.codec.digest.HmacUtils;

/**
 * Created by robevansuk on 17/03/2017.
 */
@Deprecated
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
