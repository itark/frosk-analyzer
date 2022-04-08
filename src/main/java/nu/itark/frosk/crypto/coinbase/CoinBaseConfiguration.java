package nu.itark.frosk.crypto.coinbase;

import com.coinbase.exchange.api.exchange.CoinbaseExchange;
import com.coinbase.exchange.api.exchange.CoinbaseExchangeImpl;
import com.coinbase.exchange.security.Signature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Initeras i gdax, ta bport vid tillfälle
 *
 *
 */
//@SpringBootConfiguration
    @Deprecated
public class CoinBaseConfiguration {
/*
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        return objectMapper;
    }

    @Bean
    public Signature signature(@Value("${exchange.secret}") String secret) {
        return new Signature(secret);
    }

    @Bean
    public CoinbaseExchange coinbasePro(@Value("${exchange.key}") String apiKey,
                                        @Value("${exchange.passphrase}") String passphrase,
                                        @Value("${exchange.api.baseUrl}") String baseUrl,
                                        @Value("${exchange.secret}") String secretKey,
                                        ObjectMapper objectMapper) {
        return new CoinbaseExchangeImpl(apiKey,
                passphrase,
                baseUrl,
                new Signature(secretKey),
                objectMapper);
    }

*/

}
