package nu.itark.frosk;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.crypto.coinbase.accounts.AccountService;
import nu.itark.frosk.crypto.coinbase.advanced.Coinbase;
import nu.itark.frosk.crypto.coinbase.advanced.CoinbaseImpl;
import nu.itark.frosk.crypto.coinbase.api.marketdata.MarketDataService;
import nu.itark.frosk.crypto.coinbase.api.payments.PaymentService;
import nu.itark.frosk.crypto.coinbase.api.products.ProductService;
import nu.itark.frosk.crypto.coinbase.security.Signature;
import org.h2.tools.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.sql.SQLException;

@SpringBootApplication
//@SpringBootApplication(scanBasePackages = {"se.fast2.doman.arbetsorder"})
@EnableJpaRepositories(basePackages = {"nu.itark.frosk.repo","nu.itark.frosk.bot.bot.repository"})
//@EnableJpaRepositories(basePackages = {"nu.itark.frosk.repo"})
@Slf4j
public class FroskApplication {

    private static ApplicationContext applicationContext;

	public static void main(String[] args){
        applicationContext = SpringApplication.run(FroskApplication.class, args);
       // displayAllBeans();
	}

    public static void displayAllBeans() {
        String[] allBeanNames = applicationContext.getBeanDefinitionNames();
        for(String beanName : allBeanNames) {
            log.info("Bean:"+beanName);
        }
    }

/*
    @Bean(initMethod = "start", destroyMethod = "stop")
    public Server h2Server() throws SQLException {
        return Server.createTcpServer("-tcp", "-tcpAllowOthers", "-tcpPort", "9092");
    }
*/


    @Bean
    public MarketDataService initmarketDataService(Coinbase exchange) {
        return new MarketDataService(exchange);
    }

    @Bean
    public ProductService initproductService(Coinbase exchange) {
        return new ProductService(exchange);
    }

    @Bean
    public AccountService initaccountService(Coinbase exchange) {
        return new AccountService(exchange);
    }

    @Bean
    public PaymentService initpaymentService(Coinbase exchange) {
        return new PaymentService(exchange);
    }


    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        return objectMapper;
    }

/*
    @Bean
    public Signature signature(@Value("${exchange.secret}") String secret) {
        return new Signature(secret);
    }

    @Bean
    public Coinbase coinbaseAdvanced(@Value("${exchange.key}") String apiKey,
                                     @Value("${exchange.api.baseUrl}") String baseUrl,
                                     @Value("${exchange.secret}") String secretKey,
                                     ObjectMapper objectMapper) {
        return new CoinbaseImpl(apiKey,
                baseUrl,
                new Signature(secretKey),
                objectMapper);
    }

*/

    @Bean
    @Deprecated
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        return restTemplate;
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedMethods("HEAD", "GET", "PUT", "POST", "DELETE", "PATCH");
            }
        };
    }

}
