package nu.itark.frosk.analysis;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.FroskApplication;
import nu.itark.frosk.coinbase.BaseIntegrationTest;
import nu.itark.frosk.service.BarSeriesService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;

@SpringBootTest(classes = {FroskApplication.class})
@Slf4j
public class TestJStrategiesMap extends BaseIntegrationTest {

    @Autowired
    private BarSeriesService barSeriesService;

    @Autowired
    private StrategiesMap strategiesMap;


    @Test
    public void testExludeStrategies() {

        final List<String> strategyNames = strategiesMap.buildStrategiesMap();

        log.info("strategyNames:{}",strategyNames);
    }



}
