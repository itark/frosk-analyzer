package nu.itark.frosk.dataset;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.FroskStartupApplicationListener;
import nu.itark.frosk.crypto.coinbase.advanced.Coinbase;
import nu.itark.frosk.crypto.coinbase.api.products.ProductService;
import nu.itark.frosk.rapidapi.yhfinance.model.QuotesDTO;
import nu.itark.frosk.rapidapi.yhfinance.model.StockHistoryDTO;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.List;
import java.util.Map;

// https://rapidapi.com/sparior/api/yahoo-finance15/playground/apiendpoint_b7dd3888-f254-4081-a4cb-178d5638136e


@SpringBootTest
@Slf4j
public class TestJRapidApiManager {

    @MockBean
    Coinbase coinbase;

    @MockBean
    ProductService productService;

    @MockBean
    private FroskStartupApplicationListener myEventListener;

    @Autowired
    RapidApiManager rapidApiManager;

    @Test
    public void testGetQuotes() throws IOException, InterruptedException {
        final List<QuotesDTO.Quote> quotesDTO = rapidApiManager.getQuotesReal("NIBE-B.ST", "STOCKS");
        quotesDTO.stream()
                 .forEach(dto -> log.info("dto:{}",dto));
    }

    @Test
    public void testGetHistory() throws IOException, InterruptedException {
        final Map<String, StockHistoryDTO.StockData> history = rapidApiManager.getHistory("NIBE-B.ST", RapidApiManager.Interval.ONE_DAY);//Funkar
        history.forEach((key, value) -> log.info("Key: {}, StockData: {}", key, value));
    }

    @Test void testGetTicker() throws IOException, InterruptedException {
        rapidApiManager.getTickers(1, "STOCKS");
    }

    @Test void testSearch() throws IOException, InterruptedException {
        rapidApiManager.search(".ST");
    }

/*
    @Test void testGetStockSymbols() throws IOException, InterruptedException {
        final List<Map<String, String>> stockSymbols = rapidApiManager.getStockSymbols();
    }
*/


}
