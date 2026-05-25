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

import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest
@Slf4j
public class TestJYahooFinanceDirectClient extends BaseIntegrationTest {

    @MockBean
    Coinbase coinbase;

    @MockBean
    ProductService productService;

    @Autowired
    YahooFinanceDirectClient yahooFinanceClient;

    @Test
    public void testGetQuotes() throws IOException, InterruptedException {
        String symbol = "NIBE-B.ST";
        final List<QuotesDTO.Quote> quotesDTO = yahooFinanceClient.getQuotesReal(symbol, "STOCKS");
        quotesDTO.forEach(dto -> log.info("dto:{}", ReflectionToStringBuilder.toString(dto, ToStringStyle.MULTI_LINE_STYLE)));
    }

    @Test
    public void testGetHistory() {
        final Map<String, StockHistoryDTO.StockData> history = yahooFinanceClient.getHistory("NIBE-B.ST", YahooFinanceDirectClient.Interval.ONE_DAY);
        assertFalse(history.isEmpty(), "History should not be empty");
        history.values().stream().findFirst().ifPresent(sd -> {
            assertNotNull(sd.getDate(), "date field must be populated");
            log.info("First bar: date={}, close={}", sd.getDate(), sd.getClose());
        });
        log.info("History bars: {}", history.size());
    }

    @Test
    void testGetModulesIncomeStatement() throws IOException, InterruptedException {
        String symbol = "NIBE-B.ST";
        Body result = yahooFinanceClient.getModuleIncomeStatement(symbol);
        assertNotNull(result, "IncomeStatement should not be null (crumb auth required)");
        log.info("result:{}", result);
        final double totalRevenueThisYear = result.getIncomeStatementHistory().getIncomeStatementHistory().get(0).getTotalRevenue().getRaw();
        final double totalRevenueLastYear = result.getIncomeStatementHistory().getIncomeStatementHistory().get(1).getTotalRevenue().getRaw();
        double yoyGrowth = ((totalRevenueThisYear - totalRevenueLastYear) / totalRevenueLastYear) * 100.0;
        log.info("yoyGrowth:{}", yoyGrowth);
    }

    @Test
    void testGetModuleStatistics() throws IOException, InterruptedException {
        String symbol = "ABB.ST";
        StatisticsBody moduleStatistics = yahooFinanceClient.getModuleStatistics(symbol);
        assertNotNull(moduleStatistics, "Statistics should not be null (crumb auth required)");
        log.info("enterpriseValue:{}", moduleStatistics.getEnterpriseValue().getRaw());
        log.info("trailingPE:{}", moduleStatistics.getTrailingPE());
    }

    @Test
    void testGetModuleRecommendationTrend() throws IOException, InterruptedException {
        String symbol = "ABB.ST";
        RecommendationBody moduleRecommendationTrend = yahooFinanceClient.getModuleRecommendationTrend(symbol);
        assertNotNull(moduleRecommendationTrend, "RecommendationTrend should not be null (crumb auth required)");
        log.info("moduleRecommendationTrend:{}", moduleRecommendationTrend);
    }

    @Test
    void testGetModuleAssetProfile() throws IOException, InterruptedException {
        String symbol = "ABB.ST";
        AssetProfileBody profile = yahooFinanceClient.getModuleAssetProfile(symbol);
        assertNotNull(profile, "AssetProfile should not be null (crumb auth required)");
        log.info("sector:{}, industry:{}", profile.getSector(), profile.getIndustry());
    }

    @Test
    void testGetModuleRaw() {
        String symbol = "ABB.ST";
        String result = yahooFinanceClient.getModuleRaw(symbol, "defaultKeyStatistics");
        assertNotNull(result, "Raw module response should not be null");
        log.info("raw result length: {}", result.length());
    }

    @Test
    void testGetTicker() throws IOException, InterruptedException {
        yahooFinanceClient.getTickers(1, "STOCKS");
    }

    @Test
    void testSearch() throws IOException, InterruptedException {
        yahooFinanceClient.search(".ST");
    }
}
