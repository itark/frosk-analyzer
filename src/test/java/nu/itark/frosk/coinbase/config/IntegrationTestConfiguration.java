package nu.itark.frosk.coinbase.config;

import com.coinbase.exchange.api.exchange.CoinbaseExchange;
import com.coinbase.exchange.api.exchange.CoinbaseExchangeImpl;
import com.coinbase.exchange.api.products.ProductService;
import com.coinbase.exchange.security.Signature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import nu.itark.frosk.crypto.coinbase.ProductProxy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

//@Configuration
@Deprecated
public class IntegrationTestConfiguration {

/*
    @Bean
    public ProductProxy productProxy(){
        return new ProductProxy();
    }
*/

    /*
    @Bean
    public ProductService productService(){
        return new ProductService(exchange);
    }
    *
     */

    /*
    @Bean
    public TimeSeriesService timeSeriesService(){return new TimeSeriesService();}
*/

/*
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @Bean
    public CoinbaseExchange coinbaseExchange(@Value("${exchange.key}") String apiKey,
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
