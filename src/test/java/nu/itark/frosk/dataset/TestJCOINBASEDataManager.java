package nu.itark.frosk.dataset;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.coinbase.BaseIntegrationTest;
import nu.itark.frosk.coinbase.config.IntegrationTestConfiguration;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit4.SpringRunner;

@ExtendWith(SpringExtension.class)
//@Import({IntegrationTestConfiguration.class})
@RunWith(SpringRunner.class)
@SpringBootTest
@Slf4j
public class TestJCOINBASEDataManager extends BaseIntegrationTest {

    @Autowired
    COINBASEDataManager coinbaseDataManager;

    @Test
    public void syncOne(){
        coinbaseDataManager.syncronize("BTC-EUR");
    }


}
