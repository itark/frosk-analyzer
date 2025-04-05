package nu.itark.frosk.coinbase.security;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.crypto.coinbase.advanced.Coinbase;
import nu.itark.frosk.crypto.coinbase.api.products.ProductService;
import nu.itark.frosk.crypto.coinbase.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
@Slf4j
public class TestJJwtUtil {

    @Autowired
    private JwtUtil  jwtUtil;

    @MockBean
    Coinbase coinbase;

    @MockBean
    ProductService productService;

    @Test
    public void testGetSignedJWT() throws Exception {
        final String signedJWT = jwtUtil.getSignedJWT("api.coinbase.com/api/v3/brokerage/products/BTC-EUR");
        log.info("signedJWT: {}", signedJWT);

    }


}
