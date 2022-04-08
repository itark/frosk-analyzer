package nu.itark.frosk;

import com.coinbase.exchange.api.accounts.AccountService;
import com.coinbase.exchange.api.exchange.CoinbaseExchange;
import com.coinbase.exchange.api.exchange.CoinbaseExchangeImpl;
import com.coinbase.exchange.api.marketdata.MarketDataService;
import com.coinbase.exchange.api.payments.PaymentService;
import com.coinbase.exchange.api.products.ProductService;
import com.coinbase.exchange.security.Signature;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "nu.itark.frosk.repo")
public class FroskApplication  {

	public static void main(String[] args){
		SpringApplication.run(FroskApplication.class, args);
	}

    @Bean
    public MarketDataService initmarketDataService(CoinbaseExchange exchange) {
        return new MarketDataService(exchange);
    }

    @Bean
    public ProductService initproductService(CoinbaseExchange exchange) {
        return new ProductService(exchange);
    }

    @Bean
    public AccountService initaccountService(CoinbaseExchange exchange) {
        return new AccountService(exchange);
    }

    @Bean
    public PaymentService initpaymentService(CoinbaseExchange exchange) {
        return new PaymentService(exchange);
    }

    /*
    @Primary
    public ObjectMapper objectMapper() {
        return Jackson2ObjectMapperBuilder.json()
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS) //ISODate
                .createXmlMapper(false)
                .featuresToEnable(SerializationFeature.INDENT_OUTPUT) //nicer output
                .serializationInclusion(JsonInclude.Include.NON_NULL) //exclude null values
                .build();
    }
*/

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

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate(Arrays.asList(new MappingJackson2HttpMessageConverter(objectMapper())));

        return restTemplate;
    }


}
