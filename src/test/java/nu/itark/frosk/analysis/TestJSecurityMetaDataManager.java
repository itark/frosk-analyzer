package nu.itark.frosk.analysis;

import nu.itark.frosk.FroskApplication;
import nu.itark.frosk.coinbase.BaseIntegrationTest;
import nu.itark.frosk.service.BarSeriesService;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;

import java.math.BigDecimal;
import java.util.List;

@SpringBootTest(classes = {FroskApplication.class})
public class TestJSecurityMetaDataManager extends BaseIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(TestJSecurityMetaDataManager.class);
    @Autowired
    private BarSeriesService barSeriesService;

    @Autowired
    private SecurityMetaDataManager securityMetaDataManager;


    @Test
    public void testGetMetaData() {
        final List<SecurityDTO> metaDatas= securityMetaDataManager.getSecurityMetaData("YAHOO");
        log.info("1: metadata:{}", metaDatas.get(0));

    }


    @Test
    public void testBarPercent() {

        BigDecimal barPercent = securityMetaDataManager.getBarPercent("ERN-EUR", 3);
        System.out.println("barPercent="+barPercent);

    }

    @Test
    public void testLastestClose() {

        BigDecimal closePrice = securityMetaDataManager.getLatestClose("BTC-EUR");
        System.out.println("closePrice="+closePrice);

    }



}
