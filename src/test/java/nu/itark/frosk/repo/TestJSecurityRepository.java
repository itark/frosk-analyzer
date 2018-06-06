package nu.itark.frosk.repo;

import java.io.IOException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import nu.itark.frosk.model.SecurityPrice;
import yahoofinance.Stock;
import yahoofinance.YahooFinance;
import yahoofinance.histquotes.HistoricalQuote;
import yahoofinance.histquotes.Interval;

@RunWith(SpringRunner.class)
@SpringBootTest
public class TestJSecurityRepository {
	Logger logger = Logger.getLogger(TestJSecurityRepository.class.getName());

	
	@Autowired
	SecurityRepository securityRepo;
	

	
	//TODO
	
	
}
