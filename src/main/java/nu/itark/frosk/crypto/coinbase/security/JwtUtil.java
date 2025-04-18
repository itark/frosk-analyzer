package nu.itark.frosk.crypto.coinbase.security;

import nu.itark.frosk.crypto.coinbase.advanced.Coinbase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.*;
import com.nimbusds.jwt.*;

import java.io.IOException;
import java.security.interfaces.ECPrivateKey;
import java.util.Map;
import java.util.HashMap;
import java.time.Instant;
import java.util.Base64;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.KeyFactory;
import java.io.StringReader;
import java.security.PrivateKey;
import java.security.Security;
import java.util.Objects;

@Component
public class JwtUtil {

    @Value("${exchange.key}")
    String keyName;
    @Value("${exchange.secret}")
    String keySecret;

    public String getSignedJWT(String url) throws Exception {
        if (Objects.isNull(keyName) || Objects.isNull(keySecret) ) {
            throw new RuntimeException("keyName or  keySecret not set.");
        }

        // Register BouncyCastle as a security providerx
        Security.addProvider(new BouncyCastleProvider());
        String privateKeyPEM = keySecret.replace("\\n", "\n");
        String name = keyName;

        // create header object
        Map<String, Object> header = new HashMap<>();
        header.put("alg", "ES256");
        header.put("typ", "JWT");
        header.put("kid", name);
        header.put("nonce", String.valueOf(Instant.now().getEpochSecond()));

        // create uri string for current request
        String requestMethod = "GET";
        String uri = requestMethod + " " + url;

        // create data object
        Map<String, Object> data = new HashMap<>();
        data.put("iss", "cdp");
        data.put("nbf", Instant.now().getEpochSecond());
        data.put("exp", Instant.now().getEpochSecond() + 120);
        data.put("sub", name);
        data.put("uri", uri);

        // Load private key
        PEMParser pemParser = new PEMParser(new StringReader(privateKeyPEM));
        JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");
        Object object = pemParser.readObject();
        PrivateKey privateKey;


        if (object instanceof PrivateKey) {
            privateKey = (PrivateKey) object;
        } else if (object instanceof org.bouncycastle.openssl.PEMKeyPair) {
            privateKey = converter.getPrivateKey(((org.bouncycastle.openssl.PEMKeyPair) object).getPrivateKeyInfo());
        } else {
            throw new Exception("Unexpected private key format");
        }
        pemParser.close();


        // Convert to ECPrivateKey
        KeyFactory keyFactory = KeyFactory.getInstance("EC");
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateKey.getEncoded());
        ECPrivateKey ecPrivateKey = (ECPrivateKey) keyFactory.generatePrivate(keySpec);

        // create JWT
        JWTClaimsSet.Builder claimsSetBuilder = new JWTClaimsSet.Builder();
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            claimsSetBuilder.claim(entry.getKey(), entry.getValue());
        }
        JWTClaimsSet claimsSet = claimsSetBuilder.build();

        JWSHeader jwsHeader = new JWSHeader.Builder(JWSAlgorithm.ES256).customParams(header).build();
        SignedJWT signedJWT = new SignedJWT(jwsHeader, claimsSet);

        JWSSigner signer = new ECDSASigner(ecPrivateKey);
        signedJWT.sign(signer);

        return signedJWT.serialize();

    }


}
