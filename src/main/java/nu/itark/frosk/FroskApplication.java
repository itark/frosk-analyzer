package nu.itark.frosk;

import com.coinbase.exchange.api.exchange.CoinbaseExchange;
import com.coinbase.exchange.api.marketdata.MarketDataService;
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
    public MarketDataService marketDataService(CoinbaseExchange exchange) {
        return new MarketDataService(exchange);
    }

}
