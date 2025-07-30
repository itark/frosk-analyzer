package nu.itark.frosk.dataset;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.coinbase.BaseIntegrationTest;
import nu.itark.frosk.crypto.coinbase.advanced.Coinbase;
import nu.itark.frosk.crypto.coinbase.api.products.ProductService;
import nu.itark.frosk.rapidapi.yhfinance.model.*;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.io.IOException;
import java.util.List;
import java.util.Map;

// https://rapidapi.com/sparior/api/yahoo-finance15/playground/apiendpoint_b7dd3888-f254-4081-a4cb-178d5638136e


@SpringBootTest
@Slf4j
public class TestJRapidApiManager extends BaseIntegrationTest  {

    @MockBean
    Coinbase coinbase;

    @MockBean
    ProductService productService;

    @Autowired
    RapidApiManager rapidApiManager;

    @Test
    public void testGetQuotes() throws IOException, InterruptedException {

        String symbol = "NIBE-B.ST"; //NIBE-B.ST , "FRAG.ST"

        final List<QuotesDTO.Quote> quotesDTO = rapidApiManager.getQuotesReal(symbol, "STOCKS");
        quotesDTO.stream()
                 .forEach(dto -> log.info("dto:{}",ReflectionToStringBuilder.toString(dto, ToStringStyle.MULTI_LINE_STYLE)));
    }

    @Test
    public void testGetHistory() throws IOException, InterruptedException {
        final Map<String, StockHistoryDTO.StockData> history = rapidApiManager.getHistory("NIBE-B.ST", RapidApiManager.Interval.ONE_DAY);//Funkar
        history.forEach((key, value) -> log.info("Key: {}, StockData: {}", key, value));
    }


    @Test void testGetModulesIncomeStatement() throws IOException, InterruptedException {
        String symbol = "NIBE-B.ST"; //NIBE-B.ST   ABB.ST
       Body result = rapidApiManager.getModuleIncomeStatement(symbol);
       log.info("result:{}",result);
        final double totalRevenueThisYear = result.getIncomeStatementHistory().getIncomeStatementHistory().get(0).getTotalRevenue().getRaw();
        final double totalRevenueLastYear = result.getIncomeStatementHistory().getIncomeStatementHistory().get(1).getTotalRevenue().getRaw();
        double yoyGrowth = ((totalRevenueThisYear - totalRevenueLastYear) / totalRevenueLastYear) * 100.0;
        log.info("yoyGrowth:{}",yoyGrowth);
    }

    @Test void testGetModuleIncomeStatementStatistics() throws IOException, InterruptedException {
        String symbol = "ABB.ST"; //NIBE-B.ST   ABB.ST
        String result = rapidApiManager.getModuleRaw(symbol, "statistics");
        log.info("result:{}",result);

        StatisticsBody result2 = rapidApiManager.getModuleStatistics(symbol);
        log.info("result2:{}",result2);

    }


    @Test void testGetTicker() throws IOException, InterruptedException {
        rapidApiManager.getTickers(1, "STOCKS");
    }

    @Test void testSearch() throws IOException, InterruptedException {
        rapidApiManager.search(".ST");
    }




}
