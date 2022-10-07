package nu.itark.frosk.analysis;

import nu.itark.frosk.FroskApplication;
import nu.itark.frosk.coinbase.BaseIntegrationTest;
import nu.itark.frosk.service.BarSeriesService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;

import java.math.BigDecimal;

@SpringBootTest(classes = {FroskApplication.class})
public class TestJSecurityMetaDataManager extends BaseIntegrationTest {

    @Autowired
    private BarSeriesService barSeriesService;

    @Autowired
    private SecurityMetaDataManager securityMetaDataManager;


    @Test
    public void testBarPercent() {
        BarSeries timeSeries = barSeriesService.getDataSetFromCoinbase("BTC-EUR");
 /*       securityMetaDataManager.getSecurityMetaData().forEach(s-> {
            System.out.println("s="+s);
        });
*/

        BigDecimal barPercent = securityMetaDataManager.getBarPercent("BTC-EUR", 1);
        System.out.println("barPercent="+barPercent);

    }



}
