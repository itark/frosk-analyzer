package nu.itark.frosk;

import com.coinbase.exchange.api.accounts.AccountService;
import com.coinbase.exchange.api.exchange.CoinbaseExchange;
import com.coinbase.exchange.api.marketdata.MarketDataService;
import com.coinbase.exchange.api.payments.PaymentService;
import com.coinbase.exchange.api.products.ProductService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class FroskApplication extends SpringBootServletInitializer {

	public static void main(String[] args){
		SpringApplication.run(FroskApplication.class, args);
	}

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(FroskApplication.class);
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

}
